package cert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ERR01-J: Do not allow exceptions to expose sensitive information.
 *
 * <p>Owner: Jimmy
 *
 * <p>Exception messages and error responses often contain internal details:
 * file system paths, host names, user names, or hints about which part of a
 * check failed. Passing them straight to the user helps an attacker map the
 * system. The compliant approach records the full detail on the server side
 * and gives the user a short, generic message that is the same for every
 * failure cause.
 */
public class Err01J {

    /** Internal location of account files. Callers should never learn this path. */
    private static final Path VAULT = Path.of("/internal/vault/prod-db/accounts");

    /** Server-side log, standing in for a real logger. Never shown to the user. */
    private static final List<String> internalLog = new ArrayList<>();

    /** Fake user store for the login scenario. */
    private static final Map<String, String> USERS = Map.of("alice", "correct-horse");

    /**
     * NONCOMPLIANT: returns the exception text to the caller, which includes
     * the internal file path.
     *
     * @param id account id requested by the user
     * @return the account data or an error message
     */
    static String noncompliantLoad(String id) {
        try {
            return Files.readString(VAULT.resolve(id + ".dat"));
        } catch (IOException e) {
            return "Error: " + e; // violates ERR01-J
        }
    }

    /**
     * COMPLIANT: logs the detail internally and returns a generic message.
     *
     * @param id account id requested by the user
     * @return the account data or a generic error message
     */
    static String compliantLoad(String id) {
        try {
            return Files.readString(VAULT.resolve(id + ".dat"));
        } catch (IOException e) {
            internalLog.add("load failed for id=" + id + ": " + e);
            return "Account unavailable. Please try again later.";
        }
    }

    /**
     * NONCOMPLIANT: the two failure causes produce different messages, which
     * tells an attacker which user names exist.
     *
     * @param user     user name
     * @param password password
     * @return a status message
     */
    static String noncompliantLogin(String user, String password) {
        String stored = USERS.get(user);
        if (stored == null) {
            return "no such user"; // violates ERR01-J
        }
        if (!stored.equals(password)) {
            return "wrong password"; // violates ERR01-J
        }
        return "welcome";
    }

    /**
     * COMPLIANT: every failure gives the same message; the real reason goes to
     * the internal log.
     *
     * @param user     user name
     * @param password password
     * @return a status message
     */
    static String compliantLogin(String user, String password) {
        String stored = USERS.get(user);
        if (stored == null) {
            internalLog.add("login failed: unknown user " + user);
            return "invalid credentials";
        }
        if (!stored.equals(password)) {
            internalLog.add("login failed: bad password for " + user);
            return "invalid credentials";
        }
        return "welcome";
    }

    /**
     * Runs both scenarios and a control case.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== ERR01-J: Do not allow exceptions to expose sensitive information ===");

        System.out.println("\n-- Scenario 1: loading account 42, whose file cannot be opened --");
        System.out.println("noncompliant (user sees): " + noncompliantLoad("42"));
        System.out.println("compliant    (user sees): " + compliantLoad("42"));
        System.out.println("-> noncompliant shows the internal path of the account vault to the user.");

        System.out.println("\n-- Scenario 2: login attempts that fail for different reasons --");
        System.out.println("noncompliant unknown user \"mallory\" : " + noncompliantLogin("mallory", "x"));
        System.out.println("noncompliant wrong password for alice: " + noncompliantLogin("alice", "x"));
        System.out.println("compliant    unknown user \"mallory\" : " + compliantLogin("mallory", "x"));
        System.out.println("compliant    wrong password for alice: " + compliantLogin("alice", "x"));
        System.out.println("-> noncompliant lets an attacker learn which user names exist.");

        System.out.println("\n-- Server-side log (never shown to the user; this is where the detail belongs) --");
        internalLog.forEach(line -> System.out.println("  [internal] " + line));

        System.out.println("\n-- Scenario 3 (CONTROL CASE): a successful login; identical output is expected --");
        System.out.println("noncompliant: " + noncompliantLogin("alice", "correct-horse"));
        System.out.println("compliant   : " + compliantLogin("alice", "correct-horse"));
    }
}
