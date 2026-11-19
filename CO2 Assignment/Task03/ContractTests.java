import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Q3 - Three designed tests of the equals/hashCode contract with predictions made BEFORE running.
 */
public class ContractTests {

    /** Broken on purpose: equals() without hashCode(). */
    static class BrokenStudent {
        final String regNo;
        BrokenStudent(String regNo) { this.regNo = regNo; }
        @Override public boolean equals(Object o) {
            return o instanceof BrokenStudent && regNo.equals(((BrokenStudent) o).regNo);
        }
    }

    /** Fixed class, but regNo is NOT final, so it can be changed after insertion. */
    static class MutableStudent {
        String regNo;
        MutableStudent(String regNo) { this.regNo = regNo; }
        @Override public boolean equals(Object o) {
            return o instanceof MutableStudent && regNo.equals(((MutableStudent) o).regNo);
        }
        @Override public int hashCode() { return Objects.hash(regNo); }
    }

    static class FixedStudent {
        final String regNo;
        FixedStudent(String regNo) { this.regNo = regNo; }
        @Override public boolean equals(Object o) {
            return o instanceof FixedStudent && regNo.equals(((FixedStudent) o).regNo);
        }
        @Override public int hashCode() { return regNo.hashCode(); }
    }

    public static void main(String[] args) {
        // Test 1 - scans that should work correctly: 3 different students, each scanned once.
        System.out.println("TEST 1: 3 distinct students, each scanned once");
        System.out.println("  Predicted size: 3 (no duplicates exist, so equality never needs to match)");
        Set<FixedStudent> t1 = new HashSet<>();
        t1.add(new FixedStudent("192311001"));
        t1.add(new FixedStudent("192311002"));
        t1.add(new FixedStudent("192311003"));
        System.out.println("  Observed size (fixed class): " + t1.size());

        // Test 2 - scans designed to expose duplicates: student A scanned 3 times, B scanned 2 times.
        System.out.println("\nTEST 2: A scanned 3 times, B scanned 2 times (5 scans, 2 students)");
        System.out.println("  Predicted size with broken class: 5 (no hashCode, so every object has its own identity hash and equals() is never consulted)");
        Set<BrokenStudent> t2broken = new HashSet<>();
        for (int i = 0; i < 3; i++) t2broken.add(new BrokenStudent("A"));
        for (int i = 0; i < 2; i++) t2broken.add(new BrokenStudent("B"));
        System.out.println("  Observed size (broken, no hashCode): " + t2broken.size());
        System.out.println("  Predicted size with fixed class: 2");
        Set<FixedStudent> t2fixed = new HashSet<>();
        for (int i = 0; i < 3; i++) t2fixed.add(new FixedStudent("A"));
        for (int i = 0; i < 2; i++) t2fixed.add(new FixedStudent("B"));
        System.out.println("  Observed size (fixed): " + t2fixed.size());

        // Test 3 - own test: change a hashCode field after insertion.
        System.out.println("\nTEST 3: mutate regNo after the object is inserted into a HashSet");
        System.out.println("  Predicted: size stays 1, but contains(object) is false because its bucket is stale");
        Set<MutableStudent> t3 = new HashSet<>();
        MutableStudent s = new MutableStudent("192311001");
        t3.add(s);
        s.regNo = "192311999";
        System.out.println("  Observed size: " + t3.size());
        System.out.println("  Observed contains(same object): " + t3.contains(s));
        System.out.println("  Observed contains(new object with new regNo): "
                + t3.contains(new MutableStudent("192311999")));
        System.out.println("  Observed iteration still shows the object: " + t3.iterator().next().regNo);
    }
}
