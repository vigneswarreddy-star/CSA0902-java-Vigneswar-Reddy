import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * B4.1: migrates the text pass file into the bus_pass table.
 * Step 1: read and validate EVERY line; keep valid rows, record each rejected line and why.
 * Step 2: load the valid rows with PassLoader in ONE transaction. If the database rejects
 *         anything, every row is rolled back and nothing is half-migrated.
 */
public class Migrate {

    public static void main(String[] args) throws Exception {
        Path file = Paths.get(args.length > 0 ? args[0] : "data/passes_migrate.txt");
        List<String[]> valid = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        int lines = 0;

        try (BufferedReader r = Files.newBufferedReader(file)) {
            String line;
            while ((line = r.readLine()) != null) {
                lines++;
                String reason = validate(line);
                if (reason == null) {
                    valid.add(line.split(",", -1));
                } else {
                    rejected.add("line " + lines + ": " + reason + "  | " + line);
                }
            }
        }

        System.out.println("Migration of " + file + ": read " + lines + " lines, valid "
                + valid.size() + ", rejected " + rejected.size());
        for (String msg : rejected) {
            System.out.println("  REJECTED " + msg);
        }

        try (Connection con = Db.open()) {
            long t0 = System.nanoTime();
            try {
                int loaded = PassLoader.loadAll(con, valid);
                long ms = (System.nanoTime() - t0) / 1_000_000;
                System.out.println("Loaded " + loaded + " rows in one transaction in " + ms + " ms. "
                        + "Rows in bus_pass = " + Db.count(con));
            } catch (Exception e) {
                System.out.println("Load FAILED and was rolled back: " + e.getMessage());
                System.out.println("Rows in bus_pass = " + Db.count(con) + " (nothing half-migrated)");
            }
        }
    }

    /** Returns null if the line is valid, otherwise the reason it was rejected. */
    static String validate(String line) {
        String[] f = line.split(",", -1);
        if (f.length != 5) return "wrong number of fields (" + f.length + ")";
        if (!f[0].matches("P\\d{6}")) return "pass ID must be P followed by 6 digits";
        if (!f[1].matches("\\d{9}")) return "register number must be 9 digits";
        if (f[2].isBlank()) return "name is empty";
        try {
            int route = Integer.parseInt(f[3]);
            if (route < 1 || route > 99) return "route number out of range (1-99)";
        } catch (NumberFormatException e) {
            return "route number is not a whole number";
        }
        try {
            double amount = Double.parseDouble(f[4]);
            if (!(amount > 0)) return "amount must be greater than zero";
        } catch (NumberFormatException e) {
            return "amount is not a number";
        }
        return null;
    }
}
