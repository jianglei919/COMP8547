package assignment3;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Assignment 3 – Task 5: Page Ranking Using Frequency Count and Boyer–Moore
 * <p>
 * Data source:
 * Uses Assignment2_Data.csv (A2) where each row is treated as a "web page".
 * Corpus = Title + Description + Battery_Type + Waterproof.
 * <p>
 * Goal:
 * For a set of search keywords/phrases, count their occurrences (per product/page)
 * using Boyer–Moore, sum to a total per page, and rank pages by total count.
 * <p>
 * CLI:
 * javac assignment3/PageRankingBM.java
 * java assignment3.PageRankingBM --csv Assignment2_Data.csv --keywords "toothbrush,gum care,pressure sensor" --ignore-case true --overlap false --top 15
 * <p>
 * Args:
 * --csv <file>                 CSV path (default: Assignment2_Data.csv)
 * --keywords "<k1,k2,...>"     comma-separated, phrases OK (required)
 * --ignore-case <true|false>   default: true
 * --overlap <true|false>       default: false (non-overlapping matches)
 * --top <N>                    print top-N (default: all)
 * <p>
 * Output:
 * Ranked list of pages with total occurrences and per-keyword counts.
 */
public class PageRankingBM {

    /* ======================== CSV support (header-based, quotes/newlines) ======================== */
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
            if (header == null) return rows;
            List<String> heads = parseCsvLine(header);

            String line;
            while ((line = br.readLine()) != null) {
                // If quotes are not balanced, keep reading the next lines (multiline field)
                while (countQuotes(line) % 2 != 0) {
                    String next = br.readLine();
                    if (next == null) break;
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
                        i++; // escaped quote
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
        for (char ch : s.toCharArray()) if (ch == '"') c++;
        return c;
    }

    /* ======================== Corpus Construction ======================== */
    static String buildCorpus(CsvRow r) {
        // Concatenate key fields to form the "page text" we search on
        String[] keys = {"Title", "Description", "Battery_Type", "Waterproof"};
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            String v = r.get(k);
            if (v != null && !v.trim().isEmpty()) sb.append(' ').append(v);
        }
        return sb.toString().trim();
    }

    static String safe(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ');
    }

    /* ======================== Boyer–Moore (bad-char + good-suffix) ======================== */
    static final class BoyerMoore {
        private final char[] pat;     // normalized pattern
        private final int m;          // length
        private final int[] bc;       // bad-character table: last index for each char
        private final int[] suffix;   // suffix[k] = start index of rightmost substring matching suffix len k
        private final boolean[] prefix; // prefix[k] = suffix(len k) is also a prefix of pattern

        BoyerMoore(String pattern, boolean ignoreCase) {
            if (pattern == null || pattern.isEmpty())
                throw new IllegalArgumentException("Pattern must be non-empty.");
            String p = ignoreCase ? pattern.toLowerCase() : pattern;
            this.pat = p.toCharArray();
            this.m = pat.length;
            this.bc = new int[Character.MAX_VALUE + 1];
            Arrays.fill(bc, -1);
            for (int i = 0; i < m; i++) bc[pat[i]] = i;
            this.suffix = new int[m];
            this.prefix = new boolean[m];
            generateGS();
        }

        private void generateGS() {
            Arrays.fill(suffix, -1);
            Arrays.fill(prefix, false);
            for (int i = 0; i < m - 1; i++) {
                int j = i, k = 0;
                while (j >= 0 && pat[j] == pat[m - 1 - k]) {
                    j--;
                    k++;
                    suffix[k] = j + 1;
                }
                if (j == -1) prefix[k] = true;
            }
        }

        private int moveByGS(int j) {
            int k = m - 1 - j; // length of matched suffix
            if (k <= 0) return 0;
            if (suffix[k] != -1) return j - suffix[k] + 1;
            for (int r = j + 2; r <= m - 1; r++) {
                if (prefix[m - r]) return r;
            }
            return m;
        }

        /**
         * Count occurrences of the pattern in the given text using Boyer–Moore.
         *
         * @param text         input text
         * @param ignoreCase   lower-case text if true
         * @param allowOverlap count overlapping matches if true
         * @return number of matches
         */
        int countIn(String text, boolean ignoreCase, boolean allowOverlap) {
            if (text == null || text.isEmpty()) return 0;
            String t = ignoreCase ? text.toLowerCase() : text;
            char[] txt = t.toCharArray();
            int n = txt.length;
            if (n < m) return 0;

            int count = 0, i = 0;
            while (i <= n - m) {
                int j;
                for (j = m - 1; j >= 0; j--) {
                    if (txt[i + j] != pat[j]) break;
                }
                if (j < 0) {
                    count++;
                    i += allowOverlap ? 1 : m;
                    continue;
                }
                int bcShift = j - bc[txt[i + j]];
                int gsShift = (j < m - 1) ? moveByGS(j) : 0;
                i += Math.max(bcShift, gsShift > 0 ? gsShift : 1);
            }
            return count;
        }
    }

