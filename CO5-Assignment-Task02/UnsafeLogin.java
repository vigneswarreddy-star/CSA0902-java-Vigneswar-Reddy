import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/** Task A5.2 / B2: the login check as written in the assignment. DO NOT use in real code. */
public class UnsafeLogin {
    public static boolean login(Connection con, String id, String pwd) throws Exception {
        String sql = "SELECT * FROM users WHERE id='" + id + "' AND pwd='" + pwd + "'";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next();          // any matching row = login accepted
        }
    }
}
