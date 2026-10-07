package cert;

/**
 * EXP03-J: Do not use the equality operators when comparing values of boxed primitives.
 *
 * <p>Owner: Alex
 *
 * <p>For {@code Integer}, {@code Long} and the other wrappers, {@code ==}
 * compares object references, not values. Autoboxing reuses cached objects for
 * small values (for {@code Integer}, -128 to 127) and creates new objects for
 * larger ones, so {@code ==} appears to work in small tests and then fails
 * with bigger numbers. Compare with {@code equals()} (or unbox first).
 */
public class Exp03J {

    /**
     * NONCOMPLIANT: compares two boxed values with {@code ==}.
     *
     * @param a first value
     * @param b second value
     * @return true if both references point to the same object
     */
    static boolean noncompliant(Integer a, Integer b) {
        return a == b;               // violates EXP03-J: reference comparison
    }

    /**
     * COMPLIANT: compares the values with {@code equals()}.
     *
     * @param a first value
     * @param b second value
     * @return true if both hold the same number
     */
    static boolean compliant(Integer a, Integer b) {
        return a.equals(b);
    }

    /**
     * Compares equal numbers of different sizes.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== EXP03-J: Do not use the equality operators when comparing values of boxed primitives ===");

        System.out.println("\n-- Scenario 1: two separately boxed Integers holding the SAME number --");
        for (int n : new int[] {100, 127, 128, 1000}) {
            Integer a = n;   // autoboxing: Integer.valueOf(n)
            Integer b = n;
            System.out.println("value " + n + ": noncompliant (a == b) = " + noncompliant(a, b)
                    + "   compliant (a.equals(b)) = " + compliant(a, b));
        }
        System.out.println("-> the answer from == changes at 128 (the end of the Integer cache); equals() is always right.");

        System.out.println("\n-- Scenario 2 (CONTROL CASE): values inside the cache; identical output is expected --");
        Integer x = 5;
        Integer y = 5;
        System.out.println("noncompliant: " + noncompliant(x, y));
        System.out.println("compliant   : " + compliant(x, y));
    }
}
