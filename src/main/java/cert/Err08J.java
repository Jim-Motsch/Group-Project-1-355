package cert;

import java.util.Map;
import java.util.function.Supplier;

/**
 * ERR08-J: Do not catch NullPointerException or any of its ancestors.
 *
 * <p>Owner: Martin
 *
 * <p>Catching {@link NullPointerException} (or {@link RuntimeException},
 * {@link Exception}, {@link Throwable}) to cope with a null reference hides the
 * real defect and also swallows unrelated failures thrown from the same
 * block. The fix is to test for null up front and let genuine programming
 * errors propagate.
 *
 * <p>Both scenarios call a helper that contains a real bug unrelated to the
 * null check. The noncompliant version swallows the bug and returns a
 * misleading answer; the compliant version lets the bug surface.
 */
public class Err08J {

    /** Alias table the helper forgot to initialize. This is the "real bug" in scenario 1. */
    private static Map<String, String> aliases;

    /**
     * Helper with a real, unrelated bug: it dereferences {@code aliases}, which
     * is never initialized, so it throws NullPointerException for EVERY input,
     * including non-null ones.
     *
     * @param s a non-null string
     * @return the normalized string (never actually returned because of the bug)
     */
    static String normalize(String s) {
        String alias = aliases.get(s.trim()); // BUG: aliases is null -> NPE
        return alias != null ? alias : s.trim();
    }

    /**
     * Helper with a different real bug (off-by-one): {@code charAt(length())}
     * always throws StringIndexOutOfBoundsException. This is the "real bug" in
     * scenario 2.
     *
     * @param s a non-empty string
     * @return the last character (never actually returned because of the bug)
     */
    static char lastChar(String s) {
        return s.charAt(s.length()); // BUG: should be length() - 1
    }

    /**
     * NONCOMPLIANT: catches NullPointerException to handle a null argument.
     * The NPE thrown from inside {@link #normalize} is swallowed too, so a
     * perfectly valid input is reported as if it were null.
     *
     * @param s the string whose normalized length is wanted (may be null)
     * @return the normalized length, or -1, which the caller reads as "input was null"
     */
    static int noncompliantLength(String s) {
        try {
            return normalize(s).length();
        } catch (NullPointerException e) { // violates ERR08-J
            return -1;
        }
    }

    /**
     * COMPLIANT: checks for null explicitly, outside any try block. The bug
     * inside {@link #normalize} is not caught here, so it surfaces to the caller.
     *
     * @param s the string whose normalized length is wanted (may be null)
     * @return -1 if {@code s} is null, otherwise the normalized length
     */
    static int compliantLength(String s) {
        if (s == null) {
            return -1;
        }
        return normalize(s).length(); // the real bug propagates
    }

    /**
     * NONCOMPLIANT: catches an ancestor of NullPointerException
     * ({@link Exception}). The StringIndexOutOfBoundsException from the
     * off-by-one bug in {@link #lastChar} is swallowed and reported as '?'.
     *
     * @param s the string to inspect (may be null)
     * @return the last character, or '?', which the caller reads as "no character available"
     */
    static char noncompliantLast(String s) {
        try {
            return lastChar(s);
        } catch (Exception e) { // violates ERR08-J (Exception is an ancestor of NPE)
            return '?';
        }
    }

    /**
     * COMPLIANT: validates null and empty input explicitly, then calls the
     * helper without a catch block so the off-by-one bug surfaces.
     *
     * @param s the string to inspect (may be null)
     * @return '?' if {@code s} is null or empty, otherwise the last character
     */
    static char compliantLast(String s) {
        if (s == null || s.isEmpty()) {
            return '?';
        }
        return lastChar(s); // the real bug propagates
    }

    /**
     * Runs {@code action} on a separate thread and reports whether it returned
     * or whether an exception escaped. Using an uncaught-exception handler lets
     * the demo display a propagated bug without the demo itself catching
     * NullPointerException or an ancestor.
     *
     * @param label  text printed before the result
     * @param action the code to run
     */
    private static void report(String label, Supplier<Object> action) {
        Object[] result = new Object[1];
        Throwable[] escaped = new Throwable[1];
        Thread t = new Thread(() -> result[0] = action.get());
        t.setUncaughtExceptionHandler((th, ex) -> escaped[0] = ex);
        t.start();
        try {
            t.join();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        if (escaped[0] != null) {
            System.out.println(label + "BUG SURFACED -> " + escaped[0]);
        } else {
            System.out.println(label + result[0]);
        }
    }

    /**
     * Runs both versions of each scenario and prints the results.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== ERR08-J: Do not catch NullPointerException or any of its ancestors ===");

        System.out.println("\n-- Scenario 1: valid input \"  hi \", but normalize() has an uninitialized-field bug --");
        report("noncompliant (catch NPE)  : ", () -> noncompliantLength("  hi "));
        report("compliant    (null check) : ", () -> compliantLength("  hi "));
        System.out.println("-> noncompliant answers -1 (\"input was null\") for a non-null input: misleading.");

        System.out.println("\n-- Scenario 1b (CONTROL CASE): genuinely null input; identical output is expected --");
        report("noncompliant (catch NPE)  : ", () -> noncompliantLength(null));
        report("compliant    (null check) : ", () -> compliantLength(null));

        System.out.println("\n-- Scenario 2: input \"abc\", but lastChar() has an off-by-one bug --");
        report("noncompliant (catch Exception): ", () -> noncompliantLast("abc"));
        report("compliant    (validate input) : ", () -> compliantLast("abc"));
        System.out.println("-> noncompliant answers '?' (\"no character\") for \"abc\": misleading.");

        System.out.println("\nTakeaway: catching NPE or an ancestor turned two real bugs into plausible-looking");
        System.out.println("answers. Explicit checks handle expected nulls and let real bugs surface.");
    }
}
