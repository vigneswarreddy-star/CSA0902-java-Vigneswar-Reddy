import java.io.IOException;

/** Variant for the Complete-the-Code question: the IOException is NOT passed as the cause. */
public class StorageDemoNoCause {
    static class Store {
        void save(String fileName, long sizeKb) throws SubmissionStorageException {
            try {
                if (sizeKb > 500) throw new IOException("Disk quota exceeded");
                System.out.println("Saved " + fileName);
            } catch (IOException e) {
                throw new SubmissionStorageException("Could not store " + fileName, null);
            }
        }
    }
    public static void main(String[] args) {
        Store storage = new Store();
        try {
            storage.save("answer01.pdf", 120);
            storage.save("answer02.pdf", 800);
        } catch (ExamException e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Cause: " + e.getCause());               // prints null
            System.out.println("Cause message: " + e.getCause().getMessage()); // NullPointerException
        }
    }
}
