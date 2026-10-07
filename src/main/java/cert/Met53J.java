package cert;

/**
 * MET53-J: Ensure that the clone() method calls super.clone().
 *
 * <p>Owner: Jimmy
 *
 * <p>Recommendation. {@code super.clone()} creates the copy with the same
 * runtime class as the original. A {@code clone()} that builds the copy with
 * {@code new} always returns the base class, so a subclass that inherits it
 * gets back an object of the wrong type.
 */
public class Met53J {

    /** NONCOMPLIANT base: clone() builds the copy with the constructor. */
    static class NoncompliantBase implements Cloneable {
        final String name;

        NoncompliantBase(String name) {
            this.name = name;
        }

        @Override
        public Object clone() {
            return new NoncompliantBase(name);         // violates MET53-J: no super.clone()
        }
    }

    /** Subclass that inherits the noncompliant clone(). */
    static class NoncompliantSub extends NoncompliantBase {
        NoncompliantSub(String name) {
            super(name);
        }
    }

    /** COMPLIANT base: clone() starts from super.clone(). */
    static class CompliantBase implements Cloneable {
        final String name;

        CompliantBase(String name) {
            this.name = name;
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }

    /** Subclass that inherits the compliant clone(). */
    static class CompliantSub extends CompliantBase {
        CompliantSub(String name) {
            super(name);
        }
    }

    /**
     * Clones one subclass object of each kind.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== MET53-J: Ensure that the clone() method calls super.clone() ===");

        System.out.println("\n-- Scenario 1: clone a subclass object --");
        Object badCopy = new NoncompliantSub("x").clone();
        System.out.println("noncompliant: copy is a " + badCopy.getClass().getSimpleName());
        try {
            NoncompliantSub s = (NoncompliantSub) badCopy;
            System.out.println("noncompliant: cast ok " + s.name);
        } catch (ClassCastException e) {
            System.out.println("noncompliant: cast to NoncompliantSub FAILED (ClassCastException)");
        }
        Object goodCopy = new CompliantSub("x").clone();
        System.out.println("compliant   : copy is a " + goodCopy.getClass().getSimpleName());
        CompliantSub s = (CompliantSub) goodCopy;
        System.out.println("compliant   : cast ok " + s.name);
    }
}
