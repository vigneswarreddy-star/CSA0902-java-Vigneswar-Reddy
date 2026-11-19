import java.io.Serializable;
public class Resident implements Serializable {
    private static final long serialVersionUID = 1L;
    String name; String regNo; transient String password;
    Resident(String n, String r, String p) { name = n; regNo = r; password = p; }
}
