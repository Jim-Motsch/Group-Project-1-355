package cert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * FIO02-J: Detect and handle file-related errors.
 *
 * <p>Owner: Martin
 *
 * <p>File operations fail routinely (missing file, no permission, disk full).
 * Some legacy methods such as {@link File#delete()} signal failure only through
 * a boolean, and I/O methods throw checked exceptions. Ignoring either leaves the
 * program believing an operation succeeded. The compliant versions use the
 * {@code java.nio.file} methods that throw descriptive exceptions and handle
 * them.
 */
public class Fio02J {

    /**
     * NONCOMPLIANT: swallows the IOException and returns an empty string, so
     * the caller cannot tell "empty file" from "file could not be read".
     *
     * @param path file to read
     * @return the file content, or "" on any failure
     */
    static String noncompliantRead(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            // violates FIO02-J: error ignored
            return "";
        }
    }

    /**
     * COMPLIANT: handles the missing-file case explicitly and falls back to
     * documented defaults, and reports any other I/O failure.
     *
     * @param path file to read
     * @return the file content, or a message describing the fallback taken
     */
    static String compliantRead(Path path) {
        try {
            return Files.readString(path);
        } catch (NoSuchFileException e) {
            return "<using defaults: " + path.getFileName() + " not found>";
        } catch (IOException e) {
            return "<read failed: " + e.getClass().getSimpleName() + ">";
        }
    }

    /**
     * NONCOMPLIANT: ignores the boolean returned by {@link File#delete()}, then
     * claims the cleanup worked.
     *
     * @param path file to delete
     * @return always "cleanup finished"
     */
    static String noncompliantDelete(Path path) {
        path.toFile().delete(); // violates FIO02-J: failure flag ignored
        return "cleanup finished";
    }

    /**
     * COMPLIANT: uses {@link Files#delete(Path)}, which throws a specific
     * exception on failure, and reports the outcome truthfully.
     *
     * @param path file to delete
     * @return a message that reflects whether the delete really happened
     */
    static String compliantDelete(Path path) {
        try {
            Files.delete(path);
            return "cleanup finished";
        } catch (NoSuchFileException e) {
            return "cleanup FAILED: " + path.getFileName() + " does not exist";
        } catch (IOException e) {
            return "cleanup FAILED: " + e.getClass().getSimpleName();
        }
    }

    /**
     * Runs the scenarios against a temporary directory.
     *
     * @param args unused
     * @throws IOException if the temporary directory cannot be created or cleaned up
     */
    public static void main(String[] args) throws IOException {
        System.out.println("=== FIO02-J: Detect and handle file-related errors ===");

        Path dir = Files.createTempDirectory("fio02");
        Path missing = dir.resolve("settings.cfg"); // never created
        Path empty = Files.createFile(dir.resolve("empty.cfg"));

        System.out.println("\n-- Scenario 1: reading settings.cfg, which does not exist --");
        System.out.println("noncompliant : [" + noncompliantRead(missing) + "]");
        System.out.println("compliant    : [" + compliantRead(missing) + "]");
        System.out.println("-> noncompliant returns \"\", the same as an empty file; the error is invisible.");

        System.out.println("\n-- Scenario 2: deleting settings.cfg, which does not exist --");
        System.out.println("noncompliant : " + noncompliantDelete(missing));
        System.out.println("compliant    : " + compliantDelete(missing));
        System.out.println("-> noncompliant reports success for an operation that did nothing.");

        System.out.println("\n-- Scenario 3 (CONTROL CASE): reading a real empty file; identical output is expected --");
        System.out.println("noncompliant : [" + noncompliantRead(empty) + "]");
        System.out.println("compliant    : [" + compliantRead(empty) + "]");

        Files.delete(empty);
        Files.delete(dir);
    }
}
