package cert;

import java.util.Arrays;

/**
 * ERR54-J: Use a try-with-resources statement to safely handle closeable resources.
 *
 * <p>Owner: Jimmy
 *
 * <p>Recommendation. In a {@code try/finally} that closes a resource, an
 * exception thrown by {@code close()} replaces the exception thrown by the body,
 * so the real failure is lost. try-with-resources keeps the body's exception as
 * the main one and attaches the close failure to it as a "suppressed" exception.
 */
public class Err54J {

    /** A resource whose close() always fails. */
    static class Resource implements AutoCloseable {
        @Override
        public void close() {
            throw new IllegalStateException("close failed");
        }
    }

    /** NONCOMPLIANT: manual try/finally. */
    static void noncompliant() {
        Resource r = new Resource();
        try {
            throw new IllegalArgumentException("body failed");
        } finally {
            r.close();                                  // violates ERR54-J: its exception replaces "body failed"
        }
    }

    /** COMPLIANT: try-with-resources. */
    static void compliant() {
        try (Resource r = new Resource()) {
            throw new IllegalArgumentException("body failed");
        }
    }

    /**
     * Shows which exception reaches the caller in each version.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== ERR54-J: Use a try-with-resources statement to safely handle closeable resources ===");

        System.out.println("\n-- Scenario 1: both the body and close() throw --");
        try {
            noncompliant();
        } catch (RuntimeException e) {
            System.out.println("noncompliant: caller sees \"" + e.getMessage() + "\", suppressed = "
                    + Arrays.toString(e.getSuppressed()) + "   <-- the real failure is lost");
        }
        try {
            compliant();
        } catch (RuntimeException e) {
            System.out.println("compliant   : caller sees \"" + e.getMessage() + "\", suppressed = "
                    + Arrays.toString(e.getSuppressed()));
        }
    }
}
