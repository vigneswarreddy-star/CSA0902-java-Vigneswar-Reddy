import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * B3.3: batch insert of 10,000 rows with auto-commit ON versus one transaction, with a
 * forced failure half-way (an invalid amount in row 5,001). Shows the table afterwards.
 */
public class TransactionDemo {
    static final String INS = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)";

    public static void main(String[] args) throws Exception {
        int n = 10_000, badAt = 5_001;
        List<String[]> rows = Passes.generate(n);
        rows.get(badAt - 1)[4] = "not-a-number";   // the forced failure

        try (Connection con = Db.open()) {
            // Case 1: auto-commit ON (the JDBC default). Each batch is committed as it runs.
            Db.truncate(con);
            con.setAutoCommit(true);
            long t0 = System.nanoTime();
            long rowsBefore = run(con, rows);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("AUTO-COMMIT ON : failed at row " + badAt + ", took " + ms + " ms, rows left in table = "
                    + Db.count(con) + " (partial load, cannot be undone)");

            // Case 2: one transaction. Everything is rolled back when the failure happens.
            Db.truncate(con);
            con.setAutoCommit(false);
            t0 = System.nanoTime();
            try {
                PassLoader.loadAll(con, rows);
                System.out.println("TRANSACTION: unexpectedly succeeded");
            } catch (Exception e) {
                ms = (System.nanoTime() - t0) / 1_000_000;
                System.out.println("TRANSACTION    : failed (" + e.getClass().getSimpleName() + ") after "
                        + ms + " ms, rows left in table = " + Db.count(con) + " (all rows rolled back)");
            }
            con.setAutoCommit(true);

            // Control: the same 10,000 rows without the bad value, in one transaction, succeed.
            List<String[]> good = Passes.generate(n);
            Db.truncate(con);
            t0 = System.nanoTime();
            int loaded = PassLoader.loadAll(con, good);
            ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("CONTROL (no bad row): loaded " + loaded + " rows in one transaction in " + ms
                    + " ms; rows in table = " + Db.count(con));
        }
    }

    /** Auto-commit path: same batching, no transaction. Returns the rows committed before the failure. */
    static long run(Connection con, List<String[]> rows) throws Exception {
        long committed = 0;
        try (PreparedStatement ps = con.prepareStatement(INS)) {
            int k = 0;
            for (String[] p : rows) {
                ps.setString(1, p[0]); ps.setString(2, p[1]); ps.setString(3, p[2]);
                ps.setInt(4, Integer.parseInt(p[3]));
                try {
                    ps.setDouble(5, Double.parseDouble(p[4]));
                } catch (NumberFormatException e) {
                    System.out.println("  (failure: " + e.getMessage() + ")");
                    return committed;
                }
                ps.addBatch();
                if (++k % 1000 == 0) { ps.executeBatch(); committed = k; }
            }
            ps.executeBatch();
        }
        return committed;
    }
}
