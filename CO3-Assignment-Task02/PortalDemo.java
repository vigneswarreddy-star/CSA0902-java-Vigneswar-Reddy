import java.io.IOException;
import java.util.Set;

/** Checked: the window closed before the deadline. */
class ExamWindowClosedException extends ExamException {
    ExamWindowClosedException(String message) { super(message); }
}

/** Checked: the student is not registered for the course. */
class NotRegisteredException extends ExamException {
    NotRegisteredException(String message) { super(message); }
}

/** Layer 2: business rules. Declares what it may throw and does not swallow exceptions. */
class ExamService {
    private final Storage storage = new Storage();
    private final Set<String> registrations = Set.of("CS101:S1", "CS101:S2");
    private boolean windowOpen = true;

    void closeWindow() { windowOpen = false; }

    void submit(String student, String course, String file, long sizeKb) throws ExamException {
        if (!windowOpen) throw new ExamWindowClosedException("Exam window closed for " + course);
        if (!registrations.contains(course + ":" + student)) {
            throw new NotRegisteredException(student + " is not registered for " + course);
        }
        try {
            storage.save(file, sizeKb);                    // SubmissionStorageException propagates
        } catch (SubmissionStorageException e) {
            // Rethrown WITH the cause (correct): the original IOException stays reachable.
            throw e;
        }
    }
}

/** Layer 1: the controller. The only place where the student sees a message. */
class ExamController {
    private final ExamService service = new ExamService();

    void handle(String student, String course, String file, long sizeKb, boolean bad) {
        if (bad) service.closeWindow();
        try {
            service.submit(student, course, file, sizeKb);
            System.out.println("[controller] accepted " + file);
        } catch (ExamWindowClosedException | NotRegisteredException e) {
            System.out.println("[controller] student sees: " + e.getMessage());
        } catch (ExamException e) {
            System.out.println("[controller] student sees: " + e.getMessage()
                    + " | root cause: " + rootCause(e));
        }
    }

    static String rootCause(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        return c.getClass().getSimpleName() + ": " + c.getMessage();
    }
}

public class PortalDemo {
    public static void main(String[] args) throws Exception {
        ExamController controller = new ExamController();
        System.out.println("--- Scenario 1: normal submission");
        controller.handle("S1", "CS101", "answer01.pdf", 120, false);
        System.out.println("--- Scenario 2: student not registered");
        controller.handle("S9", "CS101", "answer02.pdf", 120, false);
        System.out.println("--- Scenario 3: file too large (storage layer)");
        controller.handle("S1", "CS101", "answer03.pdf", 800, false);

        System.out.println("--- Scenario 4: window closed");
        controller.handle("S1", "CS101", "answer04.pdf", 120, true);

        System.out.println("\n--- Full chained stack trace for the storage failure");
        try {
            new ExamService().submit("S1", "CS101", "answer05.pdf", 800);
        } catch (ExamException e) {
            e.printStackTrace(System.out);
        }

        System.out.println("\n--- Same failure rethrown WITHOUT its cause");
        try {
            try {
                new ExamService().submit("S1", "CS101", "answer06.pdf", 800);
            } catch (SubmissionStorageException e) {
                throw new ExamException(e.getMessage());   // cause lost
            }
        } catch (ExamException e) {
            e.printStackTrace(System.out);
            System.out.println("getCause() = " + e.getCause());
        }
    }
}
