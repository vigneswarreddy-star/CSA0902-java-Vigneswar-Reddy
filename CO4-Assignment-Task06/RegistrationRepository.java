import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-access layer (JDBC). Only this class knows SQL. Every method uses a
 * PreparedStatement, so user input is never concatenated into the SQL text.
 */
public class RegistrationRepository {

    private final String url;
    private final String user;
    private final String password;

    public RegistrationRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Event name to event id, in alphabetical order. */
    public Map<String, Integer> findEvents() throws SQLException {
        Map<String, Integer> events = new LinkedHashMap<>();
        String sql = "SELECT event_id, event_name FROM events ORDER BY event_name";
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                events.put(rs.getString("event_name"), rs.getInt("event_id"));
            }
        }
        return events;
    }

    public boolean exists(int eventId, String studentId) throws SQLException {
        String sql = "SELECT 1 FROM registrations WHERE event_id = ? AND student_id = ?";
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setString(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insert(int eventId, String studentId, String name, String email) throws SQLException {
        String sql = "INSERT INTO registrations (event_id, student_id, student_name, email) VALUES (?, ?, ?, ?)";
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setString(2, studentId);
            ps.setString(3, name);
            ps.setString(4, email);
            ps.executeUpdate();
        }
    }

    /**
     * One page of registrations, optionally for a single event. LIMIT and OFFSET
     * let the UI load a page at a time instead of the whole table.
     * eventId == null means "all events".
     */
    public List<Registration> findPage(Integer eventId, int limit, int offset) throws SQLException {
        String sql = "SELECT r.registration_id, e.event_name, r.student_id, r.student_name, "
                   + "       r.email, r.registered_at "
                   + "FROM registrations r JOIN events e ON e.event_id = r.event_id "
                   + "WHERE (? IS NULL OR r.event_id = ?) "
                   + "ORDER BY r.registration_id "
                   + "LIMIT ? OFFSET ?";
        List<Registration> rows = new ArrayList<>();
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (eventId == null) {
                ps.setNull(1, Types.INTEGER);
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(1, eventId);
                ps.setInt(2, eventId);
            }
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Registration(
                            rs.getInt(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5), rs.getTimestamp(6)));
                }
            }
        }
        return rows;
    }
}
