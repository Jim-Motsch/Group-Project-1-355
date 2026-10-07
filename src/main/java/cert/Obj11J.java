package cert;

/**
 * OBJ11-J: Be wary of letting constructors throw exceptions.
 *
 * <p>Owner: Ian
 *
 * <p>When a constructor throws, the half-built object still exists until the
 * garbage collector removes it. A malicious subclass can override
 * {@code finalize()} to grab {@code this} from that dying object (a "finalizer
 * attack") and then call its methods, even though the constructor's security
 * check failed. The defense used here is an {@code initialized} flag that is
 * set as the very last step of the constructor and checked by every method.
 *
 * <p>TIMING: whether and when the finalizer runs is up to the JVM. The demo
 * requests garbage collection repeatedly for up to about 3 seconds and says
 * plainly if the attacker never got the object. {@code finalize()} is
 * deprecated in Java 17, so compiling gives a deprecation warning (as in Met12J).
 */
public class Obj11J {

    /** Where the attacker's finalizer stores the object it rescued. */
    private static volatile Object rescued;

    /** NONCOMPLIANT: the security check throws, but methods never check that the constructor finished. */
    static class NoncompliantVault {
        NoncompliantVault(String password) {
            if (!"correct".equals(password)) {
                throw new SecurityException("wrong password");
            }
        }

        String openSafe() {
            return "SAFE OPENED";
        }
    }

    /** Attacker subclass: its finalizer saves the half-built object. */
    static class NoncompliantAttacker extends NoncompliantVault {
        NoncompliantAttacker() {
            super("guess");
        }

        @Override
        protected void finalize() {
            rescued = this;
        }
    }

    /** COMPLIANT: an 'initialized' flag, set last, is checked before any action. */
    static class CompliantVault {
        private volatile boolean initialized;

        CompliantVault(String password) {
            if (!"correct".equals(password)) {
                throw new SecurityException("wrong password");
            }
            this.initialized = true; // last statement: only reached if the check passed
        }

        String openSafe() {
            if (!initialized) {
                throw new SecurityException("object was never fully constructed");
            }
            return "SAFE OPENED";
        }
    }

    /** Attacker subclass for the compliant vault: same trick. */
    static class CompliantAttacker extends CompliantVault {
        CompliantAttacker() {
            super("guess");
        }

        @Override
        protected void finalize() {
            rescued = this;
        }
    }

    /** Builds the attacker (its constructor fails) and waits for GC to run its finalizer. */
    private static Object attack(Runnable construct) throws InterruptedException {
        rescued = null;
        try {
            construct.run();
        } catch (SecurityException expected) {
            // constructor failed as designed; the half-built object is now garbage
        }
        for (int i = 0; i < 60 && rescued == null; i++) {
            System.gc();
            Thread.sleep(50);
        }
        return rescued;
    }

    /**
     * Runs the finalizer attack against both vaults.
     *
     * @param args unused
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== OBJ11-J: Be wary of letting constructors throw exceptions ===");

        System.out.println("\n-- Scenario 1 (CONTROL CASE): correct password; identical output is expected --");
        System.out.println("noncompliant: " + new NoncompliantVault("correct").openSafe());
        System.out.println("compliant   : " + new CompliantVault("correct").openSafe());

        System.out.println("\n-- Scenario 2: wrong password, then a finalizer attack on the failed object --");
        Object stolen = attack(() -> new NoncompliantAttacker());
        if (stolen == null) {
            System.out.println("noncompliant: attacker did NOT get the object (GC did not finalize it in time; VARIES)");
        } else {
            System.out.println("noncompliant: attacker rescued the half-built object, openSafe() -> "
                    + ((NoncompliantVault) stolen).openSafe() + "  <-- password check bypassed");
        }

        stolen = attack(() -> new CompliantAttacker());
        if (stolen == null) {
            System.out.println("compliant   : attacker did NOT get the object (GC did not finalize it in time; VARIES)");
        } else {
            try {
                System.out.println("compliant   : openSafe() -> " + ((CompliantVault) stolen).openSafe());
            } catch (SecurityException e) {
                System.out.println("compliant   : attacker rescued the object but openSafe() threw: " + e.getMessage());
            }
        }
        System.out.println("-> a constructor that throws does not make the object disappear; checking the flag does.");
    }
}
