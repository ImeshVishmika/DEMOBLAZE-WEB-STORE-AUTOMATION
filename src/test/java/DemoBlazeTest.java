import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Demoblaze smoke + regression suite (beginner level).
 *
 * Assumptions / limitations (documented as required):
 * - Demo data may change: product prices are read at runtime instead of hard-coding,
 *   and cart-total is verified dynamically against the visible Samsung row price.
 * - Alert after "Add to cart" is expected to contain "Product added" (site has shown
 *   both "Product added" and "Product added." historically), so a contains-check is used.
 * - Missing Name/Card in the order form triggers alert "Please fill out Name and Creditcard."
 * - All purchase data below is fictitious test data; no real payment/personal details are used.
 * - Each test starts with a fresh browser (see BaseTest), so tests are independent and
 *   every cart test adds its own products first.
 */
public class DemoBlazeTest extends BaseTest {

    // Reusable data: repeated product names and fictitious checkout details.
    private static final String SAMSUNG_S6 = "Samsung galaxy s6";
    private static final String NOKIA_LUMIA = "Nokia lumia 1520";

    private static final String TEST_NAME = "Smoke Test User";
    private static final String TEST_COUNTRY = "Testland";
    private static final String TEST_CITY = "Test City";
    private static final String TEST_CARD = "4111111111111111"; // fictitious, for demo site only
    private static final String TEST_MONTH = "12";
    private static final String TEST_YEAR = "2030";

