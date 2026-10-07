package cert;

/**
 * MET01-J: Never use assertions to validate method arguments.
 *
 * <p>Owner: James
 *
 * <p>Assertions are switched off by default (they need the {@code -ea} JVM
 * flag) and are meant to document internal assumptions, not to check data that
 * comes from callers. A check written as {@code assert} therefore disappears
 * in normal runs, and the invalid argument goes straight through. Argument
 * validation must use ordinary code that always runs and throws a proper
 * exception.
 */
public class Met01J {

    /**
     * Simple account used by both versions.
     */
    static class Account {
        private long balance = 100;

        /**
         * NONCOMPLIANT: validates the amount with an assertion. With assertions
         * disabled (the JVM default) a negative amount is accepted and
         * increases the balance.
         *
         * @param amount the amount to withdraw
         */
        void noncompliantWithdraw(long amount) {
            assert amount > 0 : "amount must be positive"; // violates MET01-J
            balance -= amount;
        }

        /**
         * COMPLIANT: validates the amount with an explicit check that always
         * runs, regardless of JVM flags.
         *
         * @param amount the amount to withdraw
         * @throws IllegalArgumentException if {@code amount} is not positive
         */
        void compliantWithdraw(long amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("amount must be positive but was " + amount);
            }
            balance -= amount;
        }

        long balance() {
            return balance;
        }
    }

    /**
     * Reports whether assertions are enabled in this JVM.
     *
     * @return true if {@code -ea} is active
     */
    static boolean assertionsEnabled() {
        boolean enabled = false;
        assert enabled = true; // intentional side effect, used only to detect the flag
        return enabled;
    }

    /**
     * Withdraws -50 from two fresh accounts, once per version.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== MET01-J: Never use assertions to validate method arguments ===");
        System.out.println("assertions enabled in this JVM: " + assertionsEnabled()
                + "  (default is false; the demo works either way)");

        System.out.println("\n-- Scenario 1: withdraw(-50) from an account holding 100 --");
        Account bad = new Account();
        try {
            bad.noncompliantWithdraw(-50);
            System.out.println("noncompliant: accepted -50, balance is now " + bad.balance());
        } catch (AssertionError e) {
            System.out.println("noncompliant: AssertionError (only because -ea is on): " + e.getMessage());
        }

        Account good = new Account();
        try {
            good.compliantWithdraw(-50);
            System.out.println("compliant   : accepted -50, balance is now " + good.balance());
        } catch (IllegalArgumentException e) {
            System.out.println("compliant   : rejected (" + e.getMessage() + "), balance stays " + good.balance());
        }
        System.out.println("-> without -ea the noncompliant check vanishes and the withdrawal ADDS money;");
        System.out.println("   the compliant check always runs. Re-run with -ea to see the noncompliant");
        System.out.println("   version behave differently, which proves its safety depends on a JVM flag.");

        System.out.println("\n-- Scenario 2 (CONTROL CASE): withdraw(30), a valid amount; identical output is expected --");
        Account a = new Account();
        Account b = new Account();
        a.noncompliantWithdraw(30);
        b.compliantWithdraw(30);
        System.out.println("noncompliant balance: " + a.balance());
        System.out.println("compliant    balance: " + b.balance());
    }
}
