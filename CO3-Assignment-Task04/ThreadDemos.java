/** Q4 Student B experiments: run() vs start(), life-cycle states, priorities and daemon threads. */
public class ThreadDemos {

    static long spin(long n) { long s = 0; for (long i = 0; i < n; i++) s += i ^ (i >>> 3); return s; }

    public static void main(String[] args) throws Exception {
        System.out.println("== 1. run() vs start()");
        Thread r = new Thread(() -> System.out.println("  body runs in: " + Thread.currentThread().getName()), "worker");
        r.run();                          // ordinary method call: runs in main
        r.start();                        // new thread
        r.join();

        System.out.println("\n== 2. life cycle");
        Thread sleeper = new Thread(() -> {
            try { Thread.sleep(300); } catch (InterruptedException e) { }
        }, "sleeper");
        System.out.println("  after new Thread:      " + sleeper.getState() + " alive=" + sleeper.isAlive());
        sleeper.start();
        Thread.sleep(50);
        System.out.println("  while sleeping:        " + sleeper.getState() + " alive=" + sleeper.isAlive());
        sleeper.join();
        System.out.println("  after join:            " + sleeper.getState() + " alive=" + sleeper.isAlive());

        System.out.println("\n== 3. priorities (6 runs each: default vs S1=MAX_PRIORITY, others=MIN_PRIORITY)");
        for (String mode : new String[]{"default", "priorities"}) {
            for (int run = 1; run <= 6; run++) {
                String first = runPriorityRace(mode.equals("priorities"));
                System.out.println("  " + mode + " run " + run + ": first to finish = " + first);
            }
        }

        System.out.println("\n== 4. daemon station");
        Thread worker = new Thread(() -> {
            try { Thread.sleep(400); } catch (InterruptedException e) { }
            System.out.println("  worker (non-daemon) finished");
        }, "worker-non-daemon");
        Thread ticker = new Thread(() -> {
            long ticks = 0;
            while (true) {
                ticks++;
                try { Thread.sleep(50); } catch (InterruptedException e) { return; }
                if (ticks % 4 == 0) System.out.println("  daemon tick " + ticks);
            }
        }, "daemon-ticker");
        ticker.setDaemon(true);
        ticker.start();
        worker.start();
        worker.join();
        System.out.println("  main ends; daemon isAlive=" + ticker.isAlive() + " (JVM exits without waiting for it)");
    }

    /** Four CPU-bound threads; returns the name of the first one to finish. */
    static String runPriorityRace(boolean usePriorities) throws InterruptedException {
        final String[] winner = new String[1];
        Thread[] ts = new Thread[4];
        for (int i = 0; i < ts.length; i++) {
            final String name = "S" + (i + 1);
            ts[i] = new Thread(() -> {
                spin(300_000_000L);
                synchronized (winner) { if (winner[0] == null) winner[0] = name; }
            }, name);
            if (usePriorities) ts[i].setPriority(i == 0 ? Thread.MAX_PRIORITY : Thread.MIN_PRIORITY);
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        return winner[0];
    }
}
