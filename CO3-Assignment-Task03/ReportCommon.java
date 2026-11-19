import java.io.BufferedReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

/** Shared parsing and writing so that all three report versions produce identical output. */
final class ReportCommon {
    static final Logger LOG = Logger.getLogger("ReportGenerator");

    private ReportCommon() {}

    /** Sums the amounts of one store file. A bad amount throws NumberFormatException with the line number. */
    static long readStore(BufferedReader reader, String fileName) throws IOException {
        long total = 0;
        String line;
        int lineNo = 0;
        while ((line = reader.readLine()) != null) {
            lineNo++;
            String[] parts = line.split(",");
            try {
                total += Long.parseLong(parts[1].trim());
            } catch (NumberFormatException e) {
                throw new IOException("bad amount at " + fileName + " line " + lineNo + ": \"" + parts[1] + "\"", e);
            }
        }
        return total;
    }

    static void writeReport(Path out, Map<String, Long> totals) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(out)) {
            long grand = 0;
            for (Map.Entry<String, Long> e : totals.entrySet()) {
                w.write(e.getKey() + " " + e.getValue());
                w.newLine();
                grand += e.getValue();
            }
            w.write("TOTAL " + grand);
            w.newLine();
        }
    }

    static long openFileDescriptors() {
        java.lang.management.OperatingSystemMXBean os = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        return ((com.sun.management.UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount();
    }
}
