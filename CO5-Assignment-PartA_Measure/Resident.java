import java.io.Serializable;

/** A4 example class. The password is transient so it is never written to disk. */
public class Resident implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    String regNo;
    int roomNo;
    transient String password;   // not saved

    Resident(String name, String regNo, int roomNo, String password) {
        this.name = name; this.regNo = regNo; this.roomNo = roomNo; this.password = password;
    }
}
