import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;

/**
 * Q5 alternatives: AtomicBoolean[] + compareAndSet, ReentrantLock, ConcurrentHashMap + putIfAbsent,
 * and synchronized method. Same workload: 10,000 bookings, 4 threads, s seats (100 and 10,000).
 */
public class AltBookingBench {
    static final int BOOKINGS = 10_000, THREADS = 4;

    interface Booking { boolean tryBook(int seat); }

    static long run(Booking b, int seats) throws InterruptedException {
        Thread[] ts = new Thread[THREADS];
        long t0 = System.nanoTime();
        for (int t = 0; t < THREADS; t++) {
            final int id = t;
            ts[t] = new Thread(() -> {
                Random r = new Random(id);
                for (int i = 0; i < BOOKINGS / THREADS; i++) b.tryBook(r.nextInt(seats));
            });
            ts[t].start();
        }
        for (Thread t : ts) t.join();
        return System.nanoTime() - t0;
    }

    public static void main(String[] args) throws InterruptedException {
        for (int seats : new int[]{100, 10_000}) {
            System.out.println("seats = " + seats + " (mean of 5 runs, ns per booking attempt)");
            for (String name : new String[]{"synchronized method", "AtomicBoolean[] CAS", "ReentrantLock", "ConcurrentHashMap putIfAbsent"}) {
                double total = 0;
                for (int rep = 0; rep < 5; rep++) {
                    Booking b = make(name, seats);
                    total += run(b, seats);
                }
                System.out.printf("  %-30s %10.1f%n", name, total / 5 / BOOKINGS);
            }
        }
    }

    static Booking make(String name, int seats) {
        switch (name) {
            case "synchronized method": {
                boolean[] booked = new boolean[seats];
                return seat -> sync(booked, seat);
            }
            case "AtomicBoolean[] CAS": {
                AtomicBoolean[] booked = new AtomicBoolean[seats];
                for (int i = 0; i < seats; i++) booked[i] = new AtomicBoolean(false);
                return seat -> booked[seat].compareAndSet(false, true);
            }
            case "ReentrantLock": {
                boolean[] booked = new boolean[seats];
                ReentrantLock lock = new ReentrantLock();
                return seat -> { lock.lock(); try { if (booked[seat]) return false; booked[seat] = true; return true; } finally { lock.unlock(); } };
            }
            default: {
                ConcurrentHashMap<Integer, Boolean> booked = new ConcurrentHashMap<>();
                return seat -> booked.putIfAbsent(seat, Boolean.TRUE) == null;
            }
        }
    }

    static synchronized boolean sync(boolean[] booked, int seat) {
        if (booked[seat]) return false;
        booked[seat] = true;
        return true;
    }
}
