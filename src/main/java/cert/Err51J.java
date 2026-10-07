package cert;

/**
 * ERR51-J: Prefer user-defined exceptions over more general exception types.
 *
 * <p>Owner: Alex
 *
 * <p>Recommendation. Throwing {@code RuntimeException} (or {@code Exception})
 * with only a message forces callers to catch everything alike, or to compare
 * message text. Distinct exception types let a caller handle each failure in the
 * way that fits it and let unrelated failures pass through.
 */
public class Err51J {

    /** User-defined: the balance is too low. */
    static class InsufficientFundsException extends Exception {
        private static final long serialVersionUID = 1L;

        InsufficientFundsException(String m) {
            super(m);
        }
    }

    /** User-defined: the account is frozen. */
    static class AccountFrozenException extends Exception {
        private static final long serialVersionUID = 1L;

        AccountFrozenException(String m) {
            super(m);
        }
    }

    /** NONCOMPLIANT: both failures are the same general type. */
    static void noncompliantWithdraw(boolean frozen) {
        if (frozen) {
            throw new RuntimeException("account frozen");                // violates ERR51-J
        }
        throw new RuntimeException("insufficient funds");                // violates ERR51-J
    }

    /** COMPLIANT: one type per failure. */
    static void compliantWithdraw(boolean frozen) throws InsufficientFundsException, AccountFrozenException {
        if (frozen) {
            throw new AccountFrozenException("account frozen");
        }
        throw new InsufficientFundsException("insufficient funds");
    }

    /**
     * The caller wants to offer an overdraft for low funds and send frozen accounts to the bank.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== ERR51-J: Prefer user-defined exceptions over more general exception types ===");

        for (boolean frozen : new boolean[] {false, true}) {
            System.out.println("\n-- Scenario: " + (frozen ? "frozen account" : "balance too low") + " --");
            try {
                noncompliantWithdraw(frozen);
            } catch (RuntimeException e) {
                System.out.println("noncompliant: caught a generic " + e.getClass().getSimpleName()
                        + " (\"" + e.getMessage() + "\"); the only choice is one catch-all reaction");
            }
            try {
                compliantWithdraw(frozen);
            } catch (InsufficientFundsException e) {
                System.out.println("compliant   : InsufficientFundsException -> offer an overdraft");
            } catch (AccountFrozenException e) {
                System.out.println("compliant   : AccountFrozenException -> refer the customer to the bank");
            }
        }
    }
}
