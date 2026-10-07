package cert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * IDS07-J: Sanitize untrusted data passed to the Runtime.exec() method.
 *
 * <p>Owner: Ian
 *
 * <p>When untrusted text is pasted into a command line that a shell
 * ({@code cmd /c} on Windows, {@code sh -c} elsewhere) interprets, characters
 * such as {@code &}, {@code |} and {@code ;} end the intended command and start
 * a second one chosen by the attacker. The fix is to accept only input that
 * matches a strict whitelist before it gets anywhere near the command, and to
 * pass the command to the operating system as separate arguments.
 *
 * <p>SAFETY: the "malicious" input in this demo only makes the shell run one
 * extra {@code echo}. It touches no files and needs no special rights.
 */
public class Ids07J {

    /** Whitelist: letters, digits, dot, underscore and hyphen only (no shell metacharacters). */
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{1,32}");

    private static final boolean WINDOWS =
            System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");

    /**
     * Starts the shell and returns everything it printed, one line per line.
     *
     * @param p the started process
     * @return its combined output with line breaks normalized
     */
    private static String capture(Process p) throws IOException, InterruptedException {
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        p.waitFor();
        return out.trim().replace("\r\n", "\n");
    }

    /**
     * NONCOMPLIANT: pastes the untrusted text straight into the command line
     * that the shell will interpret.
     *
     * @param text untrusted text the user wants echoed
     * @return the output of the command
     */
    static String noncompliant(String text) throws IOException, InterruptedException {
        Process p = WINDOWS
                ? Runtime.getRuntime().exec("cmd /c echo " + text)           // violates IDS07-J
                : Runtime.getRuntime().exec(new String[] {"sh", "-c", "echo " + text}); // violates IDS07-J
        return capture(p);
    }

    /**
     * COMPLIANT: only whitelisted text is allowed through, and the command is
     * handed over as an argument array rather than one concatenated string.
     *
     * @param text untrusted text the user wants echoed
     * @return the output of the command
     * @throws IllegalArgumentException if the text is not on the whitelist
     */
    static String compliant(String text) throws IOException, InterruptedException {
        if (text == null || !SAFE.matcher(text).matches()) {
            throw new IllegalArgumentException("input rejected by whitelist: " + text);
        }
        String[] cmd = WINDOWS
                ? new String[] {"cmd", "/c", "echo", text}
                : new String[] {"sh", "-c", "echo \"$1\"", "sh", text};
        return capture(Runtime.getRuntime().exec(cmd));
    }

    /**
     * Runs one input through both versions and prints what each did.
     *
     * @param label description of the scenario
     * @param input the untrusted text
     */
    private static void show(String label, String input) throws Exception {
        System.out.println("input: [" + input + "]  " + label);
        System.out.println("noncompliant output:\n  " + noncompliant(input).replace("\n", "\n  "));
        try {
            System.out.println("compliant output:\n  " + compliant(input).replace("\n", "\n  "));
        } catch (IllegalArgumentException e) {
            System.out.println("compliant output:\n  REJECTED (" + e.getMessage() + ")");
        }
    }

    /**
     * Demonstrates the injection and the whitelist defense.
     *
     * @param args unused
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== IDS07-J: Sanitize untrusted data passed to the Runtime.exec() method ===");
        System.out.println("(shell used on this machine: " + (WINDOWS ? "cmd /c" : "sh -c") + ")");

        System.out.println("\n-- Scenario 1: injected second command (harmless echo) --");
        show("<- tries to run a second 'echo INJECTED'", "hello & echo INJECTED");
        System.out.println("-> noncompliant ran the attacker's extra command (INJECTED printed on its own line);"
                + "\n   compliant refused the input before any process was started.");

        System.out.println("\n-- Scenario 2 (CONTROL CASE): ordinary input; identical output is expected --");
        show("", "report.txt");
    }
}
