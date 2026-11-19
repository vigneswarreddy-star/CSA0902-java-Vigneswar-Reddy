import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * B3.2: time to insert 1,000 / 10,000 / 100,000 rows with (a) Statement,
 * (b) PreparedStatement and (c) PreparedStatement with batches.
 * All three run inside ONE transaction (auto-commit off), so the measurement compares
 * the statement type, not the cost of one disk flush per row. B3.3 measures auto-commit.
 */
public class InsertBenchmark {
    static final String INS = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)";

    public static void main(String[] args) throws Exception {
        int[] sizes = {1_000, 10_000, 100_000};
        System.out.println("rows\tmethod\tms\trows_in_table");
        try (Connection con = Db.open()) {
            con.setAutoCommit(false);
            for (int n : sizes) {
                List<String[]> rows = Passes.generate(n);

                Db.truncate(con); con.commit();
                long t0 = System.nanoTime();
                try (Statement st = con.createStatement()) {
                    for (String[] p : rows) {
                        st.executeUpdate(String.format("INSERT INTO bus_pass VALUES ('%s','%s','%s',%s,%s)",
                                p[0], p[1], p[2], p[3], p[4]));
                    }
                }
                con.commit();
                report(con, n, "Statement", t0);

                Db.truncate(con); con.commit();
                t0 = System.nanoTime();
                try (PreparedStatement ps = con.prepareStatement(INS)) {
                    for (String[] p : rows) {
                        ps.setString(1, p[0]); ps.setString(2, p[1]); ps.setString(3, p[2]);
                        ps.setInt(4, Integer.parseInt(p[3])); ps.setDouble(5, Double.parseDouble(p[4]));
                        ps.executeUpdate();
                    }
                }
                con.commit();
                report(con, n, "PreparedStatement", t0);

                Db.truncate(con); con.commit();
                t0 = System.nanoTime();
                try (PreparedStatement ps = con.prepareStatement(INS)) {
                    int k = 0;
                    for (String[] p : rows) {
                        ps.setString(1, p[0]); ps.setString(2, p[1]); ps.setString(3, p[2]);
                        ps.setInt(4, Integer.parseInt(p[3])); ps.setDouble(5, Double.parseDouble(p[4]));
                        ps.addBatch();
                        if (++k % 1000 == 0) ps.executeBatch();
                    }
                    ps.executeBatch();
                }
                con.commit();
                report(con, n, "PreparedStatement+batch", t0);
            }
        }
    }

    static void report(Connection con, int n, String method, long t0) throws Exception {
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.println(n + "\t" + method + "\t" + ms + "\t" + Db.count(con));
    }
}
