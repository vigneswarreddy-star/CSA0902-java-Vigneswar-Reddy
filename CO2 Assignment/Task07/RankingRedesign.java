import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Q7 - Original Leaderboard (copy + full sort on every ranking() call) vs
 * redesigned SortedLeaderboard (binary insertion keeps the order; ranking() is a read-only view).
 * Measures comparator calls (exact count) and time for two workloads.
 */
class Team {
    final String name; final int points; final int goalDiff;
    Team(String name, int points, int goalDiff) { this.name = name; this.points = points; this.goalDiff = goalDiff; }
    @Override public String toString() { return name + "(" + points + "," + goalDiff + ")"; }
}

/** Wraps a comparator and counts how many times it is called. */
class CountingComparator<T> implements Comparator<T> {
    final Comparator<T> inner; long calls = 0;
    CountingComparator(Comparator<T> inner) { this.inner = inner; }
    @Override public int compare(T a, T b) { calls++; return inner.compare(a, b); }
}

/** ORIGINAL design (from Task05): sorts a fresh copy every time ranking() is called. */
class OriginalLeaderboard<T> {
    private final List<T> items = new ArrayList<>();
    private final Comparator<? super T> order;
    OriginalLeaderboard(Comparator<? super T> order) { this.order = order; }
    void add(T item) { items.add(item); }
    List<T> ranking() {
        List<T> sorted = new ArrayList<>(items);
        sorted.sort(order);
        return sorted;
    }
}

/** REDESIGN: insert each item at its sorted position once (binary search), so ranking() is O(1). */
class SortedLeaderboard<T> {
    private final List<T> items = new ArrayList<>();
    private final List<T> view = Collections.unmodifiableList(items);
    private final Comparator<? super T> order;
    SortedLeaderboard(Comparator<? super T> order) { this.order = order; }
    void add(T item) {
        // upper bound: an equal item goes AFTER existing equal items, so ties keep arrival order (stable)
        int lo = 0, hi = items.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (order.compare(items.get(mid), item) <= 0) lo = mid + 1; else hi = mid;
        }
        items.add(lo, item);   // O(n) array shift, but no comparisons
    }
    List<T> ranking() { return view; }
}

public class RankingRedesign {
    static final int[] SIZES = {1_000, 5_000, 20_000};
    static final int QUERIES = 200;     // S1: read-heavy, ranking() asked 200 times with no changes
    static final int LIVE_UPDATES = 500; // S2: a new result arrives, then the board is shown
    static final int REPEATS = 3;
    static volatile long sink;

    static final Comparator<Team> RANK = Comparator.comparingInt((Team t) -> t.points).reversed()
            .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
            .thenComparing(t -> t.name);

    static List<Team> teams(int n, long seed) {
        Random r = new Random(seed);
        List<Team> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(new Team("T" + String.format("%06d", i), r.nextInt(16), r.nextInt(41) - 20));
        Collections.shuffle(out, r);
        return out;
    }

    /** Returns {comparisons, millis} for a read-heavy workload. */
    static long[] readHeavy(boolean redesign, List<Team> data, List<Team> dummy) {
        CountingComparator<Team> cc = new CountingComparator<>(RANK);
        long t0 = System.nanoTime();
        Object board = redesign ? new SortedLeaderboard<Team>(cc) : new OriginalLeaderboard<Team>(cc);
        for (Team t : data) add(board, t);
        long sumSizes = 0;
        for (int q = 0; q < QUERIES; q++) sumSizes += ranking(board).size();
        long ms = System.nanoTime() - t0;
        sink += sumSizes;
        return new long[]{cc.calls, ms};
    }

    /** Returns {comparisons, millis} for a live workload: add one result, then show the ranking. */
    static long[] live(boolean redesign, List<Team> base, List<Team> updates) {
        CountingComparator<Team> cc = new CountingComparator<>(RANK);
        Object board = redesign ? new SortedLeaderboard<Team>(cc) : new OriginalLeaderboard<Team>(cc);
        for (Team t : base) add(board, t);
        cc.calls = 0;                       // count only the live phase
        long t0 = System.nanoTime();
        long sumSizes = 0;
        for (Team u : updates) {
            add(board, u);
            sumSizes += ranking(board).size();
        }
        long ms = System.nanoTime() - t0;
        sink += sumSizes;
        return new long[]{cc.calls, ms};
    }

    @SuppressWarnings("unchecked")
    static void add(Object board, Team t) {
        if (board instanceof SortedLeaderboard) ((SortedLeaderboard<Team>) board).add(t);
        else ((OriginalLeaderboard<Team>) board).add(t);
    }

    @SuppressWarnings("unchecked")
    static List<Team> ranking(Object board) {
        return board instanceof SortedLeaderboard
                ? ((SortedLeaderboard<Team>) board).ranking()
                : ((OriginalLeaderboard<Team>) board).ranking();
    }

    public static void main(String[] args) {
        // Correctness: both designs must give the same ranking, including ties.
        List<Team> check = teams(2_000, 99);
        OriginalLeaderboard<Team> o = new OriginalLeaderboard<>(RANK);
        SortedLeaderboard<Team> s = new SortedLeaderboard<>(RANK);
        for (Team t : check) { o.add(t); s.add(t); }
        System.out.println("Correctness (2000 teams with ties): identical rankings = " + o.ranking().equals(s.ranking()));
        System.out.println();

        List<String> csv = new ArrayList<>();
        csv.add("workload,n,design,comparisons,ms_mean");
        System.out.printf("%-12s %-8s %-10s %16s %12s%n", "workload", "n", "design", "comparisons", "ms (mean)");
        for (int n : SIZES) {
            List<Team> data = teams(n, 1);
            List<Team> updates = teams(LIVE_UPDATES, 2);
            for (String wl : new String[]{"read-heavy", "live"}) {
                for (boolean redesign : new boolean[]{false, true}) {
                    long totalMs = 0; long cmp = 0;
                    for (int rep = 0; rep < REPEATS; rep++) {
                        long[] r = wl.equals("live") ? live(redesign, data, updates) : readHeavy(redesign, data, null);
                        cmp = r[0]; totalMs += r[1];
                    }
                    String design = redesign ? "Redesign" : "Original";
                    double ms = totalMs / (double) REPEATS / 1e6;
                    System.out.printf("%-12s %-8d %-10s %,16d %12.1f%n", wl, n, design, cmp, ms);
                    csv.add(String.format("%s,%d,%s,%d,%.1f", wl, n, design, cmp, ms));
                }
            }
        }
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("results"));
            java.nio.file.Files.write(java.nio.file.Path.of("results/redesign_bench.csv"), csv);
            System.out.println("\nRaw results written to results/redesign_bench.csv (sink=" + sink + ")");
        } catch (Exception e) {
            System.out.println("Could not write CSV: " + e);
        }
    }
}
