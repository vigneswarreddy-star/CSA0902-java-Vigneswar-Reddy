import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Creates a legacy pass file with valid rows and deliberately damaged rows for the B4 test. */
public class MakeMigrationFile {
    public static void main(String[] args) throws Exception {
        Path p = Paths.get(args.length > 0 ? args[0] : "data/passes_migrate.txt");
        Files.createDirectories(p.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            for (int i = 1; i <= 20_000; i++) {
                if (i == 3_000) w.write("P003000,192300001,Damaged,7\n");                    // 4 fields
                else if (i == 6_000) w.write("P006000,192300002,Name,7,abc\n");             // amount not a number
                else if (i == 9_000) w.write("P009000,1923,Short Reg,7,3000.0\n");          // register number too short
                else if (i == 12_000) w.write("P012000,192300003,,7,3000.0\n");             // empty name
                else if (i == 15_000) w.write("\n");                                         // empty line
                else if (i == 18_000) w.write("P018000,192300004,Route,150,3000.0\n");     // route out of range
                w.write(String.format("P%06d,%09d,Student%d,%d,%d.0\n",
                        i, 192300000 + i, i, (i % 40) + 1, 3000 + (i % 2000)));
            }
        }
        System.out.println("Wrote " + Files.lines(p).count() + " lines to " + p);
    }
}
