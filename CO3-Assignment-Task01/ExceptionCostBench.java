import java.lang.management.ManagementFactory;

/**
 * Q1/Q2 alternative analysis: cost of returning an error code versus throwing an exception,
 * with and without stack-trace capture, at two call depths.
 */
public class ExceptionCostBench {
    static final int N = 200_000;
    static final int WARMUP = 1, REPEATS = 3;
    static volatile long sink;

    /** Exception with a stack trace (the normal case). */
    static class WithStack extends RuntimeException {
        WithStack(String m) { super(m); }
    }

    /** Exception that skips stack-trace capture (writableStackTrace = false). */
    static class NoStack extends RuntimeException {
        NoStack(String m) { super(m, null, false, false); }
    }

    static int errorCode(int x, int depth) {
        if (depth > 0) return errorCode(x, depth - 1);
        return x < 0 ? -1 : x;
    }

    static int withStack(int x, int depth) {
        if (depth > 0) return withStack(x, depth - 1);
        if (x < 0) throw new WithStack("negative");
        return x;
    }

    static int noStack(int x, int depth) {
        if (depth > 0) return noStack(x, depth - 1);
        if (x < 0) throw new NoStack("negative");
        return x;
    }

    interface Variant { long run(int depth); }

    public static void main(String[] args) {
        com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        String[] names = {"error code (-1)", "exception with stack trace", "exception, stack trace off"};
        int[] depths = {1, 20};
        System.out.printf("%-30s %7s %14s %16s%n", "variant (all calls invalid)", "depth", "ns per call", "bytes per call");
        StringBuilder csv = new StringBuilder("variant,depth,ns_per_call,bytes_per_call\n");
        for (int d : depths) {
            Variant[] v = {
                depth -> { long c = 0; for (int i = 0; i < N; i++) if (errorCode(-i - 1, depth) < 0) c++; return c; },
                depth -> { long c = 0; for (int i = 0; i < N; i++) { try { withStack(-i - 1, depth); } catch (WithStack e) { c++; } } return c; },
                depth -> { long c = 0; for (int i = 0; i < N; i++) { try { noStack(-i - 1, depth); } catch (NoStack e) { c++; } } return c; }
            };
            for (int k = 0; k < v.length; k++) {
                for (int w = 0; w < WARMUP; w++) sink += v[k].run(d);
                double total = 0;
                for (int r = 0; r < REPEATS; r++) {
                    long t0 = System.nanoTime();
                    sink += v[k].run(d);
                    total += System.nanoTime() - t0;
                }
                double nsPerCall = total / REPEATS / N;
                long a0 = mx.getCurrentThreadAllocatedBytes();
                sink += v[k].run(d);
                long bytes = (mx.getCurrentThreadAllocatedBytes() - a0) / N;
                System.out.printf("%-30s %7d %14.1f %16d%n", names[k], d, nsPerCall, bytes);
                csv.append(names[k]).append(',').append(d).append(',').append(String.format("%.2f", nsPerCall))
                   .append(',').append(bytes).append('\n');
            }
        }
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("results"));
            java.nio.file.Files.writeString(java.nio.file.Path.of("results/exception_cost.csv"), csv.toString());
        } catch (Exception e) {
            System.out.println("could not write CSV: " + e);
        }
        System.out.println("sink=" + sink);
    }
}
