import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Q5 - Test data sets: (A) many ties at each level, (B) data that breaks faulty comparators,
 * (C) a comparator that breaks the contract (never returns 0 for equal items).
 */
public class RankingTests {

    static final Comparator<Team> CORRECT = Comparator.comparingInt((Team t) -> t.points).reversed()
            .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
            .thenComparing(t -> t.name);

    static String rank(Comparator<Team> c, List<Team> teams) {
        Leaderboard<Team> b = new Leaderboard<>(c);
        for (Team t : teams) b.add(t);
        return b.ranking().toString();
    }

    public static void main(String[] args) {
        // (A) ties at every level. Hand-derived expected order:
        //   9 pts: GD 6 -> Eagles, Wolves (name A-Z); then GD 4 -> Bisons, Panthers
        //   6 pts: GD 3 -> Owls; GD 1 -> Cobras, Falcons; then 3 pts -> Tigers
        List<Team> tieData = new ArrayList<>(List.of(
                new Team("Panthers", 9, 4), new Team("Falcons", 6, 1), new Team("Wolves", 9, 6),
                new Team("Tigers", 3, 0), new Team("Owls", 6, 3), new Team("Bisons", 9, 4),
                new Team("Cobras", 6, 1), new Team("Eagles", 9, 6)));
        String expectedA = "[Eagles(9,6), Wolves(9,6), Bisons(9,4), Panthers(9,4), Owls(6,3), Cobras(6,1), Falcons(6,1), Tigers(3,0)]";
        String gotA = rank(CORRECT, tieData);
        System.out.println("(A) tie data, correct comparator");
        System.out.println("    expected: " + expectedA);
        System.out.println("    observed: " + gotA);
        System.out.println("    result:   " + (expectedA.equals(gotA) ? "PASS" : "FAIL"));

        // (B) data that breaks a subtraction comparator through int overflow.
        // Points 2,000,000,000 - (-2,000,000,000) = 4,000,000,000 which wraps to a negative int.
        List<Team> overflowData = new ArrayList<>(List.of(
                new Team("Alpha", 2_000_000_000, 0), new Team("Beta", -2_000_000_000, 0),
                new Team("Gamma", 0, 0)));
        Comparator<Team> faultySubtract = (a, b) -> b.points - a.points;   // descending, via subtraction
        System.out.println("\n(B) overflow data (Alpha 2e9, Gamma 0, Beta -2e9)");
        System.out.println("    correct:  " + rank(CORRECT, overflowData));
        System.out.println("    faulty subtraction comparator: " + rank(faultySubtract, overflowData));
        System.out.println("    faulty ignores tie-breakers:  " + rank(
                Comparator.comparingInt((Team t) -> t.points).reversed(), tieData));

        // (C) comparator that never returns 0: equal items are reported as "a < b" both ways.
        Comparator<Team> noZero = (a, b) -> a.points > b.points ? -1 : 1;
        List<Team> big = new ArrayList<>();
        for (int i = 0; i < 200; i++) big.add(new Team("T" + (i % 17), i % 3, 0));
        System.out.println("\n(C) 200 teams with many equal points, comparator never returns 0");
        try {
            List<Team> sorted = new ArrayList<>(big);
            sorted.sort(noZero);
            System.out.println("    sort completed (no contract check triggered); first 3: " + sorted.subList(0, 3));
        } catch (IllegalArgumentException e) {
            System.out.println("    sort threw IllegalArgumentException: " + e.getMessage());
        }
    }
}
