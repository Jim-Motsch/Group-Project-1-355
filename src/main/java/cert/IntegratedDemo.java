package cert;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * IntegratedDemo: one small, cohesive application ("Seaview" account and booking
 * service) written entirely in the COMPLIANT style of our CERT rules and
 * recommendations.
 *
 * <p>Flow of the application (each step is narrated by {@link #main}):
 * <ol>
 *   <li>load a configuration file from a temp directory, handling file errors;</li>
 *   <li>look users up in a private in-memory H2 database with a PreparedStatement;</li>
 *   <li>accept booking requests, validating every piece of user input;</li>
 *   <li>hand booking data to callers without leaking mutable internals;</li>
 *   <li>save and restore the bookings through serialization with an allow-list filter;</li>
 *   <li>log events without exposing sensitive details, and report failures to the
 *       user through meaningful, user-defined exceptions.</li>
 * </ol>
 *
 * <p>Determinism and safety: no garbage-collector timing, no Security Manager, no
 * {@code Runtime.exec}. Files exist only in a temporary directory that this class
 * creates and deletes. The database is a private in-memory H2 database.
 *
 * <h2>Coverage: rules used and where</h2>
 * <table border="1">
 *   <caption>CERT rules applied in this class</caption>
 *   <tr><th>Rule</th><th>Where</th><th>What is done</th></tr>
 *   <tr><td>ERR08-J</td><td>BookingService.book, UserRepository.findByName</td><td>explicit null checks; no catch of NullPointerException</td></tr>
 *   <tr><td>FIO02-J</td><td>loadConfig</td><td>missing/unreadable file is detected and handled (defaults), not ignored</td></tr>
 *   <tr><td>EXP02-J</td><td>Booking.sameContentAs</td><td>arrays compared with Arrays.equals, not equals()</td></tr>
 *   <tr><td>OBJ10-J</td><td>all static fields</td><td>(absence) only private static final constants</td></tr>
 *   <tr><td>EXP00-J</td><td>cleanup, Itinerary.isConsistent, BookingService.restore</td><td>results of deleteIfExists, Set.add and Map.put are used</td></tr>
 *   <tr><td>SER01-J</td><td>Booking.readObject, serialVersionUID fields</td><td>serialization hook has the exact required signature</td></tr>
 *   <tr><td>MET01-J</td><td>validate methods, setters, constructors</td><td>(absence) arguments checked with real exceptions, never assert</td></tr>
 *   <tr><td>SER04-J (concept only)</td><td>Booking.readObject</td><td>constructor checks are repeated on deserialization; the Security Manager itself is not used (deprecated)</td></tr>
 *   <tr><td>MET00-J</td><td>Config setters, Booking/Itinerary constructors, book, findByName</td><td>every public-facing argument is validated first</td></tr>
 *   <tr><td>SER05-J</td><td>Booking, Itinerary, Snapshot</td><td>serialized classes are static nested, never inner classes</td></tr>
 *   <tr><td>MET06-J</td><td>Itinerary.clone</td><td>clone calls nothing overridable (class is final)</td></tr>
 *   <tr><td>MET12-J</td><td>UserRepository</td><td>(absence) resource released by close(), no finalizer anywhere</td></tr>
 *   <tr><td>MET05-J</td><td>Config, Booking and Itinerary constructors</td><td>constructors call only private static methods</td></tr>
 *   <tr><td>ERR01-J</td><td>BookingException.userMessage, User.toString, AuditLog</td><td>users see generic text; details only go to the internal log; e-mail never logged</td></tr>
 *   <tr><td>OBJ05-J</td><td>Booking.itinerary, Itinerary.rooms, BookingService.bookingsFor</td><td>getters return copies / immutable lists</td></tr>
 *   <tr><td>OBJ11-J</td><td>Booking, Itinerary</td><td>classes are final, so no subclass finalizer can rescue a half-built object</td></tr>
 *   <tr><td>OBJ13-J</td><td>Booking, Itinerary, Snapshot constructors; Booking.readObject</td><td>mutable input is copied on the way in</td></tr>
 *   <tr><td>IDS00-J</td><td>UserRepository.findByName</td><td>PreparedStatement keeps user input out of the SQL text</td></tr>
 *   <tr><td>FIO08-J</td><td>SnapshotStore.checksum</td><td>read() result kept in an int and compared with -1</td></tr>
 *   <tr><td>SER12-J</td><td>SnapshotStore.restoreStream</td><td>ObjectInputFilter allow-list before readObject</td></tr>
 *   <tr><td>EXP03-J</td><td>BookingService.cancelBooking</td><td>boxed Integer owner compared with equals(), not ==</td></tr>
 *   <tr><td>ERR02-J</td><td>AuditLog.event</td><td>log calls are built only from values that cannot throw</td></tr>
 * </table>
 *
 * <table border="1">
 *   <caption>CERT recommendations applied in this class</caption>
 *   <tr><th>Recommendation</th><th>Where</th><th>What is done</th></tr>
 *   <tr><td>ERR50-J</td><td>loadConfig, BookingService.book</td><td>input tested with regular expressions instead of catching NumberFormatException</td></tr>
 *   <tr><td>FIO50-J</td><td>writeConfigFile, SnapshotStore.save</td><td>files created with CREATE_NEW (atomic), never check-then-create</td></tr>
 *   <tr><td>OBJ51-J</td><td>all nested classes and fields</td><td>private classes, private fields, narrow methods</td></tr>
 *   <tr><td>MET54-J</td><td>BookingService.book, cancelBooking</td><td>results are returned (Booking, boolean) instead of void</td></tr>
 *   <tr><td>MET55-J</td><td>BookingService.bookingsFor</td><td>empty list, never null, when a user has no bookings</td></tr>
 *   <tr><td>FIO53-J</td><td>SnapshotStore.save / restoreStream</td><td>plain writeObject/readObject so shared references stay shared</td></tr>
 *   <tr><td>MET50-J</td><td>whole class</td><td>(absence) no confusable overloads; operations have distinct names</td></tr>
 *   <tr><td>MET53-J</td><td>Itinerary.clone</td><td>clone starts from super.clone()</td></tr>
 *   <tr><td>ERR54-J</td><td>UserRepository, SnapshotStore, cleanup</td><td>try-with-resources for connections, statements and streams</td></tr>
 *   <tr><td>OBJ50-J</td><td>BookingService fields, bookingsFor</td><td>final fields are never exposed; returned lists are truly immutable</td></tr>
 *   <tr><td>OBJ56-J</td><td>ReadOnlyConfig</td><td>read-only wrapper handed to the service instead of the mutable Config</td></tr>
 *   <tr><td>MET51-J</td><td>whole class</td><td>(absence) no overloads that depend on runtime types</td></tr>
 *   <tr><td>ERR51-J</td><td>BookingException hierarchy</td><td>InvalidInput / RoomUnavailable / Storage exceptions instead of RuntimeException</td></tr>
 *   <tr><td>ERR52-J</td><td>UserRepository.findByName, cancelBooking</td><td>Optional and boolean instead of null/-1 error values</td></tr>
 *   <tr><td>FIO51-J</td><td>SnapshotStore.save</td><td>Files.isSameFile protects the config file even when it is spelled differently</td></tr>
 * </table>
 *
 * <h2>What did not fit (honest list)</h2>
 * <ul>
 *   <li><b>IDS07-J</b> (Runtime.exec): the service never starts an external process, so there is
 *       nothing to sanitize. Adding one only to demonstrate the rule would be forced.</li>
 *   <li><b>IDS16-J</b> (XML injection): the application has no XML. Forcing an XML export would
 *       not be a natural part of a booking service.</li>
 *   <li><b>SER08-J</b> (minimize privileges before deserializing): this concerns code running under
 *       the Security Manager, which is deprecated for removal and is deliberately not used here.</li>
 *   <li><b>SER04-J</b>: only the concept (repeat the constructor's checks in readObject) is used; the
 *       Security Manager part cannot be demonstrated on Java 17 without faking it.</li>
 * </ul>
 */
public final class IntegratedDemo {

    /**
     * OBJ10-J / OBJ51-J: the only static state in the class is private and final. There is no public
     * static non-final field anywhere.
     */
    private static final String SNAPSHOT_FILTER =
            "maxdepth=10;maxrefs=500;maxbytes=65536;maxarray=100;"
            + "cert.IntegratedDemo$Snapshot;cert.IntegratedDemo$Booking;cert.IntegratedDemo$Itinerary;"
            + "java.util.ArrayList;java.lang.Object;!*";
    // java.lang.Object is listed because ArrayList.readObject checks an Object[] with the filter;
    // allowing the bare Object type lets that check pass and does not allow any other class.

    /** Utility class: not meant to be instantiated (OBJ51-J: smallest possible visibility). */
    private IntegratedDemo() {
    }

    // =====================================================================================
    // Exceptions (ERR51-J: user-defined exceptions; ERR01-J: user text separate from detail)
    // =====================================================================================

    /**
     * ERR51-J: the base of our own exception hierarchy. Callers can catch exactly the failure they
     * can handle instead of a general {@code Exception}.
     *
     * <p>ERR01-J: every exception carries two texts. {@link #userMessage()} is safe to show to a
     * user; {@link #getMessage()} holds internal detail and goes only to the internal audit log.
     */
    private static class BookingException extends Exception {
        private static final long serialVersionUID = 1L;
        private final String userMessage;

        /**
         * Creates the exception.
         *
         * @param userMessage generic text that may be shown to the user
         * @param internalDetail technical detail for the internal log only
         * @param cause the underlying exception, or null
         */
        BookingException(String userMessage, String internalDetail, Throwable cause) {
            super(internalDetail, cause);
            this.userMessage = userMessage;
        }

        /**
         * Returns the text that is safe to show to the user.
         *
         * @return generic user-facing text
         */
        String userMessage() {
            return userMessage;
        }
    }

    /** ERR51-J: the request itself was not acceptable (bad or missing input, limits). */
    private static final class InvalidInputException extends BookingException {
        private static final long serialVersionUID = 1L;

        private InvalidInputException(String userMessage, String internalDetail) {
            super(userMessage, internalDetail, null);
        }

        /**
         * Creates an exception whose user text and internal detail are the same (no secrets involved).
         *
         * @param message explanation that is safe for the user
         * @return the exception
         */
        static InvalidInputException of(String message) {
            return new InvalidInputException(message, "invalid input: " + message);
        }

        /**
         * Creates an exception whose user text is deliberately vaguer than the internal detail.
         *
         * @param userMessage generic text for the user
         * @param internalDetail what really happened
         * @return the exception
         */
        static InvalidInputException withDetail(String userMessage, String internalDetail) {
            return new InvalidInputException(userMessage, internalDetail);
        }
    }

    /**
     * ERR51-J: a requested room is taken. ERR01-J: the user is not told WHO holds it.
     */
    private static final class RoomUnavailableException extends BookingException {
        private static final long serialVersionUID = 1L;

        RoomUnavailableException(int room, int holderUserId) {
            super("That room is not available.", "room " + room + " already held by user#" + holderUserId, null);
        }
    }

    /**
     * ERR51-J: something went wrong in storage (database or files). ERR01-J: users only ever see a
     * generic sentence; paths, SQL state and exception names stay in the internal log.
     */
    private static final class StorageException extends BookingException {
        private static final long serialVersionUID = 1L;

        StorageException(String internalDetail, Throwable cause) {
            super("The service could not complete that request. Please try again later.", internalDetail, cause);
        }
    }

    // =====================================================================================
    // Configuration (OBJ56-J: sensitive mutable class + unmodifiable wrapper)
    // =====================================================================================

    /**
     * Mutable application settings. Only the loader changes them.
     *
     * <p>MET05-J: the constructor is implicit and calls no methods at all, so no overridable method
     * can run on a half-built object even though subclasses exist.
     */
    private static class Config {
        private int maxNights = 14;
        private int maxBookingsPerUser = 2;
        private String hotel = "Default Hotel";

        /** @return the longest stay allowed, in nights */
        int maxNights() {
            return maxNights;
        }

        /** @return how many active bookings one user may hold */
        int maxBookingsPerUser() {
            return maxBookingsPerUser;
        }

        /** @return the hotel's display name */
        String hotel() {
            return hotel;
        }

        /**
         * Changes the longest stay.
         *
         * @param n new limit, 1 to 60
         * @throws IllegalArgumentException if n is out of range
         */
        void setMaxNights(int n) {
            // MET00-J / MET01-J: validate the argument with a real exception, not an assert
            if (n < 1 || n > 60) {
                throw new IllegalArgumentException("maxNights must be 1..60");
            }
            maxNights = n;
        }

        /**
         * Changes the per-user booking limit.
         *
         * @param n new limit, 1 to 10
         * @throws IllegalArgumentException if n is out of range
         */
        void setMaxBookingsPerUser(int n) {
            if (n < 1 || n > 10) {
                throw new IllegalArgumentException("maxBookingsPerUser must be 1..10");
            }
            maxBookingsPerUser = n;
        }

        /**
         * Changes the hotel name.
         *
         * @param name 1 to 40 characters
         * @throws IllegalArgumentException if the name is null, empty or too long
         */
        void setHotel(String name) {
            if (name == null || name.isBlank() || name.length() > 40) {
                throw new IllegalArgumentException("hotel name must be 1..40 characters");
            }
            hotel = name;
        }
    }

    /**
     * OBJ56-J: unmodifiable wrapper around a {@link Config}. The booking service receives this view,
     * so it can read the settings (and sees later changes by the owner) but can never alter them.
     */
    private static final class ReadOnlyConfig extends Config {
        private final Config target;

        /**
         * Wraps the real settings.
         *
         * @param target the real, mutable settings
         */
        ReadOnlyConfig(Config target) {
            this.target = target;
        }

        @Override
        int maxNights() {
            return target.maxNights();
        }

        @Override
        int maxBookingsPerUser() {
            return target.maxBookingsPerUser();
        }

        @Override
        String hotel() {
            return target.hotel();
        }

        @Override
        void setMaxNights(int n) {
            throw new UnsupportedOperationException("settings are read-only here");
        }

        @Override
        void setMaxBookingsPerUser(int n) {
            throw new UnsupportedOperationException("settings are read-only here");
        }

        @Override
        void setHotel(String name) {
            throw new UnsupportedOperationException("settings are read-only here");
        }
    }

    // =====================================================================================
    // Audit log (ERR01-J, ERR02-J)
    // =====================================================================================

    /**
     * The internal audit log, kept in memory so the demo can print it at the end.
     *
     * <p>ERR01-J: this log is for staff only. Even so, it never receives e-mail addresses or other
     * personal data, only numeric ids and exception class names.
     */
    private static final class AuditLog {
        private final List<String> lines = new ArrayList<>();

        /**
         * Records one event.
         *
         * <p>ERR02-J: logging must never throw. Callers build the message only from numbers, fixed
         * text and exception class names (values that cannot fail to render), and a null message is
         * replaced here instead of being dereferenced. That keeps logging from replacing the
         * exception that is being handled, without having to catch NullPointerException (ERR08-J).
         *
         * @param message the text to record, null is tolerated
         */
        void event(String message) {
            lines.add(message == null ? "[empty log message]" : message);
        }

        /** @return an immutable copy of all recorded lines (OBJ05-J: no internal list is exposed) */
        List<String> lines() {
            return List.copyOf(lines);
        }
    }

    // =====================================================================================
    // Users and the in-memory database (IDS00-J)
    // =====================================================================================

    /**
     * A user record. ERR01-J: the generated {@code toString()} would print the e-mail address, so it
     * is overridden to show the id only; the address can never leak through a log line by accident.
     *
     * @param id database id
     * @param name login name
     * @param email private contact address (sensitive)
     */
    private record User(int id, String name, String email) {
        @Override
        public String toString() {
            return "user#" + id;
        }
    }

    /**
     * Looks users up in a private in-memory H2 database ({@code jdbc:h2:mem:}): no files, no console,
     * no network listener.
     *
     * <p>MET12-J (absence): no {@code finalize()} anywhere. The connection is released by
     * {@link #close()}, which callers invoke through try-with-resources (ERR54-J).
     */
    private static final class UserRepository implements AutoCloseable {
        private final Connection conn;

        /**
         * Opens the database and creates three demo users.
         *
         * @throws StorageException if the database cannot be prepared
         */
        UserRepository() throws StorageException {
            Connection c = null;
            try {
                c = DriverManager.getConnection("jdbc:h2:mem:");
                // ERR54-J: the Statement is closed automatically
                try (Statement s = c.createStatement()) {
                    s.execute("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(40), email VARCHAR(80))");
                    s.execute("INSERT INTO users VALUES (1, 'alice', 'alice@example.com')");
                    s.execute("INSERT INTO users VALUES (2, 'bob', 'bob@example.com')");
                    s.execute("INSERT INTO users VALUES (3, 'carol', 'carol@example.com')");
                }
                this.conn = c;
            } catch (SQLException e) {
                // the constructor failed, so nobody will ever call close(): close the connection here
                if (c != null) {
                    try {
                        c.close();
                    } catch (SQLException closeFailure) {
                        e.addSuppressed(closeFailure);
                    }
                }
                throw new StorageException("could not create user database: " + e.getClass().getSimpleName(), e);
            }
        }

        /**
         * Finds a user by name.
         *
         * <p>ERR52-J: "not found" is an empty {@link Optional}, not null or -1.
         *
         * @param name the name typed by the user (untrusted)
         * @return the user, or empty if there is none with that name
         * @throws InvalidInputException if the name is missing or malformed
         * @throws StorageException if the database fails
         */
        Optional<User> findByName(String name) throws InvalidInputException, StorageException {
            // ERR08-J: test for null explicitly; never catch NullPointerException to find out
            if (name == null) {
                throw InvalidInputException.of("A user name is required.");
            }
            // MET00-J: validate the argument first. Quotes are NOT rejected on purpose: names such as
            // O'Brien are legitimate, so the safety of the query must not depend on filtering them.
            if (name.isEmpty() || name.length() > 40 || name.chars().anyMatch(Character::isISOControl)) {
                throw InvalidInputException.of("That user name is not valid.");
            }
            // IDS00-J: the ? placeholder sends the name to the database as DATA. Whatever the user
            // typed can never become part of the SQL text, so injection is impossible.
            try (PreparedStatement ps = conn.prepareStatement("SELECT id, name, email FROM users WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next()
                            ? Optional.of(new User(rs.getInt(1), rs.getString(2), rs.getString(3)))
                            : Optional.empty();
                }
            } catch (SQLException e) {
                // ERR01-J: no SQL text, no user input in the detail; ERR51-J: our own exception type
                throw new StorageException("user lookup failed: " + e.getClass().getSimpleName(), e);
            }
        }

        /**
         * Closes the connection (called by try-with-resources, ERR54-J).
         *
         * @throws StorageException if closing fails
         */
        @Override
        public void close() throws StorageException {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new StorageException("closing user database failed: " + e.getClass().getSimpleName(), e);
            }
        }
    }

    // =====================================================================================
    // Booking data (OBJ05-J, OBJ11-J, OBJ13-J, MET05/06/53, SER01/04/05, EXP02)
    // =====================================================================================

    /**
     * The mutable part of a booking: which rooms and for how many nights.
     *
     * <p>SER05-J: static nested class, so serializing it does not drag an outer object along.
     * OBJ11-J: the class is final, so no subclass can override finalize() to rescue an object whose
     * constructor threw. OBJ51-J: private, with only the methods callers really need.
     */
    private static final class Itinerary implements Serializable, Cloneable {
        private static final long serialVersionUID = 1L; // SER01-J: explicit version
        private int[] rooms;
        private int nights;

        /**
         * Creates an itinerary.
         *
         * @param rooms room numbers (copied)
         * @param nights number of nights
         * @throws IllegalArgumentException if the values are not consistent
         */
        Itinerary(int[] rooms, int nights) {
            // MET05-J: only a private static helper is called from the constructor
            check(rooms, nights);
            // OBJ13-J: copy mutable input; the caller keeps its own array and cannot change ours
            this.rooms = rooms.clone();
            this.nights = nights;
        }

        /** MET00-J / MET01-J: real argument validation (an exception, never an assert). */
        private static void check(int[] rooms, int nights) {
            if (rooms == null || rooms.length == 0 || nights < 1 || nights > 365) {
                throw new IllegalArgumentException("itinerary needs rooms and 1..365 nights");
            }
        }

        /**
         * Checks the data after deserialization, when the constructor did not run.
         *
         * @return true if the itinerary is internally consistent
         */
        boolean isConsistent() {
            if (rooms == null || rooms.length == 0 || rooms.length > 10 || nights < 1 || nights > 365) {
                return false;
            }
            Set<Integer> seen = new HashSet<>();
            for (int r : rooms) {
                // EXP00-J: the boolean returned by Set.add is used; false means a duplicate room
                if (r < 100 || r > 999 || !seen.add(r)) {
                    return false;
                }
            }
            return true;
        }

        /** @return a COPY of the room numbers (OBJ05-J: never the private array itself) */
        int[] rooms() {
            return rooms.clone();
        }

        /** @return the number of nights */
        int nights() {
            return nights;
        }

        /**
         * Adds nights to the stay. (This is what makes the class mutable and therefore sensitive.)
         *
         * @param more nights to add, 1 to 30
         */
        void extendBy(int more) {
            if (more < 1 || more > 30 || nights + more > 365) {
                throw new IllegalArgumentException("cannot extend by " + more);
            }
            nights += more;
        }

        /**
         * MET53-J: the copy starts from {@code super.clone()}, so it has the right runtime class.
         * MET06-J: nothing overridable is called from here (and the class is final anyway). The mutable
         * array is then copied so original and clone share nothing.
         *
         * @return an independent copy
         */
        @Override
        public Itinerary clone() {
            try {
                Itinerary copy = (Itinerary) super.clone();
                copy.rooms = rooms.clone();
                return copy;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError("Itinerary is Cloneable", e);
            }
        }
    }

    /**
     * One confirmed booking. Callers only ever see copies of its mutable parts.
     *
     * <p>OBJ11-J: final class with a validating constructor. SER05-J: static nested class.
     */
    private static final class Booking implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int id;
        private final int userId;
        // Not final on purpose: readObject replaces it with a private copy after deserialization.
        private Itinerary itinerary;

        /**
         * Creates a booking.
         *
         * @param id booking id, positive
         * @param userId owner's user id, positive
         * @param itinerary rooms and nights (copied)
         * @throws IllegalArgumentException if any argument is invalid
         */
        Booking(int id, int userId, Itinerary itinerary) {
            validate(id, userId, itinerary); // MET05-J: private static, cannot be overridden
            this.id = id;
            this.userId = userId;
            this.itinerary = itinerary.clone(); // OBJ13-J: keep our own copy, not the caller's object
        }

        /** MET00-J: shared validation for the constructor and for readObject. */
        private static void validate(int id, int userId, Itinerary itinerary) {
            if (id < 1 || userId < 1 || itinerary == null || !itinerary.isConsistent()) {
                throw new IllegalArgumentException("invalid booking");
            }
        }

        /** @return the booking id */
        int id() {
            return id;
        }

        /** @return the owner's user id */
        int userId() {
            return userId;
        }

        /** @return a COPY of the itinerary (OBJ05-J); changing it does not change this booking */
        Itinerary itinerary() {
            return itinerary.clone();
        }

        /**
         * Compares the contents of two bookings.
         *
         * <p>EXP02-J: arrays are compared with {@code Arrays.equals}; {@code array.equals(other)}
         * would only test whether both variables point at the same array object.
         *
         * @param other the booking to compare with
         * @return true if id, owner, nights and rooms are all equal
         */
        boolean sameContentAs(Booking other) {
            return other != null
                    && id == other.id
                    && userId == other.userId
                    && itinerary.nights() == other.itinerary.nights()
                    && Arrays.equals(itinerary.rooms(), other.itinerary.rooms());
        }

        /**
         * SER01-J: the hook has exactly the signature the serialization mechanism looks for
         * (private, void, one ObjectInputStream parameter, throws IOException and
         * ClassNotFoundException). A different signature would be silently ignored.
         *
         * <p>SER04-J (concept) and OBJ13-J: the constructor does not run when an object is
         * deserialized, so the same checks are repeated here, and the itinerary is replaced by a
         * private copy in case the stream contained a shared reference.
         *
         * @param in the stream being read
         * @throws IOException if reading fails or the data is invalid
         * @throws ClassNotFoundException if a class in the stream is unknown
         */
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            try {
                validate(id, userId, itinerary);
            } catch (IllegalArgumentException e) {
                throw new InvalidObjectException("invalid booking in stream");
            }
            itinerary = itinerary.clone();
        }

        /** ERR01-J: only ids and counts, no personal data. */
        @Override
        public String toString() {
            return "booking#" + id + "(user#" + userId + ", rooms=" + Arrays.toString(itinerary.rooms())
                    + ", nights=" + itinerary.nights() + ")";
        }
    }

    /**
     * The serialized form of the whole booking book.
     *
     * <p>SER05-J: static nested class. OBJ13-J: the constructor copies the list it is given.
     */
    private static final class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        private final ArrayList<Booking> bookings;
        private final int nextId;

        /**
         * Creates a snapshot.
         *
         * @param bookings the bookings to save (copied)
         * @param nextId the next free booking id
         */
        Snapshot(List<Booking> bookings, int nextId) {
            this.bookings = new ArrayList<>(bookings);
            this.nextId = nextId;
        }
    }

    // =====================================================================================
    // The service itself
    // =====================================================================================

    /**
     * The booking service: validates requests, keeps the books and exposes them safely.
     *
     * <p>MET50-J / MET51-J (absence): this class has no overloaded methods at all. Every operation
     * has its own name ({@code book}, {@code cancelBooking}, {@code bookingsFor}), so a call site
     * can never be misread, and nothing depends on the runtime type of an argument.
     */
    private static final class BookingService {
        // OBJ50-J: these fields are final, but 'final' only fixes the reference. The maps themselves
        // are mutable, so they are never returned or passed out; only copies leave this class.
        private final Config config;
        private final AuditLog audit;
        private final Map<Integer, Booking> bookings = new LinkedHashMap<>();
        private final Map<Integer, Integer> roomOwner = new HashMap<>();
        private int nextId = 1;

        /**
         * Creates the service.
         *
         * @param config a READ-ONLY view of the settings (OBJ56-J)
         * @param audit the internal log
         */
        BookingService(Config config, AuditLog audit) {
            this.config = config;
            this.audit = audit;
        }

        /**
         * Books one room.
         *
         * <p>MET54-J: the created booking is returned, and failures are reported by exceptions, so
         * the caller always learns what happened.
         *
         * @param user the (already looked-up) user
         * @param roomText room number typed by the user (untrusted)
         * @param nightsText number of nights typed by the user (untrusted)
         * @return the confirmed booking
         * @throws BookingException with a user-safe message if the request is refused
         */
        Booking book(User user, String roomText, String nightsText) throws BookingException {
            // ERR08-J: explicit null checks instead of catching NullPointerException
            if (user == null || roomText == null || nightsText == null) {
                throw InvalidInputException.of("All booking fields are required.");
            }
            // ERR50-J: test the text with a regular expression first; no exception is used as an
            // ordinary "this was not a number" signal. MET00-J: validate before using.
            if (!roomText.matches("[1-9][0-9]{2}")) {
                throw InvalidInputException.of("Room must be a 3-digit number.");
            }
            if (!nightsText.matches("[0-9]{1,3}")) {
                throw InvalidInputException.of("Nights must be a whole number.");
            }
            int room = Integer.parseInt(roomText);   // safe: the pattern guarantees digits
            int nights = Integer.parseInt(nightsText);
            if (nights < 1 || nights > config.maxNights()) {
                throw InvalidInputException.of("Nights must be between 1 and " + config.maxNights() + ".");
            }
            if (bookingsFor(user.id()).size() >= config.maxBookingsPerUser()) {
                throw InvalidInputException.of("You have reached the booking limit.");
            }
            Integer holder = roomOwner.get(room);
            if (holder != null) {
                // ERR01-J: the user text does not say who holds the room; the log does
                throw new RoomUnavailableException(room, holder);
            }
            Booking b = new Booking(nextId++, user.id(), new Itinerary(new int[] {room}, nights));
            bookings.put(b.id(), b);
            roomOwner.put(room, user.id());
            audit.event("booking " + b.id() + " created for " + user);
            return b;
        }

        /**
         * Cancels a booking owned by the given user.
         *
         * <p>MET54-J / ERR52-J: the result is a boolean the caller can act on, not a silent void and
         * not an error code mixed into a number.
         *
         * @param userId the user asking
         * @param bookingId the booking to cancel
         * @return true if the booking existed, belonged to the user, and was cancelled
         */
        boolean cancelBooking(int userId, int bookingId) {
            Booking b = bookings.get(bookingId);
            if (b == null) {
                audit.event("cancel refused: no booking " + bookingId);
                return false;
            }
            int room = b.itinerary().rooms()[0];
            Integer owner = roomOwner.get(room);
            // EXP03-J: 'owner' is a boxed Integer. '==' would compare object identity (and only
            // happens to work for small cached values); equals() compares the numbers.
            if (owner == null || !owner.equals(userId)) {
                audit.event("cancel refused: booking " + bookingId + " is not owned by user#" + userId);
                return false;
            }
            bookings.remove(bookingId);
            roomOwner.remove(room);
            audit.event("booking " + bookingId + " cancelled by user#" + userId);
            return true;
        }

        /**
         * Lists a user's bookings.
         *
         * <p>MET55-J: a user without bookings gets an EMPTY list, never null. OBJ50-J / OBJ05-J:
         * {@code List.copyOf} returns a genuinely immutable list; the internal map is not exposed.
         *
         * @param userId the user
         * @return an immutable list, possibly empty
         */
        List<Booking> bookingsFor(int userId) {
            List<Booking> mine = new ArrayList<>();
            for (Booking b : bookings.values()) {
                if (b.userId() == userId) {
                    mine.add(b);
                }
            }
            return List.copyOf(mine);
        }

        /** @return a snapshot of all bookings for saving (copies the list, OBJ13-J) */
        Snapshot snapshot() {
            return new Snapshot(new ArrayList<>(bookings.values()), nextId);
        }

        /**
         * Replaces the current books with restored data.
         *
         * @param s the restored snapshot
         * @throws StorageException if the restored data contradicts itself
         */
        void restore(Snapshot s) throws StorageException {
            bookings.clear();
            roomOwner.clear();
            for (Booking b : s.bookings) {
                int room = b.itinerary().rooms()[0];
                // EXP00-J: Map.put returns the previous value; non-null means two bookings claim one room
                if (roomOwner.put(room, b.userId()) != null) {
                    throw new StorageException("restored data holds room " + room + " twice", null);
                }
                bookings.put(b.id(), b);
            }
            nextId = s.nextId;
            audit.event("restored " + bookings.size() + " booking(s)");
        }
    }

    // =====================================================================================
    // Files: config and snapshot (FIO02, FIO08, FIO50, FIO51, FIO53, SER12, ERR54)
    // =====================================================================================

    /**
     * Writes a small configuration file.
     *
     * <p>FIO50-J: CREATE_NEW creates the file atomically and fails if it already exists, so a file
     * that appeared in the meantime is never overwritten.
     *
     * @param path where to create the file
     * @throws IOException if the file cannot be created
     */
    private static void writeConfigFile(Path path) throws IOException {
        List<String> lines = List.of(
                "hotel=Seaview",
                "maxNights=7",
                "maxBookingsPerUser=lots",      // deliberately invalid: must be ignored, not crash
                "this line has no equals sign"); // deliberately malformed
        Files.write(path, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    /**
     * Loads the configuration, falling back to defaults on any file problem.
     *
     * <p>FIO02-J: the outcome of the file operation is checked and each failure kind is handled and
     * logged; it is never ignored. ERR01-J: the log line names the failure kind, not the path.
     *
     * @param path the file to read
     * @param audit the internal log
     * @return the settings (never null)
     */
    private static Config loadConfig(Path path, AuditLog audit) {
        Config cfg = new Config();
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            audit.event("config file missing, using defaults");
            return cfg;
        } catch (IOException e) {
            audit.event("config file unreadable (" + e.getClass().getSimpleName() + "), using defaults");
            return cfg;
        }
        for (String line : lines) {
            int eq = line.indexOf('=');
            if (eq < 0) {
                audit.event("config line ignored: no '='");
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            // ERR50-J: check the value with a pattern instead of catching NumberFormatException
            boolean numeric = value.matches("[0-9]{1,4}");
            if (key.equals("maxNights") && numeric && Integer.parseInt(value) >= 1 && Integer.parseInt(value) <= 60) {
                cfg.setMaxNights(Integer.parseInt(value));
            } else if (key.equals("maxBookingsPerUser") && numeric && Integer.parseInt(value) >= 1
                    && Integer.parseInt(value) <= 10) {
                cfg.setMaxBookingsPerUser(Integer.parseInt(value));
            } else if (key.equals("hotel") && !value.isBlank() && value.length() <= 40) {
                cfg.setHotel(value);
            } else {
                audit.event("config entry ignored: key '" + key + "' has an unusable value");
            }
        }
        return cfg;
    }

    /** Saves and restores the booking book. All methods are static helpers. */
    private static final class SnapshotStore {

        private SnapshotStore() {
        }

        /**
         * Writes a snapshot to a new file.
         *
         * <p>FIO51-J: before writing, the file system itself (not the path text) is asked whether the
         * target is the same file as the protected config file, so {@code sub/../app.cfg} is caught
         * too. FIO50-J: CREATE_NEW then guarantees the target is not overwritten. FIO53-J:
         * {@code writeObject} (not writeUnshared) keeps shared references shared. ERR54-J: both
         * streams are closed by try-with-resources.
         *
         * @param target the file to create
         * @param protectedFile a file that must never be overwritten
         * @param s the snapshot
         * @throws StorageException if the target is protected, exists, or cannot be written
         */
        static void save(Path target, Path protectedFile, Snapshot s) throws StorageException {
            try {
                if (Files.exists(target) && Files.exists(protectedFile) && Files.isSameFile(target, protectedFile)) {
                    throw new StorageException("save refused: target is the protected config file", null);
                }
                try (OutputStream raw = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW);
                     ObjectOutputStream out = new ObjectOutputStream(raw)) {
                    out.writeObject(s);
                }
            } catch (FileAlreadyExistsException e) {
                throw new StorageException("save refused: target already exists", e);
            } catch (IOException e) {
                throw new StorageException("save failed: " + e.getClass().getSimpleName(), e);
            }
        }

        /**
         * Computes a checksum over the bytes of a file.
         *
         * <p>FIO08-J: {@code read()} returns an int; the value is kept in an {@code int} and compared
         * with -1 BEFORE it is treated as a byte. A {@code byte} variable would turn the data byte
         * 0xFF into -1 and stop early.
         *
         * @param file the file to read
         * @return the CRC-32 of all its bytes
         * @throws StorageException if the file cannot be read
         */
        static long checksum(Path file) throws StorageException {
            CRC32 crc = new CRC32();
            try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
                int b;
                while ((b = in.read()) != -1) {
                    crc.update(b);
                }
            } catch (IOException e) {
                throw new StorageException("checksum failed: " + e.getClass().getSimpleName(), e);
            }
            return crc.getValue();
        }

        /**
         * Restores a snapshot from a file.
         *
         * @param file the file to read
         * @param audit the internal log
         * @return the restored snapshot
         * @throws StorageException if the file is missing, unreadable or rejected
         */
        static Snapshot restoreFile(Path file, AuditLog audit) throws StorageException {
            try {
                return restoreStream(Files.newInputStream(file), audit);
            } catch (IOException e) {
                throw new StorageException("restore failed: " + e.getClass().getSimpleName(), e);
            }
        }

        /**
         * Restores a snapshot from a stream and closes it.
         *
         * <p>SER12-J: untrusted bytes decide which classes get instantiated, so an
         * {@link ObjectInputFilter} allow-list is installed BEFORE the first read. Only our three
         * classes and ArrayList may appear; {@code !*} rejects everything else, and the limits cap
         * depth, references, size and array length. FIO53-J: plain readObject.
         *
         * @param raw the stream to read (closed by this method)
         * @param audit the internal log
         * @return the restored snapshot
         * @throws StorageException if the data is rejected, damaged or of the wrong type
         */
        static Snapshot restoreStream(InputStream raw, AuditLog audit) throws StorageException {
            // ERR54-J: both the raw stream and the ObjectInputStream are closed automatically
            try (InputStream owned = raw; ObjectInputStream in = new ObjectInputStream(owned)) {
                in.setObjectInputFilter(ObjectInputFilter.Config.createFilter(SNAPSHOT_FILTER));
                Object o = in.readObject();
                if (!(o instanceof Snapshot)) {
                    throw new StorageException("restore failed: unexpected top-level object", null);
                }
                return (Snapshot) o;
            } catch (InvalidClassException e) {
                audit.event("restore rejected by filter or class check: " + e.getMessage());
                throw new StorageException("restore rejected: " + e.getClass().getSimpleName(), e);
            } catch (IOException | ClassNotFoundException e) {
                throw new StorageException("restore failed: " + e.getClass().getSimpleName(), e);
            }
        }
    }

    // =====================================================================================
    // Narrated run
    // =====================================================================================

    /** A booking attempt that may be refused with one of our exceptions. */
    @FunctionalInterface
    private interface BookingAttempt {
        /**
         * Performs the attempt.
         *
         * @return the booking if it succeeded
         * @throws BookingException if it was refused
         */
        Booking run() throws BookingException;
    }

    /**
     * Runs one attempt and prints what the USER would see; the detail goes to the log.
     *
     * @param label what is being attempted
     * @param audit the internal log
     * @param attempt the action
     */
    private static void attempt(String label, AuditLog audit, BookingAttempt attempt) {
        try {
            System.out.println("  " + label + " -> " + attempt.run());
        } catch (BookingException e) {
            audit.event("refused: " + e.getMessage());      // ERR01-J: detail only to the log
            System.out.println("  " + label + " -> user sees: \"" + e.userMessage() + "\"");
        }
    }

    /**
     * Finds a user or refuses with a deliberately vague message, so the answer does not reveal
     * which names exist (ERR01-J).
     */
    private static User requireUser(UserRepository users, String name, AuditLog audit) throws BookingException {
        Optional<User> found = users.findByName(name);
        if (found.isEmpty()) {
            throw InvalidInputException.withDetail("We could not process that request.", "unknown user name");
        }
        return found.get();
    }

    /**
     * Deletes the demo's temporary directory.
     *
     * <p>EXP00-J: the boolean returned by {@code deleteIfExists} is used and counted. ERR54-J: the
     * directory stream is closed by try-with-resources.
     *
     * @param dir the directory to remove, or null
     */
    private static void cleanup(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            List<Path> all = walk.sorted(Comparator.reverseOrder()).toList();
            int removed = 0;
            for (Path p : all) {
                if (Files.deleteIfExists(p)) {
                    removed++;
                }
            }
            System.out.println("\n[cleanup] removed " + removed + " temporary entries; directory gone = "
                    + !Files.exists(dir));
        } catch (IOException e) {
            System.out.println("\n[cleanup] could not remove the temp directory (" + e.getClass().getSimpleName() + ")");
        }
    }

    /**
     * Runs the whole story in a private temporary directory.
     *
     * @param dir the demo's own temporary directory
     * @throws IOException if the demo's own files cannot be created
     * @throws BookingException if a step that must succeed is refused
     */
    private static void runStory(Path dir) throws IOException, BookingException {
        AuditLog audit = new AuditLog();
        Path configFile = dir.resolve("app.cfg");
        Path snapshotFile = dir.resolve("bookings.bin");
        Files.createDirectory(dir.resolve("sub")); // lets us spell the config path as sub/../app.cfg

        // ---- 1. configuration ------------------------------------------------------------
        System.out.println("\n[1] Configuration (FIO02-J, FIO50-J, ERR50-J, OBJ56-J)");
        Config fallback = loadConfig(dir.resolve("absent.cfg"), audit);
        System.out.println("  missing file -> defaults: hotel=" + fallback.hotel() + ", maxNights=" + fallback.maxNights());
        writeConfigFile(configFile);
        Config config = loadConfig(configFile, audit);
        System.out.println("  app.cfg loaded: hotel=" + config.hotel() + ", maxNights=" + config.maxNights()
                + ", maxBookingsPerUser=" + config.maxBookingsPerUser() + "  (bad lines were ignored)");
        Config view = new ReadOnlyConfig(config);
        try {
            view.setMaxNights(60);
        } catch (UnsupportedOperationException e) {
            System.out.println("  service's read-only view refuses changes: " + e.getMessage());
        }

        BookingService service = new BookingService(view, audit);

        // ---- 2..5 need the database; try-with-resources closes it however we leave the block ----
        try (UserRepository users = new UserRepository()) {

            System.out.println("\n[2] User lookup (IDS00-J, ERR52-J, ERR01-J)");
            User alice = requireUser(users, "alice", audit);
            User bob = requireUser(users, "bob", audit);
            User carol = requireUser(users, "carol", audit);
            System.out.println("  found " + alice + ", " + bob + ", " + carol + " (e-mail addresses are never printed)");
            String injection = "x' OR '1'='1";
            System.out.println("  lookup of \"" + injection + "\" -> found = " + users.findByName(injection).isPresent()
                    + "  (the text was treated as a name, not as SQL)");
            attempt("lookup of unknown user \"dave\"", audit, () -> {
                requireUser(users, "dave", audit);
                return null;
            });
            try {
                users.findByName("bad\u0007name");
            } catch (InvalidInputException e) {
                System.out.println("  lookup with a control character -> user sees: \"" + e.userMessage() + "\"");
            }

            System.out.println("\n[3] Bookings (MET00-J, ERR50-J, ERR51-J, MET54-J, EXP03-J)");
            Booking first = service.book(alice, "101", "3");
            System.out.println("  alice books room 101 for 3 nights -> " + first);
            attempt("alice, nights \"3; DROP TABLE\"", audit, () -> service.book(alice, "102", "3; DROP TABLE"));
            attempt("alice, nights \"30\" (limit " + config.maxNights() + ")", audit, () -> service.book(alice, "102", "30"));
            attempt("bob, room 101 (already taken)", audit, () -> service.book(bob, "101", "2"));
            attempt("bob, room 202 for 2 nights", audit, () -> service.book(bob, "202", "2"));
            attempt("alice, room 303 for 1 night", audit, () -> service.book(alice, "303", "1"));
            attempt("alice, room 404 (third booking)", audit, () -> service.book(alice, "404", "1"));
            System.out.println("  bob tries to cancel alice's booking#1 -> " + service.cancelBooking(bob.id(), 1));
            System.out.println("  alice cancels her booking#3 -> " + service.cancelBooking(alice.id(), 3));

            System.out.println("\n[4] Exposure of booking data (OBJ05-J, OBJ13-J, OBJ50-J, MET55-J, MET53-J)");
            List<Booking> alices = service.bookingsFor(alice.id());
            System.out.println("  alice's bookings: " + alices);
            try {
                alices.clear();
            } catch (UnsupportedOperationException e) {
                System.out.println("  caller tried to clear the returned list -> refused (immutable list)");
            }
            Itinerary copy = alices.get(0).itinerary();
            copy.extendBy(5);
            System.out.println("  caller extended its COPY to " + copy.nights() + " nights; the service still has "
                    + service.bookingsFor(alice.id()).get(0).itinerary().nights());
            int[] rooms = alices.get(0).itinerary().rooms();
            rooms[0] = 999;
            System.out.println("  caller overwrote its copy of the room array; the service still has "
                    + Arrays.toString(service.bookingsFor(alice.id()).get(0).itinerary().rooms()));
            List<Booking> none = service.bookingsFor(carol.id());
            int count = 0;
            for (Booking b : none) {
                count += b.id() > 0 ? 1 : 0;
            }
            System.out.println("  carol has no bookings -> empty list (size " + none.size() + "), caller's loop ran " + count + " times, no null check needed");

            System.out.println("\n[5] Save and restore (SER01-J, SER04-J, SER12-J, FIO08-J, FIO50/51/53-J, EXP02-J)");
            Path spelledDifferently = dir.resolve("sub").resolve("..").resolve("app.cfg");
            try {
                SnapshotStore.save(spelledDifferently, configFile, service.snapshot());
            } catch (StorageException e) {
                audit.event("refused: " + e.getMessage());
                System.out.println("  saving onto sub/../app.cfg -> user sees: \"" + e.userMessage()
                        + "\"  (the config file is protected even under another spelling)");
            }
            SnapshotStore.save(snapshotFile, configFile, service.snapshot());
            long before = SnapshotStore.checksum(snapshotFile);
            System.out.println("  saved bookings.bin; checksum read back is stable = " + (before == SnapshotStore.checksum(snapshotFile)));
            try {
                SnapshotStore.save(snapshotFile, configFile, service.snapshot());
            } catch (StorageException e) {
                audit.event("refused: " + e.getMessage());
                System.out.println("  saving again onto the existing file -> user sees: \"" + e.userMessage() + "\"");
            }
            Snapshot restored = SnapshotStore.restoreFile(snapshotFile, audit);
            BookingService second = new BookingService(view, audit);
            second.restore(restored);
            Booking original = service.bookingsFor(alice.id()).get(0);
            Booking again = second.bookingsFor(alice.id()).get(0);
            System.out.println("  restored into a fresh service: " + again);
            System.out.println("  contents equal (Arrays.equals on rooms) = " + original.sameContentAs(again)
                    + ", same object = " + (original == again));

            ByteArrayOutputStream hostile = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(hostile)) {
                out.writeObject(new java.util.Date(0L)); // a class our application never expects
            }
            try {
                SnapshotStore.restoreStream(new ByteArrayInputStream(hostile.toByteArray()), audit);
            } catch (StorageException e) {
                System.out.println("  unexpected class in a saved file -> user sees: \"" + e.userMessage() + "\"");
            }
            try {
                SnapshotStore.restoreFile(dir.resolve("missing.bin"), audit);
            } catch (StorageException e) {
                audit.event("refused: " + e.getMessage());
                System.out.println("  restoring a file that does not exist -> user sees: \"" + e.userMessage() + "\"");
            }
        }

        // ---- 6. internal log -------------------------------------------------------------
        System.out.println("\n[6] Internal audit log (ERR01-J, ERR02-J): staff only, no e-mail addresses, no paths");
        for (String line : audit.lines()) {
            System.out.println("  log: " + line);
        }
    }

    /**
     * Runs the narrated demo of the Seaview booking service.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== IntegratedDemo: Seaview booking service (compliant version) ===");
        Path dir = null;
        try {
            dir = Files.createTempDirectory("integrated");
            runStory(dir);
        } catch (IOException | BookingException e) {
            // ERR01-J: even the demo's own top level prints only a generic sentence
            System.out.println("The demo could not complete (" + e.getClass().getSimpleName() + ").");        } finally {
            cleanup(dir);
        }
    }
}
