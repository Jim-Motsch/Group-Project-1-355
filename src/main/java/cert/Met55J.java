package cert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MET55-J: Return an empty array or collection instead of a null value for methods
 * that return an array or collection.
 *
 * <p>Owner: James
 *
 * <p>Recommendation. Callers normally loop over a returned collection. If "no
 * results" is signalled by {@code null}, every caller must remember a special
 * check, and the first one that forgets gets a {@code NullPointerException}.
 * An empty collection needs no special case.
 */
public class Met55J {

    private static final List<String> NAMES = List.of("alice", "bob", "anna");

    /** NONCOMPLIANT: returns null when nothing matches. */
    static List<String> noncompliantFind(String prefix) {
        List<String> found = new ArrayList<>();
        for (String n : NAMES) {
            if (n.startsWith(prefix)) {
                found.add(n);
            }
        }
        return found.isEmpty() ? null : found;      // violates MET55-J
    }

    /** COMPLIANT: returns an empty list when nothing matches. */
    static List<String> compliantFind(String prefix) {
        List<String> found = new ArrayList<>();
        for (String n : NAMES) {
            if (n.startsWith(prefix)) {
                found.add(n);
            }
        }
        return Collections.unmodifiableList(found);
    }

    /**
     * Loops over the result of each version, without any null check, for two prefixes.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== MET55-J: Return an empty array or collection instead of a null value ===");

        System.out.println("\n-- Scenario 1: search for \"z\" (no match), caller just loops over the result --");
        try {
            StringBuilder names = new StringBuilder();
            for (String n : noncompliantFind("z")) {
                names.append(n).append(' ');
            }
            System.out.println("noncompliant: results = [" + names.toString().trim() + "]");
        } catch (NullPointerException e) {
            System.out.println("noncompliant: NullPointerException (caller forgot the null check)");
        }
        StringBuilder names = new StringBuilder();
        for (String n : compliantFind("z")) {
            names.append(n).append(' ');
        }
        System.out.println("compliant   : results = [" + names.toString().trim() + "]  (loop simply ran zero times)");

        System.out.println("\n-- Scenario 2 (CONTROL CASE): search for \"a\"; identical output is expected --");
        System.out.println("noncompliant: " + noncompliantFind("a"));
        System.out.println("compliant   : " + compliantFind("a"));
    }
}
