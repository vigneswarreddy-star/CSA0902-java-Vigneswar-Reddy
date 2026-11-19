import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Random;

/** Q6 Part A: a transfer that deadlocks, detected by ThreadMXBean, then a lock-ordered version that finishes. */
public class DeadlockDemo {
    static class Account {
        final int id; long balance;
        Account(int id, long balance) { this.id = id; this.balance = balance; }
        @Override public String toString() { return "account-" + id; }
    }

    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    /** Locks "from" then "to": two opposite transfers can each hold one lock and wait for the other. */
    static void transferNaive(Account from, Account to, long amount) {
        synchronized (from) {
            sleep(100);                              // makes the bad interleaving reliable
            synchronized (to) {
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    /** Always locks the lower account id first: a global order means no circular wait. */
    static void transferOrdered(Account from, Account to, long amount) {
        Account first = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;
        synchronized (first) {
            synchronized (second) {
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== Part A1: naive transfers (A->B and B->A at the same time)");
        Account a = new Account(1, 1000), b = new Account(2, 1000);
        Thread t1 = new Thread(() -> transferNaive(a, b, 100), "transfer-A-to-B");
        Thread t2 = new Thread(() -> transferNaive(b, a, 50), "transfer-B-to-A");
        t1.setDaemon(true); t2.setDaemon(true);
        t1.start(); t2.start();
        t1.join(1500); t2.join(1500);
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        long[] ids = mx.findDeadlockedThreads();
        if (ids == null) {
            System.out.println("  no deadlock found");
        } else {
            System.out.println("  DEADLOCK: " + ids.length + " threads are waiting for each other");
            System.out.println("  lock names: DeadlockDemo$Account@" + Integer.toHexString(System.identityHashCode(a)) + " = account-1, DeadlockDemo$Account@" + Integer.toHexString(System.identityHashCode(b)) + " = account-2");
            for (ThreadInfo info : mx.getThreadInfo(ids, true, true)) {
                System.out.println("  " + info.getThreadName() + " is " + info.getThreadState()
                        + " waiting for " + info.getLockName() + " held by " + info.getLockOwnerName());
            }
        }

        System.out.println("\n== Part A2: the same two transfers with lock ordering");
        Account c = new Account(1, 1000), d = new Account(2, 1000);
        Thread u1 = new Thread(() -> transferOrdered(c, d, 100));
        Thread u2 = new Thread(() -> transferOrdered(d, c, 50));
        u1.start(); u2.start(); u1.join(); u2.join();
        System.out.println("  both finished; balances " + c.balance + " + " + d.balance + " = " + (c.balance + d.balance));

        System.out.println("\n== Part A3: 20 threads, 20,000 random transfers among 5 accounts, lock ordering");
        Account[] accounts = new Account[5];
        for (int i = 0; i < accounts.length; i++) accounts[i] = new Account(i, 10_000);
        Thread[] workers = new Thread[20];
        for (int w = 0; w < workers.length; w++) {
            final int seed = w;
            workers[w] = new Thread(() -> {
                Random r = new Random(seed);
                for (int k = 0; k < 1000; k++) {
                    int x = r.nextInt(5), y = (x + 1 + r.nextInt(4)) % 5;
                    transferOrdered(accounts[x], accounts[y], 1 + r.nextInt(20));
                }
            });
        }
        long t0 = System.nanoTime();
        for (Thread w : workers) w.start();
        for (Thread w : workers) w.join(20_000);
        long total = 0;
        for (Account acc : accounts) total += acc.balance;
        System.out.println("  finished in " + (System.nanoTime() - t0) / 1_000_000 + " ms, total money = " + total + " (expected 50000)");
        System.exit(0);
    }
}
