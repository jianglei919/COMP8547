package assignment2;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assignment2 - Task5 Page Ranking
 * <p>
 * 1) 使用 AVL 树统计词频（平衡搜索树）
 * 2) 使用 QuickSort 输出每个页面（商品）的关键词 Top-K
 * 3) 使用最大堆（PriorityQueue）输出全局 Top-N 与品牌内 Top-M
 * <p>
 * CSV 列数据结构
 * Brand,Model,Title,Description,Price (USD),Rating (out of 5),Availability,Battery_Type,Waterproof
 */
public class PageRankingMain {

    /* ====================== 数据结构 ====================== */
    static class Product {
        String brand, model, title, description;
        double price, rating;
        String availability, batteryType, waterproof;
        String corpus;       // 语料（Title + Description + Battery + Waterproof）
        AVLTree tf;          // AVL 词频树
        int totalTerms;      // 词总数（归一化用）
        double score;        // 综合得分
    }

    /* ====================== AVL 树实现（词频） ====================== */
    static class AVLNode {
        String key;
        int freq;
        int height;
        AVLNode left, right;

        AVLNode(String k) {
            key = k;
            freq = 1;
            height = 1;
        }
    }

    static class AVLTree {
        AVLNode root;

        void insert(String key) {
            if (key == null || key.isEmpty()) {
                return;
            }
            root = insert(root, key);
        }

        private AVLNode insert(AVLNode n, String k) {
            if (n == null) {
                return new AVLNode(k);
            }
            int cmp = k.compareTo(n.key);
            if (cmp == 0) {
                n.freq++;
            } else if (cmp < 0) {
                n.left = insert(n.left, k);
            } else {
                n.right = insert(n.right, k);
            }
            update(n);
            return rebalanced(n);
        }

        private void update(AVLNode n) {
            n.height = 1 + Math.max(height(n.left), height(n.right));
        }

        private int height(AVLNode n) {
            return n == null ? 0 : n.height;
        }

        private int balance(AVLNode n) {
            return n == null ? 0 : height(n.left) - height(n.right);
        }

        private AVLNode rebalanced(AVLNode z) {
            int bf = balance(z);
            if (bf > 1) {
                if (balance(z.left) < 0) {
                    z.left = rotateLeft(z.left);
                }
                return rotateRight(z);
            } else if (bf < -1) {
                if (balance(z.right) > 0) {
                    z.right = rotateRight(z.right);
                }
                return rotateLeft(z);
            }
            return z;
        }

        private AVLNode rotateRight(AVLNode y) {
            AVLNode x = y.left, t2 = x.right;
            x.right = y;
            y.left = t2;
            update(y);
            update(x);
            return x;
        }

        private AVLNode rotateLeft(AVLNode x) {
            AVLNode y = x.right, t2 = y.left;
            y.left = x;
            x.right = t2;
            update(x);
            update(y);
            return y;
        }

        List<Map.Entry<String, Integer>> toList() {
            List<Map.Entry<String, Integer>> out = new ArrayList<>();
            inorder(root, out);
            return out;
        }

        private void inorder(AVLNode n, List<Map.Entry<String, Integer>> out) {
            if (n == null) {
                return;
            }
            inorder(n.left, out);
            out.add(new AbstractMap.SimpleEntry<>(n.key, n.freq));
            inorder(n.right, out);
        }

        int sumFreq() {
            return sum(root);
        }

        private int sum(AVLNode n) {
            if (n == null) {
                return 0;
            }
            return n.freq + sum(n.left) + sum(n.right);
        }
    }

    /* ====================== QuickSort（按频次降序） ====================== */
    static class Sorts {
        static void quickSortByFreqDesc(List<Map.Entry<String, Integer>> a) {
            if (a == null || a.size() <= 1) {
                return;
            }
            qs(a, 0, a.size() - 1);
        }

        private static void qs(List<Map.Entry<String, Integer>> a, int l, int r) {
            if (l >= r) {
                return;
            }
            int i = l, j = r, pivot = a.get((l + r) >>> 1).getValue();
            while (i <= j) {
                while (a.get(i).getValue() > pivot) {
                    i++;
                }
                while (a.get(j).getValue() < pivot) {
                    j--;
                }
                if (i <= j) {
                    Collections.swap(a, i, j);
                    i++;
                    j--;
                }
            }
            if (l < j) {
                qs(a, l, j);
            }
            if (i < r) {
                qs(a, i, r);
            }
        }
    }

