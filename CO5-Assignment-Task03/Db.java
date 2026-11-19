import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Db {
    static final String URL = "jdbc:mysql://localhost:3306/transport_test";
    static final String USER = "root", PWD = "password";

    public static Connection open() throws SQLException {
        return DriverManager.getConnection(URL, USER, PWD);
    }

    public static void truncate(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("TRUNCATE TABLE bus_pass");
        }
    }

    public static int count(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
