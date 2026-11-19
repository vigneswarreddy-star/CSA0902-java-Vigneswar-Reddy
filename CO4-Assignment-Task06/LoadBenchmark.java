import java.util.Arrays;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 * Task 6 alternative analysis: time and memory for loading ALL rows into the JTable
 * model at once versus loading one page (LIMIT/OFFSET). Run after inserting
 * 100,000 test rows (see report, Task 6). Results depend on your machine.
 */
public class LoadBenchmark {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/campus_events";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password";
    private static final int RUNS = 5;

    public static void main(String[] args) throws Exception {
        RegistrationRepository repo = new RegistrationRepository(DB_URL, DB_USER, DB_PASSWORD);
        int total = (int) countRows(repo);
        System.out.println("Rows in database: " + total);

        long[] allTimes = new long[RUNS];
        long[] pageFirstTimes = new long[RUNS];
        long[] pageLastTimes = new long[RUNS];
        long lastAllBytes = 0;

        for (int i = 0; i < RUNS; i++) {
            long t0 = System.nanoTime();
            DefaultTableModel model = newModel();
            List<Registration> all = repo.findPage(null, total, 0);
            for (Registration r : all) {
                model.addRow(new Object[]{r.getId(), r.getEventName(), r.getStudentId(),
                        r.getStudentName(), r.getEmail(), r.getRegisteredAt()});
            }
            allTimes[i] = (System.nanoTime() - t0) / 1_000_000;
            // Measure heap while the fully loaded model is still referenced.
            lastAllBytes = usedMemoryAfterGc();
            System.out.println("  run " + (i + 1) + ": rows in model = " + model.getRowCount());
            model = null;
            all = null;

            t0 = System.nanoTime();
            List<Registration> first = repo.findPage(null, 51, 0);
            pageFirstTimes[i] = (System.nanoTime() - t0) / 1_000_000;

            t0 = System.nanoTime();
            List<Registration> last = repo.findPage(null, 51, total - 50);
            pageLastTimes[i] = (System.nanoTime() - t0) / 1_000_000;
            if (first.isEmpty() || last.isEmpty()) {
                throw new IllegalStateException("benchmark pages were empty");
            }
        }

        System.out.println("Load ALL rows into table model (ms), 5 runs: " + Arrays.toString(allTimes)
                + "  median = " + median(allTimes));
        System.out.println("Load first page of 50 (ms), 5 runs:          " + Arrays.toString(pageFirstTimes)
                + "  median = " + median(pageFirstTimes));
        System.out.println("Load last page (OFFSET near end) (ms):        " + Arrays.toString(pageLastTimes)
                + "  median = " + median(pageLastTimes));
        System.out.println("Heap added by full table model (MB, approx): " + (heapCost(repo) / (1024 * 1024)));
    }

    private static DefaultTableModel newModel() {
        return new DefaultTableModel(new Object[]{"ID", "Event", "Student ID", "Name", "Email", "Registered at"}, 0);
    }

    private static long countRows(RegistrationRepository repo) throws Exception {
        return repo.findPage(null, Integer.MAX_VALUE, 0).size();
    }

    private static long usedMemoryAfterGc() {
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < 3; i++) System.gc();
        return rt.totalMemory() - rt.freeMemory();
    }

    /** Heap used by the table rows only: memory before loading minus memory after loading (both after GC). */
    private static long heapCost(RegistrationRepository repo) throws Exception {
        long before = usedMemoryAfterGc();
        DefaultTableModel model = newModel();
        for (Registration r : repo.findPage(null, Integer.MAX_VALUE, 0)) {
            model.addRow(new Object[]{r.getId(), r.getEventName(), r.getStudentId(),
                    r.getStudentName(), r.getEmail(), r.getRegisteredAt()});
        }
        long after = usedMemoryAfterGc();
        System.out.println("Rows in model (memory test): " + model.getRowCount());
        return after - before;
    }

    private static long median(long[] values) {
        long[] copy = values.clone();
        Arrays.sort(copy);
        return copy[copy.length / 2];
    }
}
