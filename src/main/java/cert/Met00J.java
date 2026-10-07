package cert;

import java.util.Objects;

/**
 * MET00-J: Validate method arguments.
 *
 * <p>Owner: James
 *
 * <p>A method should check the arguments it receives before using them,
 * especially when they come from outside the class. Unchecked arguments can
 * produce nonsensical results, crash deep inside the method, or let a caller
 * steer the code somewhere it was never meant to go (for example outside an
 * intended directory).
 */
public class Met00J {

    /**
     * NONCOMPLIANT: trusts both arguments. A percentage above 100 produces a
     * negative price.
     *
     * @param price   original price
     * @param percent discount percentage
     * @return the discounted price
     */
    static double noncompliantDiscount(double price, int percent) {
        return price - price * percent / 100.0;
    }

    /**
     * COMPLIANT: rejects out-of-range percentages and negative prices.
     *
     * @param price   original price, must be zero or more
     * @param percent discount percentage, must be 0..100
     * @return the discounted price
     * @throws IllegalArgumentException if either argument is out of range
     */
    static double compliantDiscount(double price, int percent) {
        if (price < 0) {
            throw new IllegalArgumentException("price must be >= 0 but was " + price);
        }
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent must be 0..100 but was " + percent);
        }
        return price - price * percent / 100.0;
    }

    /**
     * NONCOMPLIANT: concatenates the file name onto the base directory without
     * checking it, so {@code ../} sequences escape the directory.
     *
     * @param baseDir  the directory files must stay inside
     * @param fileName caller-supplied file name
     * @return the resulting path string
     */
    static String noncompliantPath(String baseDir, String fileName) {
        return baseDir + "/" + fileName;
    }

    /**
     * COMPLIANT: requires non-null arguments and refuses names that contain a
     * path separator or a parent-directory reference.
     *
     * @param baseDir  the directory files must stay inside
     * @param fileName caller-supplied file name
     * @return the resulting path string
     * @throws NullPointerException     if an argument is null
     * @throws IllegalArgumentException if the name could escape {@code baseDir}
     */
    static String compliantPath(String baseDir, String fileName) {
        Objects.requireNonNull(baseDir, "baseDir");
        Objects.requireNonNull(fileName, "fileName");
        if (fileName.isEmpty() || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new IllegalArgumentException("illegal file name: " + fileName);
        }
        return baseDir + "/" + fileName;
    }

    /**
     * Runs both scenarios, with a valid control case at the end.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== MET00-J: Validate method arguments ===");

        System.out.println("\n-- Scenario 1: 150% discount on a price of 80.00 --");
        System.out.println("noncompliant: " + noncompliantDiscount(80.0, 150));
        try {
            System.out.println("compliant   : " + compliantDiscount(80.0, 150));
        } catch (IllegalArgumentException e) {
            System.out.println("compliant   : rejected (" + e.getMessage() + ")");
        }
        System.out.println("-> noncompliant returns a negative price (the shop would pay the customer).");

        System.out.println("\n-- Scenario 2: file name \"../../etc/passwd\" inside /srv/uploads --");
        System.out.println("noncompliant: " + noncompliantPath("/srv/uploads", "../../etc/passwd"));
        try {
            System.out.println("compliant   : " + compliantPath("/srv/uploads", "../../etc/passwd"));
        } catch (IllegalArgumentException e) {
            System.out.println("compliant   : rejected (" + e.getMessage() + ")");
        }
        System.out.println("-> noncompliant builds a path that escapes the upload directory.");

        System.out.println("\n-- Scenario 3 (CONTROL CASE): valid arguments; identical output is expected --");
        System.out.println("noncompliant: " + noncompliantDiscount(80.0, 25) + "  " + noncompliantPath("/srv/uploads", "a.txt"));
        System.out.println("compliant   : " + compliantDiscount(80.0, 25) + "  " + compliantPath("/srv/uploads", "a.txt"));
    }
}
