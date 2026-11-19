import java.io.*;
public class ReadAny {
    public static void main(String[] a) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("resident_v1.ser"))) {
            Resident r = (Resident) in.readObject();
            System.out.println("read OK: name=" + r.name + " regNo=" + r.regNo + " phone=" + r.phone + " password=" + r.password);
        } catch (InvalidClassException e) {
            System.out.println("FAILED: " + e.getMessage());
        }
    }
}
