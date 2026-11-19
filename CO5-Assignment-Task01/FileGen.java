import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Generates pass records. Each line is about 40 bytes. */
public class FileGen {

    static String record(int i) {
        return String.format("P%06d,%09d,Student%d,%d,%d.0",
                i, 192300000 + i, i, (i % 40) + 1, 3000 + (i % 2000));
    }

    /** Writes records until the file reaches at least targetBytes. Returns the number written. */
    public static int toSize(Path p, long targetBytes) throws IOException {
        int i = 0;
        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            long written = 0;
            while (written < targetBytes) {
                String line = record(++i);
                w.write(line);
                w.newLine();
                written += line.length() + 1;
            }
        }
        return i;
    }

    /** Writes exactly n records. */
    public static void count(Path p, int n) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            for (int i = 1; i <= n; i++) {
                w.write(record(i));
                w.newLine();
            }
        }
    }
}
