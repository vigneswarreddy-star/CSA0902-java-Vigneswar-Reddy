import java.io.Serializable;
public class Resident implements Serializable {
    String name; String regNo; String phone; transient String password;   // phone added, no serialVersionUID
    Resident(String n, String r, String p) { name = n; regNo = r; password = p; }
}
