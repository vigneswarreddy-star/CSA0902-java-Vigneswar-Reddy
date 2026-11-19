import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Q7 design: fixed thread pool of Callable<Outcome> tasks.
 * - Each task catches its own failures and returns a status, so one bad file never stops the run.
 * - Results go into a ConcurrentHashMap keyed by file name: each file is recorded exactly once.
 * - The main thread collects every Future with a timeout, so a hung file becomes a TIMEOUT outcome.
 * Usage: java ResultProcessor <dataDir> <manifest> <poolSize> [simulatedLatencyMs] [timeoutSec]
 */
public class ResultProcessor {

    enum Status { OK, MISSING, CORRUPT, OUT_OF_RANGE, TIMEOUT, UNEXPECTED }

    static final class Outcome {
        final String file; final Status status; final double average; final String detail;
        Outcome(String file, Status status, double average, String detail) {
            this.file = file; this.status = status; this.average = average; this.detail = detail;
        }
    }

    /** One task per file. Never throws: every failure becomes a status. */
    static final class ScoreReader implements Callable<Outcome> {
        private final String name; private final Path path; private final long latencyMs;
        ScoreReader(String name, Path path, long latencyMs) { this.name = name; this.path = path; this.latencyMs = latencyMs; }

        @Override public Outcome call() {
            try {
                if (latencyMs > 0) Thread.sleep(latencyMs);          // simulates slow storage
                if (!Files.exists(path)) return new Outcome(name, Status.MISSING, 0, "file not found");
                List<String> lines = Files.readAllLines(path);       // MalformedInputException is an IOException
                if (lines.size() != 5) return new Outcome(name, Status.CORRUPT, 0, "expected 5 scores, found " + lines.size());
                int sum = 0;
                for (String line : lines) {
                    int score;
                    try {
                        score = Integer.parseInt(line.trim());
                    } catch (NumberFormatException e) {
                        return new Outcome(name, Status.CORRUPT, 0, "not a number: \"" + line + "\"");
                    }
                    if (score < 0 || score > 100) return new Outcome(name, Status.OUT_OF_RANGE, 0, "score " + score);
                    sum += score;
                }
                return new Outcome(name, Status.OK, sum / 5.0, "");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new Outcome(name, Status.UNEXPECTED, 0, "interrupted");
            } catch (java.io.IOException e) {
                return new Outcome(name, Status.CORRUPT, 0, "unreadable: " + e.getClass().getSimpleName());
            } catch (RuntimeException e) {
                return new Outcome(name, Status.UNEXPECTED, 0, e.toString());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args[0]);
        List<String> names = Files.readAllLines(Path.of(args[1]));
        int pool = Integer.parseInt(args[2]);
        long latency = args.length > 3 ? Long.parseLong(args[3]) : 0;
        long timeoutSec = args.length > 4 ? Long.parseLong(args[4]) : 5;

        ConcurrentHashMap<String, Outcome> results = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(pool);
        long t0 = System.nanoTime();
        List<Future<Outcome>> futures = new ArrayList<>();
        for (String name : names) futures.add(executor.submit(new ScoreReader(name, dir.resolve(name), latency)));

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            Outcome o;
            try {
                o = futures.get(i).get(timeoutSec, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                futures.get(i).cancel(true);
                o = new Outcome(name, Status.TIMEOUT, 0, "no result within " + timeoutSec + " s");
            } catch (ExecutionException e) {
                o = new Outcome(name, Status.UNEXPECTED, 0, String.valueOf(e.getCause()));
            }
            if (results.putIfAbsent(name, o) != null) System.out.println("DUPLICATE " + name);
        }
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
        executor.shutdownNow();
        boolean terminated = executor.awaitTermination(2, TimeUnit.SECONDS);

        Map<Status, Integer> counts = new EnumMap<>(Status.class);
        double sumOk = 0;
        List<String> failures = new ArrayList<>();
        for (Outcome o : results.values()) {
            counts.merge(o.status, 1, Integer::sum);
            if (o.status == Status.OK) sumOk += o.average;
            else failures.add(o.file + " " + o.status + " " + o.detail);
        }
        java.util.Collections.sort(failures);
        System.out.printf("pool=%d latency=%dms elapsed=%d ms terminated=%b complete=%b%n",
                pool, latency, elapsedMs, terminated, results.size() == names.size());
        System.out.println("counts " + counts + " total=" + results.size() + " sumOfOkAverages=" + sumOk);
        if (args.length > 5) Files.write(Path.of(args[5]), failures);
    }
}
