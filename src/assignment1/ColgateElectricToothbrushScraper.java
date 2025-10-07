package assignment1;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Colgate Electric Toothbrush Scraper (en-ca)
 *
 * What this script does:
 * 1) Opens the toothbrushes listing page
 * 2) Discovers product cards potentially belonging to ELECTRIC toothbrushes
 * 3) Visits each product detail page
 * 4) Extracts fields required by the "Electric Toothbrush Recommendation System"
 * 5) Writes to CSV (UTF-8)
 *
 * NOTES:
 * - We use robust, keyword-based extraction to handle content variance across product templates.
 * - We demonstrate explicit waits and dynamic content handling (expandable sections).
 * - If Colgate adds "Load more"/infinite scroll, the scrollToBottom() function covers it.
 * - Be polite to the site: includes light throttling between requests.
 */
public class ColgateElectricToothbrushScraper {

    // ----------------- CONFIG -----------------
    private static final String LISTING_URL = "https://www.colgate.com/en-ca/products/toothbrush";
    private static final String OUTPUT_CSV = "colgate_electric_toothbrushes.csv";

    // Keywords to identify electric toothbrushes on listing cards
    private static final String[] ELECTRIC_HINTS = new String[] {
            "electric", "rechargeable", "battery", "sonic", "hum", "smart"
    };

    // Modes dictionary for normalization
    private static final String[] KNOWN_MODES = new String[] {
            "clean", "sensitive", "gum care", "gum", "whitening", "white", "deep clean", "polish", "massage", "refresh"
    };

