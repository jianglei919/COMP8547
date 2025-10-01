package lab4;

public class SortingAlgorithms {

    // Function to get the largest element from an array (used for Radix and Counting Sort)
    public static long getMax(long[] arr, long n) {
        long max = arr[0];
        for (long i = 1; i < n; i++) {
            if (arr[(int) i] > max) {
                max = arr[(int) i];
            }
        }
        return max;
    }

    // Radix Sort - Place Counting Sort
    public static void placeCountingSort(long[] arr, long size, long place) {
        long[] output = new long[(int) size];
        long[] count = new long[10];

        for (long i = 0; i < 10; i++) {
            count[(int) i] = 0;
        }

        for (long i = 0; i < size; i++) {
            count[(int) ((arr[(int) i] / place) % 10)]++;
        }

        for (long i = 1; i < 10; i++) {
            count[(int) i] += count[(int) (i - 1)];
        }

        for (long i = size - 1; i >= 0; i--) {
            output[(int) (count[(int) ((arr[(int) i] / place) % 10)] - 1)] = arr[(int) i];
            count[(int) ((arr[(int) i] / place) % 10)]--;
        }

        for (long i = 0; i < size; i++) {
            arr[(int) i] = output[(int) i];
        }
    }

    public static void radixsort(long[] arr, long size) {
        long max = getMax(arr, size);
        for (long place = 1; max / place > 0; place *= 10) {
            placeCountingSort(arr, size, place);
        }
    }

    public static void countSort(long[] arr, long size) {
        long max = getMax(arr, size);

        long[] output = new long[(int) size];
        long[] count = new long[(int) (max + 1)];

        for (long i = 0; i <= max; i++) {
            count[(int) i] = 0;
        }

        for (long i = 0; i < size; i++) {
            count[(int) arr[(int) i]]++;
        }

        for (long i = 1; i <= max; i++) {
            count[(int) i] += count[(int) (i - 1)];
        }

        for (long i = size - 1; i >= 0; i--) {
            output[(int) (count[(int) arr[(int) i]] - 1)] = arr[(int) i];
            count[(int) arr[(int) i]]--;
        }

        for (long i = 0; i < size; i++) {
            arr[(int) i] = output[(int) i];
        }
    }

    // Merge Sort
    public static void mergeSort(long[] arr, int left, int right) {
        if (left < right) {
            int mid = (left + right) / 2;

            mergeSort(arr, left, mid);
            mergeSort(arr, mid + 1, right);

            merge(arr, left, mid, right);
        }
    }

    public static void merge(long[] arr, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;

        long[] L = new long[n1];
        long[] R = new long[n2];

        for (int i = 0; i < n1; i++)
            L[i] = arr[left + i];
        for (int j = 0; j < n2; j++)
            R[j] = arr[mid + 1 + j];

        int i = 0, j = 0;

        int k = left;
        while (i < n1 && j < n2) {
            if (L[i] <= R[j]) {
                arr[k] = L[i];
                i++;
            } else {
                arr[k] = R[j];
                j++;
            }
            k++;
        }

        while (i < n1) {
            arr[k] = L[i];
            i++;
            k++;
        }

        while (j < n2) {
            arr[k] = R[j];
            j++;
            k++;
        }
    }

    // Quicksort
    public static void quickSort(long[] arr, int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);

            quickSort(arr, low, pi - 1);
            quickSort(arr, pi + 1, high);
        }
    }

    public static int partition(long[] arr, int low, int high) {
        long pivot = arr[high];
        int i = (low - 1);

        for (int j = low; j < high; j++) {
            if (arr[j] < pivot) {
                i++;
                long temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }

        long temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;

        return i + 1;
    }

    // In-place Quicksort
    public static void inPlaceQuickSort(long[] arr, int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);
            inPlaceQuickSort(arr, low, pi - 1);
            inPlaceQuickSort(arr, pi + 1, high);
        }
    }

    // Heapsort
    public static void heapSort(long[] arr, int n) {
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i);
        }

        for (int i = n - 1; i > 0; i--) {
            long temp = arr[0];
            arr[0] = arr[i];
            arr[i] = temp;

            heapify(arr, i, 0);
        }
    }

    public static void heapify(long[] arr, int n, int i) {
        int largest = i;
        int l = 2 * i + 1;
        int r = 2 * i + 2;

        if (l < n && arr[l] > arr[largest]) {
            largest = l;
        }

        if (r < n && arr[r] > arr[largest]) {
            largest = r;
        }

        if (largest != i) {
            long swap = arr[i];
            arr[i] = arr[largest];
            arr[largest] = swap;

            heapify(arr, n, largest);
        }
    }

    public static void main(String[] args) {
        long[] arr = {170, 45, 75, 90, 802, 24, 2, 66};
        int n = arr.length;

        // Display original array
        System.out.println("Original array:");
        printArray(arr);

        // Radix Sort
        long[] arr1 = arr.clone();
        radixsort(arr1, n);
        System.out.println("Radix Sorted array:");
        printArray(arr1);

        // Counting Sort
        long[] arr2 = arr.clone();
        countSort(arr2, n);
        System.out.println("Counting Sorted array:");
        printArray(arr2);

        // Merge Sort
        long[] arr3 = arr.clone();
        mergeSort(arr3, 0, n - 1);
        System.out.println("Merge Sorted array:");
        printArray(arr3);

        // Quicksort
        long[] arr4 = arr.clone();
        quickSort(arr4, 0, n - 1);
        System.out.println("Quicksort Sorted array:");
        printArray(arr4);

        // In-place Quicksort
        long[] arr5 = arr.clone();
        inPlaceQuickSort(arr5, 0, n - 1);
        System.out.println("In-place Quicksort Sorted array:");
        printArray(arr5);

        // Heapsort
        long[] arr6 = arr.clone();
        heapSort(arr6, n);
        System.out.println("Heapsort Sorted array:");
        printArray(arr6);
    }

    // Print an array
    public static void printArray(long[] arr) {
        for (long num : arr) {
            System.out.print(num + " ");
        }
        System.out.println();
    }
}
