package cert;

import java.util.ArrayList;
import java.util.List;

/**
 * MET06-J: Do not invoke overridable methods in clone().
 *
 * <p>Owner: Jimmy
 *
 * <p>If {@code clone()} calls a method that a subclass can override, that
 * subclass code runs while the clone is only half built: {@code super.clone()}
 * has copied the fields shallowly, so the clone still shares its mutable
 * fields with the original. The overriding method can then modify the
 * original's data through the shared reference. A {@code clone()} that makes
 * its deep copies first and calls nothing overridable avoids this.
 */
public class Met06J {

    /** NONCOMPLIANT base class: clone() calls an overridable hook before the deep copy. */
    static class NoncompliantBase implements Cloneable {
        List<String> items = new ArrayList<>(List.of("a"));

        @Override
        public NoncompliantBase clone() {
            try {
                NoncompliantBase copy = (NoncompliantBase) super.clone();
                copy.onClone(); // violates MET06-J: overridable, and copy.items is still shared
                copy.items = new ArrayList<>(items);
                return copy;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }

        /** Hook for subclasses; overridable. */
        protected void onClone() {
        }
    }

    /** Subclass that uses the hook to tag its copies. */
    static class NoncompliantSub extends NoncompliantBase {
        @Override
        protected void onClone() {
            items.add("cloned"); // runs while 'items' is still the ORIGINAL's list
        }
    }

    /** COMPLIANT base class: clone() makes the deep copy itself and calls nothing overridable. */
    static class CompliantBase implements Cloneable {
        List<String> items = new ArrayList<>(List.of("a"));

        @Override
        public CompliantBase clone() {
            try {
                CompliantBase copy = (CompliantBase) super.clone();
                copy.items = new ArrayList<>(items);
                return copy;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }

    /** Compliant subclass: customizes the copy by overriding clone() itself, after the base copy is complete. */
    static class CompliantSub extends CompliantBase {
        @Override
        public CompliantSub clone() {
            CompliantSub copy = (CompliantSub) super.clone();
            copy.items.add("cloned"); // 'items' is already the copy's own list
            return copy;
        }
    }

    /**
     * Clones one object of each kind and shows what happened to the originals.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== MET06-J: Do not invoke overridable methods in clone() ===");

        System.out.println("\n-- Scenario 1: clone a subclass that tags its copies with \"cloned\" --");
        NoncompliantSub badOriginal = new NoncompliantSub();
        NoncompliantBase badCopy = badOriginal.clone();
        System.out.println("noncompliant: original = " + badOriginal.items + "   copy = " + badCopy.items);

        CompliantSub goodOriginal = new CompliantSub();
        CompliantBase goodCopy = goodOriginal.clone();
        System.out.println("compliant   : original = " + goodOriginal.items + "   copy = " + goodCopy.items);
        System.out.println("-> noncompliant clone() modified the ORIGINAL through the overridable hook.");

        System.out.println("\n-- Scenario 2 (CONTROL CASE): clone a base object; identical output is expected --");
        NoncompliantBase b1 = new NoncompliantBase();
        CompliantBase b2 = new CompliantBase();
        System.out.println("noncompliant: original = " + b1.items + "   copy = " + b1.clone().items);
        System.out.println("compliant   : original = " + b2.items + "   copy = " + b2.clone().items);
    }
}
