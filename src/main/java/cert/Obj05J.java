package cert;

import java.util.Arrays;
import java.util.Date;

/**
 * OBJ05-J: Do not return references to private mutable class members.
 *
 * <p>Owner: Ian
 *
 * <p>A getter that returns the object stored in a private field hands the
 * caller a live handle to the class's internal state: the field is private,
 * but the caller can change the object it points to. Getters for mutable
 * members (arrays, {@code Date}, collections) should return a copy.
 */
public class Obj05J {

    /** NONCOMPLIANT: getters return the internal objects themselves. */
    static class NoncompliantBooking {
        private final Date checkIn = new Date(0L);       // 1970-01-01T00:00:00Z
        private final int[] roomNumbers = {101, 102};

        Date getCheckIn() {
            return checkIn;          // violates OBJ05-J: exposes the private Date
        }

        int[] getRoomNumbers() {
            return roomNumbers;      // violates OBJ05-J: exposes the private array
        }

        String describe() {
            return "checkIn.time=" + checkIn.getTime() + " rooms=" + Arrays.toString(roomNumbers);
        }
    }

    /** COMPLIANT: getters return defensive copies. */
    static class CompliantBooking {
        private final Date checkIn = new Date(0L);
        private final int[] roomNumbers = {101, 102};

        Date getCheckIn() {
            return new Date(checkIn.getTime()); // copy
        }

        int[] getRoomNumbers() {
            return roomNumbers.clone();         // copy
        }

        String describe() {
            return "checkIn.time=" + checkIn.getTime() + " rooms=" + Arrays.toString(roomNumbers);
        }
    }

    /**
     * Mutates whatever the getters returned, then shows the object's own view of itself.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== OBJ05-J: Do not return references to private mutable class members ===");

        System.out.println("\n-- Scenario 1: caller changes the Date and array it got from the getters --");
        NoncompliantBooking bad = new NoncompliantBooking();
        System.out.println("noncompliant before: " + bad.describe());
        bad.getCheckIn().setTime(999_999_999_999L);
        bad.getRoomNumbers()[0] = 666;
        System.out.println("noncompliant after : " + bad.describe());

        CompliantBooking good = new CompliantBooking();
        System.out.println("compliant    before: " + good.describe());
        good.getCheckIn().setTime(999_999_999_999L);
        good.getRoomNumbers()[0] = 666;
        System.out.println("compliant    after : " + good.describe());
        System.out.println("-> the noncompliant booking was rewritten through its getters although every field is private.");

        System.out.println("\n-- Scenario 2 (CONTROL CASE): caller only reads; identical output is expected --");
        System.out.println("noncompliant: " + new NoncompliantBooking().getCheckIn().getTime()
                + " " + new NoncompliantBooking().getRoomNumbers()[0]);
        System.out.println("compliant   : " + new CompliantBooking().getCheckIn().getTime()
                + " " + new CompliantBooking().getRoomNumbers()[0]);
    }
}
