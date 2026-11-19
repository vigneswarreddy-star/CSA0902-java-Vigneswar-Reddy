import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Version 2 (manual close with try-catch-finally): the reader is closed in finally on every path.
 * A failure in close() itself is caught so it cannot hide the real error.
 */
public class ReportManual {
    public static void main(String[] args) throws IOException {
        long before = ReportCommon.openFileDescriptors();
        int status = run(Path.of(args[0]), Path.of(args[1]));
        long after = ReportCommon.openFileDescriptors();
        System.out.println("status=" + status + " openFDs before=" + before + " after=" + after + " leaked=" + (after - before));
    }

    static int run(Path dir, Path out) throws IOException {
        Map<String, Long> totals = new TreeMap<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.sorted().toList()) {
                BufferedReader reader = null;
                try {
                    reader = Files.newBufferedReader(f);
                    totals.put(f.getFileName().toString(), ReportCommon.readStore(reader, f.toString()));
                } catch (IOException e) {
                    ReportCommon.LOG.severe("FAILED on " + f.getFileName() + ": " + e.getMessage());
                    return 1;
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException closeError) {
                            ReportCommon.LOG.warning("close failed for " + f.getFileName() + ": " + closeError.getMessage());
                        }
                    }
                }
            }
        }
        ReportCommon.writeReport(out, totals);
        return 0;
    }
}
