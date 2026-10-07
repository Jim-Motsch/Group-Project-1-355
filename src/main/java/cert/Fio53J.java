package cert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * FIO53-J: Use the serialization methods writeUnshared() and readUnshared() with care.
 *
 * <p>Owner: James
 *
 * <p>Recommendation. {@code writeObject} remembers objects it has already
 * written and later writes only a back-reference, so one shared object stays
 * ONE object after reading. {@code writeUnshared} writes a fresh copy every
 * time, so sharing is lost, and {@code readUnshared} refuses to read a
 * back-reference at all. Mixing the two styles, or using the unshared methods
 * where sharing matters, breaks the object graph.
 */
public class Fio53J {

    /** A mutable object that two parts of the program are meant to share. */
    static class Counter implements Serializable {
        private static final long serialVersionUID = 1L;
        int value = 5;
    }

    private static byte[] write(boolean unshared) throws IOException {
        Counter shared = new Counter();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            for (int i = 0; i < 2; i++) {                       // same object, written twice
                if (unshared) {
                    out.writeUnshared(shared);                  // violates FIO53-J where sharing matters
                } else {
                    out.writeObject(shared);
                }
            }
        }
        return bos.toByteArray();
    }

    /**
     * Writes one shared object twice, reads both back, changes one and looks at the other.
     *
     * @param args unused
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== FIO53-J: Use writeUnshared() and readUnshared() with care ===");

        System.out.println("\n-- Scenario 1: one Counter written twice, then read back as two references --");
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(write(true)))) {
            Counter a = (Counter) in.readUnshared();
            Counter b = (Counter) in.readUnshared();
            a.value = 99;
            System.out.println("noncompliant (writeUnshared/readUnshared): same object = " + (a == b)
                    + ", after a.value = 99, b.value = " + b.value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(write(false)))) {
            Counter a = (Counter) in.readObject();
            Counter b = (Counter) in.readObject();
            a.value = 99;
            System.out.println("compliant    (writeObject/readObject)          : same object = " + (a == b)
                    + ", after a.value = 99, b.value = " + b.value);
        }
        System.out.println("-> the unshared pair silently split one shared object into two independent copies.");

        System.out.println("\n-- Scenario 2: bytes written normally, but read with readUnshared --");
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(write(false)))) {
            in.readUnshared();
            in.readUnshared();
            System.out.println("noncompliant: read succeeded");
        } catch (java.io.ObjectStreamException e) {
            System.out.println("noncompliant: FAILED " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(write(false)))) {
            in.readObject();
            in.readObject();
            System.out.println("compliant   : read succeeded");
        }
    }
}
