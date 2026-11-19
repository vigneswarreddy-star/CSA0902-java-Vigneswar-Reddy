import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;

/**
 * B2: adversarial inputs for the login check (unsafe versus PreparedStatement)
 * and for a damaged bus-pass file. Uses only the local transport_test database.
 */
public class AdversarialTest {

    static final String URL = "jdbc:mysql://localhost:3306/transport_test";
    static final String USER = "root", PWD = "password";

    public static void main(String[] args) throws Exception {
        System.out.println("=== Part 1: login with crafted input ===");
        String[][] cases = {
            {"admin",             "secret123", "correct login (both must accept)"},
            {"admin",             "wrong",     "wrong password (both must reject)"},
            {"admin'-- ",         "anything",  "comment attack: rest of WHERE clause removed"},
            {"x' OR '1'='1",      "anything",  "tautology attack: WHERE becomes always true"},
            {"x' OR '1'='1' -- ", "",          "tautology with empty password"},
        };
        try (Connection con = DriverManager.getConnection(URL, USER, PWD)) {
            System.out.println("input_id | input_pwd | description | UnsafeLogin | SafeLogin");
            for (String[] c : cases) {
                boolean unsafe = UnsafeLogin.login(con, c[0], c[1]);
                boolean safe = SafeLogin.login(con, c[0], c[1]);
                System.out.printf("[%s] | [%s] | %s | %s | %s%n", c[0], c[1], c[2],
                        unsafe ? "ACCEPTED" : "rejected", safe ? "ACCEPTED" : "rejected");
            }
        }

        System.out.println();
        System.out.println("=== Part 2: damaged bus-pass file ===");
        Path f = Files.createTempFile("adv", ".csv");
        PassStore store = new PassStore(f);
        store.add("P2001", "192311001", "Arun", 12, 4500);
        Files.writeString(f, "P2002,only,three\n", StandardOpenOption.APPEND);
        Files.writeString(f, "P2003,192311003,Divya,7,abc\n", StandardOpenOption.APPEND);
        Files.writeString(f, "\n", StandardOpenOption.APPEND);
        Files.writeString(f, "P2005,192311005,Lee, Kumar,7,3800\n", StandardOpenOption.APPEND);
        store.add("P2006", "192311006", "Meena", 9, 3100);
        for (String id : new String[]{"P2001", "P2002", "P2003", "P2005", "P2006"}) {
            System.out.println("find(" + id + ") = " + Arrays.toString(store.find(id)));
        }
        Files.delete(f);
    }
}
