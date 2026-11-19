import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Q6 - growth of admit/serve cost and starvation (strict severity rule vs deadline rule).
 */
public class TriageGrowth {
    static final int[] SIZES = {1_000, 10_000, 100_000, 1_000_000};
    static volatile long sink;

    static final Comparator<Patient> STRICT = Comparator.comparingInt((Patient p) -> p.severity)
            .thenComparingLong(p -> p.arrival);

    /** Fair rule: deadline = arrival + severity * 30 time units, so waiting is bounded. */
    static final Comparator<Patient> DEADLINE = Comparator.comparingLong(
            (Patient p) -> p.arrival + (long) p.severity * 30_000L).thenComparingLong(p -> p.arrival);

    public static void main(String[] args) {
        System.out.println("Growth (mean ns per operation; 3 runs after 1 warm-up)");
        System.out.printf("%-10s %14s %14s%n", "size n", "admit ns/op", "serve ns/op");
        Random rnd = new Random(5);
        for (int n : SIZES) {
            int[] sev = new int[n];
            for (int i = 0; i < n; i++) sev[i] = 1 + rnd.nextInt(5);
            double addNs = 0, pollNs = 0;
            for (int run = 0; run < 4; run++) {
                PriorityQueue<Patient> q = new PriorityQueue<>(STRICT);
                long t0 = System.nanoTime();
                for (int i = 0; i < n; i++) q.add(new Patient("P" + i, sev[i], i));
                long t1 = System.nanoTime();
                long s = 0;
                while (!q.isEmpty()) s += q.poll().arrival;
                long t2 = System.nanoTime();
                sink += s;
                if (run > 0) { addNs += (t1 - t0); pollNs += (t2 - t1); }
            }
            addNs /= 3.0 * n; pollNs /= 3.0 * n;
            System.out.printf("%-10d %14.1f %14.1f%n", n, addNs, pollNs);
        }

        System.out.println("\nStarvation simulation: 1 severe patient (S5) arrives at t=0.");
        System.out.println("From t=0 to t=999, two S1 patients arrive per tick and one patient is served per tick.");
        System.out.println("Strict severity rule:");
        int strictTick = simulate(STRICT);
        System.out.println("  severity-5 patient served at tick " + strictTick);
        System.out.println("Deadline rule (arrival + severity * 30 ticks):");
        int deadlineTick = simulate(DEADLINE);
        System.out.println("  severity-5 patient served at tick " + deadlineTick);
    }

    /** Returns the tick at which the S5 patient is served. Time unit = 1000 per tick, so 30 ticks = 30000. */
    static int simulate(Comparator<Patient> rule) {
        PriorityQueue<Patient> q = new PriorityQueue<>(rule);
        q.add(new Patient("Asha", 5, 0));
        int tick = 0;
        long stamp = 1;
        while (true) {
            if (tick < 1000) {
                q.add(new Patient("S1a-" + tick, 1, tick * 1000L + stamp++));
                q.add(new Patient("S1b-" + tick, 1, tick * 1000L + stamp++));
            }
            Patient served = q.poll();
            if (served != null && served.name.equals("Asha")) return tick;
            if (served == null && tick >= 1000) return -1;
            tick++;
        }
    }
}
