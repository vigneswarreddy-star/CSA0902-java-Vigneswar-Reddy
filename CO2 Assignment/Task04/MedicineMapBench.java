import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Q4 - Hospital pharmacy inventory: HashMap vs TreeMap vs LinkedHashMap,
 * plus a sorted-array + binarySearch alternative.
 * Acceptability criterion (defined in the report): average in-memory lookup <= 1,000 ns (1 microsecond).
 */
public class MedicineMapBench {
    static final int[] SIZES = {1_000, 10_000, 100_000};
    static final int LOOKUPS = 100_000;
    static final int WARMUP = 2, REPEATS = 5;
    static volatile long sink;

    static class Medicine {
        final String code; final String name; int stock;
        Medicine(String code, String name, int stock) { this.code = code; this.name = name; this.stock = stock; }
    }

    static String code(int id) { return String.format("MED%07d", id); }   // fixed width: string order == numeric order

    static double meanMs(Runnable r) {
        for (int i = 0; i < WARMUP; i++) r.run();
        double total = 0;
        for (int i = 0; i < REPEATS; i++) {
            long t0 = System.nanoTime();
            r.run();
            total += System.nanoTime() - t0;
        }
        return total / REPEATS / 1_000_000.0;
    }

    /** Builds the report text for a map; returns the text and whether it is in the required order. */
    static String codeOrderReport(String kind, Map<String, Medicine> m) {
        StringBuilder sb = new StringBuilder();
        if (kind.equals("HashMap")) {
            List<String> keys = new ArrayList<>(m.keySet());   // extra work: copy + sort
            Collections.sort(keys);
            for (String k : keys) sb.append(k).append(' ');
        } else {
            for (String k : m.keySet()) sb.append(k).append(' ');  // TreeMap / LinkedHashMap iteration
        }
        return sb.toString();
    }

    static boolean isSorted(String text) {
        String[] t = text.trim().split(" ");
        for (int i = 1; i < t.length; i++) if (t[i - 1].compareTo(t[i]) > 0) return false;
        return true;
    }