    /* ====================== 关键词体系（v3.0） ====================== */
    // 单词级权重（用于 AVL 词频累加 Σ tf * w）
    static Map<String, Integer> buildWordWeights() {
        Map<String, Integer> w = new HashMap<>();
        // Modes
        putAll(w, 2, "clean", "white", "whitening");
        putAll(w, 3, "gum", "sensitive");
        putAll(w, 4, "deep", "massage");
        // Timer
        putAll(w, 4, "2", "minute", "two", "minutes");
        putAll(w, 3, "30", "second", "quadrant", "smart", "timer");
        // Charging / Battery life
        putAll(w, 3, "usb", "fast", "charging", "charge", "30", "days", "battery", "life", "weeks");
        putAll(w, 2, "rechargeable", "wireless", "base");
        // Smart
        putAll(w, 3, "smart", "bluetooth", "app", "ai", "coaching", "tracking", "feedback");
        // Pressure
        putAll(w, 4, "pressure", "sensor");
        // Brush head / reminder
        putAll(w, 2, "replacement", "reminder", "brush", "head", "compatible", "interchangeable");
        // Waterproof
        putAll(w, 4, "fully", "waterproof");
        putAll(w, 3, "ipx7");
        putAll(w, 1, "splash", "resistant");
        // Travel case
        putAll(w, 2, "travel", "case", "box");
        return w;
    }

    private static void putAll(Map<String, Integer> map, int weight, String... terms) {
        for (String t : terms) {
            map.put(t.toLowerCase(), weight);
        }
    }

    // 短语级权重（直接在全文中用 contains / 计数）
    static Map<String, Integer> buildPhraseWeights() {
        Map<String, Integer> p = new LinkedHashMap<>();
        p.put("gum care", 3);
        p.put("deep clean", 4);
        p.put("2-minute", 5);
        p.put("two-minute", 5);
        p.put("30-second", 4);
        p.put("smart timer", 4);
        p.put("usb fast charging", 4);
        p.put("30+ days battery", 4);
        p.put("replacement reminder", 3);
        p.put("pressure sensor", 5);
        p.put("travel case", 3);
        p.put("fully waterproof", 5);
        p.put("ipx7", 4);
        p.put("water resistant", 3);
        return p;
    }

