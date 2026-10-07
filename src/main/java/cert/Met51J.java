package cert;

/**
 * MET51-J: Do not use overloaded methods to differentiate between runtime types.
 *
 * <p>Owner: Ian
 *
 * <p>Recommendation. Java chooses between overloads using the declared
 * (compile-time) type of the argument, not the type of the object it holds at
 * run time. A variable declared as {@code Object} that holds a {@code String}
 * calls the {@code Object} overload. To branch on the real type, use
 * {@code instanceof} (or polymorphism) inside one method.
 */
public class Met51J {

    /** NONCOMPLIANT overloads: the String version is selected only when the declared type is String. */
    static String kind(Object o) {
        return "object";
    }

    static String kind(String s) {
        return "string";
    }

    /** COMPLIANT: one method that inspects the runtime type. */
    static String compliantKind(Object o) {
        if (o instanceof String) {
            return "string";
        }
        return "object";
    }

    /**
     * Passes the same two values, declared as Object, to both versions.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== MET51-J: Do not use overloaded methods to differentiate between runtime types ===");

        Object[] values = {"hello", 42};

        System.out.println("\n-- Scenario 1: values held in an Object[] (declared type Object) --");
        for (Object v : values) {
            System.out.println("value " + v + " (really a " + v.getClass().getSimpleName() + "): noncompliant kind() = "
                    + kind(v) + ", compliant kind() = " + compliantKind(v));
        }
        System.out.println("-> the overload was chosen from the declared type, so \"hello\" was called an object.");
    }
}
