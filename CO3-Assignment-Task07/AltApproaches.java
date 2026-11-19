import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.lang.management.ManagementFactory;

/**
 * Q7 alternatives: (a) parallel stream on a ForkJoinPool of a given size, (b) one platform thread per file.
 * Usage: java AltApproaches <stream|thread> <dataDir> <manifest> <parallelism> <latencyMs>
 */
public class AltApproaches {
    public static void main(String[] args) throws Exception {
        String mode = args[0];
        Path dir = Path.of(args[1]);
        List<String> names = Files.readAllLines(Path.of(args[2]));
        int parallelism = Integer.parseInt(args[3]);
        long latency = Long.parseLong(args[4]);
        long t0 = System.nanoTime();
        int ok = 0, total;
        if (mode.equals("stream")) {
            ForkJoinPool pool = new ForkJoinPool(parallelism);
            Future<Long> f = pool.submit(() -> names.parallelStream()
                    .map(n -> new ResultProcessor.ScoreReader(n, dir.resolve(n), latency).call())
                    .filter(o -> o.status == ResultProcessor.Status.OK).count());
            ok = f.get().intValue();
            pool.shutdown();
            total = names.size();
            System.out.printf("stream parallelism=%d latency=%dms ok=%d total=%d elapsed=%d ms%n",
                    parallelism, latency, ok, total, (System.nanoTime() - t0) / 1_000_000);
        } else {
            ConcurrentLinkedQueue<ResultProcessor.Outcome> out = new ConcurrentLinkedQueue<>();
            List<Thread> threads = new ArrayList<>();
            int created = 0;
            try {
                for (String n : names) {
                    Thread t = new Thread(() -> out.add(new ResultProcessor.ScoreReader(n, dir.resolve(n), latency).call()));
                    threads.add(t);
                    t.start();
                    created++;
                }
                for (Thread t : threads) t.join();
            } catch (OutOfMemoryError e) {
                System.out.println("OutOfMemoryError after creating " + created + " threads: " + e.getMessage());
            }
            long okCount = out.stream().filter(o -> o.status == ResultProcessor.Status.OK).count();
            System.out.printf("thread-per-file latency=%dms threadsStarted=%d outcomes=%d ok=%d peakThreads=%d elapsed=%d ms%n",
                    latency, created, out.size(), okCount,
                    ManagementFactory.getThreadMXBean().getPeakThreadCount(), (System.nanoTime() - t0) / 1_000_000);
        }
    }
}
