import java.util.ArrayList;
import java.util.List;

/** Generates pass rows as String[] {passId, regNo, name, route, amount}. */
public class Passes {
    public static List<String[]> generate(int n) {
        List<String[]> out = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            out.add(new String[]{
                String.format("P%06d", i),
                String.format("%09d", 192300000 + i),
                "Student" + i,
                String.valueOf((i % 40) + 1),
                String.valueOf(3000 + (i % 2000)) + ".0"});
        }
        return out;
    }
}
