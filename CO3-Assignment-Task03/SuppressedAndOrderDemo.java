/** Shows (1) the order in which try-with-resources closes resources and (2) a suppressed exception. */
public class SuppressedAndOrderDemo {
    static class Res implements AutoCloseable {
        private final String name; private final boolean failOnClose;
        Res(String name, boolean failOnClose) { this.name = name; this.failOnClose = failOnClose; System.out.println("open " + name); }
        @Override public void close() {
            System.out.println("close " + name);
            if (failOnClose) throw new IllegalStateException("close failed for " + name);
        }
    }

    public static void main(String[] args) {
        System.out.println("(1) order of closing");
        try (Res a = new Res("A", false); Res b = new Res("B", false)) {
            System.out.println("body runs");
        }

        System.out.println("\n(2) body fails AND close fails -> suppressed exception");
        try (Res c = new Res("C", true)) {
            throw new RuntimeException("body failed");
        } catch (RuntimeException e) {
            System.out.println("primary: " + e.getMessage());
            for (Throwable s : e.getSuppressed()) System.out.println("suppressed: " + s.getMessage());
        }
    }
}
