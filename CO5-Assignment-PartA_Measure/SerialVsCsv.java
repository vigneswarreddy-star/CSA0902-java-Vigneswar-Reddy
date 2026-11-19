import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** Part A4 alternative: file size and save/load time for n residents, serialization vs CSV. */
public class SerialVsCsv {
    public static void main(String[] args) throws Exception {
        Path dir = Paths.get("data");
        Files.createDirectories(dir);
        System.out.println("n\tformat\tsave_ms\tload_ms\tbytes");
        for (int n : new int[]{1_000, 10_000, 100_000}) {
            List<Resident> list = new ArrayList<>();
            for (int i = 1; i <= n; i++) list.add(new Resident("Student" + i, String.format("%09d", 192300000 + i), i % 300, "pw" + i));

            Path ser = dir.resolve("residents_" + n + ".ser");
            long t0 = System.nanoTime();
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(ser)))) {
                out.writeObject(list);
            }
            long saveMs = (System.nanoTime() - t0) / 1_000_000;
            t0 = System.nanoTime();
            List<Resident> back;
            try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(ser)))) {
                back = (List<Resident>) in.readObject();
            }
            long loadMs = (System.nanoTime() - t0) / 1_000_000;
            System.out.println(n + "\tserialization\t" + saveMs + "\t" + loadMs + "\t" + Files.size(ser) + "\t(loaded " + back.size() + ")");

            Path csv = dir.resolve("residents_" + n + ".csv");
            t0 = System.nanoTime();
            try (BufferedWriter w = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
                for (Resident r : list) { w.write(r.name + "," + r.regNo + "," + r.roomNo); w.newLine(); }
            }
            saveMs = (System.nanoTime() - t0) / 1_000_000;
            t0 = System.nanoTime();
            int count = 0;
            try (BufferedReader rd = Files.newBufferedReader(csv)) {
                while (rd.readLine() != null) count++;
            }
            loadMs = (System.nanoTime() - t0) / 1_000_000;
            System.out.println(n + "\tCSV\t" + saveMs + "\t" + loadMs + "\t" + Files.size(csv) + "\t(loaded " + count + ")");
        }
    }
}
