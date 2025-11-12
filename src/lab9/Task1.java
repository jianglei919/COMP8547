package lab9;

import java.io.File;
import java.util.Scanner;

public class Task1 {
    public static void main(String[] args) throws Exception {

        // List of sample files to be tested for the Backtracking TSP algorithm.
        // Each file represents a different number of cities (from 6 to 13).
        String[] files = {
                "intractability/RoadSetSample6.txt",
                "intractability/RoadSetSample7.txt",
                "intractability/RoadSetSample8.txt",
                "intractability/RoadSetSample9.txt",
                "intractability/RoadSetSample10.txt",
                "intractability/RoadSetSample11.txt",
                "intractability/RoadSetSample12.txt",
                "intractability/RoadSetSample13.txt"
        };

        System.out.println("===== TASK 1: Backtracking TSP =====");

        // Loop through each dataset file and execute the TSP algorithm
        for (String filename : files) {
            File file = new File(filename);

            // If the file does not exist, skip to the next one
            if (!file.exists()) {
                System.out.println("The " + filename + " not found, skipping...");
                continue;
            }

            // Read the input data (number of cities, city names & distances)
            Scanner inputData = new Scanner(file);

            // Init the data structures inside BackTSP Class
            BackTSP.init(inputData);

            // Record the starting time with nanoTime
            long start = System.nanoTime();

            // Run the backtracking TSP algorithm to compute all possible tours
            BackTSP.tour();

            // Record the ending time in nanoseconds
            long end = System.nanoTime();

            // Print the total execution time in milliseconds for each dataset
            System.out.printf("%s total time: %.3f ms%n", filename, (end - start) / 1e6);
        }
    }
}
