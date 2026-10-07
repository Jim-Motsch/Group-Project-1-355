package cert;

import java.util.HashSet;
import java.util.Set;

/**
 * EXP00-J: Do not ignore values returned by methods.
 *
 * <p>Owner: Martin
 *
 * <p>Many methods report their outcome through the return value: immutable
 * types such as String return a new object instead of changing the receiver,
 * and collection methods report whether they changed anything. Discarding that
 * value means the code carries on as if the call had worked.
 */
public class Exp00J {

    /**
     * NONCOMPLIANT: calls {@code trim()} but throws the result away. Strings
     * are immutable, so {@code raw} is left unchanged.
     *
     * @param raw text that may have surrounding whitespace
     * @return the original, still untrimmed text
     */
    static String noncompliantTrim(String raw) {
        raw.trim(); // violates EXP00-J: returned String is ignored
        return raw;
    }

    /**
     * COMPLIANT: uses the value returned by {@code trim()}.
     *
     * @param raw text that may have surrounding whitespace
     * @return the trimmed text
     */
    static String compliantTrim(String raw) {
        return raw.trim();
    }

    /**
     * NONCOMPLIANT: registers a username with {@code Set.add} but ignores the
     * boolean result, so duplicates go unnoticed and every registration is
     * reported as a success.
     *
     * @param users    the set of already registered names
     * @param username the name to register
     * @return always "registered"
     */
    static String noncompliantRegister(Set<String> users, String username) {
        users.add(username); // violates EXP00-J: boolean result ignored
        return "registered";
    }

    /**
     * COMPLIANT: checks the boolean returned by {@code Set.add} and reports a
     * duplicate.
     *
     * @param users    the set of already registered names
     * @param username the name to register
     * @return "registered" for a new name, "rejected (duplicate)" otherwise
     */
    static String compliantRegister(Set<String> users, String username) {
        if (users.add(username)) {
            return "registered";
        }
        return "rejected (duplicate)";
    }

    /**
     * Runs both scenarios.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== EXP00-J: Do not ignore values returned by methods ===");

        String raw = "   alice   ";
        System.out.println("\n-- Scenario 1: String.trim() on \"   alice   \" --");
        System.out.println("noncompliant : [" + noncompliantTrim(raw) + "]");
        System.out.println("compliant    : [" + compliantTrim(raw) + "]");
        System.out.println("-> noncompliant still has the whitespace; the trimmed copy was discarded.");

        System.out.println("\n-- Scenario 2: registering \"bob\" twice --");
        Set<String> badUsers = new HashSet<>();
        System.out.println("noncompliant 1st: " + noncompliantRegister(badUsers, "bob"));
        System.out.println("noncompliant 2nd: " + noncompliantRegister(badUsers, "bob"));
        Set<String> goodUsers = new HashSet<>();
        System.out.println("compliant    1st: " + compliantRegister(goodUsers, "bob"));
        System.out.println("compliant    2nd: " + compliantRegister(goodUsers, "bob"));
        System.out.println("-> noncompliant accepts the duplicate silently; compliant detects it.");
    }
}
