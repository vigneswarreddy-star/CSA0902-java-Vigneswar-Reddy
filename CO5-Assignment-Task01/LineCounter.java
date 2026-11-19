import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/** Task A2 / B1.3: counts lines unbuffered (one byte per read) and buffered (8 KB array). */
public class LineCounter {

    /** Returns {lines, readCalls}. Each read() call is a request to the operating system. */
    public static long[] unbuffered(Path p) throws IOException {
        long lines = 0, calls = 0;
        try (InputStream in = new FileInputStream(p.toFile())) {
            while (true) {
                int b = in.read();
                calls++;
                if (b == -1) break;
                if (b == '\n') lines++;
            }
        }
        return new long[]{lines, calls};
    }

    /** Returns {lines, readCalls}. One read(byte[]) call fills an 8 KB buffer. */
    public static long[] buffered(Path p) throws IOException {
        long lines = 0, calls = 0;
        byte[] buf = new byte[8192];
        try (InputStream in = new FileInputStream(p.toFile())) {
            int n;
            while ((n = in.read(buf)) != -1) {
                calls++;
                for (int i = 0; i < n; i++) {
                    if (buf[i] == '\n') lines++;
                }
            }
        }
        return new long[]{lines, calls};
    }
}
