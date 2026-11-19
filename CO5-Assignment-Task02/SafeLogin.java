import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** B2 redesign: the same check with a PreparedStatement. The input is never part of the SQL text. */
public class SafeLogin {
    public static boolean login(Connection con, String id, String pwd) throws Exception {
        String sql = "SELECT 1 FROM users WHERE id = ? AND pwd = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
