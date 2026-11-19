import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Q1 - Builds railway e-tickets with String (+), StringBuilder and StringBuffer.
 * Measures: average time over repeated runs, bytes allocated (churn),
 * bytes retained by the output, and an estimate of intermediate String objects.
 */
public class TicketBuilderBench {
    static final int WARMUP = 2;
    static final int REPEATS = 5;
    static volatile long sink;   // keeps the JIT from removing the work

    interface Approach { String build(String[] fields); }

    static String withPlus(String[] fields) {
        String ticket = "";
        for (String f : fields) ticket += f + "|";   // new String objects every iteration
        return ticket;
    }

    static String withBuilder(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (String f : fields) sb.append(f).append('|');
        return sb.toString();
    }

    static String withBuffer(String[] fields) {
        StringBuffer sb = new StringBuffer();
        for (String f : fields) sb.append(f).append('|');
        return sb.toString();
    }

    static String[] makeFields(int n, int ticketId) {
        String[] f = new String[n];
        for (int i = 0; i < n; i++) {
            f[i] = "F" + i + "=PAX" + ticketId + "-" + (i * 37 % 1000);
        }
        return f;
    }

    /** Workload = list of field arrays, one per ticket to build. */
    static String[][] workload(String name, int tickets, int fieldsPerTicket) {
        String[][] w = new String[tickets][];
        for (int t = 0; t < tickets; t++) w[t] = makeFields(fieldsPerTicket, t);
        return w;
    }

    static double timeMs(Approach a, String[][] w) {
        long t0 = System.nanoTime();
        long h = 0;
        for (String[] fields : w) h += a.build(fields).length();
        long t1 = System.nanoTime();
        sink += h;
        return (t1 - t0) / 1_000_000.0;
    }

    static long allocatedBytes(Approach a, String[][] w) {
        com.sun.management.ThreadMXBean mx =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long a0 = mx.getCurrentThreadAllocatedBytes();
        long h = 0;
        for (String[] fields : w) h += a.build(fields).length();
        long a1 = mx.getCurrentThreadAllocatedBytes();
        sink += h;
        return a1 - a0;
    }

    static long retainedBytes(Approach a, String[][] w) {
        Runtime rt = Runtime.getRuntime();
        System.gc();
        long before = rt.totalMemory() - rt.freeMemory();
        List<String> out = new ArrayList<>(w.length);
        for (String[] fields : w) out.add(a.build(fields));
        System.gc();
        long after = rt.totalMemory() - rt.freeMemory();
        sink += out.size();          // keep 'out' reachable until after the measurement
        return after - before;
    }

    public static void main(String[] args) throws Exception {
        String[] names = {"String (+)", "StringBuilder", "StringBuffer"};
        Approach[] approaches = {TicketBuilderBench::withPlus, TicketBuilderBench::withBuilder, TicketBuilderBench::withBuffer};

        // Workload W1 (short): one 10-field ticket built 100,000 times so the time is measurable.
        // W2 (many tickets): 10,000 tickets x 50 fields (the sample from the brief).
        // W3 (own, long manifest): 50 tickets x 1,000 fields (group booking / chart of passengers).
        String[] wNames = {"W1 short 10 fields x100000", "W2 10000 tickets x 50 fields", "W3 50 tickets x 1000 fields"};
        String[][][] loads = new String[3][][];
        loads[0] = new String[100_000][];
        String[] one = makeFields(10, 0);
        for (int i = 0; i < loads[0].length; i++) loads[0][i] = one;
        loads[1] = workload("W2", 10_000, 50);
        loads[2] = workload("W3", 50, 1_000);
        int[] fieldCounts = {10, 50, 1_000};

        List<String> csv = new ArrayList<>();
        csv.add("workload,approach,mean_ms,stddev_ms,allocated_MB,retained_MB,est_intermediate_strings");
        System.out.printf("%-30s %-14s %12s %10s %12s %12s %16s%n",
                "Workload", "Approach", "mean ms", "stddev", "alloc MB", "retained MB", "intermediate Str");

        for (int w = 0; w < 3; w++) {
            for (int a = 0; a < 3; a++) {
                String[][] load = loads[w];
                for (int i = 0; i < WARMUP; i++) timeMs(approaches[a], load);
                double[] t = new double[REPEATS];
                for (int r = 0; r < REPEATS; r++) t[r] = timeMs(approaches[a], load);
                double mean = 0;
                for (double x : t) mean += x;
                mean /= REPEATS;
                double var = 0;
                for (double x : t) var += (x - mean) * (x - mean);
                double sd = Math.sqrt(var / REPEATS);
                double allocMB = allocatedBytes(approaches[a], load) / 1048576.0;
                double retMB = retainedBytes(approaches[a], load) / 1048576.0;
                // Estimate: '+' creates about 2 Strings per field (f + "|" and the running ticket);
                // builders create only the final String per ticket.
                long est = (a == 0) ? 2L * fieldCounts[w] * load.length : load.length;
                System.out.printf("%-30s %-14s %12.2f %10.2f %12.2f %12.2f %,16d%n",
                        wNames[w], names[a], mean, sd, allocMB, retMB, est);
                csv.add(String.format("%s,%s,%.3f,%.3f,%.3f,%.3f,%d",
                        wNames[w], names[a], mean, sd, allocMB, retMB, est));
            }
        }
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("results"));
        java.nio.file.Files.write(java.nio.file.Path.of("results/ticket_bench.csv"), csv);
        System.out.println("Raw results written to results/ticket_bench.csv (sink=" + sink + ")");
    }
}
