package cert;

/**
 * MET05-J: Ensure that constructors do not call overridable methods.
 *
 * <p>Owner: Jimmy
 *
 * <p>A superclass constructor runs before the subclass's own constructor body
 * and field setup. If it calls a method the subclass overrides, that override
 * executes on an object whose subclass fields still hold default values
 * (null, 0, false). The fix is to have the constructor call only
 * {@code private}, {@code static} or {@code final} methods, or to pass the
 * needed data up through the constructor.
 */
public class Met05J {

    /** NONCOMPLIANT base class: constructor calls an overridable method. */
    static class NoncompliantBase {
        final String label;

        NoncompliantBase() {
            label = makeLabel(); // violates MET05-J
        }

        /** Overridable. */
        String makeLabel() {
            return "base";
        }
    }

    /** Subclass whose override depends on a field that is not set yet when the super constructor runs. */
    static class NoncompliantSub extends NoncompliantBase {
        private final String prefix;

        NoncompliantSub(String prefix) {
            super();
            this.prefix = prefix; // assigned AFTER the super constructor has already called makeLabel()
        }

        @Override
        String makeLabel() {
            return prefix + "!";
        }
    }

    /** COMPLIANT base class: receives the label through its constructor, calls nothing overridable. */
    static class CompliantBase {
        final String label;

        CompliantBase(String label) {
            this.label = label;
        }
    }

    /** Compliant subclass: computes the label itself and passes it up. */
    static class CompliantSub extends CompliantBase {
        CompliantSub(String prefix) {
            super(prefix + "!");
        }
    }

    /**
     * Builds one object of each kind and prints the resulting label.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== MET05-J: Ensure that constructors do not call overridable methods ===");

        System.out.println("\n-- Scenario 1: subclass built with prefix \"Sub\", expected label \"Sub!\" --");
        System.out.println("noncompliant label: " + new NoncompliantSub("Sub").label);
        System.out.println("compliant    label: " + new CompliantSub("Sub").label);
        System.out.println("-> the override ran before 'prefix' was assigned, so the noncompliant label is \"null!\".");

        System.out.println("\n-- Scenario 2 (CONTROL CASE): plain base objects; identical output is expected --");
        System.out.println("noncompliant label: " + new NoncompliantBase().label);
        System.out.println("compliant    label: " + new CompliantBase("base").label);
    }
}
