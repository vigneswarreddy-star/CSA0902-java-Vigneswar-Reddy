import java.sql.Timestamp;

/** Model: one registration row as shown in the table. */
public class Registration {
    private final int id;
    private final String eventName;
    private final String studentId;
    private final String studentName;
    private final String email;
    private final Timestamp registeredAt;

    public Registration(int id, String eventName, String studentId, String studentName,
                        String email, Timestamp registeredAt) {
        this.id = id;
        this.eventName = eventName;
        this.studentId = studentId;
        this.studentName = studentName;
        this.email = email;
        this.registeredAt = registeredAt;
    }

    public int getId() { return id; }
    public String getEventName() { return eventName; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getEmail() { return email; }
    public Timestamp getRegisteredAt() { return registeredAt; }
}
