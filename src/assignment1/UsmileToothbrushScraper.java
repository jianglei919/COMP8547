package assignment1;

import io.opentelemetry.api.internal.StringUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
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
 * Usmile 电动牙刷抓取脚手架（中文注释版）
 * <p>
 * 功能概览：
 * 1) 打开“电动牙刷集合页”，发现所有 /products/ 的商品链接（支持多页/懒加载兜底）
 * 2) 对每个商品详情页抽取：名称、价格/货币、库存、图片、SKU/品牌、描述、变体颜色/选项、评分等（优先 JSON-LD 兜底）
 * 3) 结合推荐系统需要的字段（类型/模式/续航/智能等）做关键词推断（若页面有明确字段则取显式文本）
 * 4) 输出到 CSV（UTF-8）
 * <p>
 * 运行前置：
 * - 安装 Chrome 与匹配版本的 chromedriver（已在 PATH 中）；
 * - 项目已引入 Selenium 4.x 所需 JAR（非 Maven 情况下需要把所有依赖 jar 加到 lib 并挂到模块）。
 * <p>
 * 提示：
 * - 先关闭无头模式（便于调试），跑通后再打开 headless。
 * - 这是“稳健通用版”，适应 Shopify 常见主题；若后续发现更稳定的 CSS 选择器，可替换成“精准定位”。
 */
public class UsmileToothbrushScraper {

    private static final String INVALID_VALUE = "N/A";

    /* ====================== 可调配置（根据需要修改） ====================== */

    // 集合页入口（电动牙刷列表）
    private static final String COLLECTION_URL =
            "https://usmile.us/en-ca/collections/electric-toothbrush";

    // 输出文件名
    private static final String OUTPUT_CSV = "usmile_electric_toothbrushes.csv";

    // 是否使用无头模式（调试建议 false；CI 可设 true）
    private static final boolean USE_HEADLESS = false;

    // 页面加载与显式等待的超时（秒）
    private static final long PAGELOAD_TIMEOUT = 25;
    private static final long EXWAIT_TIMEOUT = 10;

    // 礼貌性间隔（毫秒），避免太快触发风控
    private static final long POLITE_DELAY_MS = 500;

    /* ====================== 启动 & WebDriver 辅助 ====================== */

