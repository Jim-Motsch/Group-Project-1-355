package cert;

/**
 * OBJ10-J: Do not use public static nonfinal fields.
 *
 * <p>Owner: Martin
 *
 * <p>A public static field that is not final can be changed by any code in the
 * program at any time, with no validation and no way for the owning class to
 * react. Keeping the field private and exposing a getter plus a validating
 * setter lets the class protect its own invariants.
 */
public class Obj10J {

    /** NONCOMPLIANT settings holder: any code can write to the public static field. */
    static class NoncompliantSettings {
        /** Violates OBJ10-J: public, static and not final. */
        public static int maxRetries = 3;
    }

    /** COMPLIANT settings holder: the field is private and only changed through a validating setter. */
    static class CompliantSettings {
        private static int maxRetries = 3;

        /**
         * Returns the current retry limit.
         *
         * @return the retry limit
         */
        static int getMaxRetries() {
            return maxRetries;
        }

        /**
         * Updates the retry limit, rejecting values outside 1..10.
         *
         * @param value the requested limit
         * @throws IllegalArgumentException if {@code value} is out of range
         */
        static void setMaxRetries(int value) {
            if (value < 1 || value > 10) {
                throw new IllegalArgumentException("maxRetries must be 1..10 but was " + value);
            }
            maxRetries = value;
        }
    }

    /**
     * Simulates a service that tries an operation up to {@code limit} times.
     *
     * @param limit number of attempts allowed
     * @return a description of how many attempts were made
     */
    static String runService(int limit) {
        int attempts = 0;
        for (int i = 0; i < limit; i++) {
            attempts++;
        }
        return attempts + " attempt(s) made";
    }

    /**
     * Shows other code corrupting the noncompliant field while the compliant
     * version refuses the same change.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== OBJ10-J: Do not use public static nonfinal fields ===");

        System.out.println("\n-- Scenario 1: unrelated code sets maxRetries = -1 --");
        NoncompliantSettings.maxRetries = -1; // nothing stops this write
        System.out.println("noncompliant: maxRetries=" + NoncompliantSettings.maxRetries
                + " -> " + runService(NoncompliantSettings.maxRetries));

        try {
            CompliantSettings.setMaxRetries(-1);
        } catch (IllegalArgumentException e) {
            System.out.println("compliant   : setter rejected the change (" + e.getMessage() + ")");
        }
        System.out.println("compliant   : maxRetries=" + CompliantSettings.getMaxRetries()
                + " -> " + runService(CompliantSettings.getMaxRetries()));
        System.out.println("-> noncompliant service silently does nothing; compliant keeps a valid value.");

        System.out.println("\n-- Scenario 2 (CONTROL CASE): a valid change; both accept it --");
        NoncompliantSettings.maxRetries = 5;
        CompliantSettings.setMaxRetries(5);
        System.out.println("noncompliant: " + runService(NoncompliantSettings.maxRetries));
        System.out.println("compliant   : " + runService(CompliantSettings.getMaxRetries()));
    }
}
