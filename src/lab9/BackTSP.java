package lab9;

/* Backtracking solution to the Traveling Salesman Problem.  Generates all
 * Hamiltonian paths through recursion and keep them in a list.  Then
 * simply reports the smallest one.  There is no bounding function used.
 * A Tour object is only generated when a complete tour has been found.

 * Input file expectation:
 *   Number of vertices in the graph (crossroads in the map)
 *   That many lines giving names
 *   one blank line
 *   Lines specifying roads:  two double-quoted names and an integer

 * Language:  Java 1.5.x due to Scanner, enhanced for loop . . .

 * Solution adapted from Timothy Rolfe's from Eastern Washington University
 *  http://penguin.ewu.edu/class/cscd320/Topic/Graph/AllPaths/TSP/index.html
 *  Note this code can only be used in 60-654 for experimental purposes
 */
import java.util.*;     // Arrays and Collections and . . .

@SuppressWarnings("unchecked")
public class BackTSP
{  static int[][]  wt;                 // Matrix of edge weights
   static String[] city;               // Vector of city names
   static int      n;                  // Dimension for wt and city
   static ArrayList<Tour> soln = new ArrayList<Tour>();
   static int      bestTour = Integer.MAX_VALUE,
                   so_far;
   static boolean  DEBUG   =  false;    // Show accept/reject decisions
   static boolean  VERBOSE =  false;    // Show all tours discovered
   static boolean  TRIM    = false;    // Reject partial longer than known

   // Comparable allows sorting the ArrayList of solutions for smallest.
   private static class Tour implements Comparable
   {  int[] soln;       // Permutation vector of city subscripts
      int   dist;

      private Tour(int[] vect)
      {  dist = wt[vect[n-1]][vect[0]]; // Start with return edge
         for (int k = 1; k < n; k++)   // Add in all the others
            dist += wt[vect[k-1]][vect[k]];
         soln = (int[]) vect.clone();
      }

      public int compareTo ( Object o )
      {  return this.dist - ((Tour)o).dist;  }

      // For debugging convenience:  show the current state.
      public String toString()
      {  StringBuilder val = new StringBuilder(city[soln[0]]);
         for ( int k = 1; k < n; k++ )
            val.append(", " + city[soln[k]]);
         val.append(", " + city[soln[0]]);
         val.append( String.format(" for %d", dist) );
         return val.toString();
      }
   } // end class Tour

   // For debugging convenience:  show state UP TO this index
   private static void partial(int[] vect, int index)
   {  int dist = 0;

      System.out.print (city[vect[0]]);
      for ( int k = 1; k <= index; k++ )
      {  System.out.print (", " + city[vect[k]]);
         dist += wt[vect[k-1]][vect[k]];
      }
      System.out.println (" for distance " + dist);
   }

   // Initialize the global variables based on the file passed through
   // the Scanner inp.  See the header documentation for the
   // specifications for the input file.
   private static void init(Scanner inp)
   {  int sub1,
          sub2;
      String line;

      n = inp.nextInt();
      wt = new int[n][n];
      city = new String[n];
      // Initially, there are NO edges; hence -1.
      for ( sub1 = 0; sub1 < n; sub1++ )
         Arrays.fill(wt[sub1], -1);

      inp.nextLine();   // Discard rest of first line
      for ( sub1 = 0; sub1 < n; sub1++ )
         city[sub1] = inp.nextLine();
      Arrays.sort(city);     // Just to be sure (binarySearch)

      inp.nextLine();   // Discard blank spacing line;

      while ( inp.hasNext() )
      {  int    head, tail;
         int    dist;
         String src, dst;

         line = inp.nextLine();   // E.g.:  "George" "Pasco" 91
         // Chop out the double-quoted substrings.
         head = line.indexOf('"') + 1;
         tail = line.indexOf('"', head);
         src = line.substring(head, tail);

         head = line.indexOf('"', tail+1) + 1;
         tail = line.indexOf('"', head);
         dst = line.substring(head, tail);

         dist = Integer.parseInt( line.substring(tail+1).trim() );
         sub1 = Arrays.binarySearch(city, src);
         sub2 = Arrays.binarySearch(city, dst);
         wt[sub1][sub2] = wt[sub2][sub1] = dist;
      }
   }

   // Public access for generating the tours.
   // Generate the initial permutation vector, then call recursive tour
   public  static void tour()
   {
      int[] vect = new int[n];
      int   start = Arrays.binarySearch(city, "Spokane");

      // First permutation n vector.
      for ( int k = 0; k < n; k++ )
         vect[k] = k;
      // We will, however, start from Spokane, not Coulee City
      // --- IF the data file is the one for the inland Pacific NW
      if ( start >= 0 )
      {  vect[start] = 0; vect[0] = start;  }
      so_far = 0;  // Distance traveled SO FAR
      // Consequently, we start the permutations at [1], NOT [0].
      tour(1, vect);
   }

   // Used below in generating permutations.
   private static void swap ( int[] x, int p, int q )
   {  int tmp = x[p];  x[p] = x[q]; x[q] = tmp;  }

   // Recursive generation of the permutation vectors that constitute
   // possible paths.
   private static void tour(int index, int[] vect)
   {
      if ( index == n )      // I.e., we have a full permutation vector
      {  Tour current;

         if ( wt[vect[n-1]][vect[0]] > 0 )  // IS there a return edge?
         {//Save the state in the list
            current = new Tour(vect);
            if ( !TRIM || current.dist < bestTour )
            {
               soln.add(current);
               if ( DEBUG )
                  System.out.println("Accept " + current);
            }
            else if (DEBUG)
            {  System.out.println ("Not better " + current);  }
            bestTour = Math.min(current.dist, bestTour);
         }
         else if (DEBUG)
         {  System.out.print ("No return "); partial ( vect, n-1 );  }
      }
      else                   // Continue generating permutations
      {
         int k;         // Loop variable
         int hold;      // Used in regenerating the original state

         for ( k = index; k < n; k++ )
         {
            swap ( vect, index, k );
            if ( wt[vect[index-1]][vect[index]] > 0 )
            {
               so_far += wt[vect[index-1]][vect[index]];
               if (!TRIM || so_far < bestTour)
                  tour ( index+1, vect );
               else if (TRIM && DEBUG)
               {  System.out.print ("Too long "); partial ( vect, index ); }
               so_far -= wt[vect[index-1]][vect[index]];
            }
         }
         // Restore permutation
         hold = vect[index];
         for ( k = index+1; k < n; k++ )
            vect[k-1] = vect[k];
         vect[n-1] = hold;
      }
   }

   public static void main (String[] args) throws Exception
   {
      //String filename = args.length == 0 ? "RoadSet.txt"
      //                                   : args[0];
	  String filename = "intractability/RoadSetSample11.txt";
      Scanner inp = new Scanner(new java.io.File(filename));
      long    start;

      System.out.println("Data read from file " + filename);
      init(inp);
      start = System.nanoTime();
      tour();
      System.out.printf ("Total time:  %3.3f milliseconds\n",
         1.0E-06 * (System.nanoTime() - start) );

      if (VERBOSE)
      {  System.out.println ("Tours discovered:");
         for ( Tour opt : soln )
            System.out.println(opt);
      }
      if ( soln.size() == 0 )
         System.out.println("NO tours discovered.  Exiting.");
      else
      {  Collections.sort(soln);
         System.out.println("Best tour:  ");
         System.out.println(soln.get(0));
         System.out.println("Worst tour retained:  ");
         System.out.println(soln.get(soln.size()-1));
      }
   }
}