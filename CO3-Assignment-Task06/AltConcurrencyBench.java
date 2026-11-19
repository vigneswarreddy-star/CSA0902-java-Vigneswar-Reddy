import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;

/**
 * Q6 alternatives.
 * (1) producer-consumer: TokenCounter (wait/notifyAll) vs ArrayBlockingQueue(5), 1 producer, 2 consumers, 200,000 items.
 * (2) transfers: lock ordering vs ReentrantLock.tryLock with timeout and back-off, 4 threads, 100,000 transfers, 5 accounts.
 */
public class AltConcurrencyBench {
    static final int ITEMS = 200_000;
    static volatile long sink;

    static long waitNotify() throws InterruptedException {
        TokenCounter c = new TokenCounter();
        Thread p = new Thread(() -> { try { for (int i = 0; i < ITEMS; i++) c.put(1); } catch (InterruptedException e) { } });
        long[] got = new long[2];
        Thread[] cs = new Thread[2];
        for (int k = 0; k < 2; k++) {
            final int id = k;
            cs[k] = new Thread(() -> { try { for (int i = 0; i < ITEMS / 2; i++) got[id] += c.take(); } catch (InterruptedException e) { } });
        }
        long t0 = System.nanoTime();
        p.start(); for (Thread t : cs) t.start();
        p.join(); for (Thread t : cs) t.join();
        sink += got[0] + got[1];
        return System.nanoTime() - t0;
    }

    static long blockingQueue() throws InterruptedException {
        ArrayBlockingQueue<Integer> q = new ArrayBlockingQueue<>(5);
        Thread p = new Thread(() -> { try { for (int i = 0; i < ITEMS; i++) q.put(1); } catch (InterruptedException e) { } });
        long[] got = new long[2];
        Thread[] cs = new Thread[2];
        for (int k = 0; k < 2; k++) {
            final int id = k;
            cs[k] = new Thread(() -> { try { for (int i = 0; i < ITEMS / 2; i++) got[id] += q.take(); } catch (InterruptedException e) { } });
        }
        long t0 = System.nanoTime();
        p.start(); for (Thread t : cs) t.start();
        p.join(); for (Thread t : cs) t.join();
        sink += got[0] + got[1];
        return System.nanoTime() - t0;
    }

    static final int ACCOUNTS = 5, TRANSFERS = 100_000, THREADS = 4;

    static long orderedTransfers(long[] bal) throws InterruptedException {
        Object[] locks = new Object[ACCOUNTS];
        for (int i = 0; i < ACCOUNTS; i++) locks[i] = new Object();
        Thread[] ts = new Thread[THREADS];
        long t0 = System.nanoTime();
        for (int t = 0; t < THREADS; t++) {
            final int seed = t;
            ts[t] = new Thread(() -> {
                Random r = new Random(seed);
                for (int k = 0; k < TRANSFERS / THREADS; k++) {
                    int x = r.nextInt(ACCOUNTS), y = (x + 1 + r.nextInt(ACCOUNTS - 1)) % ACCOUNTS;
                    int lo = Math.min(x, y), hi = Math.max(x, y);
                    synchronized (locks[lo]) { synchronized (locks[hi]) { bal[x]--; bal[y]++; } }
                }
            });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        return System.nanoTime() - t0;
    }

    static long tryLockTransfers(long[] bal, long[] retries) throws InterruptedException {
        ReentrantLock[] locks = new ReentrantLock[ACCOUNTS];
        for (int i = 0; i < ACCOUNTS; i++) locks[i] = new ReentrantLock();
        Thread[] ts = new Thread[THREADS];
        long t0 = System.nanoTime();
        for (int t = 0; t < THREADS; t++) {
            final int seed = t;
            ts[t] = new Thread(() -> {
                Random r = new Random(seed);
                for (int k = 0; k < TRANSFERS / THREADS; k++) {
                    int x = r.nextInt(ACCOUNTS), y = (x + 1 + r.nextInt(ACCOUNTS - 1)) % ACCOUNTS;
                    boolean done = false;
                    while (!done) {
                        try {
                            if (locks[x].tryLock(1, TimeUnit.MILLISECONDS)) {
                                try {
                                    if (locks[y].tryLock(1, TimeUnit.MILLISECONDS)) {
                                        try { bal[x]--; bal[y]++; done = true; } finally { locks[y].unlock(); }
                                    }
                                } finally { locks[x].unlock(); }
                            }
                        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                        if (!done) { retries[0]++; Thread.yield(); }
                    }
                }
            });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        return System.nanoTime() - t0;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("== (1) producer-consumer, 200,000 items, mean of 5 runs");
        double wn = 0, bq = 0;
        for (int r = 0; r < 5; r++) { wn += waitNotify(); bq += blockingQueue(); }
        System.out.printf("  wait/notifyAll (TokenCounter): %8.1f ns per item%n", wn / 5 / ITEMS);
        System.out.printf("  ArrayBlockingQueue(5):         %8.1f ns per item%n", bq / 5 / ITEMS);

        System.out.println("== (2) transfers, 100,000 transfers, 4 threads, 5 accounts, mean of 5 runs");
        double ord = 0, tl = 0; long totalRetries = 0; long[] lastRetries = new long[1];
        for (int r = 0; r < 5; r++) {
            long[] bal = {10000, 10000, 10000, 10000, 10000};
            ord += orderedTransfers(bal);
            long[] b2 = {10000, 10000, 10000, 10000, 10000};
            long[] retries = new long[1];
            tl += tryLockTransfers(b2, retries);
            totalRetries += retries[0];
            long sum = 0; for (long v : b2) sum += v;
            if (sum != 50000) System.out.println("  MONEY NOT CONSERVED: " + sum);
        }
        System.out.printf("  lock ordering:                 %8.1f ns per transfer%n", ord / 5 / TRANSFERS);
        System.out.printf("  tryLock with timeout + retry:  %8.1f ns per transfer, mean retries per run = %.1f%n",
                tl / 5 / TRANSFERS, totalRetries / 5.0);
        System.out.println("sink=" + sink);
    }
}
