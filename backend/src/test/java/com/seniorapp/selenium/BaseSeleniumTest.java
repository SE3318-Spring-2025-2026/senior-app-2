package com.seniorapp.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test") // Uses H2 db and populates UserDataSeeder
public abstract class BaseSeleniumTest {

    protected WebDriver driver;
    // Assume Vite frontend is running locally on 5173
    protected final String FRONTEND_URL = "http://localhost:5173";

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUpTest() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        // Remove or comment out --headless for debugging visually, keep headless for CI
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDownTest() {
        if (driver != null) {
            driver.quit();
        }
    }

    // Helper method to login using roles from UserDataSeeder
    protected void login(String email, String password) {
        driver.get(FRONTEND_URL + "/login"); // Adjust route if frontend uses a different login route
        // Assuming there are input fields with these selectors in the frontend
        // If not, these need to be updated to match the actual Vite frontend UI
        try {
            driver.findElement(org.openqa.selenium.By.id("email")).sendKeys(email);
            driver.findElement(org.openqa.selenium.By.id("password")).sendKeys(password);
            driver.findElement(org.openqa.selenium.By.xpath("//button[@type='submit']")).click();
            // Wait for login to complete
            Thread.sleep(1000);
        } catch (Exception e) {
            System.err.println("Login UI elements not found. Please verify the Vite frontend has #email, #password, and a submit button.");
        }
    }
}
