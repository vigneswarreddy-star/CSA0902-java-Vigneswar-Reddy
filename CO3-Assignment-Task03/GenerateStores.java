import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Creates data_ok/ (5 clean store files) and data_fail/ (same, but store3 has a corrupt amount on line 7). */
public class GenerateStores {
    public static void main(String[] args) throws IOException {
        for (String dirName : new String[]{"data_ok", "data_fail"}) {
            Path dir = Path.of(dirName);
            Files.createDirectories(dir);
            for (int s = 1; s <= 5; s++) {
                List<String> lines = new ArrayList<>();
                for (int i = 1; i <= 20; i++) {
                    int amount = (s * 100 + i * 7) % 1000;
                    if (dirName.equals("data_fail") && s == 3 && i == 7) {
                        lines.add("S" + s + "-" + i + ",12O0");          // letter O instead of zero
                    } else {
                        lines.add("S" + s + "-" + i + "," + amount);
                    }
                }
                Files.write(dir.resolve("store" + s + ".csv"), lines);
            }
        }
        System.out.println("Created data_ok/ and data_fail/");
    }
}
