package cert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;

/**
 * SER04-J: Do not allow serialization and deserialization to bypass the security manager.
 *
 * <p>Owner: James
 *
 * <p>A class can protect its constructor with a security-manager check so that
 * only trusted callers may create instances. Deserialization does not call the
 * constructor of a serializable class, so without extra care anyone who can
 * feed bytes to {@code ObjectInputStream} can create an instance and skip the
 * check. The fix is to repeat the same check inside {@code readObject} (and
 * {@code writeObject} when needed).
 *
 * <p><b>Java 17 note:</b> the Security Manager is deprecated for removal
 * (JEP 411). JDK 17 still lets a program install one at runtime (the default
 * of {@code java.security.manager} is {@code allow}, with a warning on
 * stderr). JDK 18 to 23 require {@code -Djava.security.manager=allow}, and on
 * JDK 24 and later it is permanently disabled. Part B needs a JVM that allows
 * it; if the JVM refuses, Part B is skipped and says so.
 */
@SuppressWarnings("removal")
public class Ser04J {

    /** The permission a caller must hold to create a sensitive action. */
    static final RuntimePermission CREATE_PERMISSION = new RuntimePermission("createSensitiveAction");

    /** Number of times the permission check was executed. */
    static int permissionChecks;

    /** Asks the installed security manager (if any) for {@link #CREATE_PERMISSION}. */
    static void checkCreatePermission() {
        permissionChecks++;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(CREATE_PERMISSION);
        }
    }

    /**
     * NONCOMPLIANT: the constructor is guarded, but deserialization skips the
     * constructor, so the guard can be bypassed.
     */
    static class NoncompliantAction implements Serializable {
        private static final long serialVersionUID = 1L;
        final String name;

        NoncompliantAction(String name) {
            checkCreatePermission(); // only protects the "new" path
            this.name = name;
        }
    }

    /** COMPLIANT: {@code readObject} repeats the check, so both creation paths are guarded. */
    static class CompliantAction implements Serializable {
        private static final long serialVersionUID = 1L;
        final String name;

        CompliantAction(String name) {
            checkCreatePermission();
            this.name = name;
        }

        /**
         * Repeats the constructor's security check before the object is rebuilt.
         *
         * @param in the stream
         * @throws IOException            on read failure
         * @throws ClassNotFoundException if a class is missing
         */
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            checkCreatePermission();
            in.defaultReadObject();
        }
    }

    /** Policy that grants everything except {@link #CREATE_PERMISSION}. */
    static class DenyCreatePolicy extends Policy {
        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            return !CREATE_PERMISSION.equals(permission);
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
     * Part A shows, without any security manager, that deserialization skips
     * the constructor check. Part B installs a security manager that denies the
     * permission and shows what that means in practice.
     *
     * @param args unused
     * @throws Exception on any serialization failure
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== SER04-J: Do not allow serialization and deserialization to bypass the security manager ===");

        // Instances are created and serialized BEFORE any security manager exists.
        byte[] badBytes = serialize(new NoncompliantAction("transfer-funds"));
        byte[] goodBytes = serialize(new CompliantAction("transfer-funds"));

        System.out.println("\n-- Part A (no security manager needed): is the check executed on deserialization? --");
        permissionChecks = 0;
        deserialize(badBytes);
        System.out.println("noncompliant: checks executed while deserializing = " + permissionChecks);
        permissionChecks = 0;
        deserialize(goodBytes);
        System.out.println("compliant   : checks executed while deserializing = " + permissionChecks);
        System.out.println("-> the constructor (and its check) is skipped; only readObject can repeat the check.");

        System.out.println("\n-- Part B: with a real security manager that DENIES " + CREATE_PERMISSION.getName() + " --");
        Policy oldPolicy = Policy.getPolicy();
        Policy.setPolicy(new DenyCreatePolicy());
        try {
            System.setSecurityManager(new SecurityManager());
        } catch (UnsupportedOperationException e) {
            Policy.setPolicy(oldPolicy);
            System.out.println("[CANNOT SHOW ON THIS RUN] this JVM refuses to install a security manager.");
            System.out.println("Start it with -Djava.security.manager=allow (needed on JDK 18-23; impossible on JDK 24+).");
            System.out.println("No result is shown for Part B and none is faked; Part A above is still real.");
            return;
        }
        try {
            System.out.println("[security manager installed at runtime; Java 17 prints a deprecation warning on stderr]");

            System.out.println("\nScenario 1 (CONTROL CASE): creating with 'new'; identical output is expected");
            System.out.println("noncompliant: " + tryNew(true));
            System.out.println("compliant   : " + tryNew(false));

            System.out.println("\nScenario 2: creating through deserialization");
            System.out.println("noncompliant: " + tryDeserialize(badBytes));
            System.out.println("compliant   : " + tryDeserialize(goodBytes));
            System.out.println("-> noncompliant let untrusted bytes create an object the policy forbids;");
            System.out.println("   compliant denied it, exactly like the constructor path.");
        } finally {
            System.setSecurityManager(null);
            Policy.setPolicy(oldPolicy);
        }
    }

    private static String tryNew(boolean noncompliant) {
        try {
            Object o = noncompliant ? new NoncompliantAction("direct") : new CompliantAction("direct");
            return "created " + o.getClass().getSimpleName();
        } catch (SecurityException e) {
            return "DENIED (" + e.getClass().getSimpleName() + ")";
        }
    }

    private static String tryDeserialize(byte[] data) throws IOException, ClassNotFoundException {
        try {
            Object o = deserialize(data);
            return "CREATED " + o.getClass().getSimpleName() + " despite the denying policy  <-- bypass";
        } catch (SecurityException e) {
            return "DENIED (" + e.getClass().getSimpleName() + ")";
        }
    }
}