    // 构建 ChromeDriver
    private static WebDriver buildDriver() {
        ChromeOptions options = new ChromeOptions();
        // 无头开关
        if (USE_HEADLESS) options.addArguments("--headless=new");
        // 稳定性参数
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--window-size=1400,1000");
        WebDriver driver = new ChromeDriver(options);
        // 页面加载策略 & 隐式等待（隐式等待设置小一点，主要靠显式等待）
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(PAGELOAD_TIMEOUT));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        return driver;
    }

    // 等待页面 readyState 为 complete
    private static void waitForPageReady(WebDriver driver) {
        new WebDriverWait(driver, Duration.ofSeconds(EXWAIT_TIMEOUT)).until(
                (ExpectedCondition<Boolean>) wd ->
                        ((JavascriptExecutor) wd).executeScript("return document.readyState")
                                .toString().equals("complete")
        );
    }

    // 简单 sleep 封装
    private static void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    // 显式等待某元素可见
    private static WebElement waitVisible(WebDriver driver, By locator, long timeoutSec) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /* ====================== 元素/文本 小工具 ====================== */

    // 在 driver 全局查找一个元素（找不到返回 null）
    private static WebElement findOne(WebDriver driver, By locator) {
        try {
            return driver.findElement(locator);
        } catch (Exception e) {
            return null;
        }
    }

    // 在 parent 元素内查找一个元素（找不到返回 null）
    private static WebElement findOne(WebElement parent, By locator) {
        if (parent == null) return null;
        try {
            return parent.findElement(locator);
        } catch (Exception e) {
            return null;
        }
    }

    // 在 driver 全局查找所有元素（失败返回空列表）
    private static List<WebElement> findAll(WebDriver driver, By locator) {
        try {
            return driver.findElements(locator);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // 取元素文本（null/异常返回空串）
    private static String text(WebElement el) {
        if (el == null) return "";
        try {
            return el.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    // 取元素属性（null/异常返回空串）
    private static String attr(WebElement el, String name) {
        if (el == null) return "";
        try {
            String v = el.getAttribute(name);
            return v == null ? "" : v.trim();
        } catch (Exception e) {
            return "";
        }
    }

    // 获取整页可见文本（用于关键词推断）
    private static String visibleText(WebDriver driver) {
        StringBuilder sb = new StringBuilder();
        for (WebElement e : findAll(driver, By.xpath("//*[not(self::script) and not(self::style)]"))) {
            try {
                String t = e.getText();
                if (t != null && !t.trim().isEmpty()) sb.append(t).append('\n');
            } catch (StaleElementReferenceException ignored) {
            }
        }
        return sb.toString();
    }

    /* ====================== CSV 输出 ====================== */

    // CSV 转义
    private static String csv(String s) {
        if (s == null) s = "";
        boolean q = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        s = s.replace("\"", "\"\"");
        return q ? "\"" + s + "\"" : s;
    }

    // 写表头（已对齐推荐系统字段）
    private static void writeHeader(BufferedWriter bw) throws IOException {
        String[] cols = {
                // 基础识别
                "brand", "product_name", "product_url", "image_url",
                // 价格/库存
                "price", "currency", "availability", "variant_options",
                // 推荐系统规格（尽量从页面/描述抽取，缺失为空）
                "type", "brushing_modes", "battery_type", "battery_life", "intensity_levels",
                "timer", "brush_head_compatibility", "smart_features", "pressure_sensor",
                "uv_sanitizer", "travel_case", "water_resistance",
                // 口碑
                "rating_value", "review_count",
                // 描述
                "description"
        };
        bw.write(String.join(",", cols));
        bw.write("\n");
    }

    // 写一行
    private static void writeRow(BufferedWriter bw, Map<String, String> row) throws IOException {
        String[] cols = {
                "brand", "product_name", "product_url", "image_url",
                "price", "currency", "availability", "variant_options",
                "type", "brushing_modes", "battery_type", "battery_life", "intensity_levels",
                "timer", "brush_head_compatibility", "smart_features", "pressure_sensor",
                "uv_sanitizer", "travel_case", "water_resistance",
                "rating_value", "review_count",
                "description"
        };
        List<String> out = new ArrayList<>();
        for (String c : cols) out.add(csv(row.getOrDefault(c, "")));
        bw.write(String.join(",", out));
        bw.write("\n");
    }

    /* ====================== 集合页：发现商品链接 ====================== */

    /**
     * 从集合页发现产品详情链接：
     * - 选择器优先“集合页网格里的卡片链接”
     * - 兜底：抓所有包含 "/products/" 的 <a>
     * - 去重 & 归一化（去 querystring）
     */
    private static LinkedHashSet<String> collectProductLinks(WebDriver driver) {
        LinkedHashSet<String> links = new LinkedHashSet<>();

        // 打开集合页
        driver.get(COLLECTION_URL);
        waitForPageReady(driver);
        sleep(POLITE_DELAY_MS);

        // 关 cookie / 隐私弹窗（如果有）
        try {
            WebElement accept = findOne(driver,
                    By.xpath("//button[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'accept')]"));
            if (accept != null) accept.click();
        } catch (Exception ignored) {
        }

        // 尝试滚动触发懒加载（多滚几次）
        long last = 0;
        for (int i = 0; i < 6; i++) {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            sleep(600);
            long h = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
            if (h == last) break;
            last = h;
        }

        // 1) 优先：集合页商品卡片（Shopify 常见结构）
        List<WebElement> cardLinks = findAll(driver, By.cssSelector("a[href*='/products/']"));
        for (WebElement a : cardLinks) {
            String href = attr(a, "href");
            if (href.contains("/products/")) links.add(stripQuery(href));
        }

        // 2) 兜底：全页所有 /products/ 链接
        if (links.isEmpty()) {
            for (WebElement a : findAll(driver, By.xpath("//a[@href]"))) {
                String href = attr(a, "href");
                if (href.contains("/products/")) links.add(stripQuery(href));
            }
        }

        System.out.println("[INFO] Found product links: " + links.size());
        return links;
    }

    // 去除 ?query 的规范化
    private static String stripQuery(String url) {
        int p = url.indexOf('?');
        return p > 0 ? url.substring(0, p) : url;
    }

    /* ====================== 详情页抽取 ====================== */

    // 主入口：抽取一个商品的所有字段
    private static Map<String, String> extractProduct(WebDriver driver, String url) {
        Map<String, String> row = new LinkedHashMap<>();

        // 基础信息
        row.put("brand", "usmile");           // 品牌固定
        row.put("product_url", url);          // 记录 URL

        // 打开详情页
        driver.get(url);
        waitForPageReady(driver);
        sleep(POLITE_DELAY_MS);

        // 1) 先用 JSON-LD 兜底（Shopify 大多包含 Product schema）
        Map<String, String> ld = parseJsonLdProduct(driver);
        row.putAll(ld);

        // 2) 再取可见标题/主图（覆盖空值）
        row.putIfAbsent("product_name", firstNonEmpty(
                text(findOne(driver, By.cssSelector("main h1"))),
                text(findOne(driver, By.cssSelector("h1")))
        ));

        // 主图 & 所有相册图（优先 /products/ 路径）
        Map<String, String> imgInfo = extractImages(driver);
        row.putIfAbsent("image_url", imgInfo.getOrDefault("primary", ""));   // 单张主图

        // 3) 变体/颜色等（通常在选项区）
        row.put("variant_options", collectVariantOptions(driver));

        // 4) 描述（取一个较长的段落）
        String description = pickLongParagraph(driver);
        row.putIfAbsent("description", StringUtils.isNullOrEmpty(description) ? extractDescription(driver) : INVALID_VALUE);

        // 5) 可见价格/货币/库存（若 JSON-LD 没拿到）
        fillVisiblePriceCurrencyAvailability(driver, row);

        // 6) 规格推断（结合页面文本进行关键词抽取，缺失则留空）
        String all = visibleText(driver).toLowerCase(Locale.ROOT);
        // 类型：Usmile 电动多为 sonic
        if (!row.containsKey("type") || row.get("type").isEmpty()) {
            row.put("type", inferType(all));
        }
        row.putIfAbsent("brushing_modes", inferModes(all));
        row.putIfAbsent("battery_type", inferBatteryType(all));
        row.putIfAbsent("battery_life", inferBatteryLife(all));
        row.putIfAbsent("intensity_levels", inferIntensityLevels(all));
        row.putIfAbsent("timer", inferTimer(all));
        row.putIfAbsent("brush_head_compatibility", inferBrushHeadCompatibility(all));
        row.putIfAbsent("smart_features", inferSmart(all));
        row.putIfAbsent("pressure_sensor", hasHints(all, "pressure", "too hard") ? "true" : "");
        row.putIfAbsent("uv_sanitizer", hasHints(all, "uv", "ultraviolet") ? "true" : "");
        row.putIfAbsent("travel_case", hasHints(all, "travel case") ? "true" : "");
        row.putIfAbsent("water_resistance", inferWaterResistance(all));

        // 7) 评分与评论兜底（若 JSON-LD 没拿到 AggregateRating）
        if (!row.containsKey("rating_value")) {
            String rating = firstMatch(visibleText(driver), "([0-9]\\.?[0-9]?)\\s*/\\s*5");
            if (!rating.isEmpty()) row.put("rating_value", rating);
        }
        if (!row.containsKey("review_count")) {
            String rc = firstMatch(visibleText(driver), "(\\d+)\\s+reviews?");
            if (!rc.isEmpty()) row.put("review_count", rc);
        }

        return row;
    }

    /* ====================== JSON-LD 解析（不引第三方 JSON 库的轻量法） ====================== */

    /**
     * 解析 <script type="application/ld+json"> 里的 Product/Offer/AggregateRating
     * 说明：这里用正则“抓关键字段”而非完整 JSON 解析，避免引入额外依赖。
     */
    private static Map<String, String> parseJsonLdProduct(WebDriver driver) {
        Map<String, String> out = new HashMap<>();
        List<WebElement> scripts = findAll(driver, By.cssSelector("script[type='application/ld+json']"));
        for (WebElement s : scripts) {
            String json = text(s);
            if (json == null || json.isEmpty()) continue;
            // 只处理包含 "Product" 的块
            if (!json.toLowerCase().contains("product")) continue;

            putIfEmpty(out, "product_name",
                    firstMatch(json, "\"name\"\\s*:\\s*\"([^\"]+)\""));

            // 品牌既可能是字符串，也可能是 { "name": "usmile" }
            putIfEmpty(out, "brand",
                    firstMatch(json, "\"brand\"\\s*:\\s*\"([^\"]+)\""),
                    firstMatch(json, "\"brand\"\\s*:\\s*\\{[^}]*?\"name\"\\s*:\\s*\"([^\"]+)\""));

            // Offer: 价格、货币、库存状态
            putIfEmpty(out, "price", firstMatch(json, "\"price\"\\s*:\\s*\"?([0-9.]+)\"?"));
            putIfEmpty(out, "currency", firstMatch(json, "\"priceCurrency\"\\s*:\\s*\"([A-Z]{3})\""));
            putIfEmpty(out, "availability", firstMatch(json, "\"availability\"\\s*:\\s*\"([^\"]+)\""));

            // 图片
            putIfEmpty(out, "image_url",
                    firstMatch(json, "\"image\"\\s*:\\s*\"(https?://[^\"]+)\""),
                    firstMatch(json, "\"image\"\\s*:\\s*\\[\\s*\"(https?://[^\"]+)\""));

            // AggregateRating: 评分/评论数
            putIfEmpty(out, "rating_value", firstMatch(json, "\"ratingValue\"\\s*:\\s*\"?([0-9.]+)\"?"));
            putIfEmpty(out, "review_count", firstMatch(json, "\"reviewCount\"\\s*:\\s*\"?(\\d+)\"?"));

            // 描述
            putIfEmpty(out, "description", firstMatch(json, "\"description\"\\s*:\\s*\"([\\s\\S]*?)\"\\s*(,|\\})"));
        }
        return out;
    }

    // 如果目标 key 目前为空，则填入第一个非空值
    private static void putIfEmpty(Map<String, String> map, String key, String... candidates) {
        if (map.containsKey(key) && map.get(key) != null && !map.get(key).isEmpty()) return;
        for (String c : candidates) {
            if (c != null && !c.isEmpty()) {
                map.put(key, c);
                return;
            }
        }
    }

    // 从字符串按正则提取首个匹配分组
    private static String firstMatch(String src, String regex) {
        if (src == null) return "";
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(src);
        return m.find() ? m.group(1).trim() : "";
    }

    // 返回第一个非空字符串
    private static String firstNonEmpty(String... ss) {
        for (String s : ss) if (s != null && !s.trim().isEmpty()) return s.trim();
        return "";
    }

    /* ====================== 变体/描述/可见价格 ====================== */

    // 收集变体/选项（颜色、刷头、套装等），用 "Color: Pink | Mode: Pro" 形式返回
    private static String collectVariantOptions(WebDriver driver) {
        // Shopify 常见：选择器在 form / variant / option 下，有 label + option/按钮
        List<String> parts = new ArrayList<>();

        // A. 下拉选择（<select>）
        for (WebElement sel : findAll(driver, By.cssSelector("select"))) {
            String label = "";
            WebElement lab = findOne(sel, By.xpath("./preceding::label[1]"));
            if (lab != null) label = text(lab);
            String val = text(findOne(sel, By.cssSelector("option[selected]")));
            if (val == null || val.isEmpty()) {
                WebElement first = findOne(sel, By.cssSelector("option"));
                if (first != null) val = text(first);
            }
            if (!label.isEmpty() || !val.isEmpty()) {
                parts.add((label.isEmpty() ? "Option" : label) + ": " + val);
            }
        }

        // B. 按钮式选项（如颜色 chips）
        for (WebElement grp : findAll(driver, By.cssSelector("[role='radiogroup'], .product-form__input, fieldset"))) {
            String label = text(findOne(grp, By.cssSelector("legend, .form__label, .product-form__label")));
            WebElement active = findOne(grp, By.cssSelector("[aria-checked='true'], .is-selected, .active, input:checked + *"));
            String val = firstNonEmpty(text(active), attr(active, "aria-label"), attr(active, "title"));
            if (!label.isEmpty() || !val.isEmpty()) {
                parts.add((label.isEmpty() ? "Option" : label) + ": " + val);
            }
        }

        return String.join(" | ", parts);
    }

    // 选择较长段落作为描述
    private static String pickLongParagraph(WebDriver driver) {
        for (WebElement p : findAll(driver, By.cssSelector("main p"))) {
            String t = text(p);
            if (t != null && t.length() >= 80) {
                return t.length() > 800 ? t.substring(0, 800) + " ..." : t;
            }
        }
        return "";
    }

    // ========== 取“主图 + 全部相册图”的稳妥方法（Shopify 优化） ==========
    private static Map<String, String> extractImages(WebDriver driver) {
        Map<String, String> out = new HashMap<>();
        List<String> candidates = new ArrayList<>();

        // A. 优先：JSON-LD 里的 image（通常是数组，且多为 /products/）
        for (WebElement s : findAll(driver, By.cssSelector("script[type='application/ld+json']"))) {
            String json = text(s);
            if (json == null) continue;
            // 提取数组或单值
            java.util.regex.Matcher mArr = java.util.regex.Pattern
                    .compile("\"image\"\\s*:\\s*\\[(.*?)\\]", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL)
                    .matcher(json);
            if (mArr.find()) {
                String arr = mArr.group(1);
                java.util.regex.Matcher mUrl = java.util.regex.Pattern
                        .compile("\"(https?://[^\"]+)\"")
                        .matcher(arr);
                while (mUrl.find()) candidates.add(mUrl.group(1));
            } else {
                String single = firstMatch(json, "\"image\"\\s*:\\s*\"(https?://[^\"]+)\"");
                if (!single.isEmpty()) candidates.add(single);
            }
        }

        // B. OpenGraph（有时只给一张）
        String og = attr(findOne(driver, By.cssSelector("meta[property='og:image']")), "content");
        if (og != null && !og.isEmpty()) candidates.add(og);

        // C. DOM 相册：只收集 “/products/” 路径的图片（排除 /files/ 营销图）
        //   - 主画廊大图
        for (WebElement img : findAll(driver, By.cssSelector("main img"))) {
            String src = pickBestSrc(img);
            if (src.contains("/cdn/shop/products/")) candidates.add(src);
        }
        //   - 缩略图
        for (WebElement img : findAll(driver, By.cssSelector("a[href*='/products/'] img, [data-media-id] img, .thumbnail-list__item img"))) {
            String src = pickBestSrc(img);
            if (src.contains("/cdn/shop/products/")) candidates.add(src);
        }

        // 去重 & 归一化
        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        for (String u : candidates) {
            if (u == null || u.isEmpty()) continue;
            dedup.add(stripQuery(u));
        }
        List<String> imgs = new ArrayList<>(dedup);

        // 选“主图”的策略：
        // 1) 优先第一个 /products/ 的；
        // 2) 若没有，则退回 JSON-LD/OG 的第一张；
        // 3) 如果还没有，最后再允许 /files/ 的图。
        String primary = "";
        for (String u : imgs) {
            if (u.contains("/cdn/shop/products/")) {
                primary = u;
                break;
            }
        }
        if (primary.isEmpty() && !imgs.isEmpty()) primary = imgs.get(0);
        if (primary.isEmpty()) {
            // 兜底再扫一遍允许 /files/
            String any = firstNonEmpty(
                    attr(findOne(driver, By.cssSelector("main picture img")), "src"),
                    attr(findOne(driver, By.cssSelector("img")), "src")
            );
            if (!any.isEmpty()) primary = stripQuery(any);
        }

        out.put("primary", primary);
        out.put("all", String.join(" | ", imgs));
        return out;
    }

    // 选择 img 的最佳 URL：优先 srcset 最大尺寸、再退回 data-src/data-original/src
    private static String pickBestSrc(WebElement img) {
        if (img == null) return "";
        String srcset = attr(img, "srcset");
        if (srcset != null && !srcset.isEmpty()) {
            // 解析 srcset，挑最大宽度那张
            String best = "";
            int bestW = -1;
            for (String part : srcset.split(",")) {
                String[] kv = part.trim().split("\\s+");
                if (kv.length >= 1) {
                    String url = kv[0].trim();
                    int w = -1;
                    if (kv.length >= 2 && kv[1].endsWith("w")) {
                        try {
                            w = Integer.parseInt(kv[1].replace("w", "").trim());
                        } catch (Exception ignored) {
                        }
                    }
                    if (w > bestW) {
                        bestW = w;
                        best = url;
                    }
                }
            }
            if (!best.isEmpty()) return best;
        }
        String dsrc = attr(img, "data-src");
        if (!dsrc.isEmpty()) return dsrc;
        String dorig = attr(img, "data-original");
        if (!dorig.isEmpty()) return dorig;
        return attr(img, "src");
    }

    // ========== 更稳的商品描述抽取（针对 Usmile/Shopify） ==========
// 思路：
// 1) 先显式等待“描述区域”出现（包含你截图里的 .product-selling-points-container）
// 2) 依次在多个“高命中率”选择器中找文本：
//    - .product-selling-points-container .metafield-rich_text_field（你这页用的）
//    - .product__description / [data-product-description] / #ProductAccordion-Description 等常见位置
// 3) 取“可见且字符数较长”的段落（优先取最长），用 JS innerText 兜底确保拿到完整文本
// 4) 统一清理空白并截断到 800 字
    private static String extractDescription(WebDriver driver) {
        // 1) 等“描述区域”出现（最多等 EXWAIT_TIMEOUT 秒）
        By waitTarget = By.cssSelector(
                ".product-selling-points-container, " +           // 你截图里的容器
                        ".product__description, " +
                        "[data-product-description], " +
                        "#ProductAccordion-Description, .product-accordion"
        );
        try {
            new WebDriverWait(driver, Duration.ofSeconds(EXWAIT_TIMEOUT))
                    .until(ExpectedConditions.presenceOfElementLocated(waitTarget));
        } catch (Exception ignore) {
        }

        // 2) 候选选择器（优先级从高到低）
        String[] selectors = new String[]{
                // 你的页面：简介在 metafield-rich_text_field 里
                ".product-selling-points-container .metafield-rich_text_field",
                ".product-selling-points-container p",

                // 常见 Shopify 描述容器
                ".product__description",
                "[data-product-description]",
                "#ProductAccordion-Description .accordion__content",
                ".product-accordion .accordion__content",
                "section[id*='MainProduct'] .rte",  // 富文本区
                ".rte",

                // 兜底：产品信息区内的较长段落
                ".product-information__inner p, main p"
        };

        String best = "";
        int bestLen = 0;

        for (String css : selectors) {
            List<WebElement> nodes = findAll(driver, By.cssSelector(css));
            for (WebElement el : nodes) {
                // 跳过不可见节点
                if (!el.isDisplayed()) continue;

                // 先用 Selenium 的 getText，再用 JS 的 innerText 兜底（有些主题 getText 截断）
                String t = text(el);
                if (t == null || t.trim().isEmpty()) {
                    try {
                        t = (String) ((JavascriptExecutor) driver)
                                .executeScript("return (arguments[0].innerText||arguments[0].textContent)||'';", el);
                    } catch (Exception ignored) {
                    }
                }
                if (t == null) t = "";
                // 清理多余空白
                t = t.replace('\u00A0', ' ')              // 不断行空格 -> 普通空格
                        .replaceAll("\\s+", " ")            // 连续空白合并
                        .trim();

                // 过滤掉太短/噪声段落（阈值 60 比原来的 80 更宽松）
                if (t.length() >= 60) {
                    if (t.length() > bestLen) {
                        best = t;
                        bestLen = t.length();
                    }
                }
            }
            // 已经拿到较长文本就不继续更低优先级的容器了
            if (bestLen >= 80) break;
        }

        // 3) 兜底：meta[name=description]
        if (best.isEmpty()) {
            WebElement meta = findOne(driver, By.cssSelector("meta[name='description']"));
            String c = attr(meta, "content");
            if (c != null && c.trim().length() >= 40) best = c.trim();
        }

        // 4) 截断到 800 字以内，避免 CSV 过长
        if (best.length() > 800) best = best.substring(0, 800) + " ...";
        return best;
    }

    // 如果 JSON-LD 没价格，则从可见区抓一个价格/货币/库存状态
    private static void fillVisiblePriceCurrencyAvailability(WebDriver driver, Map<String, String> row) {
        // A. 抓现价（优先 <ins> 或 .amount 内的价格）
        WebElement priceNode = findOne(driver, By.cssSelector("ins .amount, .price ins .amount, .price .amount, span.amount"));
        String priceText = text(priceNode);

        // 如果没取到，就全页正则兜底
        if (priceText.isEmpty()) {
            priceText = firstMatch(visibleText(driver), "(\\$?\\s*[0-9]+[\\.,][0-9]{2}\\s*(?:CAD|USD|CNY|EUR)?)");
        }

        if (!priceText.isEmpty()) {
            // 提取数字部分
            String num = firstMatch(priceText.replace(",", "."), "([0-9]+\\.[0-9]{2})");
            if (!num.isEmpty()) row.put("price", num);

            // 提取货币：看文本中是否包含 CAD / USD / CNY / EUR / ¥ / $ 等
            String cur = "";
            if (priceText.toUpperCase().contains("CAD")) cur = "CAD";
            else if (priceText.toUpperCase().contains("USD")) cur = "USD";
            else if (priceText.toUpperCase().contains("CNY") || priceText.contains("¥")) cur = "CNY";
            else if (priceText.toUpperCase().contains("EUR") || priceText.contains("€")) cur = "EUR";
            else if (priceText.contains("$")) cur = "USD"; // 默认美元符号当 USD（Usmile 加拿大站默认 CAD，但可改）
            if (!cur.isEmpty()) row.put("currency", cur);
        }

        // B. 抓“划线原价”（<del> 内）
        WebElement oldNode = findOne(driver, By.cssSelector("del .amount, .price del .amount, del span"));
        String oldText = text(oldNode);
        if (!oldText.isEmpty()) {
            String oldNum = firstMatch(oldText.replace(",", "."), "([0-9]+\\.[0-9]{2})");
            if (!oldNum.isEmpty()) row.put("price_original", oldNum);
        }

        // C. 抓库存状态（先用按钮法判断，再用可见文本兜底）
        if (!row.containsKey("availability") || row.get("availability").isEmpty()) {
            String byButton = detectAvailability(driver);
            if (!byButton.isEmpty()) {
                row.put("availability", byButton);
            } else {
                // 兜底：全页可见文本
                String vt = visibleText(driver).toLowerCase();
                if (vt.contains("out of stock") || vt.contains("sold out") || vt.contains("unavailable"))
                    row.put("availability", "OutOfStock");
                else if (vt.contains("in stock") || vt.contains("available"))
                    row.put("availability", "InStock");
            }
        }
    }

    // ========== 基于“加入购物车按钮”判断的库存检测（Shopify 友好） ==========
    private static String detectAvailability(WebDriver driver) {
        // 常见按钮选择器（不同主题命名不一，尽量覆盖）
        List<By> candidates = Arrays.asList(
                By.cssSelector("button[name='add']"),
                By.cssSelector("form[action*='/cart'] button[type='submit']"),
                By.cssSelector(".product-form__submit"),
                By.cssSelector("button#AddToCart, #AddToCart"),
                By.cssSelector("button.add-to-cart, input.add-to-cart"),
                By.cssSelector("[data-add-to-cart]"),
                By.cssSelector("button[aria-label*='add to cart' i], a[aria-label*='add to cart' i]"),
                By.cssSelector("button[title*='add to cart' i], a[title*='add to cart' i]"),
                // 有些主题把“Sold out”也用同一选择器，只是禁用了
                By.cssSelector(".product-form [type='submit']")
        );

        // 文案关键词（小写比较）
        String[] soldOutKeys = new String[]{"sold out", "out of stock", "unavailable", "notify me", "pre-order", "preorder"};
        String[] addKeys = new String[]{"add to cart", "add to bag", "buy now", "add to basket"};

        // 遍历候选按钮，只要能命中有效状态就返回
        for (By sel : candidates) {
            for (WebElement btn : findAll(driver, sel)) {
                if (btn == null || !btn.isDisplayed()) continue;

                String txt = (text(btn) + " " + attr(btn, "aria-label") + " " + attr(btn, "title")).toLowerCase();

                // 1) 明确售罄类文案
                for (String k : soldOutKeys) {
                    if (txt.contains(k)) return "OutOfStock";
                }

                // 2) 如果按钮被禁用（disabled/aria-disabled 或样式类名包含 sold-out）
                boolean disabled = false;
                try {
                    disabled = btn.getAttribute("disabled") != null
                            || "true".equalsIgnoreCase(attr(btn, "aria-disabled"))
                            || attr(btn, "class").toLowerCase().contains("sold")
                            || attr(btn, "class").toLowerCase().contains("disabled");
                } catch (Exception ignore) {
                }
                if (disabled) return "OutOfStock";

                // 3) 存在“加入购物车/立即购买”且按钮可点击 → 有货
                for (String k : addKeys) {
                    if (txt.contains(k)) return "InStock";
                }
            }
        }

        // 4) 进一步检查常见库存展示区（不少主题会显示 “In stock / Low stock / Sold out”）
        WebElement inv = findOne(driver, By.cssSelector(".product__inventory, .inventory, [data-inventory]"));
        String invText = inv == null ? "" : text(inv).toLowerCase();
        if (!invText.isEmpty()) {
            if (invText.contains("sold out") || invText.contains("out of stock") || invText.contains("unavailable"))
                return "OutOfStock";
            if (invText.contains("in stock") || invText.contains("available") || invText.contains("low stock"))
                return "InStock";
        }

        // 5) 仍无法判断则返回空串（保持原逻辑兜底）
        return INVALID_VALUE;
    }

    /* ====================== 规格推断（关键词法，缺了就留空） ====================== */

    private static boolean hasHints(String txt, String... keys) {
        for (String k : keys) if (txt.contains(k.toLowerCase())) return true;
        return false;
    }

    private static String inferType(String txt) {
        if (txt.contains("sonic")) return "sonic";                 // 超声波/声波
        if (txt.contains("oscillating")) return "oscillating";     // 旋转/摆动
        if (txt.contains("ultrasonic")) return "ultrasonic";
        return "";
    }

    private static String inferModes(String txt) {
        // 常见模式：clean / white / polish / gum care / sensitive / deep clean
        String[] ms = {"clean", "whitening", "white", "polish", "gum care", "gum", "sensitive", "deep clean", "massage"};
        LinkedHashSet<String> r = new LinkedHashSet<>();
        for (String m : ms) {
            if (txt.contains(m)) r.add(m.equals("gum") ? "gum care" : m);
        }
        return String.join(" | ", r);
    }

    private static String inferBatteryType(String txt) {
        if (txt.contains("rechargeable") || txt.contains("usb")) return "rechargeable";
        if (txt.contains("replaceable") || txt.contains("aa battery") || txt.contains("aaa battery"))
            return "replaceable";
        return "";
    }

    private static String inferBatteryLife(String txt) {
        Matcher m = Pattern.compile("(\\d+\\s*(days?|weeks?|hours?|hrs?|minutes?|mins?))").matcher(txt);
        if (m.find()) return m.group(1);
        m = Pattern.compile("up to\\s+(\\d+\\s*(days?|weeks?|hours?|hrs?|minutes?|mins?))").matcher(txt);
        if (m.find()) return m.group(1);
        return "";
    }

    private static String inferIntensityLevels(String txt) {
        Matcher m = Pattern.compile("(\\d+)\\s*(speeds?|intensity settings?|levels?)").matcher(txt);
        if (m.find()) return m.group(1);
        if (txt.contains("adjustable")) return "adjustable";
        return INVALID_VALUE;
    }

    private static String inferTimer(String txt) {
        if (txt.contains("quadrant") || txt.contains("30-second")) return "2-min with quadrant";
        if (txt.contains("2-minute") || txt.contains("two-minute")) return "2-min";
        if (txt.contains("timer")) return "yes";
        return "";
    }

    private static String inferBrushHeadCompatibility(String txt) {
        Matcher m = Pattern.compile("compatible with[^\\.\\n]+").matcher(txt);
        if (m.find()) return m.group(0);
        if (txt.contains("compatible") || txt.contains("interchangeable")) return "compatible (see page)";
        return "";
    }

    private static String inferSmart(String txt) {
        List<String> feats = new ArrayList<>();
        if (txt.contains("bluetooth")) feats.add("bluetooth");
        if (txt.contains("app")) feats.add("app integration");
        if (txt.contains("real-time")) feats.add("real-time feedback");
        if (txt.contains("coach") || txt.contains("track")) feats.add("coaching/tracking");
        return String.join(" | ", feats);
    }

    private static String inferWaterResistance(String txt) {
        Matcher m = Pattern.compile("(ipx\\s*\\d)").matcher(txt);
        if (m.find()) return m.group(1).toUpperCase(Locale.ROOT);
        if (txt.contains("waterproof") || txt.contains("water resistant")) return "water-resistant";
        return "";
    }

    /* ====================== 主程序 ====================== */

    public static void main(String[] args) {
        WebDriver driver = null;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_CSV, StandardCharsets.UTF_8))) {
            // 写 CSV 头
            writeHeader(bw);

            // 启动浏览器
            driver = buildDriver();

            // 采集集合页的所有产品链接
            LinkedHashSet<String> productLinks = collectProductLinks(driver);

            int ok = 0, fail = 0;
            for (String url : productLinks) {
                try {
                    Map<String, String> row = extractProduct(driver, url);
                    writeRow(bw, row);
                    bw.flush();
                    ok++;
                    System.out.println("[OK] " + ok + " -> " + row.getOrDefault("product_name", url));
                    sleep(POLITE_DELAY_MS);
                } catch (Exception e) {
                    fail++;
                    System.err.println("[ERR] " + url + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }

            System.out.println("\nDONE. rows=" + ok + ", failed=" + fail + ", file=" + OUTPUT_CSV);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) try {
                driver.quit();
            } catch (Exception ignore) {
            }
        }
    }
}