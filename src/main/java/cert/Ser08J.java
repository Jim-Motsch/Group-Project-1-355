package cert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;

/**
 * SER08-J: Minimize privileges before deserializing from a privileged context.
 *
 * <p>Owner: James
 *
 * <p>Deserializing runs {@code readObject} methods of whatever classes the
 * stream names, driven by bytes the caller may not control. If the caller is
 * running with broad privileges (for example inside {@code doPrivileged}),
 * those methods inherit them, and a crafted stream can make a trusted class do
 * something harmful. The fix is to deserialize inside a
 * {@code doPrivileged} call that carries a restricted
 * {@link AccessControlContext}, so the work runs with the smallest permission
 * set that is enough.
 *
 * <p><b>Java 17 note:</b> the Security Manager and {@code AccessController}
 * are deprecated for removal (JEP 411). JDK 17 still lets a program install a
 * security manager at runtime (the default of {@code java.security.manager}
 * is {@code allow}, with a warning on stderr). JDK 18 to 23 require
 * {@code -Djava.security.manager=allow}, and on JDK 24 and later it is
 * permanently disabled. If the JVM refuses, the demo says what it cannot show
 * and stops.
 */
@SuppressWarnings("removal")
public class Ser08J {

    /**
     * A trusted class whose {@code readObject} does a privileged-looking
     * action (writing a file) based on a field from the stream. This stands in
     * for a "gadget": legitimate code that becomes dangerous when driven by
     * attacker-chosen data.
     */
    static class Gadget implements Serializable {
        private static final long serialVersionUID = 1L;
        final String targetFile;

        Gadget(String targetFile) {
            this.targetFile = targetFile;
        }

        /**
         * Writes a file named by the stream during deserialization.
         *
         * @param in the stream
         * @throws IOException            on read or write failure
         * @throws ClassNotFoundException if a class is missing
         */
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            Files.writeString(Path.of(targetFile), "written during deserialization");
        }
    }

    /** Policy that gives every normally-loaded code base all permissions. */
    static class TrustingPolicy extends Policy {
        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            return true;
        }
    }

    private static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(o);
        }
        return bytes.toByteArray();
    }

    /**
     * NONCOMPLIANT: deserializes inside a plain {@code doPrivileged} block, so
     * {@code readObject} runs with all of this code's permissions.
     *
     * @param data the serialized bytes
     * @return the deserialized object
     * @throws Exception on failure
     */
    static Object noncompliantDeserialize(byte[] data) throws Exception {
        PrivilegedExceptionAction<Object> action = () -> {
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
                return in.readObject();
            }
        };
        return AccessController.doPrivileged(action); // violates SER08-J: full privileges
    }

    /**
     * COMPLIANT: deserializes inside {@code doPrivileged} with an
     * {@link AccessControlContext} whose only protection domain holds no
     * permissions, so the work runs with (almost) none.
     *
     * @param data the serialized bytes
     * @return the deserialized object
     * @throws Exception on failure
     */
    static Object compliantDeserialize(byte[] data) throws Exception {
        PermissionCollection none = new Permissions(); // grant nothing
        AccessControlContext restricted = new AccessControlContext(
                new ProtectionDomain[] {new ProtectionDomain(null, none)});
        PrivilegedExceptionAction<Object> action = () -> {
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
                return in.readObject();
            }
        };
        return AccessController.doPrivileged(action, restricted);
    }

    /**
     * Installs a security manager and deserializes the same bytes both ways.
     *
     * @param args unused
     * @throws Exception on setup failure
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== SER08-J: Minimize privileges before deserializing from a privileged context ===");

        Path dir = Files.createTempDirectory("ser08");
        Path badTarget = dir.resolve("noncompliant-output.txt");
        Path goodTarget = dir.resolve("compliant-output.txt");
        byte[] badBytes = serialize(new Gadget(badTarget.toString()));
        byte[] goodBytes = serialize(new Gadget(goodTarget.toString()));

        Policy oldPolicy = Policy.getPolicy();
        Policy.setPolicy(new TrustingPolicy());
        try {
            System.setSecurityManager(new SecurityManager());
        } catch (UnsupportedOperationException e) {
            Policy.setPolicy(oldPolicy);
            System.out.println("\n[CANNOT SHOW ON THIS RUN] this JVM refuses to install a security manager.");
            System.out.println("Start it with -Djava.security.manager=allow (needed on JDK 18-23; impossible on JDK 24+).");
            System.out.println("Without a security manager there are no privileges to minimize, so no result");
            System.out.println("is shown and none is faked.");
            cleanup(dir);
            return;
        }

        String badResult;
        String goodResult;
        try {
            System.out.println("[security manager installed at runtime; Java 17 prints a deprecation warning on stderr]");
            badResult = run(true, badBytes, badTarget);
            goodResult = run(false, goodBytes, goodTarget);
        } finally {
            System.setSecurityManager(null);
            Policy.setPolicy(oldPolicy);
        }

        System.out.println("\n-- Scenario: the stream asks a trusted class to write a file while deserializing --");
        System.out.println("noncompliant (full privileges)   : " + badResult);
        System.out.println("compliant    (restricted context): " + goodResult);
        System.out.println("-> the same bytes did real work with full privileges and were stopped when the");
        System.out.println("   deserialization ran with a minimal permission set.");
        System.out.println("\nWhat this demo cannot show on this JDK: a policy file / signed-code setup as used in real");
        System.out.println("deployments, and any of it on JDK 24+ where the Security Manager can no longer be enabled.");

        cleanup(dir);
    }

    private static String run(boolean noncompliant, byte[] data, Path target) throws Exception {
        try {
            if (noncompliant) {
                noncompliantDeserialize(data);
            } else {
                compliantDeserialize(data);
            }
            return "deserialized; file written = " + Files.exists(target) + "  <-- gadget ran with full privileges";
        } catch (AccessControlException e) {
            Permission p = e.getPermission();
            return "BLOCKED by " + e.getClass().getSimpleName() + " (" + p.getClass().getSimpleName()
                    + " '" + p.getActions() + "'); file written = " + Files.exists(target);
        } catch (IOException e) {
            // Files.writeString wraps nothing; an AccessControlException surfaces directly above.
            return "I/O failure: " + e.getClass().getSimpleName();
        }
    }

    private static void cleanup(Path dir) throws IOException {
        try (var files = Files.list(dir)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                Files.delete(p);
            }
        }
        Files.delete(dir);
    }
}
