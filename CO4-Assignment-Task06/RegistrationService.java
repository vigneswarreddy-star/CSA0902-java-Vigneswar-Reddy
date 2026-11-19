import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service layer: validation rules, the duplicate rule, paging and CSV export.
 * No Swing code here, so every rule can be tested from a console program.
 */
public class RegistrationService {

    public static final int PAGE_SIZE = 50;
    private static final int EXPORT_BATCH = 1000;

    private static final Pattern STUDENT_ID = Pattern.compile("[A-Za-z0-9]{1,20}");
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z .'\\-]{0,99}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    public static class ValidationException extends Exception {
        public ValidationException(String message) { super(message); }
    }

    public static class DuplicateRegistrationException extends Exception {
        public DuplicateRegistrationException(String message) { super(message); }
    }

    public static class DataAccessException extends Exception {
        public DataAccessException(String message, Throwable cause) { super(message, cause); }
    }

    /** One page of rows plus a flag saying whether another page exists. */
    public static class Page {
        private final List<Registration> rows;
        private final boolean hasNext;

        public Page(List<Registration> rows, boolean hasNext) {
            this.rows = rows;
            this.hasNext = hasNext;
        }

        public List<Registration> getRows() { return rows; }
        public boolean hasNext() { return hasNext; }
    }

    private final RegistrationRepository repository;

    public RegistrationService(RegistrationRepository repository) {
        this.repository = repository;
    }

    public Map<String, Integer> loadEvents() throws DataAccessException {
        try {
            return repository.findEvents();
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the event list.", e);
        }
    }

    /** Fetches PAGE_SIZE + 1 rows: the extra row tells us whether a Next page exists. */
    public Page page(Integer eventId, int pageIndex) throws DataAccessException {
        try {
            List<Registration> rows = repository.findPage(eventId, PAGE_SIZE + 1, pageIndex * PAGE_SIZE);
            boolean hasNext = rows.size() > PAGE_SIZE;
            if (hasNext) {
                rows = rows.subList(0, PAGE_SIZE);
            }
            return new Page(rows, hasNext);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load registrations.", e);
        }
    }

    public void register(int eventId, String studentId, String name, String email)
            throws ValidationException, DuplicateRegistrationException, DataAccessException {
        String sid = trim(studentId);
        String nm = trim(name);
        String em = trim(email);

        if (!STUDENT_ID.matcher(sid).matches()) {
            throw new ValidationException("Student ID must be 1 to 20 letters or digits.");
        }
        if (!NAME.matcher(nm).matches()) {
            throw new ValidationException("Name must start with a letter and may contain only letters, spaces, dots, apostrophes and hyphens.");
        }
        if (em.length() > 120 || !EMAIL.matcher(em).matches()) {
            throw new ValidationException("Enter a valid email address, for example name@campus.edu.");
        }
        try {
            if (repository.exists(eventId, sid)) {          // first check gives a friendly message
                throw new DuplicateRegistrationException("Student " + sid + " is already registered for this event.");
            }
            repository.insert(eventId, sid, nm, em);
        } catch (SQLIntegrityConstraintViolationException e) {
            // Second line of defence: the UNIQUE(event_id, student_id) constraint caught a double submit.
            throw new DuplicateRegistrationException("Student " + sid + " is already registered for this event.");
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the registration.", e);
        }
    }

    /** Writes every matching row to a CSV file and returns the number of rows written. */
    public int exportCsv(Integer eventId, Path file) throws DataAccessException, IOException {
        int count = 0;
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("ID,Event,Student ID,Name,Email,Registered at");
            w.newLine();
            int offset = 0;
            while (true) {
                List<Registration> batch = repository.findPage(eventId, EXPORT_BATCH, offset);
                for (Registration r : batch) {
                    w.write(csv(String.valueOf(r.getId())) + "," + csv(r.getEventName()) + ","
                            + csv(r.getStudentId()) + "," + csv(r.getStudentName()) + ","
                            + csv(r.getEmail()) + "," + csv(String.valueOf(r.getRegisteredAt())));
                    w.newLine();
                    count++;
                }
                if (batch.size() < EXPORT_BATCH) {
                    break;
                }
                offset += EXPORT_BATCH;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Export failed while reading the database.", e);
        }
        return count;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /** Quotes a CSV field and doubles any embedded quote characters. */
    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
