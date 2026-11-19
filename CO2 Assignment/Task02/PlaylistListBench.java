import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Q2 - Playlist editor experiments: ArrayList vs LinkedList vs ArrayDeque.
 * Each operation is timed over WARMUP + REPEATS fresh structures; the mean ns/op is reported.
 */
public class PlaylistListBench {
    static final int[] SIZES = {1_000, 10_000, 100_000};
    static final int K = 1_000;           // operations per timed run for indexed / middle / front ops
    static final int WARMUP = 2, REPEATS = 5;
    static volatile long sink;

    static List<Integer> newList(boolean linked, int n) {
        List<Integer> l = linked ? new LinkedList<>() : new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(i);
        return l;
    }

    static Deque<Integer> newDeque(int n) {
        Deque<Integer> d = new ArrayDeque<>(n + 2 * K + 16);   // headroom: avoids a resize inside the timed part
        for (int i = 0; i < n; i++) d.addLast(i);
        return d;
    }

    /** Mean time per operation in nanoseconds. make() builds a fresh structure outside the timed part. */
    static <T> double nsPerOp(Supplier<T> make, Consumer<T> op, int ops) {
        for (int i = 0; i < WARMUP; i++) op.accept(make.get());
        double total = 0;
        for (int r = 0; r < REPEATS; r++) {
            T s = make.get();
            long t0 = System.nanoTime();
            op.accept(s);
            total += System.nanoTime() - t0;
            sink += s.hashCode();
        }
        return total / REPEATS / ops;
    }

    static String fmt(double ns) { return Double.isNaN(ns) ? "   n/a" : String.format("%10.1f", ns); }

    public static void main(String[] args) {
        List<String> csv = new ArrayList<>();
        csv.add("size,operation,ArrayList_ns_per_op,LinkedList_ns_per_op,ArrayDeque_ns_per_op");
        System.out.printf("%-10s %-22s %12s %12s %12s%n", "size", "operation (ns/op)", "ArrayList", "LinkedList", "ArrayDeque");

        for (int n : SIZES) {
            Random rnd = new Random(42);
            int[] randomIdx = new int[K];
            for (int i = 0; i < K; i++) randomIdx[i] = rnd.nextInt(n);
            Supplier<List<Integer>> al = () -> newList(false, n);
            Supplier<List<Integer>> ll = () -> newList(true, n);
            Supplier<Deque<Integer>> dq = () -> newDeque(n);

            // 1. add at end (K adds)
            double a1 = nsPerOp(al, l -> { for (int i = 0; i < K; i++) l.add(i); }, K);
            double l1 = nsPerOp(ll, l -> { for (int i = 0; i < K; i++) l.add(i); }, K);
            double d1 = nsPerOp(dq, d -> { for (int i = 0; i < K; i++) d.addLast(i); }, K);
            row(csv, n, "add at end", a1, l1, d1);

            // 2. insert at front (K inserts)
            double a2 = nsPerOp(al, l -> { for (int i = 0; i < K; i++) l.add(0, i); }, K);
            double l2 = nsPerOp(ll, l -> { for (int i = 0; i < K; i++) l.add(0, i); }, K);
            double d2 = nsPerOp(dq, d -> { for (int i = 0; i < K; i++) d.addFirst(i); }, K);
            row(csv, n, "insert at front", a2, l2, d2);

            // 3. insert in the middle (K inserts at index size/2 of the original size)
            double a3 = nsPerOp(al, l -> { int m = l.size() / 2; for (int i = 0; i < K; i++) l.add(m, i); }, K);
            double l3 = nsPerOp(ll, l -> { int m = l.size() / 2; for (int i = 0; i < K; i++) l.add(m, i); }, K);
            row(csv, n, "insert in middle", a3, l3, Double.NaN);

            // 4. remove at front (K removals)
            double a4 = nsPerOp(al, l -> { for (int i = 0; i < K; i++) l.remove(0); }, K);
            double l4 = nsPerOp(ll, l -> { for (int i = 0; i < K; i++) l.remove(0); }, K);
            double d4 = nsPerOp(dq, d -> { for (int i = 0; i < K; i++) d.pollFirst(); }, K);
            row(csv, n, "remove at front", a4, l4, d4);

            // 5. remove in middle (K removals)
            double a5 = nsPerOp(al, l -> { for (int i = 0; i < K; i++) l.remove(l.size() / 2); }, K);
            double l5 = nsPerOp(ll, l -> { for (int i = 0; i < K; i++) l.remove(l.size() / 2); }, K);
            row(csv, n, "remove in middle", a5, l5, Double.NaN);

            // 6. get k-th element (K random indexes)
            double a6 = nsPerOp(al, l -> { long s = 0; for (int i : randomIdx) s += l.get(i); sink += s; }, K);
            double l6 = nsPerOp(ll, l -> { long s = 0; for (int i : randomIdx) s += l.get(i); sink += s; }, K);
            row(csv, n, "get k-th", a6, l6, Double.NaN);

            // 7. iterate whole list (ns per element)
            double a7 = nsPerOp(al, l -> { long s = 0; for (int x : l) s += x; sink += s; }, n);
            double l7 = nsPerOp(ll, l -> { long s = 0; for (int x : l) s += x; sink += s; }, n);
            double d7 = nsPerOp(dq, d -> { long s = 0; Iterator<Integer> it = d.iterator(); while (it.hasNext()) s += it.next(); sink += s; }, n);
            row(csv, n, "iterate (per elem)", a7, l7, d7);

            // 8. "Up Next" queue churn: add at end + remove from front, K pairs
            double qa = nsPerOp(al, l -> { for (int i = 0; i < K; i++) { l.add(i); l.remove(0); } }, K);
            double ql = nsPerOp(ll, l -> { for (int i = 0; i < K; i++) { l.add(i); l.remove(0); } }, K);
            double qd = nsPerOp(dq, d -> { for (int i = 0; i < K; i++) { d.addLast(i); d.pollFirst(); } }, K);
            row(csv, n, "queue add+remove front", qa, ql, qd);
            System.out.println();
        }
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("results"));
            java.nio.file.Files.write(java.nio.file.Path.of("results/list_bench.csv"), csv);
            System.out.println("Raw results written to results/list_bench.csv (sink=" + sink + ")");
        } catch (Exception e) {
            System.out.println("Could not write CSV: " + e);
        }
    }

    static void row(List<String> csv, int n, String op, double a, double l, double d) {
        System.out.printf("%-10d %-22s %s %s %s%n", n, op, fmt(a), fmt(l), fmt(d));
        csv.add(String.format("%d,%s,%.2f,%.2f,%s", n, op, a, l, Double.isNaN(d) ? "" : String.format("%.2f", d)));
    }
}
