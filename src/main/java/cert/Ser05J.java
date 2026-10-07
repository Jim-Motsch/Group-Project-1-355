package cert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * SER05-J: Do not serialize instances of inner classes.
 *
 * <p>Owner: Jimmy
 *
 * <p>A non-static inner class carries a hidden reference to the instance of
 * the enclosing class that created it. Serializing the inner object therefore
 * tries to serialize the outer object too. That fails if the outer class is not
 * serializable, and if it is serializable it silently drags the outer object's
 * fields (possibly secrets) into the stream. The layout of the compiler-made
 * hidden field is also not a stable format. A {@code static} nested class has
 * no such reference and serializes only its own data.
 */
public class Ser05J {

    /** Enclosing class that is NOT serializable. */
    static class PlainOuter {
        /** NONCOMPLIANT: inner (non-static) class that is serializable. */
        class Inner implements Serializable { // violates SER05-J
            private static final long serialVersionUID = 1L;
            final String payload;

            Inner(String payload) {
                this.payload = payload;
            }
        }

        /** COMPLIANT: static nested class, no hidden outer reference. */
        static class Nested implements Serializable {
            private static final long serialVersionUID = 1L;
            final String payload;

            Nested(String payload) {
                this.payload = payload;
            }
        }
    }

    /** Enclosing class that IS serializable and holds a secret. */
    static class LeakyOuter implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String apiKey = "s3cret-key";

        /**
         * Returns the secret for legitimate use by the owning class.
         *
         * @return the API key
         */
        String apiKey() {
            return apiKey;
        }

        /** NONCOMPLIANT: serializing this also serializes {@code apiKey}. */
        class Inner implements Serializable { // violates SER05-J
            private static final long serialVersionUID = 1L;
            final String payload;

            Inner(String payload) {
                this.payload = payload;
            }
        }

        /** COMPLIANT: only its own field is serialized. */
        static class Nested implements Serializable {
            private static final long serialVersionUID = 1L;
            final String payload;

            Nested(String payload) {
                this.payload = payload;
            }
        }
    }

    private static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(o);
        }
        return bytes.toByteArray();
    }

    private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return in.readObject();
        }
    }

    private static String attempt(Object o) throws ClassNotFoundException {
        try {
            serialize(o);
            return "serialized OK";
        } catch (IOException e) {
            return "FAILED " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    /**
     * Serializes inner and static nested instances of both outer classes.
     *
     * @param args unused
     * @throws Exception on unexpected serialization failure
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== SER05-J: Do not serialize instances of inner classes ===");

        System.out.println("\n-- Scenario 1: enclosing class is not serializable --");
        PlainOuter plain = new PlainOuter();
        System.out.println("noncompliant (inner)  : " + attempt(plain.new Inner("hello")));
        System.out.println("compliant    (static) : " + attempt(new PlainOuter.Nested("hello")));
        System.out.println("-> the inner object cannot be serialized: it drags its non-serializable outer along.");

        System.out.println("\n-- Scenario 2: enclosing class is serializable and holds the secret \"s3cret-key\" --");
        LeakyOuter leaky = new LeakyOuter();
        byte[] innerBytes = serialize(leaky.new Inner("hello"));
        byte[] nestedBytes = serialize(new LeakyOuter.Nested("hello"));
        System.out.println("noncompliant (inner)  : stream contains the secret = "
                + new String(innerBytes, StandardCharsets.ISO_8859_1).contains("s3cret-key"));
        System.out.println("compliant    (static) : stream contains the secret = "
                + new String(nestedBytes, StandardCharsets.ISO_8859_1).contains("s3cret-key"));
        System.out.println("-> serializing the inner object leaked a field of the outer object.");

        System.out.println("\n-- Scenario 3 (CONTROL CASE): the payload field itself; identical output is expected --");
        System.out.println("noncompliant payload after round trip: "
                + ((LeakyOuter.Inner) deserialize(innerBytes)).payload);
        System.out.println("compliant    payload after round trip: "
                + ((LeakyOuter.Nested) deserialize(nestedBytes)).payload);
    }
}
