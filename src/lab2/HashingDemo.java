package lab2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class HashingDemo {

    // Linear Probing Hash Table
    static class LinearProbingHashTable {
        int[] table;
        int size;

        public LinearProbingHashTable(int size) {
            this.size = size;
            table = new int[size];
            Arrays.fill(table, 0); // 0 indicates an empty slot
        }

        public void insert(int key) {
            int hash = key % size;
            while (table[hash] != 0) {
                hash = (hash + 1) % size;
            }
            table[hash] = key;
        }

        public boolean search(int key) {
            int hash = key % size;
            int count = 0;
            while (table[hash] != 0 && count < size) {
                if (table[hash] == key) return true;
                hash = (hash + 1) % size;
                count++;
            }
            return false;
        }

        /**
         * Insert multiple keys into the hash table and measure the total time.
         *
         * @param keys  an array of candidate keys
         * @param count how many keys to insert (from the beginning of the array)
         * @return elapsed time in nanoseconds
         * @see LinearProbingHashTable#insertKeys(int[], int)
         */
        public long insertKeys(int[] keys, int count) {
            long start = System.nanoTime();
            for (int i = 0; i < count; i++) {
                if (keys[i] != 0) {   // avoid using 0 because it is treated as an empty slot sentinel
                    insert(keys[i]);  // call the existing single-key insert method
                }
            }
            long end = System.nanoTime();
            return end - start;
        }

        /**
         * Search for keys in the hash table and measure the total time.
         * Half of the queries are guaranteed to be present (hits),
         * the other half are from outside the inserted set (misses).
         *
         * @param keys an array containing inserted keys followed by extra keys for misses
         * @param K    number of searches to perform
         * @return elapsed time in nanoseconds
         * @see LinearProbingHashTable#searchKeys(int[], int)
         */
        public long searchKeys(int[] keys, int K) {
            Random rand = new Random();
            int n = Math.min(K, keys.length);          // number of inserted keys
            int missPool = Math.max(0, keys.length - n); // number of available non-inserted keys
            long start = System.nanoTime();
            for (int i = 0; i < n; i++) {
                int q;
                if ((i % 2 == 0) && n > 0) {
                    q = keys[rand.nextInt(n)];        // hit: pick a key that was inserted
                } else if (missPool > 0) {
                    q = keys[n + rand.nextInt(missPool)]; // miss: pick a key outside the inserted set
                } else {
                    q = Integer.MIN_VALUE + i;        // fallback: guaranteed miss
                }
                search(q); // call the existing single-key search method
            }
            long end = System.nanoTime();
            return end - start;
        }

        /**
         * LinearProbingHashTable - Reset the hash table to its empty state by filling the table with 0.
         * This is useful for running repeated experiments without interference.
         *
         * @see LinearProbingHashTable#clear()
         */
        public void clear() {
            Arrays.fill(table, 0);
        }
    }

    // Quadratic Probing Hash Table
    static class QuadraticProbingHashTable {
        int[] table;
        int size;

        public QuadraticProbingHashTable(int size) {
            this.size = size;
            table = new int[size];
            Arrays.fill(table, 0); // 0 indicates an empty slot
        }

        public void insert(int key) {
            int hash = key % size;
            int i = 1;
            while (table[hash] != 0) {
                hash = (hash + i * i) % size;
                i++;
            }
            table[hash] = key;
        }

        public boolean search(int key) {
            int hash = key % size;
            int i = 1;
            int count = 0;
            while (table[hash] != 0 && count < size) {
                if (table[hash] == key) return true;
                hash = (hash + i * i) % size;
                i++;
                count++;
            }
            return false;
        }

        /**
         * Insert multiple keys into the hash table and measure the total time.
         *
         * @param keys  an array of candidate keys
         * @param count how many keys to insert (from the beginning of the array)
         * @return elapsed time in nanoseconds
         * @see QuadraticProbingHashTable#insertKeys(int[], int)
         */
        public long insertKeys(int[] keys, int count) {
            long start = System.nanoTime();
            for (int i = 0; i < count; i++) {
                if (keys[i] != 0) {   // avoid using 0 because it is treated as an empty slot sentinel
                    insert(keys[i]);  // call the existing single-key insert method
                }
            }
            long end = System.nanoTime();
            return end - start;
        }

        /**
         * Search for keys in the hash table and measure the total time.
         * Half of the queries are guaranteed to be present (hits),
         * the other half are from outside the inserted set (misses).
         *
         * @param keys an array containing inserted keys followed by extra keys for misses
         * @param K    number of searches to perform
         * @return elapsed time in nanoseconds
         * @see QuadraticProbingHashTable#searchKeys(int[], int)
         */
        public long searchKeys(int[] keys, int K) {
            Random rand = new Random();
            int n = Math.min(K, keys.length);          // number of inserted keys
            int missPool = Math.max(0, keys.length - n); // number of available non-inserted keys
            long start = System.nanoTime();
            for (int i = 0; i < n; i++) {
                int q;
                if ((i % 2 == 0) && n > 0) {
                    q = keys[rand.nextInt(n)];        // hit: pick a key that was inserted
                } else if (missPool > 0) {
                    q = keys[n + rand.nextInt(missPool)]; // miss: pick a key outside the inserted set
                } else {
                    q = Integer.MIN_VALUE + i;        // fallback: guaranteed miss
                }
                search(q); // call the existing single-key search method
            }
            long end = System.nanoTime();
            return end - start;
        }

        /**
         * Reset the hash table to its empty state by filling the table with 0.
         * This is useful for running repeated experiments without interference.
         *
         * @see QuadraticProbingHashTable#clear()
         */
        public void clear() {
            Arrays.fill(table, 0);
        }
    }

    // Double Hashing Hash Table
    static class DoubleHashingHashTable {
        int[] table;
        int size;

        public DoubleHashingHashTable(int size) {
            this.size = size;
            table = new int[size];
            Arrays.fill(table, 0); // 0 indicates an empty slot
        }

        private int hash1(int key) {
            return key % size;
        }

        private int hash2(int key) {
            return 7 - (key % 7); // Second hash function (prime less than size)
        }

        public void insert(int key) {
            int hash = hash1(key);
            int stepSize = hash2(key);
            while (table[hash] != 0) {
                hash = (hash + stepSize) % size;
            }
            table[hash] = key;
        }

        public boolean search(int key) {
            int hash = hash1(key);
            int stepSize = hash2(key);
            int count = 0;
            while (table[hash] != 0 && count < size) {
                if (table[hash] == key) return true;
                hash = (hash + stepSize) % size;
                count++;
            }
            return false;
        }

        /**
         * Insert multiple keys into the hash table and measure the total time.
         *
         * @param keys  an array of candidate keys
         * @param count how many keys to insert (from the beginning of the array)
         * @return elapsed time in nanoseconds
         * @see DoubleHashingHashTable#insertKeys(int[], int)
         */
        public long insertKeys(int[] keys, int count) {
            long start = System.nanoTime();
            for (int i = 0; i < count; i++) {
                if (keys[i] != 0) {   // avoid using 0 because it is treated as an empty slot sentinel
                    insert(keys[i]);  // call the existing single-key insert method
                }
            }
            long end = System.nanoTime();
            return end - start;
        }

        /**
         * Search for keys in the hash table and measure the total time.
         * Half of the queries are guaranteed to be present (hits),
         * the other half are from outside the inserted set (misses).
         *
         * @param keys an array containing inserted keys followed by extra keys for misses
         * @param K    number of searches to perform
         * @return elapsed time in nanoseconds
         * @see DoubleHashingHashTable#searchKeys(int[], int)
         */
        public long searchKeys(int[] keys, int K) {
            Random rand = new Random();
            int n = Math.min(K, keys.length);          // number of inserted keys
            int missPool = Math.max(0, keys.length - n); // number of available non-inserted keys
            long start = System.nanoTime();
            for (int i = 0; i < n; i++) {
                int q;
                if ((i % 2 == 0) && n > 0) {
                    q = keys[rand.nextInt(n)];        // hit: pick a key that was inserted
                } else if (missPool > 0) {
                    q = keys[n + rand.nextInt(missPool)]; // miss: pick a key outside the inserted set
                } else {
                    q = Integer.MIN_VALUE + i;        // fallback: guaranteed miss
                }
                search(q); // call the existing single-key search method
            }
            long end = System.nanoTime();
            return end - start;
        }

        /**
         * Reset the hash table to its empty state by filling the table with 0.
         * This is useful for running repeated experiments without interference.
         *
         * @see DoubleHashingHashTable#clear()
         */
        public void clear() {
            Arrays.fill(table, 0);
        }
    }

    // Cuckoo Hashing Table
    static class CuckooHashTable {
        int[] table1, table2;
        int size;
        int MAX_ITERATIONS = 50; // To prevent infinite loops

        public CuckooHashTable(int size) {
            this.size = size;
            table1 = new int[size];
            table2 = new int[size];
            Arrays.fill(table1, 0); // 0 indicates an empty slot
            Arrays.fill(table2, 0); // 0 indicates an empty slot
        }

        private int hash1(int key) {
            return key % size;
        }

        private int hash2(int key) {
            return (key / size) % size;
        }

        public void insert(int key) {
            int count = 0;
            int tableIndicator = 1; // Start with table1
            while (count < MAX_ITERATIONS) {
                if (tableIndicator == 1) {
                    int hash = hash1(key);
                    if (table1[hash] == 0) {
                        table1[hash] = key;
                        return;
                    } else {
                        int temp = table1[hash];
                        table1[hash] = key;
                        key = temp;
                        tableIndicator = 2; // Switch to table2
                    }
                } else {
                    int hash = hash2(key);
                    if (table2[hash] == 0) {
                        table2[hash] = key;
                        return;
                    } else {
                        int temp = table2[hash];
                        table2[hash] = key;
                        key = temp;
                        tableIndicator = 1; // Switch to table1
                    }
                }
                count++;
            }
            System.out.println("Failed to place key " + key + ". Rehash needed.");
        }

        public boolean search(int key) {
            int hash1 = hash1(key);
            int hash2 = hash2(key);
            if (table1[hash1] == key) return true;
            if (table2[hash2] == key) return true;
            return false;
        }

        /**
         * Insert multiple keys into the hash table and measure the total time.
         *
         * @param keys  an array of candidate keys
         * @param count how many keys to insert (from the beginning of the array)
         * @return elapsed time in nanoseconds
         * @see CuckooHashTable#insertKeys(int[], int)
         */
        public long insertKeys(int[] keys, int count) {
            long start = System.nanoTime();
            for (int i = 0; i < count; i++) {
                if (keys[i] != 0) {   // avoid using 0 because it is treated as an empty slot sentinel
                    insert(keys[i]);  // call the existing single-key insert method
                }
            }
            long end = System.nanoTime();
            return end - start;
        }

        /**
         * Search for keys in the hash table and measure the total time.
         * Half of the queries are guaranteed to be present (hits),
         * the other half are from outside the inserted set (misses).
         *
         * @param keys an array containing inserted keys followed by extra keys for misses
         * @param K    number of searches to perform
         * @return elapsed time in nanoseconds
         * @see CuckooHashTable#searchKeys(int[], int)
         */
        public long searchKeys(int[] keys, int K) {
            Random rand = new Random();
            int n = Math.min(K, keys.length);          // number of inserted keys
            int missPool = Math.max(0, keys.length - n); // number of available non-inserted keys
            long start = System.nanoTime();
            for (int i = 0; i < n; i++) {
                int q;
                if ((i % 2 == 0) && n > 0) {
                    q = keys[rand.nextInt(n)];        // hit: pick a key that was inserted
                } else if (missPool > 0) {
                    q = keys[n + rand.nextInt(missPool)]; // miss: pick a key outside the inserted set
                } else {
                    q = Integer.MIN_VALUE + i;        // fallback: guaranteed miss
                }
                search(q); // call the existing single-key search method
            }
            long end = System.nanoTime();
            return end - start;
        }

        /**
         * Reset the hash table to its empty state by filling the table with 0.
         * This is useful for running repeated experiments without interference.
         *
         * @see CuckooHashTable#clear()
         */
        public void clear() {
            Arrays.fill(table1, 0);
            Arrays.fill(table2, 0);
        }
    }

    /**
     * Generate the next random key.
     * Ensures the key is positive and non-zero, and avoids negative values.
     *
     * @param random Random generator
     * @return a positive integer key in the range [1, Integer.MAX_VALUE]
     */
    public static int nextKey(Random random) {
        return (random.nextInt() & Integer.MAX_VALUE) % Integer.MAX_VALUE + 1; //Transform negative numbers into positive numbers, and Modulo operation, limiting the range to [0, upper -1]
    }

    /**
     * Generate a specified number of distinct random keys.
     *
     * @param count number of unique keys to generate
     * @return an array containing distinct positive integer keys
     */
    static int[] generateDistinctKeys(int count) {
        Random random = new Random(2025L); // fixed seed for reproducibility
        Set<Integer> set = new HashSet<>(count * 2);
        while (set.size() < count) {
            set.add(nextKey(random));
        }
        return set.stream().mapToInt(Integer::intValue).toArray();
    }

    public static void main(String[] args) {
        int M = 2003; // Table capacity (prime number)
        double[] alphas = {0.25, 0.5, 0.75, 1.0};

        // Generate enough unique keys
        int[] keys = generateDistinctKeys(M * 2);

        LinearProbingHashTable lp = new LinearProbingHashTable(M);
        System.out.println("Linear Probing:");
        for (double a : alphas) {
            int K = (int) (a * M);
            long ins = lp.insertKeys(keys, K);
            long srch = lp.searchKeys(keys, K);
            System.out.printf("Load Factor %.2f: Insertion Time: %.3f ms, Search Time: %.3f ms%n",
                    a, ins / 1e6, srch / 1e6);
            lp.clear();
        }

        QuadraticProbingHashTable qp = new QuadraticProbingHashTable(M);
        System.out.println("\nQuadratic Probing:");
        for (double a : alphas) {
            int K = (int) (a * M);
            long ins = qp.insertKeys(keys, K);
            long srch = qp.searchKeys(keys, K);
            System.out.printf("Load Factor %.2f: Insertion Time: %.3f ms, Search Time: %.3f ms%n",
                    a, ins / 1e6, srch / 1e6);
            qp.clear();
        }

        DoubleHashingHashTable dh = new DoubleHashingHashTable(M);
        System.out.println("\nDouble Hashing:");
        for (double a : alphas) {
            int K = (int) (a * M);
            long ins = dh.insertKeys(keys, K);
            long srch = dh.searchKeys(keys, K);
            System.out.printf("Load Factor %.2f: Insertion Time: %.3f ms, Search Time: %.3f ms%n",
                    a, ins / 1e6, srch / 1e6);
            dh.clear();
        }

        CuckooHashTable ch = new CuckooHashTable(M);
        System.out.println("\nCuckoo Hashing:");
        for (double a : alphas) {
            int K = (int) (a * M);
            long ins = ch.insertKeys(keys, K);
            long srch = ch.searchKeys(keys, K);
            System.out.printf("Load Factor %.2f: Insertion Time: %.3f ms, Search Time: %.3f ms%n",
                    a, ins / 1e6, srch / 1e6);
            ch.clear();
        }
    }
}

