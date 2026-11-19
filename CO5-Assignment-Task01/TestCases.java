import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** B1.1: normal case, a pass ID that does not exist, and a damaged line. */
public class TestCases {
    static int failed = 0;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + "  " + detail);
        if (!ok) failed++;
    }

    public static void main(String[] args) throws Exception {
        Path f = Files.createTempFile("b1test", ".csv");
        PassStore s = new PassStore(f);
        s.add("P1001", "192311001", "Arun", 12, 4500);
        Files.writeString(f, "P1002,DAMAGED LINE\n", java.nio.file.StandardOpenOption.APPEND);
        s.add("P1003", "192311003", "Divya", 7, 3800);
        Files.writeString(f, "\n", java.nio.file.StandardOpenOption.APPEND);
        s.add("P1004", "192311004", "Kiran", 3, 2900);

        String[] normal = s.find("P1003");
        check("normal case: find existing pass P1003",
              normal != null && Arrays.equals(normal, new String[]{"P1003", "192311003", "Divya", "7", "3800.0"}),
              Arrays.toString(normal));

        check("pass after damaged line is still found (P1004)",
              s.find("P1004") != null, Arrays.toString(s.find("P1004")));

        check("non-existent ID returns null",
              s.find("P9999") == null, String.valueOf(s.find("P9999")));

        check("damaged line P1002 is not returned as a pass",
              s.find("P1002") == null, String.valueOf(s.find("P1002")));

        Path missing = Path.of(f.toString() + ".does-not-exist");
        check("missing file returns null, no exception",
              new PassStore(missing).find("P1001") == null, "null");

        Files.delete(f);
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : failed + " TEST(S) FAILED");
    }
}
