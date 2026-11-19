import java.util.Random;

/**
 * Q5 Student B - 10,000 bookings across 100 seats by 4 threads, three strategies.
 * "work" is simulated preparation (for example, payment form checks) done BEFORE the seat check.
 * Strategy A: synchronized method (the work is inside the lock).
 * Strategy B: synchronized block (only the check-then-book is locked).
 * Strategy C: no lock (incorrect; baseline for speed only).
 */
public class BookingStrategies {
    static final int SEATS = 100, BOOKINGS = 10_000, THREADS = 4, WORK_NS = 2_000;
    static volatile long sink;

    static void work() {                     // about 2 microseconds of busy work
        long end = System.nanoTime() + WORK_NS, x = 0;
        while (System.nanoTime() < end) x++;
        sink += x;
    }

    static abstract class Booker {
        final boolean[] booked = new boolean[SEATS];
        int sold = 0;
        abstract void book(int seat);
    }

    static class MethodLock extends Booker {
        @Override synchronized void book(int seat) {
            work();                                  // inside the lock
            if (!booked[seat]) { booked[seat] = true; sold++; }
        }
    }

    static class BlockLock extends Booker {
        @Override void book(int seat) {
            work();                                  // outside the lock
            synchronized (this) {
                if (!booked[seat]) { booked[seat] = true; sold++; }
            }
        }
    }

    static class NoLock extends Booker {
        @Override void book(int seat) {
            work();
            if (!booked[seat]) { booked[seat] = true; sold++; }
        }
    }

    static long[] run(Booker b) throws InterruptedException {
        Thread[] ts = new Thread[THREADS];
        long t0 = System.nanoTime();
        for (int t = 0; t < THREADS; t++) {
            final int id = t;
            ts[t] = new Thread(() -> {
                Random r = new Random(id);
                for (int i = 0; i < BOOKINGS / THREADS; i++) b.book(r.nextInt(SEATS));
            });
            ts[t].start();
        }
        for (Thread t : ts) t.join();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        return new long[]{ms, b.sold};
    }

    public static void main(String[] args) throws InterruptedException {
        String[] names = {"synchronized method", "synchronized block", "no lock (incorrect)"};
        Booker[][] unused = new Booker[0][];
        System.out.println("10,000 bookings, 100 seats, 4 threads, ~2 us work per booking (5 runs each, mean ms)");
        for (int k = 0; k < 3; k++) {
            long total = 0; long sold = 0;
            for (int rep = 0; rep < 5; rep++) {
                Booker b = k == 0 ? new MethodLock() : k == 1 ? new BlockLock() : new NoLock();
                long[] r = run(b);
                total += r[0]; sold = r[1];
            }
            System.out.printf("%-22s mean %8.1f ms   seats sold = %d (max possible %d)%n",
                    names[k], total / 5.0, sold, SEATS);
        }
        System.out.println("sink=" + sink);
    }
}
