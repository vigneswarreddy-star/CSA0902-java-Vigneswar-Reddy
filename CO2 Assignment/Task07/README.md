# Task 7 (Q7) – Diagnose, Redesign and Validate

Chosen solution: the Task05 `Leaderboard`, whose `ranking()` copies and fully re-sorts on every call.

- `RankingRedesign.java` – contains the original design (`OriginalLeaderboard`), the redesign (`SortedLeaderboard`, binary insertion with a read-only view), a counting comparator, and the correctness check (identical rankings with ties). Compares comparisons and time for a read-heavy and a live workload at 1,000 / 5,000 / 20,000 teams. Writes `results/redesign_bench.csv`.

Run:
```bash
javac -d . RankingRedesign.java
java -cp . RankingRedesign
```

Results: `results/redesign_output.txt`, `results/redesign_bench.csv`. This program takes about 20 seconds.
