import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** B1.3: time to count lines in 1 MB, 10 MB and 50 MB files, unbuffered and buffered. */
public class BufferingBenchmark {
    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "data");
        Files.createDirectories(dir);
        long[] sizesMb = {1, 10, 50};
        System.out.println("size_MB\tlines\tunbuffered_ms\tunbuffered_read_calls\tbuffered_ms\tbuffered_read_calls\tfile_bytes");
        for (long mb : sizesMb) {
            Path p = dir.resolve("log_" + mb + "mb.txt");
            FileGen.toSize(p, mb * 1024 * 1024);
            long bytes = Files.size(p);

            // Buffered: median of 5 runs (fast).
            long[] bufMs = new long[5];
            long[] bufRes = null;
            for (int r = 0; r < 5; r++) {
                long t0 = System.nanoTime();
                bufRes = LineCounter.buffered(p);
                bufMs[r] = (System.nanoTime() - t0) / 1_000_000;
            }
            java.util.Arrays.sort(bufMs);

            // Unbuffered: 1 run for 50 MB (it is slow), 3 runs for smaller files.
            int unbufRuns = mb == 50 ? 1 : 3;
            long[] unMs = new long[unbufRuns];
            long[] unRes = null;
            for (int r = 0; r < unbufRuns; r++) {
                long t0 = System.nanoTime();
                unRes = LineCounter.unbuffered(p);
                unMs[r] = (System.nanoTime() - t0) / 1_000_000;
            }
            java.util.Arrays.sort(unMs);

            if (unRes[0] != bufRes[0]) throw new IllegalStateException("line counts differ");
            System.out.println(mb + "\t" + unRes[0] + "\t" + unMs[unMs.length / 2] + "\t" + unRes[1]
                    + "\t" + bufMs[2] + "\t" + bufRes[1] + "\t" + bytes);
        }
    }
}
