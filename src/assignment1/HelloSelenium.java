package assignment1;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class HelloSelenium {
    public static void main(String[] args) {
//        System.setProperty("webdriver.chrome.driver", "/绝对路径/chromedriver"); // 若已在 PATH 可删
        WebDriver driver = new ChromeDriver();
        driver.get("https://google.com");
        System.out.println("Title = " + driver.getTitle());
        driver.quit();
    }
}
