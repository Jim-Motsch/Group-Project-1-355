package cert;

import java.util.Map;
import java.util.OptionalInt;

/**
 * ERR52-J: Avoid in-band error indicators.
 *
 * <p>Owner: Alex
 *
 * <p>Recommendation. If a method signals "not found" or "failed" with a value
 * from the same range as its real results (such as {@code -1}), a legitimate
 * result can be mistaken for the error. Return a type that can express absence
 * ({@code OptionalInt}) or throw an exception.
 */
public class Err52J {

    private static final Map<String, Integer> TEMPERATURES_C = Map.of("Cairo", 30, "Oslo", -1);

    /** NONCOMPLIANT: -1 means "unknown city", but -1 is also a real temperature. */
    static int noncompliantTemperature(String city) {
        Integer t = TEMPERATURES_C.get(city);
        return t == null ? -1 : t;                     // violates ERR52-J
    }

    /** COMPLIANT: absence is its own state. */
    static OptionalInt compliantTemperature(String city) {
        Integer t = TEMPERATURES_C.get(city);
        return t == null ? OptionalInt.empty() : OptionalInt.of(t);
    }

    private static String badText(String city) {
        int t = noncompliantTemperature(city);
        return t == -1 ? "unknown" : t + " C";
    }

    private static String goodText(String city) {
        OptionalInt t = compliantTemperature(city);
        return t.isPresent() ? t.getAsInt() + " C" : "unknown";
    }

    /**
     * Looks up one city with a real -1 reading, one unknown city and one ordinary city.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== ERR52-J: Avoid in-band error indicators ===");

        System.out.println("\n-- Scenario 1: Oslo really is at -1 C --");
        System.out.println("noncompliant: Oslo -> " + badText("Oslo") + "   <-- real reading mistaken for the error");
        System.out.println("compliant   : Oslo -> " + goodText("Oslo"));

        System.out.println("\n-- Scenario 2 (CONTROL CASE): Atlantis is not in the table; identical output is expected --");
        System.out.println("noncompliant: Atlantis -> " + badText("Atlantis"));
        System.out.println("compliant   : Atlantis -> " + goodText("Atlantis"));

        System.out.println("\n-- Scenario 3 (CONTROL CASE): Cairo; identical output is expected --");
        System.out.println("noncompliant: Cairo -> " + badText("Cairo"));
        System.out.println("compliant   : Cairo -> " + goodText("Cairo"));
    }
}
