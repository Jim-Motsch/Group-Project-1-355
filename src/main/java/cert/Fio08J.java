package cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * FIO08-J: Distinguish between characters or bytes read from a stream and -1.
 *
 * <p>Owner: Alex
 *
 * <p>{@code InputStream.read()} returns an {@code int}: 0 to 255 for a real
 * byte, or -1 for end of stream. If the result is first stored in a
 * {@code byte}, the real byte 0xFF (255) turns into -1 and looks like the end
 * of the stream, so the loop stops early and the rest of the data is silently
 * dropped. Keep the result in an {@code int}, compare it with -1, and only then
 * cast it to {@code byte}.
 *
 * <p>The streams here are in memory ({@link ByteArrayInputStream}).
 */
public class Fio08J {

    /** Five bytes; the third one is 0xFF, a perfectly valid data byte. */
    private static byte[] sample() {
        return new byte[] {1, 2, (byte) 0xFF, 3, 4};
    }

    /**
     * NONCOMPLIANT: narrows the return value to {@code byte} before comparing with -1.
     *
     * @param in the stream to read to its end
     * @return the bytes it believes it read
     */
    static byte[] noncompliant(InputStream in) throws IOException {
        byte[] buf = new byte[16];
        int n = 0;
        byte b;
        while ((b = (byte) in.read()) != -1) {   // violates FIO08-J: 0xFF becomes -1
            buf[n++] = b;
        }
        return Arrays.copyOf(buf, n);
    }

    /**
     * COMPLIANT: keeps the return value in an {@code int} and compares that with -1.
     *
     * @param in the stream to read to its end
     * @return all the bytes of the stream
     */
    static byte[] compliant(InputStream in) throws IOException {
        byte[] buf = new byte[16];
        int n = 0;
        int data;
        while ((data = in.read()) != -1) {
            buf[n++] = (byte) data;              // cast only after the end-of-stream test
        }
        return Arrays.copyOf(buf, n);
    }

    private static String show(byte[] bytes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bytes.length; i++) {
            sb.append(i == 0 ? "" : ", ").append(bytes[i] & 0xFF);
        }
        return sb.append("] (").append(bytes.length).append(" bytes)").toString();
    }

    /**
     * Reads the same five bytes with both versions.
     *
     * @param args unused
     */
    public static void main(String[] args) throws IOException {
        System.out.println("=== FIO08-J: Distinguish between characters or bytes read from a stream and -1 ===");

        System.out.println("\n-- Scenario 1: stream holds the bytes 1, 2, 255, 3, 4 --");
        System.out.println("noncompliant: " + show(noncompliant(new ByteArrayInputStream(sample()))));
        System.out.println("compliant   : " + show(compliant(new ByteArrayInputStream(sample()))));
        System.out.println("-> noncompliant took the data byte 255 for end-of-stream and lost the last two bytes.");

        System.out.println("\n-- Scenario 2 (CONTROL CASE): stream without a 255 byte; identical output is expected --");
        byte[] plain = {1, 2, 3};
        System.out.println("noncompliant: " + show(noncompliant(new ByteArrayInputStream(plain))));
        System.out.println("compliant   : " + show(compliant(new ByteArrayInputStream(plain))));
    }
}
