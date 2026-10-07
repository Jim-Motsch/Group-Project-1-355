package cert;

/**
 * MET54-J: Always provide feedback about the resulting value of a method.
 *
 * <p>Owner: James
 *
 * <p>Recommendation. A method that can fail or do nothing should tell its
 * caller what happened (a return value or an exception). A {@code void} method
 * that silently skips its work leaves callers believing it succeeded.
 */
public class Met54J {

    /** NONCOMPLIANT: withdraw() returns nothing and silently ignores a request it cannot honor. */
    static class NoncompliantAccount {
        int balance = 100;

        void withdraw(int amount) {
            if (amount <= balance) {
                balance -= amount;
            }                                   // violates MET54-J: otherwise nothing, and no feedback
        }
    }

    /** COMPLIANT: withdraw() reports whether the money was taken. */
    static class CompliantAccount {
        int balance = 100;

        boolean withdraw(int amount) {
            if (amount > balance) {
                return false;
            }
            balance -= amount;
            return true;
        }
    }

    /**
     * Withdraws 150 from an account holding 100, then 30.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== MET54-J: Always provide feedback about the resulting value of a method ===");

        System.out.println("\n-- Scenario 1: withdraw 150 from a balance of 100 --");
        NoncompliantAccount bad = new NoncompliantAccount();
        bad.withdraw(150);
        System.out.println("noncompliant: caller learns nothing; balance = " + bad.balance
                + "  (did the withdrawal happen? the caller cannot tell)");
        CompliantAccount good = new CompliantAccount();
        boolean ok = good.withdraw(150);
        System.out.println("compliant   : withdraw returned " + ok + "  -> "
                + (ok ? "paid out" : "refused: insufficient funds") + "; balance = " + good.balance);

        System.out.println("\n-- Scenario 2 (CONTROL CASE): withdraw 30; identical balance expected --");
        bad.withdraw(30);
        good.withdraw(30);
        System.out.println("noncompliant balance: " + bad.balance);
        System.out.println("compliant    balance: " + good.balance);
    }
}
