# Task 5 (Q5) – Generic Ranking Component

- `LeaderboardDemo.java` – the Complete-the-Code program. TODO 1 (`ranking()` returns a sorted copy) and TODO 2 (points high-first, then goal difference high-first, then name A–Z) are completed. Expected output: `[Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]`.
- `RankingTests.java` – test data set A (ties at every level, hand-derived expected order), data set B (int overflow breaks a subtraction comparator), a comparator that breaks tie-breaking, and a comparator that never returns 0 (sort throws).

Run:
```bash
javac -d . LeaderboardDemo.java RankingTests.java
java -cp . LeaderboardDemo
java -cp . RankingTests
```

Results: `results/leaderboard_output.txt`, `results/ranking_tests_output.txt`.
