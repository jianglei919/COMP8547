package lab3;

import java.util.*;

public class TreeLabTemplateCopy {

    // ===== 实验参数 =====
    // 小规模实验数据量，固定为 100
    private static final int N_SMALL = 100;
    // 大规模实验数据量，这里设置为 1200（即“1000+”的意思，可以根据需要改为 1000 或 10000）
    private static final int N_LARGE = 1200;
    // 每组实验重复次数，用于减少偶然误差，这里设为 3 次取平均
    private static final int TRIALS = 3;

    // 随机数种子，用于生成插入键，保证实验可复现
    private static final long SEED_INSERT = 20250924L;
    // 随机数种子，用于生成查询键，保证查询数据可复现
    private static final long SEED_QUERY = 20250925L;

    // ===== 计时结果容器 =====
    // 用于保存单轮实验中 插入/查找/删除 三个操作的总耗时（纳秒）
    private static class Timings {
        long insertNs; // 插入总耗时
        long searchNs; // 查找总耗时
        long deleteNs; // 删除总耗时
    }

    // ===== 基准执行：一次 trial 覆盖 插入->查找->删除，返回总用时（可多次试验累加）=====
    // 参数说明：
    // tree   : 具体的树实现（BST/AVL/RedBlack/Splay）
    // asc    : 升序排列的插入键（模拟最坏情况）
    // desc   : 降序排列的删除键
    // queries: 查找操作的查询键数组（50%命中，50%未命中）
    // trials : 重复实验次数
    private static Timings benchTree(TreeInterface tree,
                                     long[] asc, long[] desc, long[] queries,
                                     int trials) {
        Timings total = new Timings();
        for (int t = 0; t < trials; t++) {
            // 插入阶段：按升序逐个插入
            long start = System.nanoTime();
            for (long k : asc) {
                tree.insert(k);
            }
            long insertNs = System.nanoTime() - start;

            // 查找阶段：对 queries 数组里的键逐个执行 search()
            // queries 已经预先混合了命中与未命中的键
            start = System.nanoTime();
            for (long q : queries) {
                tree.search(q);
            }
            long searchNs = System.nanoTime() - start;

            // 删除阶段：按降序逐个删除所有键
            start = System.nanoTime();
            for (long k : desc) {
                tree.delete(k);
            }
            long deleteNs = System.nanoTime() - start;

            // 累加三类操作的耗时
            total.insertNs += insertNs;
            total.searchNs += searchNs;
            total.deleteNs += deleteNs;
            // 删除完成后树已为空，不需要额外清理
        }
        return total;
    }

    // ===== 生成 n 个唯一 long 键（7–10 位），与树实现解耦 =====
    // 返回一个包含 n 个唯一随机 long 整数的数组，每个整数的位数在 7~10 位之间
    private static long[] genUniqueKeys(int n) {
        Random rnd = new Random(SEED_INSERT);
        HashSet<Long> set = new HashSet<>(n * 2); // 用于去重，避免重复键
        while (set.size() < n) {
            int digits = 7 + rnd.nextInt(4); // 随机选择 7~10 位
            long low = pow10(digits - 1);    // 该位数的最小值，例如 7 位数就是 1,000,000
            long high = pow10(digits) - 1;   // 该位数的最大值，例如 7 位数就是 9,999,999
            long span = high - low + 1;
            // 使用随机 long 取模的方式保证落在 [low, high] 区间
            long v = low + Math.floorMod(rnd.nextLong(), span);
            set.add(v);
        }
        // 将 HashSet 转为数组
        long[] arr = new long[n];
        int i = 0;
        for (Long x : set) {
            arr[i++] = x;
        }
        return arr;
    }

    // 计算 10 的 e 次方，例如 pow10(3) = 1000
    private static long pow10(int e) {
        long v = 1;
        for (int i = 0; i < e; i++) {
            v *= 10L;
        }
        return v;
    }

