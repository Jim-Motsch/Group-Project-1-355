package cert;

/**
 * ERR50-J: Use exceptions only for exceptional conditions.
 *
 * <p>Owner: Martin
 *
 * <p>Recommendation. Reaching the end of an array is expected, so a loop that
 * ends by catching {@code ArrayIndexOutOfBoundsException} uses an exception as
 * an ordinary {@code break}. It also cannot tell the expected end from a real
 * bug that throws the same exception inside the loop body.
 */
public class Err50J {

    /** Counts exceptions thrown on purpose as part of normal control flow. */
    static int exceptionsThrown;

    /**
     * NONCOMPLIANT: the loop ends when the array index runs out and the exception is caught.
     *
     * @param a values to add
     * @return the sum
     */
    static int noncompliantSum(int[] a) {
        int sum = 0;
        try {
            for (int i = 0; ; i++) {
                sum += a[i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {   // violates ERR50-J: used as the loop exit
            exceptionsThrown++;
        }
        return sum;
    }

    /**
     * COMPLIANT: the loop tests its bound.
     *
     * @param a values to add
     * @return the sum
     */
    static int compliantSum(int[] a) {
        int sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i];
        }
        return sum;
    }

    /** NONCOMPLIANT with a body bug: reads a[i + 1] instead of a[i]; the catch hides it. */
    static int noncompliantBuggySum(int[] a) {
        int sum = 0;
        try {
            for (int i = 0; ; i++) {
                sum += a[i + 1];            // bug
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            exceptionsThrown++;
        }
        return sum;
    }

    /** COMPLIANT loop with the same body bug: the bug is no longer disguised as the loop end. */
    static int compliantBuggySum(int[] a) {
        int sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i + 1];                // same bug
        }
        return sum;
    }

    /**
     * Sums {10, 20, 30} with each version.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== ERR50-J: Use exceptions only for exceptional conditions ===");
        int[] data = {10, 20, 30};

        System.out.println("\n-- Scenario 1: sum {10, 20, 30} --");
        exceptionsThrown = 0;
        System.out.println("noncompliant: sum = " + noncompliantSum(data) + ", exceptions thrown = " + exceptionsThrown);
        System.out.println("compliant   : sum = " + compliantSum(data) + ", exceptions thrown = 0");
        System.out.println("-> same sum, but noncompliant needed an exception just to stop.");

        System.out.println("\n-- Scenario 2: same loops, body accidentally reads a[i + 1] --");
        System.out.println("noncompliant: sum = " + noncompliantBuggySum(data) + "  (wrong answer, looks normal)");
        try {
            System.out.println("compliant   : sum = " + compliantBuggySum(data));
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("compliant   : BUG SURFACED -> " + e);
        }
    }
}
