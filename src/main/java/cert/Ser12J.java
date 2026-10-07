package cert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * SER12-J: Prevent deserialization of untrusted data.
 *
 * <p>Owner: Alex
 *
 * <p>Deserializing creates objects of whatever classes the byte stream names and
 * runs their {@code readObject} code, before the caller has any chance to look
 * at the result. If the bytes come from an attacker, they choose which of the
 * program's classes get instantiated. The defense in Java 9+ is an
 * {@link ObjectInputFilter} allow-list: only the listed classes may be read,
 * everything else is rejected before any object is built.
 *
 * <p>SAFETY: the "malicious" class here only sets a flag and prints one line.
 * It is not an exploit chain. The bytes are created in memory by this demo.
 */
public class Ser12J {

    /** Set to true by {@link Gadget#readObject}; proves that foreign code ran during deserialization. */
    static boolean sideEffectRan;

    /** The one class this application actually expects to receive. */
    static class SafeNote implements Serializable {
        private static final long serialVersionUID = 1L;
        final String text;

        SafeNote(String text) {
            this.text = text;
        }
    }

    /** A class the application never expects to receive; its readObject has a (harmless) side effect. */
    static class Gadget implements Serializable {
        private static final long serialVersionUID = 1L;

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            sideEffectRan = true;
            System.out.println("   [Gadget.readObject ran: attacker-chosen class executed during deserialization]");
        }
    }

    private static byte[] bytesOf(Serializable o) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(o);
        }
        return bos.toByteArray();
    }

    /**
     * NONCOMPLIANT: deserializes whatever it is given.
     *
     * @param untrusted the received bytes
     * @return the deserialized object
     */
    static Object noncompliant(byte[] untrusted) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(untrusted))) {
            return in.readObject();     // violates SER12-J: any class in the stream is instantiated
        }
    }

    /**
     * COMPLIANT: installs an allow-list filter before reading. Only
     * {@code SafeNote} and {@code String} may appear; {@code !*} rejects all else.
     *
     * @param untrusted the received bytes
     * @return the deserialized object
     */
    static Object compliant(byte[] untrusted) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(untrusted))) {
            in.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                    "cert.Ser12J$SafeNote;java.lang.String;!*"));
            return in.readObject();
        }
    }

    private static void run(String label, java.util.concurrent.Callable<Object> call) {
        sideEffectRan = false;
        String result;
        try {
            Object o = call.call();
            result = "accepted " + (o instanceof SafeNote ? "SafeNote(\"" + ((SafeNote) o).text + "\")" : o.getClass().getName());
        } catch (InvalidClassException e) {
            result = "REJECTED by filter (" + e.getMessage() + ")";
        } catch (Exception e) {
            result = "failed: " + e;
        }
        System.out.println(label + result + "; foreign code ran = " + sideEffectRan);
    }

    /**
     * Feeds a harmless gadget and a legitimate note to both versions.
     *
     * @param args unused
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== SER12-J: Prevent deserialization of untrusted data ===");

        byte[] attack = bytesOf(new Gadget());
        byte[] honest = bytesOf(new SafeNote("hello"));

        System.out.println("\n-- Scenario 1: untrusted bytes contain a Gadget the application never expects --");
        run("noncompliant: ", () -> noncompliant(attack));
        run("compliant   : ", () -> compliant(attack));
        System.out.println("-> noncompliant let the stream pick the class and ran its code; the allow-list refused it before any object was built.");

        System.out.println("\n-- Scenario 2 (CONTROL CASE): the expected SafeNote; identical output is expected --");
        run("noncompliant: ", () -> noncompliant(honest));
        run("compliant   : ", () -> compliant(honest));
    }
}
