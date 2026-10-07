package cert;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * OBJ13-J: Ensure that references to mutable objects are not exposed.
 *
 * <p>Owner: Ian
 *
 * <p>Exposure also happens in the other direction and through constants: a
 * constructor that stores the caller's own array or {@code Date} keeps sharing
 * it with the caller, and a {@code public static final} array is a constant
 * only in name, because anyone can overwrite its elements. Copy mutable input
 * on the way in, and make shared constants unmodifiable.
 */
public class Obj13J {

    /** NONCOMPLIANT: "constant" array whose elements anyone can overwrite. */
    public static final String[] NONCOMPLIANT_ROLES = {"guest", "member"};

    /** COMPLIANT: an unmodifiable list. */
    public static final List<String> COMPLIANT_ROLES = List.of("guest", "member");

    /** NONCOMPLIANT: keeps the caller's own objects. */
    static class NoncompliantEvent {
        private final Date start;
        private final int[] seats;

        NoncompliantEvent(Date start, int[] seats) {
            this.start = start;    // violates OBJ13-J: shares the caller's Date
            this.seats = seats;    // violates OBJ13-J: shares the caller's array
        }

        String describe() {
            return "start.time=" + start.getTime() + " seats=" + Arrays.toString(seats);
        }
    }

    /** COMPLIANT: copies mutable input in the constructor. */
    static class CompliantEvent {
        private final Date start;
        private final int[] seats;

        CompliantEvent(Date start, int[] seats) {
            this.start = new Date(start.getTime());
            this.seats = seats.clone();
        }

        String describe() {
            return "start.time=" + start.getTime() + " seats=" + Arrays.toString(seats);
        }
    }

    /**
     * Changes the objects after handing them over, then tries to overwrite the shared roles.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== OBJ13-J: Ensure that references to mutable objects are not exposed ===");

        System.out.println("\n-- Scenario 1: caller edits its own Date and array AFTER passing them to the constructor --");
        Date d1 = new Date(0L);
        int[] s1 = {1, 2, 3};
        NoncompliantEvent bad = new NoncompliantEvent(d1, s1);
        d1.setTime(999_999_999_999L);
        s1[0] = 99;
        System.out.println("noncompliant event: " + bad.describe());

        Date d2 = new Date(0L);
        int[] s2 = {1, 2, 3};
        CompliantEvent good = new CompliantEvent(d2, s2);
        d2.setTime(999_999_999_999L);
        s2[0] = 99;
        System.out.println("compliant    event: " + good.describe());
        System.out.println("-> the noncompliant event changed behind its own back; the compliant one kept its copy.");

        System.out.println("\n-- Scenario 2: overwrite the first element of the public 'constant' --");
        NONCOMPLIANT_ROLES[0] = "admin";
        System.out.println("noncompliant ROLES: " + Arrays.toString(NONCOMPLIANT_ROLES) + "  <-- guest became admin");
        try {
            COMPLIANT_ROLES.set(0, "admin");
            System.out.println("compliant    ROLES: " + COMPLIANT_ROLES);
        } catch (UnsupportedOperationException e) {
            System.out.println("compliant    ROLES: " + COMPLIANT_ROLES + "  (write rejected: UnsupportedOperationException)");
        }

        System.out.println("\n-- Scenario 3 (CONTROL CASE): no one edits after construction; identical output is expected --");
        System.out.println("noncompliant event: " + new NoncompliantEvent(new Date(0L), new int[] {7}).describe());
        System.out.println("compliant    event: " + new CompliantEvent(new Date(0L), new int[] {7}).describe());
    }
}
