import java.io.*;
public class WriteV1 {
    public static void main(String[] a) throws Exception {
        try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream("resident_v1.ser"))) {
            o.writeObject(new Resident("Arun", "192311001", "secret-pw"));
        }
        System.out.println("v1 written");
    }
}