    public static void main(String[] args) {
        List<String> csv = new ArrayList<>();
        csv.add("size,structure,build_ms,lookups_100k_ms,ns_per_lookup,codeorder_report_ms,codeorder_sorted,addorder_report_ms,addorder_matches_insertion");
        System.out.printf("%-8s %-16s %10s %14s %12s %16s %10s %16s %12s%n", "size", "structure",
                "build ms", "100k lookup ms", "ns/lookup", "code-order ms", "sorted?", "add-order ms", "insert-ord?");

        for (int n : SIZES) {
            // Insertion order is a random permutation, so LinkedHashMap's order is NOT the code order.
            List<Integer> ids = new ArrayList<>();
            for (int i = 0; i < n; i++) ids.add(i);
            Collections.shuffle(ids, new Random(7));
            List<String> insertOrder = new ArrayList<>(n);
            for (int id : ids) insertOrder.add(code(id));
            // 100,000 random probes of existing codes
            Random rnd = new Random(11);
            String[] probes = new String[LOOKUPS];
            for (int i = 0; i < LOOKUPS; i++) probes[i] = code(rnd.nextInt(n));

            for (String kind : new String[]{"HashMap", "TreeMap", "LinkedHashMap"}) {
                Map<String, Medicine> m = kind.equals("HashMap") ? new HashMap<>(n * 2)
                        : kind.equals("TreeMap") ? new TreeMap<>() : new LinkedHashMap<>(n * 2);
                // build (put) time
                double buildMs = meanMs(() -> {
                    Map<String, Medicine> fresh = kind.equals("HashMap") ? new HashMap<>(n * 2)
                            : kind.equals("TreeMap") ? new TreeMap<>() : new LinkedHashMap<>(n * 2);
                    for (String c : insertOrder) fresh.put(c, new Medicine(c, "Drug-" + c, 10));
                });
                for (String c : insertOrder) m.put(c, new Medicine(c, "Drug-" + c, 10));

                // 100,000 lookups
                double lookupMs = meanMs(() -> {
                    long s = 0;
                    for (String p : probes) s += m.get(p).stock;
                    sink += s;
                });
                double nsPerLookup = lookupMs * 1_000_000.0 / LOOKUPS;

                // Code-order report
                final String[] rep = new String[1];
                double codeMs = meanMs(() -> rep[0] = codeOrderReport(kind, m));
                boolean sorted = isSorted(rep[0]);

                // Add-order (insertion order) report: LinkedHashMap iterates directly; HashMap cannot;
                // TreeMap needs an extra list that remembers insertion order.
                final String[] addRep = new String[1];
                double addMs = meanMs(() -> {
                    StringBuilder sb = new StringBuilder();
                    if (kind.equals("LinkedHashMap")) {
                        for (String k : m.keySet()) sb.append(k).append(' ');
                    } else if (kind.equals("HashMap")) {
                        for (String k : m.keySet()) sb.append(k).append(' ');  // hash order
                    } else {
                        for (String k : insertOrder) sb.append(k).append(' ');  // side list
                    }
                    addRep[0] = sb.toString();
                });
                boolean addMatches = addRep[0].equals(String.join(" ", insertOrder) + " ")
                        || addRep[0].trim().equals(String.join(" ", insertOrder));

                System.out.printf("%-8d %-16s %10.2f %14.2f %12.1f %16.2f %10s %16.2f %12s%n", n, kind,
                        buildMs, lookupMs, nsPerLookup, codeMs, sorted, addMs, addMatches);
                csv.add(String.format("%d,%s,%.3f,%.3f,%.1f,%.3f,%b,%.3f,%b", n, kind, buildMs, lookupMs,
                        nsPerLookup, codeMs, sorted, addMs, addMatches));
            }

            // Alternative: sorted array + Arrays.binarySearch
            String[] sortedCodes = insertOrder.toArray(new String[0]);
            Arrays.sort(sortedCodes);
            Medicine[] meds = new Medicine[n];
            for (int i = 0; i < n; i++) meds[i] = new Medicine(sortedCodes[i], "Drug-" + sortedCodes[i], 10);
            double bsMs = meanMs(() -> {
                long s = 0;
                for (String p : probes) s += meds[Arrays.binarySearch(sortedCodes, p)].stock;
                sink += s;
            });
            double bsNs = bsMs * 1_000_000.0 / LOOKUPS;
            // 1,000 inserts into the sorted array (each shifts up to n elements)
            double insMs = meanMs(() -> {
                String[] codes = Arrays.copyOf(sortedCodes, n + 1000);
                Medicine[] ms = Arrays.copyOf(meds, n + 1000);
                int size = n;
                for (int i = 0; i < 1000; i++) {
                    String c = "MEW" + String.format("%07d", i);
                    int pos = Arrays.binarySearch(codes, 0, size, c);
                    pos = pos >= 0 ? pos : -pos - 1;
                    System.arraycopy(codes, pos, codes, pos + 1, size - pos);
                    System.arraycopy(ms, pos, ms, pos + 1, size - pos);
                    codes[pos] = c; ms[pos] = new Medicine(c, "New", 1);
                    size++;
                }
                sink += size;
            });
            System.out.printf("%-8d %-16s %10s %14.2f %12.1f %16s %10s %16.2f %12s%n", n, "SortedArray+bs",
                    "-", bsMs, bsNs, "(sorted)", "true", insMs, "-");
            csv.add(String.format("%d,SortedArray+binarySearch,,%.3f,%.1f,,true,%.3f,", n, bsMs, bsNs, insMs));
            System.out.println();
        }
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("results"));
            java.nio.file.Files.write(java.nio.file.Path.of("results/map_bench.csv"), csv);
            System.out.println("Raw results written to results/map_bench.csv (sink=" + sink + ")");
        } catch (Exception e) {
            System.out.println("Could not write CSV: " + e);
        }
    }
}
