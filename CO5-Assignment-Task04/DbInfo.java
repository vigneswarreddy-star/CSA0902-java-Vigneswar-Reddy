import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/** B4.2: reports database product, driver version and tables using DatabaseMetaData. */
public class DbInfo {
    public static void main(String[] args) throws Exception {
        try (Connection con = Db.open()) {
            DatabaseMetaData md = con.getMetaData();
            System.out.println("Database product : " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            System.out.println("Driver           : " + md.getDriverName() + " " + md.getDriverVersion()
                    + " (JDBC " + md.getJDBCMajorVersion() + "." + md.getJDBCMinorVersion() + ")");
            System.out.println("Connected URL    : " + md.getURL());
            System.out.println("User             : " + md.getUserName());
            System.out.println("Identifier quote : [" + md.getIdentifierQuoteString() + "]");
            System.out.println("Tables:");
            try (ResultSet t = md.getTables(con.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (t.next()) {
                    String table = t.getString("TABLE_NAME");
                    System.out.println("  " + table);
                    if (table.equals("bus_pass")) {
                        try (ResultSet c = md.getColumns(con.getCatalog(), null, table, "%")) {
                            while (c.next()) {
                                System.out.printf("      %-10s %-14s size=%s nullable=%s%n",
                                        c.getString("COLUMN_NAME"), c.getString("TYPE_NAME"),
                                        c.getString("COLUMN_SIZE"), c.getString("IS_NULLABLE"));
                            }
                        }
                        try (ResultSet pk = md.getPrimaryKeys(con.getCatalog(), null, table)) {
                            while (pk.next()) System.out.println("      primary key: " + pk.getString("COLUMN_NAME"));
                        }
                    }
                }
            }
        }
    }
}
