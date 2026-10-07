package cert;

import java.util.Arrays;

/**
 * EXP02-J: Do not use the Object.equals() method to compare two arrays.
 *
 * <p>Owner: Martin
 *
 * <p>Arrays do not override {@link Object#equals(Object)}, so
 * {@code a.equals(b)} only tests whether both variables point at the very same
 * array object. Two arrays holding identical elements still compare as
 * different. {@link Arrays#equals(int[], int[])} compares the contents.
 */
public class Exp02J {

    /**
     * NONCOMPLIANT: uses Object.equals() on two arrays, which compares
     * references, not elements.
     *
     * @param a first array
     * @param b second array
     * @return true only if {@code a} and {@code b} are the same object
     */
    static boolean noncompliantSame(int[] a, int[] b) {
        return a.equals(b); // violates EXP02-J
    }

    /**
     * COMPLIANT: compares the arrays element by element.
     *
     * @param a first array
     * @param b second array
     * @return true if both arrays have the same length and equal elements
     */
    static boolean compliantSame(int[] a, int[] b) {
        return Arrays.equals(a, b);
    }

    /**
     * COMPLIANT (nested arrays): {@link Arrays#deepEquals(Object[], Object[])}
     * is needed when the elements are themselves arrays; plain
     * {@code Arrays.equals} would compare the inner arrays by reference.
     *
     * @param a first 2-D array
     * @param b second 2-D array
     * @return true if all nested elements are equal
     */
    static boolean compliantDeepSame(int[][] a, int[][] b) {
        return Arrays.deepEquals(a, b);
    }

    /**
     * Runs the noncompliant and compliant comparisons on equal-content arrays.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== EXP02-J: Do not use Object.equals() to compare two arrays ===");

        int[] a = {1, 2, 3};
        int[] b = {1, 2, 3};
        int[] same = a;

        System.out.println("\n-- Scenario 1: two separate arrays, both {1, 2, 3} --");
        System.out.println("noncompliant a.equals(b)        : " + noncompliantSame(a, b));
        System.out.println("compliant    Arrays.equals(a,b) : " + compliantSame(a, b));
        System.out.println("-> noncompliant says the arrays differ even though their contents are identical.");

        System.out.println("\n-- Scenario 2: nested arrays {{1,2},{3}} --");
        int[][] n1 = {{1, 2}, {3}};
        int[][] n2 = {{1, 2}, {3}};
        System.out.println("noncompliant n1.equals(n2)        : " + n1.equals(n2));
        System.out.println("compliant    Arrays.deepEquals    : " + compliantDeepSame(n1, n2));

        System.out.println("\n-- Scenario 3 (CONTROL CASE): same array object; identical output is expected --");
        System.out.println("noncompliant a.equals(same)       : " + noncompliantSame(a, same));
        System.out.println("compliant    Arrays.equals(a,same): " + compliantSame(a, same));
    }
}
