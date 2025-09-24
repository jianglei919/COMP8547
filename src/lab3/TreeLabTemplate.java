package lab3;

public class TreeLabTemplate {

    public static void main(String[] args) {
        // Initialize the different tree structures
        BinarySearchTree bst = new BinarySearchTree();
        AVLTree avlTree = new AVLTree();
        RedBlackTree rbTree = new RedBlackTree();
        SplayTree splayTree = new SplayTree();

        // Example keys for demo
        long[] keys = {40, 20, 10, 30, 60, 50, 70};

        System.out.println("Binary Search Tree Operations:");
        performOperations(bst, keys);

        System.out.println("\nAVL Tree Operations:");
        performOperations(avlTree, keys);

        System.out.println("\nRed-Black Tree Operations:");
        performOperations(rbTree, keys);

        System.out.println("\nSplay Tree Operations:");
        performOperations(splayTree, keys);
    }

    public static void performOperations(TreeInterface tree, long[] keys) {
        // Insert keys
        System.out.println("Inserting keys...");
        for (long key : keys) {
            tree.insert(key);
        }

        // Search keys
        System.out.println("Searching keys...");
        for (long key : keys) {
            System.out.println("Key " + key + " found: " + tree.search(key));
        }

        // Delete keys
        System.out.println("Deleting keys...");
        for (long key : keys) {
            tree.delete(key);
            System.out.println("Key " + key + " deleted.");
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