    // ----------------- SELENIUM SETUP -----------------
    public static WebDriver buildDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--window-size=1400,1000");
        // Headless (recommended in CI); comment this out if you want to see the browser.
        options.addArguments("--headless=new");
        return new ChromeDriver(options);
    }

    // ----------------- UTILITIES -----------------
    private static WebElement waitVisible(WebDriver driver, By locator, long timeoutSec) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private static List<WebElement> findAll(WebDriver driver, By locator) {
        try {
            return driver.findElements(locator);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static WebElement findOne(WebDriver driver, By locator) {
        try {
            return driver.findElement(locator);
        } catch (Exception e) {
            return null;
        }
    }

    private static void safeClick(WebDriver driver, By locator, long timeoutSec) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                    .until(ExpectedConditions.elementToBeClickable(locator)).click();
        } catch (Exception ignored) {}
    }

    private static void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    private static String getTextOrEmpty(WebElement el) {
        if (el == null) return "";
        try { return el.getText().trim(); } catch (Exception e) { return ""; }
    }

    private static String attributeOrEmpty(WebElement el, String name) {
        if (el == null) return "";
        try {
            String v = el.getAttribute(name);
            return v == null ? "" : v.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String csvEscape(String s) {
        if (s == null) s = "";
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        s = s.replace("\"", "\"\"");
        return needQuote ? ("\"" + s + "\"") : s;
    }

    private static void writeCsvHeader(BufferedWriter bw) throws IOException {
        String[] cols = {
                "brand","product_name","product_url","image_url","price",
                "type","brushing_modes","battery_type","battery_life","intensity_levels",
                "timer","brush_head_compatibility","smart_features","pressure_sensor",
                "uv_sanitizer","travel_case","water_resistance","description"
        };
        bw.write(String.join(",", cols));
        bw.write("\n");
    }

    private static void writeCsvRow(BufferedWriter bw, Map<String, String> row) throws IOException {
        String[] cols = {
                "brand","product_name","product_url","image_url","price",
                "type","brushing_modes","battery_type","battery_life","intensity_levels",
                "timer","brush_head_compatibility","smart_features","pressure_sensor",
                "uv_sanitizer","travel_case","water_resistance","description"
        };
        List<String> escaped = new ArrayList<>();
        for (String c : cols) {
            escaped.add(csvEscape(row.getOrDefault(c, "")));
        }
        bw.write(String.join(",", escaped));
        bw.write("\n");
    }

    // Scroll to bottom multiple times to trigger lazy loading if present
    private static void scrollToBottom(WebDriver driver) {
        long lastHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
        for (int i = 0; i < 10; i++) {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            sleep(800);
            long newHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
            if (newHeight == lastHeight) break;
            lastHeight = newHeight;
        }
    }

    // ----------------- LISTING DISCOVERY -----------------
    /**
     * Collect candidate product links from listing page by inspecting product cards.
     * We do a permissive match by keywords (electric/sonic/hum/rechargeable/battery/smart).
     */
    private static Set<String> collectElectricProductLinks(WebDriver driver) {
        driver.get(LISTING_URL);
        sleep(1200);

        // Try to accept cookies if banner exists
        try {
            // Common cookie accept buttons
            List<By> cookieButtons = Arrays.asList(
                    By.xpath("//button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'accept')]"),
                    By.xpath("//button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'agree')]"),
                    By.xpath("//button[contains(., 'Accept All')]")
            );
            for (By b : cookieButtons) { safeClick(driver, b, 2); }
        } catch (Exception ignored) {}

        // Scroll a few times to load all cards
        scrollToBottom(driver);

        // Heuristic: product cards often have anchors with product name + image
        List<WebElement> anchors = findAll(driver, By.xpath("//a[@href and not(contains(@href,'#'))]"));
        Set<String> links = new LinkedHashSet<>();

        for (WebElement a : anchors) {
            String href = attributeOrEmpty(a, "href");
            String text = getTextOrEmpty(a).toLowerCase(Locale.ROOT);

            // filter to product domain path
            if (!href.contains("/en-ca/")) continue;
            if (!href.contains("/products/")) continue;

            // card text OR aria labels may live on descendant elements -> also check @aria-label, @title, img alt
            String aria = attributeOrEmpty(a, "aria-label").toLowerCase(Locale.ROOT);
            String title = attributeOrEmpty(a, "title").toLowerCase(Locale.ROOT);

            String joined = text + " " + aria + " " + title;

            boolean electricLike = false;
            for (String k : ELECTRIC_HINTS) {
                if (joined.contains(k)) { electricLike = true; break; }
            }

            if (electricLike) {
                links.add(href.split("\\?")[0]); // normalize
            }
        }

        // Fallback: also mine image alts within cards
        if (links.isEmpty()) {
            List<WebElement> imgs = findAll(driver, By.xpath("//a[@href]//img[@alt]"));
            for (WebElement img : imgs) {
                WebElement parentA = img.findElement(By.xpath("./ancestor::a[1]"));
                String href = attributeOrEmpty(parentA, "href");
                String alt = attributeOrEmpty(img, "alt").toLowerCase(Locale.ROOT);
                if (href.contains("/en-ca/") && href.contains("/products/")) {
                    for (String k : ELECTRIC_HINTS) {
                        if (alt.contains(k)) {
                            links.add(href.split("\\?")[0]);
                            break;
                        }
                    }
                }
            }
        }

        System.out.println("[INFO] Candidate electric product links discovered: " + links.size());
        return links;
    }

    // ----------------- DETAIL EXTRACTION -----------------
    private static Map<String, String> extractProduct(WebDriver driver, String url) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("brand", "Colgate");
        row.put("product_url", url);

        driver.get(url);
        sleep(1000);

        // Main title
        String name = tryManyTexts(driver, Arrays.asList(
                By.xpath("//h1"),
                By.xpath("//h1//*[self::span or self::strong or self::em]"),
                By.xpath("//*[contains(@class,'product')]//h1")
        ));
        row.put("product_name", name);

        // Primary image
        String image = tryManyAttrs(driver, Arrays.asList(
                By.xpath("//img[contains(@src,'/en-ca/')]"),
                By.xpath("//picture//img")
        ), "src");
        row.put("image_url", image);

        // Price (if present on official site; often not — may be retailer-only)
        String price = firstRegexOnPage(driver, "(\\$\\s?\\d+[\\.,]?\\d*)");
        row.put("price", price);

        // FULL visible text for heuristic parsing
        String pageText = getFullVisibleText(driver).toLowerCase(Locale.ROOT);

        // Type
        String type = inferType(pageText); // sonic/oscillating/rechargeable/battery
        row.put("type", type);

        // Modes
        String modes = inferModes(pageText);
        row.put("brushing_modes", modes);

        // Battery type & life
        row.put("battery_type", inferBatteryType(pageText));
        row.put("battery_life", inferBatteryLife(pageText));

        // Intensity levels
        row.put("intensity_levels", inferIntensityLevels(pageText));

        // Timer
        row.put("timer", inferTimer(pageText));

        // Brush head compatibility
        row.put("brush_head_compatibility", inferBrushHeadCompatibility(pageText));

        // Smart features & others
        row.put("smart_features", inferSmart(pageText));
        row.put("pressure_sensor", inferBoolean(pageText, new String[]{"pressure sensor","pressure-sensor","too hard","pressing too hard"}) ? "true" : "false");
        row.put("uv_sanitizer", inferBoolean(pageText, new String[]{"uv sanitizer","uv light","uv clean"}) ? "true" : "false");
        row.put("travel_case", inferBoolean(pageText, new String[]{"travel case","travel-capable","travel-friendly"}) ? "true" : "false");
        row.put("water_resistance", inferWaterResistance(pageText));

        // Description (take a moderate size block)
        row.put("description", summarizeDescription(driver));

        // Expand common collapsible sections (if exist) to capture more text; best-effort.
        expandIfPresent(driver, Arrays.asList(
                By.xpath("//button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'ingredient')]"),
                By.xpath("//button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'direction')]"),
                By.xpath("//button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'warning')]"),
                By.xpath("//button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'feature')]"),
                By.xpath("//button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'benefit')]")
        ));

        return row;
    }

    private static void expandIfPresent(WebDriver driver, List<By> buttons) {
        for (By b : buttons) {
            try {
                WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(1))
                        .until(ExpectedConditions.elementToBeClickable(b));
                btn.click();
                sleep(300);
            } catch (Exception ignored) {}
        }
    }

    private static String getFullVisibleText(WebDriver driver) {
        // Grab visible text chunks; avoid scripts/styles
        List<WebElement> sections = findAll(driver, By.xpath("//*[not(self::script) and not(self::style)]"));
        StringBuilder sb = new StringBuilder();
        int added = 0;
        for (WebElement el : sections) {
            try {
                String t = el.getText();
                if (t != null && t.trim().length() > 0) {
                    sb.append(t).append("\n");
                    if (++added > 400) break; // keep it bounded
                }
            } catch (StaleElementReferenceException ignored) {}
        }
        return sb.toString();
    }

    private static String tryManyTexts(WebDriver driver, List<By> locators) {
        for (By by : locators) {
            WebElement e = findOne(driver, by);
            String t = getTextOrEmpty(e);
            if (!t.isEmpty()) return t;
        }
        return "";
    }

    private static String tryManyAttrs(WebDriver driver, List<By> locators, String attr) {
        for (By by : locators) {
            WebElement e = findOne(driver, by);
            String v = attributeOrEmpty(e, attr);
            if (!v.isEmpty()) return v;
        }
        return "";
    }

    // ----------------- HEURISTICS -----------------
    private static String inferType(String text) {
        if (text.contains("sonic")) return "sonic";
        if (text.contains("oscillating")) return "oscillating";
        if (text.contains("rotating")) return "rotating";
        if (text.contains("ultrasonic")) return "ultrasonic";
        if (text.contains("rechargeable")) return "rechargeable";
        if (text.contains("battery")) return "battery-powered";
        if (text.contains("hum")) return "sonic"; // Colgate hum is sonic line
        return "";
    }

    private static String inferModes(String text) {
        Set<String> modes = new LinkedHashSet<>();
        for (String m : KNOWN_MODES) {
            String k = m.toLowerCase(Locale.ROOT);
            if (text.contains(k)) {
                // normalize "gum" -> "gum care"
                if ("gum".equals(k) && !modes.contains("gum care")) {
                    modes.add("gum care");
                } else {
                    modes.add(k);
                }
            }
        }
        return String.join(" | ", modes);
    }

    private static String inferBatteryType(String text) {
        if (text.contains("usb") || text.contains("rechargeable")) return "rechargeable";
        if (text.contains("aa battery") || text.contains("aaa battery") || text.contains("replaceable battery") || text.contains("replaceable batteries"))
            return "replaceable";
        if (text.contains("battery-powered")) return "battery-powered";
        return "";
    }

    private static String inferBatteryLife(String text) {
        // capture “10 days”, “14 days”, “2 weeks”, “60 minutes”, “2 hours”
        Pattern p = Pattern.compile("(\\d+\\s*(days?|weeks?|hours?|hrs?|minutes?|mins?))");
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        // phrases like “up to X days”
        p = Pattern.compile("up to\\s+(\\d+\\s*(days?|weeks?|hours?|hrs?|minutes?|mins?))");
        m = p.matcher(text);
        if (m.find()) return m.group(1);
        return "";
    }

    private static String inferIntensityLevels(String text) {
        // look for “X intensity settings / X speeds”
        Pattern p = Pattern.compile("(\\d+)\\s*(intensity settings|intensity|speeds?)");
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        // “adjustable intensity / multiple intensity”
        if (text.contains("adjustable intensity") || text.contains("multiple intensity") || text.contains("intensity control"))
            return "adjustable";
        return "";
    }

    private static String inferTimer(String text) {
        if (text.contains("quadrant") || text.contains("30-second") || text.contains("30 second")) return "2-min with quadrant";
        if (text.contains("2-minute") || text.contains("two-minute") || text.contains("2 minute")) return "2-min";
        if (text.contains("timer")) return "yes";
        return "";
    }

    private static String inferBrushHeadCompatibility(String text) {
        // look for “compatible with” phrases
        Pattern p = Pattern.compile("compatible with[^\\.\\n]+");
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(0);
        if (text.contains("compatible") || text.contains("interchangeable")) return "compatible (see page)";
        return "";
    }

    private static String inferSmart(String text) {
        List<String> feats = new ArrayList<>();
        if (text.contains("bluetooth")) feats.add("bluetooth");
        if (text.contains("app")) feats.add("app integration");
        if (text.contains("real-time") || text.contains("real time")) feats.add("real-time feedback");
        if (text.contains("coaching") || text.contains("track")) feats.add("coaching/tracking");
        return String.join(" | ", feats);
    }

    private static boolean inferBoolean(String text, String[] hints) {
        for (String h : hints) {
            if (text.contains(h)) return true;
        }
        return false;
    }

    private static String inferWaterResistance(String text) {
        // capture IPX rating
        Pattern p = Pattern.compile("(ipx\\s*\\d)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1).toUpperCase(Locale.ROOT);
        if (text.contains("waterproof") || text.contains("water resistant") || text.contains("water-resistan"))
            return "water-resistant";
        return "";
    }

    private static String summarizeDescription(WebDriver driver) {
        // Heuristic: take the first non-empty paragraph under “About / Description / Overview / Hero”
        List<By> candidates = Arrays.asList(
                By.xpath("//section//*[self::p or self::div][string-length(normalize-space())>40]"),
                By.xpath("//div[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'about')]/p"),
                By.xpath("//p[string-length(normalize-space())>60]")
        );
        for (By by : candidates) {
            List<WebElement> els = findAll(driver, by);
            for (WebElement e : els) {
                String t = getTextOrEmpty(e);
                if (t.length() > 60) {
                    return t.length() > 600 ? t.substring(0, 600) + " ..." : t;
                }
            }
        }
        return "";
    }

    private static String firstRegexOnPage(WebDriver driver, String regex) {
        String text = getFullVisibleText(driver);
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        return "";
    }

    // ----------------- MAIN -----------------
    public static void main(String[] args) throws Exception {
        WebDriver driver = null;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_CSV, StandardCharsets.UTF_8))) {
            writeCsvHeader(bw);

            driver = buildDriver();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));

            // 1) Collect listing links (electric-like)
            Set<String> productLinks = collectElectricProductLinks(driver);

            // (Optional) If you already know a few seed URLs, you can add them here.
            // productLinks.add("https://www.colgate.com/en-ca/products/toothbrushes/.....");

            // 2) Visit detail pages
            int count = 0;
            for (String url : productLinks) {
                try {
                    Map<String, String> row = extractProduct(driver, url);
                    writeCsvRow(bw, row);
                    bw.flush();
                    count++;
                    System.out.println("[OK] (" + count + ") " + row.get("product_name"));
                } catch (Exception e) {
                    System.err.println("[ERR] Failed on: " + url + " -> " + e.getMessage());
                }
                // polite delay
                sleep(700);
            }

            System.out.println("\nDONE. Wrote " + count + " rows to: " + OUTPUT_CSV);
        } finally {
            if (driver != null) driver.quit();
        }
    }
}
