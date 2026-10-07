package cert;

import java.util.ArrayList;
import java.util.List;

/**
 * OBJ50-J: Never confuse the immutability of a reference with that of the referenced object.
 *
 * <p>Owner: Ian
 *
 * <p>Recommendation. {@code final} means the variable cannot be pointed at a
 * different object. It does not make the object itself unchangeable. A
 * {@code final List} can still be added to; use an immutable object where the
 * contents must not change.
 */
public class Obj50J {

    /** NONCOMPLIANT: the reference is final, the list it points to is not. */
    static class NoncompliantPermissions {
        final List<String> allowed = new ArrayList<>(List.of("read"));   // violates OBJ50-J
    }

    /** COMPLIANT: the reference is final and the list is immutable. */
    static class CompliantPermissions {
        final List<String> allowed = List.of("read");
    }

    /**
     * Tries to add "admin" to both "final" lists.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== OBJ50-J: Never confuse the immutability of a reference with that of the referenced object ===");

        System.out.println("\n-- Scenario 1: client code calls allowed.add(\"admin\") --");
        NoncompliantPermissions bad = new NoncompliantPermissions();
        bad.allowed.add("admin");
        System.out.println("noncompliant: allowed = " + bad.allowed + "   <-- 'final' did not stop the change");
        CompliantPermissions good = new CompliantPermissions();
        try {
            good.allowed.add("admin");
            System.out.println("compliant   : allowed = " + good.allowed);
        } catch (UnsupportedOperationException e) {
            System.out.println("compliant   : allowed = " + good.allowed + "   (add rejected: UnsupportedOperationException)");
        }
    }
}
