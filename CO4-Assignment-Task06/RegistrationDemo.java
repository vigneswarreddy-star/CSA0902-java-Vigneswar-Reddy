import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Map;

/**
 * Console demonstration of the service layer (no window needed).
 * It shows a normal registration, invalid input, a duplicate, paging and CSV export.
 * It removes only its own test rows (student IDs starting with TEST) before it starts.
 */
public class RegistrationDemo {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/campus_events";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password";

    public static void main(String[] args) throws Exception {
        RegistrationService service = new RegistrationService(
                new RegistrationRepository(DB_URL, DB_USER, DB_PASSWORD));
        cleanTestRows();

        Map<String, Integer> events = service.loadEvents();
        int music = events.get("Music Night");

        System.out.println("TEST 1 normal registration");
        attempt(service, music, "TEST01", "Asha Kumar", "asha@campus.edu");

        System.out.println("TEST 2 invalid student ID");
        attempt(service, music, "TEST-02!", "Ravi Singh", "ravi@campus.edu");

        System.out.println("TEST 3 invalid name");
        attempt(service, music, "TEST03", "123 Name", "name@campus.edu");

        System.out.println("TEST 4 invalid email");
        attempt(service, music, "TEST04", "Meena Iyer", "meena-at-campus");

        System.out.println("TEST 5 duplicate for the same event");
        attempt(service, music, "TEST01", "Asha Kumar", "asha@campus.edu");

        System.out.println("TEST 6 same student, different event (allowed)");
        attempt(service, events.get("Tech Talk"), "TEST01", "Asha Kumar", "asha@campus.edu");

        System.out.println("TEST 7 paging (51 students, page size " + RegistrationService.PAGE_SIZE + ")");
        for (int i = 10; i <= 60; i++) {
            attempt(service, music, "TEST" + i, "Test Student", "s" + i + "@campus.edu", false);
        }
        RegistrationService.Page first = service.page(music, 0);
        RegistrationService.Page second = service.page(music, 1);
        System.out.println("  page 1 rows = " + first.getRows().size() + ", hasNext = " + first.hasNext());
        System.out.println("  page 2 rows = " + second.getRows().size() + ", hasNext = " + second.hasNext());

        System.out.println("TEST 8 CSV export for Music Night");
        Path file = Paths.get("registrations_export_test.csv");
        int written = service.exportCsv(music, file);
        System.out.println("  rows written = " + written + " (file " + file.toAbsolutePath() + ")");
        System.out.println("  first two lines of file:");
        java.util.List<String> lines = Files.readAllLines(file);
        System.out.println("    " + lines.get(0));
        System.out.println("    " + lines.get(1));
    }

    private static void attempt(RegistrationService service, int eventId, String sid, String name, String email) {
        attempt(service, eventId, sid, name, email, true);
    }

    private static void attempt(RegistrationService service, int eventId, String sid, String name, String email,
                                boolean print) {
        try {
            service.register(eventId, sid, name, email);
            if (print) System.out.println("  ACCEPTED: " + sid);
        } catch (RegistrationService.ValidationException e) {
            if (print) System.out.println("  REJECTED (validation): " + e.getMessage());
        } catch (RegistrationService.DuplicateRegistrationException e) {
            if (print) System.out.println("  REJECTED (duplicate): " + e.getMessage());
        } catch (RegistrationService.DataAccessException e) {
            System.out.println("  DATABASE ERROR: " + e.getMessage() + " / " + e.getCause());
        }
    }

    private static void cleanTestRows() throws Exception {
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = c.prepareStatement("DELETE FROM registrations WHERE student_id LIKE 'TEST%'")) {
            ps.executeUpdate();
        }
    }
}
