package cert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * FIO51-J: Identify files using multiple file attributes.
 *
 * <p>Owner: Alex
 *
 * <p>Recommendation. A file name or path string is not an identity: different
 * strings can name the same file ({@code dir/sub/../a.txt}), and the same name
 * can belong to different files in different directories. Ask the file system
 * whether two paths are the same file ({@code Files.isSameFile}, which compares
 * file-system attributes such as the file key) instead of comparing text.
 *
 * <p>All files live in a temporary directory owned by the demo and deleted at the end.
 */
public class Fio51J {

    /** NONCOMPLIANT: decides "same file?" by comparing path text (scenario 1) or file names (scenario 2). */
    static boolean noncompliantSame(Path a, Path b, boolean byName) {
        return byName ? a.getFileName().equals(b.getFileName()) : a.equals(b);   // violates FIO51-J
    }

    /** COMPLIANT: lets the file system compare the actual files. */
    static boolean compliantSame(Path a, Path b) throws IOException {
        return Files.isSameFile(a, b);
    }

    /**
     * Compares two spellings of one file, and two different files with the same name.
     *
     * @param args unused
     */
    public static void main(String[] args) throws IOException {
        System.out.println("=== FIO51-J: Identify files using multiple file attributes ===");
        Path dir = Files.createTempDirectory("fio51");
        try {
            Path sub = Files.createDirectory(dir.resolve("sub"));
            Path other = Files.createDirectory(dir.resolve("other"));
            Path file = Files.writeString(dir.resolve("data.txt"), "one");
            Path spelledDifferently = sub.resolve("..").resolve("data.txt");
            Path sameNameElsewhere = Files.writeString(other.resolve("data.txt"), "two");

            System.out.println("\n-- Scenario 1: data.txt and sub/../data.txt (two spellings, ONE file) --");
            System.out.println("noncompliant (compare paths): same file = " + noncompliantSame(file, spelledDifferently, false));
            System.out.println("compliant    (isSameFile)   : same file = " + compliantSame(file, spelledDifferently));

            System.out.println("\n-- Scenario 2: data.txt and other/data.txt (same name, TWO files) --");
            System.out.println("noncompliant (compare names): same file = " + noncompliantSame(file, sameNameElsewhere, true));
            System.out.println("compliant    (isSameFile)   : same file = " + compliantSame(file, sameNameElsewhere));
        } finally {
            try (Stream<Path> walk = Files.walk(dir)) {
                for (Path p : (Iterable<Path>) walk.sorted(Comparator.reverseOrder())::iterator) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }
}