    // 数组反转，用于得到降序序列
    private static void reverse(long[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            long tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    // ===== 构造查询集合 =====
// 要求：构造一个大小为 n 的查询数组，其中
//       - 一半（50%）是命中项（即确实存在于已插入的树中）
//       - 一半（50%）是未命中项（即不在树中）
//       - 最终顺序随机打乱，避免命中和未命中分布不均
    private static long[] buildQueries(long[] insertedAsc, int n) {
        // 使用固定随机种子，保证实验结果可复现
        Random rnd = new Random(SEED_QUERY);

        // 命中查询数量 = n/2
        int hitCount = n / 2;

        // 用于保存最终的查询集合（命中 + 未命中）
        ArrayList<Long> queries = new ArrayList<>(n);

        // ================= Step 1: 采样命中项 =================
        // 从已插入的升序数组 insertedAsc 中，随机抽取 hitCount 个元素作为命中查询

        int m = insertedAsc.length;
        // 先构建一个索引数组 [0, 1, 2, ..., m-1]
        int[] idx = new int[m];
        for (int i = 0; i < m; i++) {
            idx[i] = i;
        }

        // 打乱索引数组（Fisher–Yates 洗牌算法），保证抽样是随机的
        for (int i = m - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);  // 随机选 [0, i] 范围的索引
            int tmp = idx[i];
            idx[i] = idx[j];
            idx[j] = tmp;
        }

        // 从打乱后的索引中取前 hitCount 个位置对应的键作为命中查询
        for (int i = 0; i < hitCount; i++) {
            queries.add(insertedAsc[idx[i]]);
        }

        // ================= Step 2: 生成未命中项 =================
        // 未命中项必须保证“不在已插入的集合中”
        HashSet<Long> used = new HashSet<>();
        for (long v : insertedAsc) {
            used.add(v); // 存储已插入键，便于快速查重
        }

        // 不断生成随机数，直到 queries 填满 n 个元素为止
        while (queries.size() < n) {
            // (a) 随机决定生成 7~10 位的数
            int digits = 7 + rnd.nextInt(4); // 7, 8, 9, 10

            // (b) 计算该位数的取值范围
            long low = pow10(digits - 1);  // 例如 7 位数：1,000,000
            long high = pow10(digits) - 1;  // 例如 7 位数：9,999,999
            long span = high - low + 1;

            // (c) 随机生成一个落在 [low, high] 的数 v
            long v = low + Math.floorMod(rnd.nextLong(), span);

            // (d) 检查是否已经在 used（已插入集合）中
            //     - 如果存在：说明是命中项，跳过
            //     - 如果不存在：说明是未命中项，加入 queries
            if (!used.contains(v)) {
                queries.add(v); // 确保是未命中查询
            }
        }

        // ================= Step 3: 打乱顺序 =================
        // 经过前两步，queries 中有一半命中 + 一半未命中，但顺序是“命中在前，未命中在后”
        // 为避免顺序偏差，用 Collections.shuffle 随机打乱查询顺序
        Collections.shuffle(queries, rnd);

        // ================= 返回结果 =================
        // 转换为数组形式，方便后续遍历
        long[] q = new long[n];
        for (int i = 0; i < n; i++) {
            q[i] = queries.get(i);
        }
        return q;
    }

    // ===== 打印表格 =====
    // 打印表头，包含 n 的大小以及列名
    private static void printHeader(int n) {
        System.out.println("\n========================== n = " + n + " ==========================");
        System.out.printf("%-10s %16s %16s %16s%n",
                "Tree", "Insert (ns)", "Search (ns)", "Delete (ns)");
    }

    // 打印单行结果，显示某棵树的插入/查找/删除耗时
    private static void printRow(String name, Timings t) {
        System.out.printf("%-10s %16d %16d %16d%n",
                name, t.insertNs, t.searchNs, t.deleteNs);
    }

    // 主函数入口
    public static void main(String[] args) {
        // 初始化四棵树的实例（对应四种不同实现）
        TreeInterface[] trees = {
                new BinarySearchTree(),
                new AVLTree(),
                new RedBlackTree(),
                new SplayTree()
        };
        String[] names = {"BST", "AVL", "RedBlack", "Splay"};

        final int[] NS = {N_SMALL, N_LARGE}; // 数据规模数组，100 与 1200
        for (int n : NS) {
            // Step 1: 生成 n 个唯一随机键
            long[] raw = genUniqueKeys(n);
            long[] asc = Arrays.copyOf(raw, raw.length);
            Arrays.sort(asc); // 升序
            long[] desc = Arrays.copyOf(asc, asc.length);
            reverse(desc); // 降序

            // Step 2: 构造查询集，50%命中 + 50%未命中，顺序随机
            long[] queries = buildQueries(asc, n);

            // Step 3: 打印表头
            printHeader(n);

            // Step 4: 对四棵树依次执行基准测试，并打印结果
            for (int i = 0; i < trees.length; i++) {
                Timings t = benchTree(trees[i], asc, desc, queries, TRIALS);
                printRow(names[i], t);
            }
            System.out.println(); // 空行分隔不同规模的实验结果
        }
    }

    // TreeInterface to be implemented by different tree structures
    public interface TreeInterface {
        void insert(long key);

        boolean search(long key);

        void delete(long key);
    }

    // ==================== Binary Search Tree Implementation ====================
    public static class BinarySearchTree implements TreeInterface {
        class Node {
            long key;
            Node left, right;

            public Node(long item) {
                key = item;
                left = right = null;
            }
        }

        Node root;

        BinarySearchTree() {
            root = null;
        }

        // Insert a key into the BST
        public void insert(long key) {
            root = insertRec(root, key);
        }

        Node insertRec(Node root, long key) {
            if (root == null) {
                root = new Node(key);
                return root;
            }
            if (key < root.key)
                root.left = insertRec(root.left, key);
            else if (key > root.key)
                root.right = insertRec(root.right, key);
            return root;
        }

        // Search a key in the BST
        public boolean search(long key) {
            return searchRec(root, key);
        }

        boolean searchRec(Node root, long key) {
            if (root == null) {
                return false;
            }
            if (root.key == key) {
                return true;
            }
            if (key < root.key) {
                return searchRec(root.left, key);
            } else {
                return searchRec(root.right, key);
            }
        }

        // Delete a key from the BST
        public void delete(long key) {
            root = deleteRec(root, key);
        }

        Node deleteRec(Node root, long key) {
            if (root == null) return root;

            if (key < root.key)
                root.left = deleteRec(root.left, key);
            else if (key > root.key)
                root.right = deleteRec(root.right, key);
            else {
                if (root.left == null)
                    return root.right;
                else if (root.right == null)
                    return root.left;
                root.key = minValue(root.right);
                root.right = deleteRec(root.right, root.key);
            }
            return root;
        }

        long minValue(Node root) {
            long minVal = root.key;
            while (root.left != null) {
                minVal = root.left.key;
                root = root.left;
            }
            return minVal;
        }
    }

    // ==================== AVL Tree Implementation ====================
    public static class AVLTree implements TreeInterface {
        class Node {
            long key;
            int height;
            Node left, right;

            Node(long d) {
                key = d;
                height = 1;
            }
        }

        Node root;

        // Insert a key into the AVL tree
        public void insert(long key) {
            root = insertRec(root, key);
        }

        Node insertRec(Node node, long key) {
            if (node == null)
                return new Node(key);

            if (key < node.key)
                node.left = insertRec(node.left, key);
            else if (key > node.key)
                node.right = insertRec(node.right, key);
            else
                return node;

            node.height = 1 + Math.max(height(node.left), height(node.right));

            int balance = getBalance(node);

            // Left Left Case
            if (balance > 1 && key < node.left.key)
                return rightRotate(node);

            // Right Right Case
            if (balance < -1 && key > node.right.key)
                return leftRotate(node);

            // Left Right Case
            if (balance > 1 && key > node.left.key) {
                node.left = leftRotate(node.left);
                return rightRotate(node);
            }

            // Right Left Case
            if (balance < -1 && key < node.right.key) {
                node.right = rightRotate(node.right);
                return leftRotate(node);
            }

            return node;
        }

        int height(Node N) {
            return (N == null) ? 0 : N.height;
        }

        int getBalance(Node N) {
            return (N == null) ? 0 : height(N.left) - height(N.right);
        }

        Node rightRotate(Node y) {
            Node x = y.left;
            Node T2 = x.right;
            x.right = y;
            y.left = T2;
            y.height = Math.max(height(y.left), height(y.right)) + 1;
            x.height = Math.max(height(x.left), height(x.right)) + 1;
            return x;
        }

        Node leftRotate(Node x) {
            Node y = x.right;
            Node T2 = y.left;
            y.left = x;
            x.right = T2;
            x.height = Math.max(height(x.left), height(x.right)) + 1;
            y.height = Math.max(height(y.left), height(y.right)) + 1;
            return y;
        }

        // Search a key in AVL Tree
        public boolean search(long key) {
            return searchRec(root, key);
        }

        boolean searchRec(Node node, long key) {
            if (node == null)
                return false;
            if (node.key == key)
                return true;
            if (key < node.key)
                return searchRec(node.left, key);
            return searchRec(node.right, key);
        }

        // Delete a key from the AVL Tree
        public void delete(long key) {
            root = deleteRec(root, key);
        }

        Node deleteRec(Node root, long key) {
            if (root == null)
                return root;

            if (key < root.key)
                root.left = deleteRec(root.left, key);
            else if (key > root.key)
                root.right = deleteRec(root.right, key);
            else {
                if ((root.left == null) || (root.right == null)) {
                    Node temp = (root.left != null) ? root.left : root.right;

                    if (temp == null) {
                        temp = root;
                        root = null;
                    } else {
                        root = temp;
                    }
                } else {
                    Node temp = minValueNode(root.right);
                    root.key = temp.key;
                    root.right = deleteRec(root.right, temp.key);
                }
            }

            if (root == null)
                return root;

            root.height = Math.max(height(root.left), height(root.right)) + 1;

            int balance = getBalance(root);

            if (balance > 1 && getBalance(root.left) >= 0)
                return rightRotate(root);

            if (balance > 1 && getBalance(root.left) < 0) {
                root.left = leftRotate(root.left);
                return rightRotate(root);
            }

            if (balance < -1 && getBalance(root.right) <= 0)
                return leftRotate(root);

            if (balance < -1 && getBalance(root.right) > 0) {
                root.right = rightRotate(root.right);
                return leftRotate(root);
            }

            return root;
        }

        Node minValueNode(Node node) {
            Node current = node;
            while (current.left != null)
                current = current.left;
            return current;
        }
    }

    // ==================== Red-Black Tree Implementation ====================
    public static class RedBlackTree implements TreeInterface {
        private final int RED = 0;
        private final int BLACK = 1;

        class Node {
            long key;
            int color;
            Node left, right, parent;

            Node(long key) {
                this.key = key;
                this.color = RED; // New nodes are always red
                this.left = null;
                this.right = null;
                this.parent = null;
            }
        }

        private Node root = null;
        private final Node TNULL = new Node(0); // Sentinel node for null leaves

        public RedBlackTree() {
            TNULL.color = BLACK; // TNULL is always black
            root = TNULL; // Initialize root to point to sentinel node
        }

        // Insert a node into the Red-Black Tree
        public void insert(long key) {
            Node node = new Node(key);
            node.parent = null;
            node.left = TNULL;
            node.right = TNULL;

            Node y = null;
            Node x = this.root;

            while (x != TNULL) { // Find the correct position for the new node
                y = x;
                if (node.key < x.key) {
                    x = x.left;
                } else {
                    x = x.right;
                }
            }

            node.parent = y; // Set the parent of the new node
            if (y == null) {
                root = node; // If the tree was empty, set root to the new node
            } else if (node.key < y.key) {
                y.left = node;
            } else {
                y.right = node;
            }

            if (node.parent == null) {
                node.color = BLACK; // The root node should be black
                return;
            }

            if (node.parent.parent == null) {
                return;
            }

            fixInsert(node); // Fix the tree to maintain Red-Black properties
        }

        // Fixing the red-black tree after insert
        private void fixInsert(Node k) {
            Node u;
            while (k.parent.color == RED) {
                if (k.parent == k.parent.parent.right) {
                    u = k.parent.parent.left; // uncle
                    if (u.color == RED) {
                        // Case 1: Uncle is RED
                        u.color = BLACK;
                        k.parent.color = BLACK;
                        k.parent.parent.color = RED;
                        k = k.parent.parent;
                    } else {
                        if (k == k.parent.left) {
                            // Case 2: Uncle is BLACK and k is left child
                            k = k.parent;
                            rightRotate(k);
                        }
                        // Case 3: Uncle is BLACK and k is right child
                        k.parent.color = BLACK;
                        k.parent.parent.color = RED;
                        leftRotate(k.parent.parent);
                    }
                } else {
                    u = k.parent.parent.right; // uncle
                    if (u.color == RED) {
                        // Mirror Case 1: Uncle is RED
                        u.color = BLACK;
                        k.parent.color = BLACK;
                        k.parent.parent.color = RED;
                        k = k.parent.parent;
                    } else {
                        if (k == k.parent.right) {
                            // Mirror Case 2: Uncle is BLACK and k is right child
                            k = k.parent;
                            leftRotate(k);
                        }
                        // Mirror Case 3: Uncle is BLACK and k is left child
                        k.parent.color = BLACK;
                        k.parent.parent.color = RED;
                        rightRotate(k.parent.parent);
                    }
                }
                if (k == root) {
                    break;
                }
            }
            root.color = BLACK;
        }

        // Perform left rotation
        private void leftRotate(Node x) {
            Node y = x.right;
            x.right = y.left;
            if (y.left != TNULL) {
                y.left.parent = x;
            }
            y.parent = x.parent;
            if (x.parent == null) {
                this.root = y;
            } else if (x == x.parent.left) {
                x.parent.left = y;
            } else {
                x.parent.right = y;
            }
            y.left = x;
            x.parent = y;
        }

        // Perform right rotation
        private void rightRotate(Node x) {
            Node y = x.left;
            x.left = y.right;
            if (y.right != TNULL) {
                y.right.parent = x;
            }
            y.parent = x.parent;
            if (x.parent == null) {
                this.root = y;
            } else if (x == x.parent.right) {
                x.parent.right = y;
            } else {
                x.parent.left = y;
            }
            y.right = x;
            x.parent = y;
        }

        // Search the tree
        public boolean search(long key) {
            return searchTreeHelper(this.root, key);
        }

        // Search the tree helper
        private boolean searchTreeHelper(Node node, long key) {
            if (node == TNULL || key == node.key) {
                return node != TNULL;
            }
            if (key < node.key) {
                return searchTreeHelper(node.left, key);
            }
            return searchTreeHelper(node.right, key);
        }

        // Delete a node from the tree
        public void delete(long key) {
            deleteNodeHelper(this.root, key);
        }

        private void deleteNodeHelper(Node node, long key) {
            Node z = TNULL;
            Node x, y;
            while (node != TNULL) {
                if (node.key == key) {
                    z = node;
                }
                if (node.key <= key) {
                    node = node.right;
                } else {
                    node = node.left;
                }
            }

            if (z == TNULL) {
                System.out.println("Couldn't find key in the tree");
                return;
            }

            y = z;
            int yOriginalColor = y.color;
            if (z.left == TNULL) {
                x = z.right;
                rbTransplant(z, z.right);
            } else if (z.right == TNULL) {
                x = z.left;
                rbTransplant(z, z.left);
            } else {
                y = minValueNode(z.right);
                yOriginalColor = y.color;
                x = y.right;
                if (y.parent == z) {
                    x.parent = y;
                } else {
                    rbTransplant(y, y.right);
                    y.right = z.right;
                    y.right.parent = y;
                }
                rbTransplant(z, y);
                y.left = z.left;
                y.left.parent = y;
                y.color = z.color;
            }
            if (yOriginalColor == BLACK) {
                fixDelete(x);
            }
        }

        private void rbTransplant(Node u, Node v) {
            if (u.parent == null) {
                root = v;
            } else if (u == u.parent.left) {
                u.parent.left = v;
            } else {
                u.parent.right = v;
            }
            v.parent = u.parent;
        }

        private Node minValueNode(Node node) {
            while (node.left != TNULL) {
                node = node.left;
            }
            return node;
        }

        private void fixDelete(Node x) {
            Node s;
            while (x != root && x.color == BLACK) {
                if (x == x.parent.left) {
                    s = x.parent.right;
                    if (s.color == RED) {
                        s.color = BLACK;
                        x.parent.color = RED;
                        leftRotate(x.parent);
                        s = x.parent.right;
                    }
                    if (s.left.color == BLACK && s.right.color == BLACK) {
                        s.color = RED;
                        x = x.parent;
                    } else {
                        if (s.right.color == BLACK) {
                            s.left.color = BLACK;
                            s.color = RED;
                            rightRotate(s);
                            s = x.parent.right;
                        }
                        s.color = x.parent.color;
                        x.parent.color = BLACK;
                        s.right.color = BLACK;
                        leftRotate(x.parent);
                        x = root;
                    }
                } else {
                    s = x.parent.left;
                    if (s.color == RED) {
                        s.color = BLACK;
                        x.parent.color = RED;
                        rightRotate(x.parent);
                        s = x.parent.left;
                    }
                    if (s.left.color == BLACK && s.right.color == BLACK) {
                        s.color = RED;
                        x = x.parent;
                    } else {
                        if (s.left.color == BLACK) {
                            s.right.color = BLACK;
                            s.color = RED;
                            leftRotate(s);
                            s = x.parent.left;
                        }
                        s.color = x.parent.color;
                        x.parent.color = BLACK;
                        s.left.color = BLACK;
                        rightRotate(x.parent);
                        x = root;
                    }
                }
            }
            x.color = BLACK;
        }
    }

    // ==================== Splay Tree Implementation ====================
    public static class SplayTree implements TreeInterface {
        class Node {
            long key;
            Node left, right;

            Node(long key) {
                this.key = key;
                this.left = this.right = null;
            }
        }

        Node root;

        // Insert a key into the Splay Tree
        public void insert(long key) {
            root = insertRec(root, key);
            root = splay(root, key);
        }

        private Node insertRec(Node node, long key) {
            if (node == null) return new Node(key);
            if (key < node.key) node.left = insertRec(node.left, key);
            else node.right = insertRec(node.right, key);
            return node;
        }

        // Search a key in the Splay Tree
        public boolean search(long key) {
            root = splay(root, key);
            return (root != null && root.key == key);
        }

        // Delete a key from the Splay Tree
        public void delete(long key) {
            if (root == null) return;
            root = splay(root, key);
            if (root.key != key) return;

            if (root.left == null) {
                root = root.right;
            } else {
                Node temp = root.right;
                root = root.left;
                root = splay(root, key);
                root.right = temp;
            }
        }

        // Splay operation
        private Node splay(Node node, long key) {
            if (node == null || node.key == key) return node;

            // Key lies in left subtree
            if (key < node.key) {
                if (node.left == null) return node;

                // Zig-Zig (Left Left)
                if (key < node.left.key) {
                    node.left.left = splay(node.left.left, key);
                    node = rightRotate(node);
                } else if (key > node.left.key) { // Zig-Zag (Left Right)
                    node.left.right = splay(node.left.right, key);
                    if (node.left.right != null) {
                        node.left = leftRotate(node.left);
                    }
                }

                return (node.left == null) ? node : rightRotate(node);

            } else { // Key lies in right subtree
                if (node.right == null) return node;

                // Zag-Zig (Right Left)
                if (key < node.right.key) {
                    node.right.left = splay(node.right.left, key);
                    if (node.right.left != null) {
                        node.right = rightRotate(node.right);
                    }
                } else if (key > node.right.key) { // Zag-Zag (Right Right)
                    node.right.right = splay(node.right.right, key);
                    node = leftRotate(node);
                }

                return (node.right == null) ? node : leftRotate(node);
            }
        }

        // Rotate left at node x
        private Node leftRotate(Node x) {
            Node y = x.right;
            x.right = y.left;
            y.left = x;
            return y;
        }

        // Rotate right at node x
        private Node rightRotate(Node x) {
            Node y = x.left;
            x.left = y.right;
            y.right = x;
            return y;
        }
    }
}

