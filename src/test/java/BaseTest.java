import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

// Common setup for all Demoblaze tests: browser control + reusable data + cleanup.
public abstract class BaseTest {

    // Reusable data: base URL and shared timeout (used by all test methods).
    protected static final String BASE_URL = "https://www.demoblaze.com/";
    protected static final int TIMEOUT_SECONDS = 15;

    protected WebDriver driver;
    protected WebDriverWait wait;

    // Runs before every @Test: open Chrome, maximize, navigate to the store.
    @BeforeMethod
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        // Keep the suite headed by default (beginner-friendly); allows CI headless via env var.
        if ("true".equalsIgnoreCase(System.getenv("HEADLESS"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--start-maximized");

        driver = new ChromeDriver(options);
        // Maximize explicitly as required (start-maximized may be ignored in some environments).
        try {
            driver.manage().window().maximize();
        } catch (Exception ignored) {
            // Non-fatal: continue even if maximize is not supported (e.g. headless).
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0)); // rely on explicit waits only
        wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SECONDS));
        driver.get(BASE_URL);
    }

    // Guaranteed browser cleanup even if a test fails.
    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
                // Browser already closed - nothing to do.
            } finally {
                driver = null;
            }
        }
    }

    // Test evidence helper: saves a PNG screenshot under target/screenshots.
    protected void takeScreenshot(String testName) {
        if (driver == null) {
            return;
        }
        try {
            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path dir = Paths.get("target", "screenshots");
            Files.createDirectories(dir);
            String safeName = testName.replaceAll("[^a-zA-Z0-9-_]", "_");
            Path target = dir.resolve(safeName + "-" + System.currentTimeMillis() + ".png");
            Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Screenshot saved: " + target.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("WARNING: could not save screenshot for " + testName + ": " + e.getMessage());
        } catch (Exception e) {
            System.out.println("WARNING: screenshot failed for " + testName + ": " + e.getMessage());
        }
    }
}
