import java.util.concurrent.atomic.AtomicLong;

/**
 * Stress test for the while/if question: 1 cook puts 50,000 tokens (each token = 1),
 * 4 students take them. A correct counter gives exactly 50,000 and never returns a stale value.
 * Run with an argument "if" to use TokenCounterIf, otherwise TokenCounter.
 */
public class StressIfVsWhile {
    static final int TOKENS = 50_000, STUDENTS = 4;

    public static void main(String[] args) throws InterruptedException {
        boolean useIf = args.length > 0 && args[0].equals("if");
        int runs = 10;
        int wrong = 0;
        for (int run = 1; run <= runs; run++) {
            AtomicLong sum = new AtomicLong();
            Object counter = useIf ? new TokenCounterIf() : new TokenCounter();
            Thread cook = new Thread(() -> {
                try {
                    for (int t = 0; t < TOKENS; t++) put(counter, 1);
                } catch (Exception e) { System.out.println("cook error: " + e); }
            });
            Thread[] students = new Thread[STUDENTS];
            int per = TOKENS / STUDENTS;
            for (int s = 0; s < STUDENTS; s++) {
                students[s] = new Thread(() -> {
                    try {
                        for (int k = 0; k < per; k++) sum.addAndGet(take(counter));
                    } catch (Exception e) { System.out.println("student error: " + e); }
                });
            }
            cook.start(); for (Thread t : students) t.start();
            cook.join(); for (Thread t : students) t.join();
            boolean ok = sum.get() == TOKENS;
            if (!ok) wrong++;
            System.out.println("run " + run + ": tokens collected value sum = " + sum.get() + " (expected " + TOKENS + ")" + (ok ? "" : "  <-- WRONG"));
        }
        System.out.println((useIf ? "TokenCounterIf" : "TokenCounter") + ": wrong runs = " + wrong + " of " + runs);
    }

    static void put(Object c, int v) throws InterruptedException {
        if (c instanceof TokenCounterIf) ((TokenCounterIf) c).put(v); else ((TokenCounter) c).put(v);
    }

    static int take(Object c) throws InterruptedException {
        return c instanceof TokenCounterIf ? ((TokenCounterIf) c).take() : ((TokenCounter) c).take();
    }
}
