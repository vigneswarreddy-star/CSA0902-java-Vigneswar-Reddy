import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * B1.4: time to find a pass as the file grows from 1,000 to 100,000 records.
 * Also the Alternative analysis: 1,000 searches with a scan versus one HashMap load.
 */
public class GrowthBenchmark {
    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "data");
        Files.createDirectories(dir);
        int[] sizes = {1_000, 10_000, 100_000};
        System.out.println("records\tfile_bytes\tfind_first_ms(median)\tfind_last_ms(median)\tfind_missing_ms(median)");
        for (int n : sizes) {
            Path p = dir.resolve("passes_" + n + ".csv");
            FileGen.count(p, n);
            PassStore store = new PassStore(p);
            String first = FileGen.record(1).split(",")[0];
            String last = FileGen.record(n).split(",")[0];
            String missing = "P999999";
            long[] tf = new long[5], tl = new long[5], tm = new long[5];
            for (int r = 0; r < 5; r++) {
                long t0 = System.nanoTime(); store.find(first); tf[r] = (System.nanoTime() - t0) / 1000;
                t0 = System.nanoTime(); store.find(last); tl[r] = (System.nanoTime() - t0) / 1000;
                t0 = System.nanoTime(); store.find(missing); tm[r] = (System.nanoTime() - t0) / 1000;
            }
            Arrays.sort(tf); Arrays.sort(tl); Arrays.sort(tm);
            System.out.println(n + "\t" + Files.size(p) + "\t" + tf[2] / 1000.0 + "\t" + tl[2] / 1000.0 + "\t" + tm[2] / 1000.0);
        }

        // Alternative: q = 1000 lookups, scan every time versus one HashMap load.
        Path p = dir.resolve("passes_100000.csv");
        PassStore store = new PassStore(p);
        int q = 1000;
        long t0 = System.nanoTime();
        for (int i = 1; i <= q; i++) store.find(FileGen.record(i * 97 % 100_000 + 1).split(",")[0]);
        long scanMs = (System.nanoTime() - t0) / 1_000_000;

        t0 = System.nanoTime();
        Map<String, String[]> map = new HashMap<>();
        for (String line : Files.readAllLines(p)) {
            String[] f = line.split(",", -1);
            if (f.length == 5) map.put(f[0], f);
        }
        long loadMs = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime();
        int hits = 0;
        for (int i = 1; i <= q; i++) if (map.get(FileGen.record(i * 97 % 100_000 + 1).split(",")[0]) != null) hits++;
        long lookupMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("ALT q=" + q + " n=100000: scan_each_time_ms=" + scanMs
                + " | hashmap_load_ms=" + loadMs + " + lookups_ms=" + lookupMs + " hits=" + hits
                + " | hashmap_entries=" + map.size());
    }
}
