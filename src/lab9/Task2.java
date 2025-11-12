package lab9;

import lab6.*;

public class Task2 {
    public static void main(String[] args) {

        // List of input graph files used to test Dijkstra's Shortest Path algorithm.
        // Each file contains an edge-weighted directed graph of different sizes.
        String[] files = {"1000EWG.txt", "mediumEWG.txt", "10000EWG.txt"};

        System.out.println("===== TASK 2: Dijkstra Shortest Path =====");

        // Traverse each graphic file and measure execution time
        for (String filename : files) {
            try {
                // Read the graph data using Princeton's 'In' class.
                // Each file describes a weighted directed graph (EdgeWeightedDigraph).
                EdgeWeightedDigraph G = new EdgeWeightedDigraph(new In(filename));

                // Record the start time before running the algorithm
                long start = System.nanoTime();

                // Execute Dijkstra’s algorithm starting from vertex 0
                new DijkstraSP(G, 0);

                // Record the end time after the algorithm finishes
                long end = System.nanoTime();

                // Print the total execution time (converted from nanoseconds to milliseconds)
                System.out.printf("%s total time: %.3f ms%n", filename, (end - start) / 1e6);

            } catch (Exception e) {
                // If the file is missing or cannot be read, display an error message
                System.out.println(filename + " not found or error: " + e.getMessage());
            }
        }
    }
}