    /* ====================== 停用词（英文） ====================== */
    static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "but", "if", "then", "else", "when", "while", "of", "in", "on", "at", "to", "from", "by",
            "for", "with", "about", "as", "into", "like", "through", "after", "over", "between", "out", "against", "during", "without",
            "before", "under", "around", "among", "is", "am", "are", "was", "were", "be", "been", "being", "this", "that", "these",
            "those", "it", "its", "we", "you", "they", "your", "our", "their", "i", "he", "she", "them", "his", "her", "ours", "yours",
            "my", "mine", "me", "do", "does", "did", "up", "down", "so", "than", "too", "very", "can", "cannot", "could", "should", "would"
    ));

    /* ====================== CSV 读取（支持引号与换行） ====================== */
    static class CsvRow {
        Map<String, String> map = new HashMap<>();

        String get(String k) {
            return map.getOrDefault(k, "");
        }
    }

    static List<CsvRow> readCsv(Path p) throws IOException {
        List<CsvRow> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String header = br.readLine();
            if (header == null) {
                return rows;
            }
            List<String> heads = parseCsvLine(header);

            String line;
            while ((line = br.readLine()) != null) {
                // 处理多行引号字段
                while (countQuotes(line) % 2 != 0) {
                    String next = br.readLine();
                    if (next == null) {
                        break;
                    }
                    line += "\n" + next;
                }
                List<String> cells = parseCsvLine(line);
                CsvRow r = new CsvRow();
                for (int i = 0; i < heads.size(); i++) {
                    String key = heads.get(i);
                    String val = i < cells.size() ? cells.get(i) : "";
                    r.map.put(key, val);
                }
                rows.add(r);
            }
        }
        return rows;
    }

    static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQ = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQ) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQ = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == ',') {
                    out.add(sb.toString());
                    sb.setLength(0);
                } else if (c == '"') {
                    inQ = true;
                } else {
                    sb.append(c);
                }
            }
        }
        out.add(sb.toString());
        return out;
    }

    static int countQuotes(String s) {
        int c = 0;
        for (char ch : s.toCharArray()) {
            if (ch == '"') {
                c++;
            }
        }
        return c;
    }

    /* ====================== 文本构建与分词 ====================== */
    static String buildCorpus(CsvRow r) {
        String[] keys = {"Title", "Description", "Battery_Type", "Waterproof"};
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            String v = r.get(k);
            if (v != null && !v.trim().isEmpty()) {
                sb.append(' ').append(v);
            }
        }
        return sb.toString().trim();
    }

    static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        String[] parts = text.toLowerCase()
                .replace('_', ' ')
                .replace('-', ' ')  // 让 "2-minute" -> ["2","minute"]，配合短语表也有加权
                .replace('\n', ' ')
                .split("[^a-z0-9]+");
        for (String t : parts) {
            if (t.isEmpty()) {
                continue;
            }
            if (t.length() < 2 && !t.matches("\\d")) {
                continue; // 过滤过短token，但保留纯数字如 "2","30"
            }
            if (STOP.contains(t)) {
                continue;
            }
            out.add(t);
        }
        return out;
    }

    /* ====================== 评分模块 ====================== */
    // 词级分：Σ tf(word) * weight(word)，可选再除以 log(2+总词数)
    static double wordScore(AVLTree tf, Map<String, Integer> wordW, boolean normalize) {
        if (tf == null || tf.root == null) {
            return 0.0;
        }
        double s = 0.0;
        int total = 0;
        for (Map.Entry<String, Integer> e : tf.toList()) {
            int w = wordW.getOrDefault(e.getKey(), 0);
            s += w * e.getValue();
            total += e.getValue();
        }
        if (normalize && total > 0) {
            s /= Math.log(2 + total);
        }
        return s;
    }

    // 短语分：全文 contains 计数 * 短语权重
    static double phraseScore(String text, Map<String, Integer> phraseW) {
        if (text == null) {
            return 0.0;
        }
        String t = text.toLowerCase();
        double s = 0.0;
        for (Map.Entry<String, Integer> e : phraseW.entrySet()) {
            int c = countOccurrences(t, e.getKey());
            s += c * e.getValue();
        }
        return s;
    }

    static int countOccurrences(String text, String sub) {
        Matcher m = Pattern.compile(Pattern.quote(sub)).matcher(text);
        int c = 0;
        while (m.find()) c++;
        return c;
    }

    // 评分子项：评分、价格、续航/防水、库存
    static double ratingNorm(double rating) {
        double x = Math.min(Math.max(rating, 0.0), 5.0) / 5.0;
        if (rating >= 4.5) x += 0.1; // 口碑档加成
        return Math.min(x, 1.0);
    }

    static double priceFactor(double price, double median) {
        if (price <= 0 || median <= 0) return 0.8;
        // 离中位数越近越好；过高或过低都略衰减
        return Math.exp(-Math.abs(price - median) / median);
    }

    static double batteryBonus(String b) {
        if (b == null) return 0.0;
        String s = b.toLowerCase();
        if (s.contains("lithium")) return 0.30;
        if (s.contains("nimh")) return 0.20;
        if (s.contains("aa")) return 0.10;
        return 0.0;
    }

    static double waterBonus(String w) {
        if (w == null) return 0.0;
        String s = w.toLowerCase();
        if (s.contains("fully")) return 0.30;
        if (s.contains("ipx7")) return 0.25;
        if (s.contains("splash")) return 0.10;
        return 0.0;
    }

    static double availBonus(String a) {
        if (a == null) return 0.0;
        String s = a.toLowerCase();
        if (s.contains("in stock")) return 0.30;
        if (s.contains("limited")) return 0.20;
        if (s.contains("online only")) return 0.10;
        if (s.contains("out of stock")) return -0.20; // 缺货减分
        return 0.0;
    }

    // 权重（可微调）
    static double wText = 0.40, wRate = 0.25, wPrice = 0.20, wExtra = 0.15;

    // 综合分：Text(词+短语) + Rating + Price + Extras
    static double computeScore(Product p, Map<String, Integer> wordW, Map<String, Integer> phraseW, double brandMedian) {
        double textWord = wordScore(p.tf, wordW, true);
        double textPhrase = phraseScore(p.corpus, phraseW);
        double text = textWord + textPhrase;              // 文本总体

        double rating = ratingNorm(p.rating);             // [0,1]
        double price = priceFactor(p.price, brandMedian);// (0,1]
        double extra = (batteryBonus(p.batteryType) + waterBonus(p.waterproof) + availBonus(p.availability)) / 3.0;

        return wText * text + wRate * rating + wPrice * price + wExtra * extra;
    }

    /* ====================== 品牌中位价 ====================== */
    static Map<String, Double> brandMedianPrice(List<Product> prods) {
        Map<String, List<Double>> map = new HashMap<>();
        for (Product p : prods) {
            if (p.price > 0) {
                map.computeIfAbsent(p.brand, k -> new ArrayList<>()).add(p.price);
            }
        }
        Map<String, Double> med = new HashMap<>();
        for (Map.Entry<String, List<Double>> e : map.entrySet()) {
            List<Double> xs = e.getValue();
            Collections.sort(xs);
            double m = xs.get(xs.size() / 2);
            med.put(e.getKey(), m);
        }
        return med;
    }

    /* ====================== 工具 ====================== */
    static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    static String safe(String s) {
        return s == null ? "" : s.replace("\n", " ");
    }

    /* ====================== 主程序 ====================== */
    public static void main(String[] args) throws Exception {
        // 参数
        Map<String, String> arg = parseArgs(args);
        String csv = arg.getOrDefault("csv", "Assignment2_Data.csv");
        int topN = Integer.parseInt(arg.getOrDefault("top", "10"));
        int brandTop = Integer.parseInt(arg.getOrDefault("brand-top", "5"));

        Path path = Paths.get(csv);
        if (!Files.exists(path)) {
            System.err.println("CSV 不存在: " + path.toAbsolutePath());
            return;
        }

        // 读取 CSV -> Product
        List<CsvRow> rows = readCsv(path);
        if (rows.isEmpty()) {
            System.err.println("CSV 没有数据（可能只有表头）。");
            return;
        }

        List<Product> products = new ArrayList<>();
        for (CsvRow r : rows) {
            Product product = new Product();
            product.brand = r.get("Brand");
            product.model = r.get("Model");
            product.title = r.get("Title");
            product.description = r.get("Description");
            product.price = parseDouble(r.get("Price (USD)"));
            product.rating = parseDouble(r.get("Rating (out of 5)"));
            product.availability = r.get("Availability");
            product.batteryType = r.get("Battery_Type");
            product.waterproof = r.get("Waterproof");
            product.corpus = buildCorpus(r);

            // 分词 -> AVL 词频树
            product.tf = new AVLTree();
            List<String> tokens = tokenize(product.corpus);
            for (String t : tokens) {
                product.tf.insert(t);
            }
            product.totalTerms = product.tf.sumFreq();

            products.add(product);
        }

        // 关键词权重
        Map<String, Integer> wordW = buildWordWeights();
        Map<String, Integer> phraseW = buildPhraseWeights();
        Map<String, Double> brandMed = brandMedianPrice(products);

        // 每个产品计算得分
        for (Product product : products) {
            double med = brandMed.getOrDefault(product.brand, 60.0);
            product.score = computeScore(product, wordW, phraseW, med);
        }

        /* --------- 逐页输出关键词 Top-10（QuickSort） --------- */
        System.out.println("===== Per-Product Keyword Top-10 (by AVL + QuickSort) =====");
        for (Product product : products) {
            List<Map.Entry<String, Integer>> list = product.tf.toList();
            Sorts.quickSortByFreqDesc(list);
            System.out.println(safe(product.title) + "(" + safe(product.model) + ")" );
            System.out.print("==> Top10 Keywords Frequency[ ");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(10, list.size()); i++) {
                Map.Entry<String, Integer> e = list.get(i);
                sb.append(e.getKey()).append(":").append(e.getValue()).append("| ");
            }
            System.out.println(sb.substring(0, sb.length() - 2) + " ]");
            System.out.println();
        }

        /* --------- 全局 Top-N（最大堆） --------- */
        PriorityQueue<Product> maxHeap = new PriorityQueue<>((a, b) -> Double.compare(b.score, a.score));
        maxHeap.addAll(products);
        System.out.println("##### Global Top-" + topN + " Electric Toothbrushes #####");
        for (int i = 0; i < Math.min(topN, maxHeap.size()); i++) {
            Product p = maxHeap.poll();
            System.out.printf("%2d) [%.4f] %s | %s | $%.2f | ⭐ %.1f%n",
                    i + 1, p.score, safe(p.brand), safe(p.title), p.price, p.rating);
        }
        System.out.println();

        /* --------- 品牌内 Top-M（每个品牌一个最大堆） --------- */
        Map<String, PriorityQueue<Product>> brandHeaps = new LinkedHashMap<>();
        for (Product p : products) {
            brandHeaps.computeIfAbsent(p.brand, k -> new PriorityQueue<>((a, b) -> Double.compare(b.score, a.score)))
                    .add(p);
        }
        System.out.println("##### Per-Brand Top-" + brandTop + " #####");
        for (Map.Entry<String, PriorityQueue<Product>> e : brandHeaps.entrySet()) {
            System.out.println("Brand: " + e.getKey());
            PriorityQueue<Product> heap = e.getValue();
            for (int i = 0; i < Math.min(brandTop, heap.size()); i++) {
                Product p = heap.poll();
                System.out.printf("  %2d) [%.4f] %s ($%.2f, ⭐%.1f)%n",
                        i + 1, p.score, safe(p.title), p.price, p.rating);
            }
            System.out.println();
        }
    }

    /* ====================== 参数解析 ====================== */
    static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String k = args[i].substring(2);
                String v = "true";
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    v = args[++i];
                }
                m.put(k, v);
            }
        }
        return m;
    }
}