import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;

/** B3.1: insert 10 records with each approach, then verify with SELECT and ResultSetMetaData. */
public class CorrectnessCheck {
    public static void main(String[] args) throws Exception {
        List<String[]> rows = Passes.generate(10);
        String ins = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Db.open()) {
            // (a) Statement, one SQL string per row
            Db.truncate(con);
            try (Statement st = con.createStatement()) {
                for (String[] p : rows) {
                    st.executeUpdate(String.format(
                        "INSERT INTO bus_pass VALUES ('%s','%s','%s',%s,%s)",
                        p[0], p[1], p[2], p[3], p[4]));
                }
            }
            verify(con, "Statement", rows);

            // (b) PreparedStatement, one row per execute
            Db.truncate(con);
            try (PreparedStatement ps = con.prepareStatement(ins)) {
                for (String[] p : rows) {
                    ps.setString(1, p[0]); ps.setString(2, p[1]); ps.setString(3, p[2]);
                    ps.setInt(4, Integer.parseInt(p[3])); ps.setDouble(5, Double.parseDouble(p[4]));
                    ps.executeUpdate();
                }
            }
            verify(con, "PreparedStatement", rows);

            // (c) PreparedStatement with addBatch / executeBatch
            Db.truncate(con);
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(ins)) {
                for (String[] p : rows) {
                    ps.setString(1, p[0]); ps.setString(2, p[1]); ps.setString(3, p[2]);
                    ps.setInt(4, Integer.parseInt(p[3])); ps.setDouble(5, Double.parseDouble(p[4]));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            con.commit();
            con.setAutoCommit(true);
            verify(con, "Batch", rows);
        }
    }

    static void verify(Connection con, String method, List<String[]> expected) throws Exception {
        int matches = 0;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM bus_pass ORDER BY pass_id")) {
            ResultSetMetaData md = rs.getMetaData();
            if (method.equals("Statement")) {
                System.out.println("Column metadata (read from ResultSetMetaData):");
                for (int c = 1; c <= md.getColumnCount(); c++) {
                    System.out.printf("  %-9s %-14s nullable=%d%n",
                        md.getColumnName(c), md.getColumnTypeName(c), md.isNullable(c));
                }
            }
            int i = 0;
            while (rs.next()) {
                String[] p = expected.get(i++);
                boolean ok = rs.getString(1).equals(p[0]) && rs.getString(2).equals(p[1])
                        && rs.getString(3).equals(p[2]) && rs.getInt(4) == Integer.parseInt(p[3])
                        && Math.abs(rs.getDouble(5) - Double.parseDouble(p[4])) < 0.001;
                if (ok) matches++;
            }
        }
        System.out.println(method + ": rows in table = " + Db.count(con)
                + ", rows matching input = " + matches + " of " + expected.size()
                + (matches == expected.size() ? "  -> CORRECT" : "  -> MISMATCH"));
    }
}
