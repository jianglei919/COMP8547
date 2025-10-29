package lab7;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringTokenizer;

/**
 * Task2
 * Implementation notes:
 * - We do NOT modify the provided TST source; instead we perform external accumulation:
 *   get current count -> put(count + 1).
 * - Tokenization is case-insensitive: we lowercase everything and keep only [a-z]+ tokens.
 * - "PPI" is queried as lowercase "ppi" to match the dictionary's normalization.
 */
public class Task2 {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Task2: TST Word Index on Protein.txt ===");
        String path = "Protein.txt"; // Place Protein.txt in the program's working directory

        String text;
        try {
            text = Files.readString(Path.of(path));
        } catch (IOException e) {
            System.out.println("Cannot read " + path + " : " + e.getMessage());
            return;
        }

        // Lowercase everything; StringTokenizer with non-letters as delimiters; keep only [a-z]+
        TST<Integer> tst = new TST<>();
        String lower = text.toLowerCase();
        StringTokenizer st = new StringTokenizer(
                lower,
                " \t\r\n.,;:!?()[]{}<>\"'`~@#$%^&*-_=+/\\|0123456789"
        );

        while (st.hasMoreTokens()) {
            String w = st.nextToken();
            if (!isAlphaWord(w)) continue;      // Skip tokens that contain non-letters
            Integer cnt = tst.get(w);           // External accumulation: get current count
            tst.put(w, cnt == null ? 1 : cnt + 1); // ... then put incremented count
        }

        // B) Queries (case-insensitive; we query in lowercase)
        queryAndPrint(tst, "protein");
        queryAndPrint(tst, "complex");
        queryAndPrint(tst, "ppi");          // "PPI" queried as lowercase to match normalization
        queryAndPrint(tst, "prediction");
    }

    /** Returns true if the string consists of letters [a-z]+ only (lowercase expected). */
    private static boolean isAlphaWord(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 'a' || c > 'z') return false;
        }
        return true;
    }

    /** Helper to print the count for a given (lowercased) key, interpreting null as 0. */
    private static void queryAndPrint(TST<Integer> tst, String keyLower) {
        Integer cnt = tst.get(keyLower);
        System.out.printf("%-12s: %d%n", keyLower, cnt == null ? 0 : cnt);
    }
}