    // TC01 - Home Page Smoke Test: non-empty title + displayed PRODUCT STORE heading.
    @Test(description = "TC01 - Home Page Smoke Test")
    public void tc01HomePageSmokeTest() {
        // Non-XPath locator (id) + XPath locator for the same heading.
        WebElement storeHeadingById = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("nava")));
        WebElement storeHeadingByXPath = driver.findElement(
                By.xpath("//a[contains(text(),'PRODUCT STORE')]"));

        String title = driver.getTitle();
        System.out.println("TC01 - Page title: '" + title + "'");
        System.out.println("TC01 - Heading text: '" + storeHeadingById.getText() + "'");

        Assert.assertNotNull(title, "Page title should not be null");
        Assert.assertFalse(title.trim().isEmpty(), "Page title should be non-empty");
        Assert.assertTrue(storeHeadingById.isDisplayed(), "PRODUCT STORE heading (id=nava) should be displayed");
        Assert.assertTrue(storeHeadingByXPath.isDisplayed(), "PRODUCT STORE heading (XPath) should be displayed");
        Assert.assertTrue(driver.getCurrentUrl().contains("demoblaze.com"), "URL should be the demo store");

        takeScreenshot("TC01-HomePage");
    }

    // TC02 - Product Selection: open Phones, select S6, verify heading, print price.
    @Test(description = "TC02 - Product Selection")
    public void tc02ProductSelection() {
        // Non-XPath locator (linkText) for the Phones category.
        WebElement phonesCategory = wait.until(
                ExpectedConditions.elementToBeClickable(By.linkText("Phones")));
        phonesCategory.click();

        // XPath locator for the product link.
        WebElement samsungLink = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//a[normalize-space()='" + SAMSUNG_S6 + "']")));
        samsungLink.click();

        // Product detail heading + price (both XPath).
        WebElement productHeading = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//h2[@class='name']")));
        WebElement productPrice = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h3[contains(@class,'price-container')]")));

        System.out.println("TC02 - Product heading: '" + productHeading.getText() + "'");
        System.out.println("TC02 - Product price: '" + productPrice.getText() + "'");

        Assert.assertTrue(productHeading.isDisplayed(), "Product heading should be displayed");
        Assert.assertEquals(productHeading.getText().trim(), SAMSUNG_S6, "Product heading should match");
        Assert.assertTrue(productPrice.isDisplayed(), "Product price should be displayed");
        Assert.assertTrue(productPrice.getText().contains("$"), "Product price should contain '$'");

        takeScreenshot("TC02-ProductSelection");
    }

    // TC03 - Add to Cart: add S6, wait for JS alert, print text, accept.
    @Test(description = "TC03 - Add to Cart")
    public void tc03AddToCart() {
        addProductToCartViaUi(SAMSUNG_S6);

        // If we reach here the alert was seen and accepted inside the helper.
        // Re-open cart to prove the product actually landed in the cart.
        openCart();
        List<WebElement> rows = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.xpath("//tbody[@id='tbodyid']/tr")));
        boolean samsungInCart = rows.stream().anyMatch(row -> row.getText().contains(SAMSUNG_S6));
        System.out.println("TC03 - Cart rows after add: " + rows.size());
        Assert.assertTrue(samsungInCart, SAMSUNG_S6 + " should be present in the cart");

        takeScreenshot("TC03-AddToCart");
    }

    // TC04 - Cart Management: add S6 + Nokia, count/print rows, remove Nokia,
    // verify one row remains with Samsung, print + verify total.
    @Test(description = "TC04 - Cart Management")
    public void tc04CartManagement() {
        addProductToCartViaUi(SAMSUNG_S6);
        addProductToCartViaUi(NOKIA_LUMIA);
        openCart();

        // Collections: findElements() + List<WebElement>.
        List<WebElement> cartRows = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.xpath("//tbody[@id='tbodyid']/tr")));
        System.out.println("TC04 - Cart row count: " + cartRows.size());

        // Loop over cart rows: print product names and prices.
        for (WebElement row : cartRows) {
            String rowTitle = row.findElement(By.xpath("./td[2]")).getText();
            String rowPrice = row.findElement(By.xpath("./td[3]")).getText();
            System.out.println("TC04 - Cart item: " + rowTitle + " | Price: " + rowPrice);
        }
        Assert.assertEquals(cartRows.size(), 2, "Cart should contain 2 products before removal");

        // Remove Nokia lumia 1520 (XPath scoped to its row).
        WebElement deleteNokia = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//tr[td[text()='" + NOKIA_LUMIA + "']]//a[text()='Delete']")));
        deleteNokia.click();

        // Synchronize on the row count dropping to 1 (no Thread.sleep).
        wait.until(ExpectedConditions.numberOfElementsToBe(
                By.xpath("//tbody[@id='tbodyid']/tr"), 1));

        List<WebElement> remainingRows = driver.findElements(By.xpath("//tbody[@id='tbodyid']/tr"));
        Assert.assertEquals(remainingRows.size(), 1, "Exactly one row should remain after removal");

        String remainingText = remainingRows.get(0).getText();
        System.out.println("TC04 - Remaining row: " + remainingText);
        Assert.assertTrue(remainingText.contains(SAMSUNG_S6),
                SAMSUNG_S6 + " should remain after removing " + NOKIA_LUMIA);
        Assert.assertFalse(remainingText.contains(NOKIA_LUMIA),
                NOKIA_LUMIA + " should no longer be in the cart");

        // Cart total verification: compare total against the visible Samsung row price.
        String samsungPriceText = remainingRows.get(0).findElement(By.xpath("./td[3]")).getText().trim();
        int expectedTotal = parsePrice(samsungPriceText);
        WebElement totalElement = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("totalp"))); // non-XPath
        System.out.println("TC04 - Cart total: '" + totalElement.getText() + "'");
        int actualTotal = parsePrice(totalElement.getText());
        Assert.assertEquals(actualTotal, expectedTotal,
                "Cart total should equal the remaining Samsung price (" + samsungPriceText + ")");

        takeScreenshot("TC04-CartManagement");
    }

    // TC05 - Checkout Validation: invalid submit (missing Name/Card) then valid
    // fictitious submit, verifying the purchase success message.
    @Test(description = "TC05 - Checkout Validation")
    public void tc05CheckoutValidation() {
        addProductToCartViaUi(SAMSUNG_S6);
        openCart();

        // Open the Place Order modal.
        WebElement placeOrderButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[text()='Place Order']")));
        placeOrderButton.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("orderModal")));

        // INVALID: leave Name and Card empty, attempt purchase.
        WebElement purchaseButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[text()='Purchase']")));
        purchaseButton.click();

        Alert invalidAlert = wait.until(ExpectedConditions.alertIsPresent());
        String invalidAlertText = invalidAlert.getText();
        System.out.println("TC05 - Invalid-form alert text: '" + invalidAlertText + "'");
        Assert.assertTrue(invalidAlertText.contains("Please fill out Name and Creditcard"),
                "Missing Name/Card should trigger the fill-out alert, got: " + invalidAlertText);
        invalidAlert.accept();

        // After dismissing the alert the modal must still be open and no success shown.
        Assert.assertTrue(driver.findElement(By.id("orderModal")).isDisplayed(),
                "Order modal should remain open after invalid submit");
        Assert.assertTrue(driver.findElements(
                        By.xpath("//h2[contains(text(),'Thank you for your purchase')]")).isEmpty(),
                "Success message must NOT appear for the invalid submit");

        // VALID: fill the form with fictitious test data (sendKeys) and submit.
        driver.findElement(By.id("name")).sendKeys(TEST_NAME); // non-XPath locators
        driver.findElement(By.id("country")).sendKeys(TEST_COUNTRY);
        driver.findElement(By.id("city")).sendKeys(TEST_CITY);
        driver.findElement(By.id("card")).sendKeys(TEST_CARD);
        driver.findElement(By.id("month")).sendKeys(TEST_MONTH);
        driver.findElement(By.id("year")).sendKeys(TEST_YEAR);
        System.out.println("TC05 - Submitted fictitious order as '" + TEST_NAME + "'");

        driver.findElement(By.xpath("//button[text()='Purchase']")).click();

        // Success message verification.
        WebElement successHeading = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h2[contains(text(),'Thank you for your purchase')]")));
        System.out.println("TC05 - Success message: '" + successHeading.getText() + "'");
        Assert.assertTrue(successHeading.isDisplayed(), "Purchase success message should be displayed");
        Assert.assertTrue(successHeading.getText().contains("Thank you for your purchase"),
                "Success text should confirm the purchase");

        takeScreenshot("TC05-CheckoutSuccess");

        // Dismiss the confirmation so the browser is left clean.
        WebElement okButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[text()='OK']")));
        okButton.click();
    }

    // ---- Reusable helpers (keeps tests short and readable) ----

    // Adds a product by navigating the store UI and handling the JS alert.
    private void addProductToCartViaUi(String productName) {
        driver.get(BASE_URL);

        WebElement phonesCategory = wait.until(
                ExpectedConditions.elementToBeClickable(By.linkText("Phones")));
        phonesCategory.click();

        WebElement productLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[normalize-space()='" + productName + "']")));
        productLink.click();

        WebElement addToCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[text()='Add to cart']")));
        addToCartButton.click();

        // Explicit wait for the JavaScript alert: read its text, print it, accept it.
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        String alertText = alert.getText();
        System.out.println("Add to cart alert for '" + productName + "': '" + alertText + "'");
        Assert.assertTrue(alertText.contains("Product added"),
                "Add-to-cart alert should confirm the product was added, got: " + alertText);
        alert.accept();
    }

    // Opens the cart page and waits until it is loaded (rows or total visible).
    private void openCart() {
        WebElement cartLink = wait.until(
                ExpectedConditions.elementToBeClickable(By.linkText("Cart"))); // non-XPath
        cartLink.click();
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.xpath("//tbody[@id='tbodyid']/tr")),
                ExpectedConditions.visibilityOfElementLocated(By.id("totalp"))));
    }

    // Parses values like "$360 *includes tax" or "360" into an int (360).
    private int parsePrice(String rawText) {
        String digits = rawText.replaceAll("[^0-9]", " ").trim().split("\\s+")[0];
        return Integer.parseInt(digits);
    }
}
