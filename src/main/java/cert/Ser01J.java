package cert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * SER01-J: Do not deviate from the proper signatures of serialization methods.
 *
 * <p>Owner: James
 *
 * <p>Java serialization finds {@code writeObject}, {@code readObject} and
 * friends by reflection and only uses them when the signature matches exactly
 * (for example {@code private void readObject(ObjectInputStream)}). A method
 * with the right name but a wrong modifier, parameter list or return type is
 * not an error: it compiles and is simply ignored, so the custom logic never
 * runs.
 *
 * <p>In this demo the custom methods store a {@code transient} secret in
 * reversed form. With correct signatures the secret survives the round trip;
 * with deviating signatures it is silently lost.
 */
public class Ser01J {

    /** Counts calls to the custom serialization hooks of the noncompliant class. */
    static int noncompliantHookCalls;
    /** Counts calls to the custom serialization hooks of the compliant class. */
    static int compliantHookCalls;

    /**
     * NONCOMPLIANT: the hooks are {@code public}, so serialization does not
     * recognize them. The transient field is never written.
     */
    static class NoncompliantToken implements Serializable {
        private static final long serialVersionUID = 1L;
        final String user;
        transient String secret;

        NoncompliantToken(String user, String secret) {
            this.user = user;
            this.secret = secret;
        }

        /**
         * Wrong signature: must be {@code private}. Never called by serialization.
         *
         * @param out the stream
         * @throws IOException on write failure
         */
        public void writeObject(ObjectOutputStream out) throws IOException { // violates SER01-J
            noncompliantHookCalls++;
            out.defaultWriteObject();
            out.writeObject(new StringBuilder(secret).reverse().toString());
        }

        /**
         * Wrong signature: must be {@code private}. Never called by serialization.
         *
         * @param in the stream
         * @throws IOException            on read failure
         * @throws ClassNotFoundException if a class is missing
         */
        public void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException { // violates SER01-J
            noncompliantHookCalls++;
            in.defaultReadObject();
            secret = new StringBuilder((String) in.readObject()).reverse().toString();
        }
    }

    /** COMPLIANT: the hooks have the exact signatures serialization looks for. */
    static class CompliantToken implements Serializable {
        private static final long serialVersionUID = 1L;
        final String user;
        transient String secret;

        CompliantToken(String user, String secret) {
            this.user = user;
            this.secret = secret;
        }

        /**
         * Correct signature, called by serialization.
         *
         * @param out the stream
         * @throws IOException on write failure
         */
        private void writeObject(ObjectOutputStream out) throws IOException {
            compliantHookCalls++;
            out.defaultWriteObject();
            out.writeObject(new StringBuilder(secret).reverse().toString());
        }

        /**
         * Correct signature, called by serialization.
         *
         * @param in the stream
         * @throws IOException            on read failure
         * @throws ClassNotFoundException if a class is missing
         */
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            compliantHookCalls++;
            in.defaultReadObject();
            secret = new StringBuilder((String) in.readObject()).reverse().toString();
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

    /**
     * Round-trips one token of each kind and reports what survived.
     *
     * @param args unused
     * @throws Exception on any serialization failure
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== SER01-J: Do not deviate from the proper signatures of serialization methods ===");

        System.out.println("\n-- Scenario 1: round trip of a token whose secret is \"hunter2\" --");
        byte[] badBytes = serialize(new NoncompliantToken("alice", "hunter2"));
        NoncompliantToken bad = (NoncompliantToken) deserialize(badBytes);
        System.out.println("noncompliant: secret after round trip = " + bad.secret
                + "   (hook calls: " + noncompliantHookCalls + ")");

        byte[] goodBytes = serialize(new CompliantToken("alice", "hunter2"));
        CompliantToken good = (CompliantToken) deserialize(goodBytes);
        System.out.println("compliant   : secret after round trip = " + good.secret
                + "   (hook calls: " + compliantHookCalls + ")");
        System.out.println("-> noncompliant silently lost the secret: its public hooks were ignored.");

        System.out.println("\n-- Scenario 2: what ended up in the serialized bytes --");
        String badText = new String(badBytes, StandardCharsets.ISO_8859_1);
        String goodText = new String(goodBytes, StandardCharsets.ISO_8859_1);
        System.out.println("noncompliant stream contains reversed secret \"2retnuh\": " + badText.contains("2retnuh"));
        System.out.println("compliant    stream contains reversed secret \"2retnuh\": " + goodText.contains("2retnuh"));

        System.out.println("\n-- Scenario 3 (CONTROL CASE): the non-transient field; identical output is expected --");
        System.out.println("noncompliant user = " + bad.user);
        System.out.println("compliant    user = " + good.user);
    }
}
