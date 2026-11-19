/**
 * Q1 design decision - several booking threads append to ONE shared booking log.
 * Compares StringBuilder (unsynchronised) with StringBuffer (synchronised).
 */
public class SharedLogDemo {
    static final int THREADS = 4;
    static final int APPENDS = 100_000;

    static void run(String label, CharSequenceLog log) throws InterruptedException {
        Thread[] ts = new Thread[THREADS];
        long t0 = System.nanoTime();
        for (int i = 0; i < THREADS; i++) {
            ts[i] = new Thread(() -> {
                for (int k = 0; k < APPENDS; k++) log.append("x");
            });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        long expected = (long) THREADS * APPENDS;
        long actual = log.length();
        System.out.printf("%-14s expected=%d actual=%d lost=%d time=%d ms%n",
                label, expected, actual, expected - actual, ms);
    }

    interface CharSequenceLog {
        void append(String s);
        long length();
    }

    public static void main(String[] args) throws Exception {
        for (int run = 1; run <= 3; run++) {
            System.out.println("Run " + run);
            StringBuilder sb = new StringBuilder();
            try {
                run("StringBuilder", new CharSequenceLog() {
                    public void append(String s) { sb.append(s); }
                    public long length() { return sb.length(); }
                });
            } catch (Throwable e) {
                System.out.println("StringBuilder threw " + e);
            }
            StringBuffer sf = new StringBuffer();
            run("StringBuffer", new CharSequenceLog() {
                public void append(String s) { sf.append(s); }
                public long length() { return sf.length(); }
            });
        }
    }
}
