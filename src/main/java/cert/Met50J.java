package cert;

import java.util.ArrayList;
import java.util.List;

/**
 * MET50-J: Avoid ambiguous or confusing uses of overloading.
 *
 * <p>Owner: Jimmy
 *
 * <p>Recommendation. When two overloads accept arguments that look
 * interchangeable (here {@code int} and {@code Integer}), the call site does not
 * show which one runs, and a reader can pick the wrong one. Give methods with
 * different meanings different names.
 */
public class Met50J {

    /** NONCOMPLIANT: remove(int) means "by position", remove(Integer) means "by id". */
    static class NoncompliantRegistry {
        final List<Integer> ids = new ArrayList<>(List.of(2, 5, 7));

        void remove(int position) {          // violates MET50-J: confusable overloads
            ids.remove(position);
        }

        void remove(Integer id) {
            ids.remove(id);
        }
    }

    /** COMPLIANT: the two operations have two names. */
    static class CompliantRegistry {
        final List<Integer> ids = new ArrayList<>(List.of(2, 5, 7));

        void removePosition(int position) {
            ids.remove(position);
        }

        void removeId(int id) {
            ids.remove(Integer.valueOf(id));
        }
    }

    /**
     * Tries to remove the id 2 from {2, 5, 7}.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== MET50-J: Avoid ambiguous or confusing uses of overloading ===");

        System.out.println("\n-- Scenario 1: the caller wants to remove the ID 2 from [2, 5, 7] --");
        NoncompliantRegistry bad = new NoncompliantRegistry();
        bad.remove(2);                       // looks like "remove id 2"; actually picks remove(int position)
        System.out.println("noncompliant: remove(2) -> " + bad.ids + "  (removed position 2, i.e. the id 7)");
        CompliantRegistry good = new CompliantRegistry();
        good.removeId(2);
        System.out.println("compliant   : removeId(2) -> " + good.ids);
    }
}
