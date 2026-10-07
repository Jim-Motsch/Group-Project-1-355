package cert;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * FIO50-J: Do not make assumptions about file creation.
 *
 * <p>Owner: Martin
 *
 * <p>Recommendation. Checking {@code exists()} and then creating the file are
 * two steps; another process can create the file in between, and the default
 * write then silently overwrites it. Create the file in one atomic step
 * ({@code CREATE_NEW}) and handle the failure if it already exists.
 *
 * <p>The other process is simulated by a {@link Runnable} that runs exactly
 * between the two steps, so the result is the same every run. All files live
 * in a temporary directory owned by the demo and deleted at the end.
 */
public class Fio50J {

    /**
     * NONCOMPLIANT: check, then create (the default options truncate an existing file).
     *
     * @param p target file
     * @param intruder code that runs after the check, simulating another process
     * @return what the program reported
     */
    static String noncompliant(Path p, Runnable intruder) throws IOException {
        if (Files.exists(p)) {
            return "file exists, not touched";
        }
        intruder.run();                      // the race window
        Files.writeString(p, "mine");        // violates FIO50-J: overwrites whatever appeared meanwhile
        return "created my file";
    }

    /**
     * COMPLIANT: one atomic create-if-absent.
     *
     * @param p target file
     * @param intruder code that runs just before the create, simulating another process
     * @return what the program reported
     */
    static String compliant(Path p, Runnable intruder) throws IOException {
        intruder.run();
        try {
            Files.writeString(p, "mine", StandardOpenOption.CREATE_NEW);
            return "created my file";
        } catch (FileAlreadyExistsException e) {
            return "someone else created it first, left it alone";
        }
    }

    /**
     * Runs both versions in a private temp directory.
     *
     * @param args unused
     */
    public static void main(String[] args) throws IOException {
        System.out.println("=== FIO50-J: Do not make assumptions about file creation ===");
        Path dir = Files.createTempDirectory("fio50");
        try {
            System.out.println("\n-- Scenario 1: another process creates the file right after our decision to create it --");
            Path a = dir.resolve("a.txt");
            String r1 = noncompliant(a, () -> write(a, "someone else's data"));
            System.out.println("noncompliant: " + r1 + "; file now contains [" + Files.readString(a) + "]");
            Path b = dir.resolve("b.txt");
            String r2 = compliant(b, () -> write(b, "someone else's data"));
            System.out.println("compliant   : " + r2 + "; file now contains [" + Files.readString(b) + "]");
            System.out.println("-> noncompliant destroyed the other file and reported success.");

            System.out.println("\n-- Scenario 2 (CONTROL CASE): nobody interferes; identical output is expected --");
            Path c = dir.resolve("c.txt");
            Path d = dir.resolve("d.txt");
            System.out.println("noncompliant: " + noncompliant(c, () -> { }) + "; contains [" + Files.readString(c) + "]");
            System.out.println("compliant   : " + compliant(d, () -> { }) + "; contains [" + Files.readString(d) + "]");
        } finally {
            try (var files = Files.list(dir)) {
                for (Path f : (Iterable<Path>) files::iterator) {
                    Files.deleteIfExists(f);
                }
            }
            Files.deleteIfExists(dir);
        }
    }

    private static void write(Path p, String s) {
        try {
            Files.writeString(p, s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
