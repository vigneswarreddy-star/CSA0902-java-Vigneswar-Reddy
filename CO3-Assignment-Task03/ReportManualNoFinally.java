import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Version 1 (the naive manual version): close() is only reached on the success path.
 * On a failure the reader for the failing file is never closed -> LEAK.
 */
public class ReportManualNoFinally {
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
                    reader.close();                                   // only on success
                } catch (IOException e) {
                    ReportCommon.LOG.severe("FAILED on " + f.getFileName() + ": " + e.getMessage());
                    return 1;                                         // reader still open here
                }
            }
        }
        ReportCommon.writeReport(out, totals);
        return 0;
    }
}
