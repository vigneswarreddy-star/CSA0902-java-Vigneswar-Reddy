import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates 10,000 score files (5 subject scores each) plus a manifest of every expected file name.
 * Deliberate faults (deterministic, so the expected summary is known):
 *   i % 100 == 0           -> file NOT created (missing)                 : 100 files
 *   i % 50 == 0 (not %100)  -> corrupt: a non-numeric score and a letter  : 100 files
 *   i % 200 == 7           -> out of range score 150                      : 50 files
 *   everything else         -> valid                                      : 9,750 files
 */
public class GenerateScores {
    public static void main(String[] args) throws IOException {
        Path dir = Path.of("data");
        Files.createDirectories(dir);
        List<String> manifest = new ArrayList<>();
        int valid = 0, corrupt = 0, missing = 0, range = 0;
        for (int i = 1; i <= 10_000; i++) {
            String name = String.format("score_%05d.txt", i);
            manifest.add(name);
            if (i % 100 == 0) { missing++; continue; }
            List<String> lines = new ArrayList<>();
            if (i % 50 == 0) {
                lines = List.of("67", "78", "n/a", "80", "90"); corrupt++;
            } else if (i % 200 == 7) {
                lines = List.of("67", "78", "150", "80", "90"); range++;
            } else {
                for (int k = 0; k < 5; k++) lines.add(String.valueOf((i * 7 + k * 13) % 101));
                valid++;
            }
            Files.write(dir.resolve(name), lines);
        }
        Files.write(Path.of("manifest.txt"), manifest);
        Files.write(Path.of("expected_summary.txt"), List.of(
                "total=10000 OK=" + valid + " CORRUPT=" + corrupt + " MISSING=" + missing + " OUT_OF_RANGE=" + range));
        System.out.println("expected: OK=" + valid + " CORRUPT=" + corrupt + " MISSING=" + missing + " OUT_OF_RANGE=" + range);
    }
}
