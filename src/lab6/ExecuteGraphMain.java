package lab6;

/**
 * ExecuteGraphMain
 * ------------------------------------------------------------
 * This class executes and benchmarks four graph algorithms:
 *  1. Depth-First Search (DFS)
 *  2. Dijkstra’s Shortest Path
 *  3. Kruskal’s Minimum Spanning Tree (MST)
 *  4. Tarjan’s Strongly Connected Components (SCC)
 *
 * For each algorithm, the program runs multiple test cases
 * (Tiny, Medium, and Large graphs) using the corresponding
 * data files, measures the average execution time, and
 * displays the results in nanoseconds.
 *
 * Author: LEI JIANG
 * ------------------------------------------------------------
 */
public class ExecuteGraphMain {

    /**
     * Entry point of the program.
     * Executes the test suite for three graph sizes: tiny, medium, and large.
     */
    public static void main(String[] args) {
        testGraph("tiny");
        testGraph("medium");
        testGraph("large");
    }

    /**
     * Runs all four algorithms (DFS, Dijkstra, Kruskal, Tarjan)
     * on the specified dataset size and prints average running times.
     *
     * @param size  Graph size prefix ("tiny", "medium", or "large")
     */
    private static void testGraph(String size) {
        // Print a formatted header, e.g., "Tiny DB"
        System.out.println(size.substring(0, 1).toUpperCase() + size.substring(1) + " DB");

        // File names for directed and edge-weighted graphs
        String dgFile = size + "DG.txt";   // Directed Graph (for DFS and Tarjan)
        String ewgFile = size + "EWG.txt"; // Edge-Weighted Graph (for Dijkstra and Kruskal)

        // Run each algorithm and measure average execution time
        runDFS(dgFile);
        runDijkstra(ewgFile);
        runKruskal(ewgFile);
        runTarjan(dgFile);

        System.out.println(); // Blank line between datasets
    }

    /**
     * Measures average execution time of Depth-First Search (DFS).
     * DFS explores all vertices and edges using a recursive or iterative traversal.
     *
     * @param file Input file containing a directed graph
     */
    private static void runDFS(String file) {
        long total = 0;
        int runs = 10; // Number of runs to average results

        for (int i = 0; i < runs; i++) {
            In in = new In(file);             // Load input graph
            Digraph G = new Digraph(in);      // Construct directed graph
            long start = System.nanoTime();   // Start timing
            new DepthFirstOrder(G);           // Perform DFS traversal
            long end = System.nanoTime();     // End timing
            total += (end - start);           // Accumulate elapsed time
        }

        System.out.println("Average time taken for Depth-First Search in "
                + file + " is " + (total / runs) + " ns");
    }

    /**
     * Measures average execution time of Dijkstra’s algorithm.
     * Dijkstra finds the shortest path from a source vertex (0)
     * to all other vertices in an edge-weighted directed graph.
     *
     * @param file Input file containing an edge-weighted digraph
     */
    private static void runDijkstra(String file) {
        long total = 0;
        int runs = 10;

        for (int i = 0; i < runs; i++) {
            In in = new In(file);
            EdgeWeightedDigraph G = new EdgeWeightedDigraph(in); // Create weighted directed graph
            long start = System.nanoTime();
            new DijkstraSP(G, 0); // Run Dijkstra from source vertex 0
            long end = System.nanoTime();
            total += (end - start);
        }

        System.out.println("Average time taken for Dijkstra's algorithm in "
                + file + " is " + (total / runs) + " ns");
    }

    /**
     * Measures average execution time of Kruskal’s MST algorithm.
     * Kruskal computes the minimum spanning tree of an edge-weighted
     * undirected graph by sorting edges and applying union–find.
     *
     * @param file Input file containing an edge-weighted undirected graph
     */
    private static void runKruskal(String file) {
        long total = 0;
        int runs = 10;

        for (int i = 0; i < runs; i++) {
            In in = new In(file);
            EdgeWeightedGraph G = new EdgeWeightedGraph(in); // Construct weighted undirected graph
            long start = System.nanoTime();
            new KruskalMST(G); // Compute minimum spanning tree
            long end = System.nanoTime();
            total += (end - start);
        }

        System.out.println("Average time taken for Kruskal's algorithm in "
                + file + " is " + (total / runs) + " ns");
    }

    /**
     * Measures average execution time of Tarjan’s SCC algorithm.
     * Tarjan identifies strongly connected components (SCCs)
     * in a directed graph using a depth-first search approach.
     *
     * @param file Input file containing a directed graph
     */
    private static void runTarjan(String file) {
        long total = 0;
        int runs = 10;

        for (int i = 0; i < runs; i++) {
            In in = new In(file);
            Digraph G = new Digraph(in);      // Construct directed graph
            long start = System.nanoTime();
            new TarjanSCC(G);                 // Find SCCs using Tarjan’s algorithm
            long end = System.nanoTime();
            total += (end - start);
        }

        System.out.println("Average time taken for Tarjan's SCC algorithm in "
                + file + " is " + (total / runs) + " ns");
    }
}