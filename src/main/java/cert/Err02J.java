package cert;

import java.util.ArrayList;
import java.util.List;

/**
 * ERR02-J: Prevent exceptions while logging data.
 *
 * <p>Owner: Alex
 *
 * <p>Logging is usually called from inside error handling. If building the log
 * message itself throws (a null field, a {@code toString()} that fails), the new
 * exception replaces the one being handled: the original error is lost and the
 * rest of the handler, such as cleanup, never runs. Keep log statements free of
 * risky expressions, and guard the logging call so that a failure to log can
 * never break the program.
 */
public class Err02J {

    /** Stand-in for a log file: lines are collected in memory so the demo can print them. */
    static final List<String> LOG = new ArrayList<>();

    /** Order whose customer may be missing (null). */
    static class Order {
        final String id;
        final Customer customer;

        Order(String id, Customer customer) {
            this.id = id;
            this.customer = customer;
        }
    }

    /** Customer record. */
    static class Customer {
        final String name;

        Customer(String name) {
            this.name = name;
        }
    }

    /** Always fails: the payment is declined. */
    private static void charge(Order o) {
        throw new IllegalStateException("payment declined");
    }

    /**
     * NONCOMPLIANT: the log message is built with an expression that can throw,
     * inside the catch block, and the cleanup comes after it.
     *
     * @param o the order to process
     * @param released receives "released" if the cleanup ran
     * @return a short description of the outcome
     */
    static String noncompliant(Order o, List<String> released) {
        try {
            charge(o);
            return "paid";
        } catch (IllegalStateException e) {
            LOG.add("order " + o.id + " for " + o.customer.name.toUpperCase() + " failed: " + e.getMessage()); // violates ERR02-J
            released.add("released");                       // cleanup
            return "handled: " + e.getMessage();
        }
    }

    /**
     * COMPLIANT: the logging call is isolated; a failure to log is reported on a
     * fallback path instead of replacing the exception being handled.
     *
     * @param o the order to process
     * @param released receives "released" if the cleanup ran
     * @return a short description of the outcome
     */
    static String compliant(Order o, List<String> released) {
        try {
            charge(o);
            return "paid";
        } catch (IllegalStateException e) {
            safeLog(o, e);
            released.add("released");                       // cleanup always runs
            return "handled: " + e.getMessage();
        }
    }

    /** Builds and writes the log line; if that fails, writes a plain fallback line. */
    private static void safeLog(Order o, Exception cause) {
        try {
            LOG.add("order " + o.id + " for " + o.customer.name.toUpperCase() + " failed: " + cause.getMessage());
        } catch (RuntimeException loggingFailure) {
            LOG.add("order " + o.id + " failed: " + cause.getMessage()
                    + " [customer details unavailable: " + loggingFailure.getClass().getSimpleName() + "]");
        }
    }

    private static void run(String label, Order o, boolean compliantVersion) {
        LOG.clear();
        List<String> released = new ArrayList<>();
        String outcome;
        try {
            outcome = compliantVersion ? compliant(o, released) : noncompliant(o, released);
        } catch (RuntimeException escaped) {
            outcome = "ESCAPED " + escaped.getClass().getSimpleName() + " from the log call; original error lost";
        }
        System.out.println(label + outcome + " | cleanup ran = " + released.contains("released")
                + " | log = " + LOG);
    }

    /**
     * Processes an order whose customer is null, then a normal order.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== ERR02-J: Prevent exceptions while logging data ===");

        Order broken = new Order("A-17", null);
        Order normal = new Order("B-02", new Customer("dana"));

        System.out.println("\n-- Scenario 1: payment declined, and the order has no customer record --");
        run("noncompliant: ", broken, false);
        run("compliant   : ", broken, true);
        System.out.println("-> noncompliant: a NullPointerException thrown while logging replaced the real error and skipped the cleanup.");

        System.out.println("\n-- Scenario 2 (CONTROL CASE): payment declined, customer present; identical output is expected --");
        run("noncompliant: ", normal, false);
        run("compliant   : ", normal, true);
    }
}
