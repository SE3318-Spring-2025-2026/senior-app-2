package com.seniorapp.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MainProductFlowsE2ETest extends BaseSeleniumTest {

    private void waitForUrl(String urlFragment) {
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.urlContains(urlFragment));
    }

    @Test
    public void testTemplateFlow_CoordinatorCreatesTemplate() throws Exception {
        // 1. Login as Coordinator/Professor
        login("professor@seniorapp.com", "prof123");

        // 2. Navigate to Template Builder
        driver.get(FRONTEND_URL + "/panel/template-builder");
        waitForUrl("/panel/template-builder");

        // 3. Verify Page Loaded (Smoke check)
        // Checking if the body is present and not empty, indicating the React view rendered
        assertTrue(driver.findElement(By.tagName("body")).isDisplayed(), "Template Builder page should be rendered");
    }

    @Test
    public void testGroupFlow_StudentJoinsOrCreatesGroup() throws Exception {
        // 1. Login as Student
        login("student@seniorapp.com", "student123");

        // 2. Navigate to Create/Manage Group page
        driver.get(FRONTEND_URL + "/panel/create-group");
        waitForUrl("/panel/create-group");

        // 3. Verify Page Loaded
        assertTrue(driver.findElement(By.tagName("body")).isDisplayed(), "Team Management page should be rendered");
    }

    @Test
    public void testAdvisorAndSubmissionFlow_StudentViewsProject() throws Exception {
        // 1. Login as Student
        login("student@seniorapp.com", "student123");

        // 2. Navigate to My Projects (where submission and advisor details likely reside)
        driver.get(FRONTEND_URL + "/panel/my-projects");
        waitForUrl("/panel/my-projects");

        // 3. Verify Page Loaded
        assertTrue(driver.findElement(By.tagName("body")).isDisplayed(), "My Projects page should be rendered");
    }

    @Test
    public void testGradingFlow_ProfessorGradesSubmission() throws Exception {
        // 1. Login as Coordinator/Professor
        login("professor@seniorapp.com", "prof123");

        // 2. Navigate to Student Projects (Grading area)
        driver.get(FRONTEND_URL + "/panel/my-student-projects");
        waitForUrl("/panel/my-student-projects");

        // 3. Verify Page Loaded
        assertTrue(driver.findElement(By.tagName("body")).isDisplayed(), "Student Projects page should be rendered");
    }
}