    /* ======================== Ranking Structures ======================== */
    static class Product {
        String brand, model, title, corpus;
        double price, rating;
    }

    static class PageResult {
        Product prod;
        int total;
        Map<String, Integer> perKeyword = new LinkedHashMap<>();
    }

    /* ======================== Main ======================== */
    public static void main(String[] args) throws Exception {
        Map<String, String> arg = parseArgs(args);
        String csv = arg.getOrDefault("csv", "Assignment2_Data.csv");
        String keywordsStr = arg.get("keywords");
        boolean ignoreCase = Boolean.parseBoolean(arg.getOrDefault("ignore-case", "true"));
        boolean overlap = Boolean.parseBoolean(arg.getOrDefault("overlap", "false"));
        int topN = arg.containsKey("top") ? Integer.parseInt(arg.get("top")) : Integer.MAX_VALUE;

        if (keywordsStr == null || keywordsStr.trim().isEmpty()) {
            System.err.println("Missing --keywords \"k1,k2,...\"");
            return;
        }
        List<String> keywords = Arrays.stream(keywordsStr.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        if (keywords.isEmpty()) {
            System.err.println("No valid keywords provided.");
            return;
        }

        Path path = Paths.get(csv);
        if (!Files.exists(path)) {
            System.err.println("CSV not found: " + path.toAbsolutePath());
            return;
        }

        // Load CSV
        List<CsvRow> rows = readCsv(path);
        if (rows.isEmpty()) {
            System.err.println("CSV has no data rows (maybe only header).");
            return;
        }

        // Build products
        List<Product> products = new ArrayList<>();
        for (CsvRow r : rows) {
            Product p = new Product();
            p.brand = r.get("Brand");
            p.model = r.get("Model");
            p.title = r.get("Title");
            p.corpus = buildCorpus(r);
            p.price = parseDouble(r.get("Price (USD)"));
            p.rating = parseDouble(r.get("Rating (out of 5)"));
            products.add(p);
        }

        // Build Boyer–Moore matchers
        Map<String, BoyerMoore> matchers = new LinkedHashMap<>();
        for (String k : keywords) {
            matchers.put(k, new BoyerMoore(k, ignoreCase));
        }

        // Count occurrences for each product
        List<PageResult> results = new ArrayList<>();
        for (Product p : products) {
            PageResult pr = new PageResult();
            pr.prod = p;
            int sum = 0;
            for (Map.Entry<String, BoyerMoore> e : matchers.entrySet()) {
                int c = e.getValue().countIn(p.corpus, ignoreCase, overlap);
                pr.perKeyword.put(e.getKey(), c);
                sum += c;
            }
            pr.total = sum;
            results.add(pr);
        }

        // Rank by total desc; tie-breaker: higher rating, then title asc
        results.sort((a, b) -> {
            int cmp = Integer.compare(b.total, a.total);
            if (cmp != 0) return cmp;
            cmp = Double.compare(b.prod.rating, a.prod.rating);
            if (cmp != 0) return cmp;
            return safe(a.prod.title).compareToIgnoreCase(safe(b.prod.title));
        });

        // Print
        System.out.println("===== Page Ranking by Keyword Frequency (Boyer–Moore over CSV) =====");
        System.out.printf("CSV       : %s%n", path.toAbsolutePath());
        System.out.printf("Keywords  : %s%n", String.join(", ", keywords));
        System.out.printf("IgnoreCase: %s, Overlap: %s%n%n", ignoreCase, overlap);

        int limit = Math.min(topN, results.size());
        for (int i = 0; i < limit; i++) {
            PageResult r = results.get(i);
            System.out.printf(
                    "%2d) Total=%-4d | %s | $%.2f | ⭐ %.1f%n",
                    i + 1, r.total, safe(r.prod.title), r.prod.price, r.prod.rating
            );
            System.out.print("    Per-keyword: ");
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : r.perKeyword.entrySet()) {
                sb.append("\"").append(e.getKey()).append("\": ").append(e.getValue()).append("  ");
            }
            System.out.println(sb.toString().trim());
        }
    }

    /* ======================== Utils ======================== */
    static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                String key = a.substring(2);
                String val = "true";
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    val = args[++i];
                }
                m.put(key, val);
            }
        }
        return m;
    }
}
