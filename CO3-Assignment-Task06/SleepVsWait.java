/** sleep() keeps the monitor; wait() releases it. */
public class SleepVsWait {
    static final Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread sleeper = new Thread(() -> {
            synchronized (lock) {
                try { Thread.sleep(500); } catch (InterruptedException e) { }   // keeps lock
                System.out.println("  sleeper done, still held the lock");
            }
        });
        Thread other = new Thread(() -> {
            long t0 = System.nanoTime();
            synchronized (lock) {                                              // blocked by sleeper
                System.out.println("  other got the lock after " + (System.nanoTime() - t0) / 1_000_000 + " ms");
            }
        });
        System.out.println("== sleep inside synchronized");
        sleeper.start(); Thread.sleep(50); other.start();
        sleeper.join(); other.join();

        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                try { lock.wait(); } catch (InterruptedException e) { }
            }
        });
        Thread notifier = new Thread(() -> {
            long t0 = System.nanoTime();
            synchronized (lock) {                                              // acquired at once
                System.out.println("== wait(): notifier got the lock after " + (System.nanoTime() - t0) / 1_000_000 + " ms while waiter waits");
                lock.notify();
            }
        });
        waiter.start(); Thread.sleep(50); notifier.start();
        waiter.join(); notifier.join();
    }
}
