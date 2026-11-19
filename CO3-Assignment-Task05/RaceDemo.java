import java.util.concurrent.CountDownLatch;

/**
 * Q5 Student A - reproducing double booking.
 * Problem with the plain program: Thread.start() launches the 100 users one after another and each
 * booking finishes before the next thread starts, so the check and the write never interleave.
 * Fix for the demo: a start gate makes all 100 users wait, then release together.
 * Mode "gate": all threads released at once (no pause inside booking).
 * Mode "gate+yield": a Thread.yield() between the check and the write widens the race window.
 */
public class RaceDemo {
    static class Cinema {
        final boolean[] booked = new boolean[10];
        final int[] salesPerSeat = new int[10];
        final boolean yieldBetween;
        Cinema(boolean yieldBetween) { this.yieldBetween = yieldBetween; }
        void book(int seat) {
            if (!booked[seat]) {                 // CHECK
                if (yieldBetween) Thread.yield();  // another thread may run here
                booked[seat] = true;            // ACT
                salesPerSeat[seat]++;
            }
        }
    }

    static int[] trial(boolean yieldBetween) throws InterruptedException {
        Cinema c = new Cinema(yieldBetween);
        CountDownLatch gate = new CountDownLatch(1);
        Thread[] users = new Thread[100];
        for (int i = 0; i < users.length; i++) {
            final int seat = i % 10;
            users[i] = new Thread(() -> {
                try { gate.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                c.book(seat);
            });
            users[i].start();
        }
        gate.countDown();                        // release all users together
        for (Thread t : users) t.join();
        int total = 0, doubleSeats = 0;
        for (int s : c.salesPerSeat) { total += s; if (s > 1) doubleSeats++; }
        return new int[]{total, doubleSeats};
    }

    public static void main(String[] args) throws InterruptedException {
        int trials = 200;
        for (boolean y : new boolean[]{false, true}) {
            int withDouble = 0, worstSold = 0, sumSold = 0;
            for (int t = 0; t < trials; t++) {
                int[] r = trial(y);
                if (r[1] > 0) withDouble++;
                worstSold = Math.max(worstSold, r[0]);
                sumSold += r[0];
            }
            System.out.println((y ? "mode gate+yield" : "mode gate only") + ": trials=" + trials
                    + ", trials with double booking=" + withDouble
                    + ", max seats sold in one trial=" + worstSold
                    + ", mean seats sold=" + String.format("%.3f", sumSold / (double) trials));
        }
    }
}
