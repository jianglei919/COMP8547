package lab7;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Task1
 * - We use your provided classes: BruteForceMatch, BoyerMoore, KMP (unchanged).
 * - Timing includes both preprocessing (e.g., building DFA or bad-character table) and search.
 * - When underlying implementations return N on "not found", we normalize to -1 for printing.
 */
public class Task1 {

    private static final String[] PATTERNS = {"disk", "protein", "PPI"};
    private static final int TEXT_LEN = 10_000;
    private static final int TRIALS = 100;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Task1: String Matching Benchmark ===");
        runRandomBenchmarks();
        runProteinKmpSearch();
        printAsymptoticNotes();
    }

    /** ===== A/B/C: random texts + average timing over 100 trials ===== */
    private static void runRandomBenchmarks() {
        System.out.println("1. Random 10,000-letter text × 100 trials");

        // Per-pattern accumulators: patternIndex -> sum of nanoseconds across trials
        long[] timeBF = new long[PATTERNS.length];
        long[] timeBM = new long[PATTERNS.length];
        long[] timeKMP = new long[PATTERNS.length];

        // Print offsets only once (on the first trial) to satisfy "show offset"
        boolean printedOffsetsOnce = false;

        for (int t = 0; t < TRIALS; t++) {
            // Reproducible yet different per-trial random text (letters only)
            String text = randAlpha(TEXT_LEN, 42 + t);

            for (int pi = 0; pi < PATTERNS.length; pi++) {
                String p = PATTERNS[pi];

                // BruteForce (uses search1 on String; returns N if not found → we convert to -1)
                long t0 = System.nanoTime();
                int offBF = searchBrute(p, text);
                long t1 = System.nanoTime();
                timeBF[pi] += (t1 - t0);

                // Boyer–Moore (includes preprocessing in timing)
                t0 = System.nanoTime();
                int offBM = searchBM(p, text);
                t1 = System.nanoTime();
                timeBM[pi] += (t1 - t0);

                // KMP (includes DFA build in timing)
                t0 = System.nanoTime();
                int offKMP = searchKmp(p, text);
                t1 = System.nanoTime();
                timeKMP[pi] += (t1 - t0);

                // Show offsets once on trial #1 for each pattern (N→-1 already normalized)
                if (!printedOffsetsOnce && t == 0) {
                    System.out.printf("Offset example on trial#1, pattern=\"%s\":  BF=%d  BM=%d  KMP=%d%n",
                            p, offBF, offBM, offKMP);
                }
            }
            printedOffsetsOnce = true;
        }

        System.out.printf("\n%-10s  %-18s  %-18s  %-18s%n",
                "Pattern", "BruteForce Avg(μs)", "BoyerMoore Avg(μs)", "KMP Avg(μs)");
        for (int pi = 0; pi < PATTERNS.length; pi++) {
            double avgBF = nanosToMicros(timeBF[pi]) / TRIALS;
            double avgBM = nanosToMicros(timeBM[pi]) / TRIALS;
            double avgK = nanosToMicros(timeKMP[pi]) / TRIALS;
            System.out.printf("%-10s  %-18.2f  %-18.2f  %-18.2f%n",
                    PATTERNS[pi], avgBF, avgBM, avgK);
        }
    }

    /** ===== E: search Protein.txt with KMP ===== */
    private static void runProteinKmpSearch() {
        System.out.println("\n2. KMP on Protein.txt (case-insensitive)");
        String path = "Protein.txt"; // Place Protein.txt in the program's working directory
        try {
            String raw = Files.readString(Path.of(path));
            // Make it case-insensitive by lowercasing both text and pattern
            String txt = raw.toLowerCase();
            int off1 = searchKmp("complex".toLowerCase(), txt);
            int off2 = searchKmp("prediction".toLowerCase(), txt);
            System.out.println("First offset of \"complex\"   : " + off1);
            System.out.println("First offset of \"prediction\": " + off2);
        } catch (IOException e) {
            System.out.println("Cannot read " + path + " : " + e.getMessage());
        }
    }

    /** ===== D: comments on asymptotic running time ===== */
    private static void printAsymptoticNotes() {
        System.out.println("\n3. Notes on asymptotic running time:");
        System.out.println("- Brute Force:   O(N*M). Both worst-case and average grow with the product of text and pattern lengths;");
        System.out.println("                 typically slowest on random texts with no matches.");
        System.out.println("- KMP:           O(N+M). After building the DFA, scanning is linear;");
        System.out.println("                 worst-case and average are similar and stable.");
        System.out.println("- Boyer–Moore:   Sublinear on average (often fastest on random texts),");
        System.out.println("                 but can degrade to O(N*M) in the worst case.");
        System.out.println("In practice on random texts: BM ≤ KMP ≪ BF (especially when patterns do not occur).");
    }

    /* ----------------- Adapters calling the provided algorithms -----------------
       Normalize 'not found' from N (the text length used by those implementations)
       to -1 for consistent output. */

    private static int searchBrute(String pat, String txt) {
        int N = txt.length();
        int off = BruteForceMatch.search1(pat, txt); // could also use search2
        return (off == N) ? -1 : off;
    }

    private static int searchBM(String pat, String txt) {
        BoyerMoore bm = new BoyerMoore(pat); // include preprocessing in timing
        int N = txt.length();
        int off = bm.search(txt);
        return (off == N) ? -1 : off;
    }

    private static int searchKmp(String pat, String txt) {
        KMP kmp = new KMP(pat); // include DFA construction in timing
        int N = txt.length();
        int off = kmp.search(txt);
        return (off == N) ? -1 : off;
    }

    /** Generate a letters-only random string with a fixed seed for reproducibility. */
    private static String randAlpha(int n, long seed) {
        Random r = new Random(seed);
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            // Lowercase letters only; note that pattern "PPI" will often be a miss, which is expected.
            sb.append((char) ('a' + r.nextInt(26)));
        }
        return sb.toString();
    }

    /** Convert nanoseconds to microseconds as a double. */
    private static double nanosToMicros(long ns) {
        return ns / 1000.0;
    }
}
