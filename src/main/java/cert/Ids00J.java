package cert;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * IDS00-J: Prevent SQL injection.
 *
 * <p>Owner: Alex
 *
 * <p>If untrusted text is concatenated into an SQL statement, the attacker can
 * add quote characters and SQL of their own, changing what the statement means.
 * A parameterized statement ({@code PreparedStatement}) sends the SQL and the
 * values separately, so a value can never turn into SQL.
 *
 * <p>SAFETY: the database is a private in-memory H2 database ({@code jdbc:h2:mem:}):
 * no files, no H2 console, no network listener. It holds only the demo rows
 * created below and disappears when the connection closes.
 */
public class Ids00J {

    /**
     * Creates the demo data in a fresh in-memory database.
     *
     * @return an open connection to the private database
     */
    private static Connection newDatabase() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:h2:mem:");
        try (Statement s = c.createStatement()) {
            s.execute("CREATE TABLE users (name VARCHAR(20), password VARCHAR(20), role VARCHAR(20))");
            s.execute("INSERT INTO users VALUES ('alice', 'pw-alice', 'admin')");
            s.execute("INSERT INTO users VALUES ('bob', 'pw-bob', 'member')");
            s.execute("INSERT INTO users VALUES ('carol', 'pw-carol', 'member')");
        }
        return c;
    }

    /**
     * NONCOMPLIANT: builds the SQL by concatenating the untrusted values.
     *
     * @param c connection
     * @param name untrusted user name
     * @param password untrusted password
     * @return the role of the matching user, or null if no row matched
     */
    static String noncompliantLogin(Connection c, String name, String password) throws SQLException {
        String sql = "SELECT role FROM users WHERE name = '" + name   // violates IDS00-J
                + "' AND password = '" + password + "'";
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    /**
     * COMPLIANT: the same query with {@code ?} placeholders.
     *
     * @param c connection
     * @param name untrusted user name
     * @param password untrusted password
     * @return the role of the matching user, or null if no row matched
     */
    static String compliantLogin(Connection c, String name, String password) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT role FROM users WHERE name = ? AND password = ?")) {
            ps.setString(1, name);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /**
     * NONCOMPLIANT: looks a user up by name using string concatenation.
     *
     * @param c connection
     * @param name untrusted user name
     * @return the names of all rows the statement returned
     */
    static List<String> noncompliantFind(Connection c, String name) throws SQLException {
        List<String> found = new ArrayList<>();
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT name FROM users WHERE name = '" + name + "'")) { // violates IDS00-J
            while (rs.next()) {
                found.add(rs.getString(1));
            }
        }
        return found;
    }

    /**
     * COMPLIANT: looks a user up by name using a parameter.
     *
     * @param c connection
     * @param name untrusted user name
     * @return the names of all rows the statement returned
     */
    static List<String> compliantFind(Connection c, String name) throws SQLException {
        List<String> found = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT name FROM users WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    found.add(rs.getString(1));
                }
            }
        }
        return found;
    }

    /**
     * Runs both versions against the same in-memory data.
     *
     * @param args unused
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== IDS00-J: Prevent SQL injection (private in-memory H2 database) ===");
        try (Connection c = newDatabase()) {

            System.out.println("\n-- Scenario 1: login as alice with the WRONG password, name = \"alice' --\" --");
            String bad = "alice' --";
            System.out.println("noncompliant: role = " + noncompliantLogin(c, bad, "wrong"));
            System.out.println("compliant   : role = " + compliantLogin(c, bad, "wrong"));
            System.out.println("-> noncompliant treated the quote and -- as SQL, cut off the password check and logged in as alice (role: admin).");

            System.out.println("\n-- Scenario 2: look up the name \"x' OR '1'='1\" --");
            String all = "x' OR '1'='1";
            System.out.println("noncompliant: rows = " + noncompliantFind(c, all));
            System.out.println("compliant   : rows = " + compliantFind(c, all));
            System.out.println("-> noncompliant returned every user; compliant looked for a user literally named that and found none.");

            System.out.println("\n-- Scenario 3 (CONTROL CASE): honest input; identical output is expected --");
            System.out.println("noncompliant: role = " + noncompliantLogin(c, "bob", "pw-bob")
                    + ", rows = " + noncompliantFind(c, "carol"));
            System.out.println("compliant   : role = " + compliantLogin(c, "bob", "pw-bob")
                    + ", rows = " + compliantFind(c, "carol"));
        }
    }
}
