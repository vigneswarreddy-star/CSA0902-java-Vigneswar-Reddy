import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Q4 - four scanner stations. Run with no argument for Thread subclasses, or with "runnable"
 * for Runnable implementations. Each station scans 5 batches of 10 scripts with random pauses.
 */
public class ScanStations {
    static final AtomicInteger total = new AtomicInteger();

    static void scan(String station) throws InterruptedException {
        Random pause = new Random();
        int scanned = 0;
        for (int batch = 0; batch < 5; batch++) {
            Thread.sleep(5 + pause.nextInt(20));       // simulated scanning time per batch
            scanned += 10;
        }
        total.addAndGet(scanned);
        System.out.println(station + " finished, scanned " + scanned);
    }

    /** Way 1: extend Thread and override run(). */
    static class Station extends Thread {
        Station(String name) { super(name); }
        @Override public void run() {
            try { scan(getName()); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    /** Way 2: implement Runnable; the class can still extend another class. */
    static class StationTask implements Runnable {
        private final String name;
        StationTask(String name) { this.name = name; }
        @Override public void run() {
            try { scan(name); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        boolean runnableWay = args.length > 0 && args[0].equals("runnable");
        Thread[] stations = new Thread[4];
        for (int i = 0; i < stations.length; i++) {
            String name = "Station-" + (i + 1);
            stations[i] = runnableWay ? new Thread(new StationTask(name), name) : new Station(name);
        }
        for (Thread t : stations) t.start();
        for (Thread t : stations) t.join();          // main waits for every station
        System.out.println("TOTAL scanned = " + total.get() + " (expected 200)");
    }
}
