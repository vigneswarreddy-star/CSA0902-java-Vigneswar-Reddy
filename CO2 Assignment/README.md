# CO2 Java Assignment 2 – Collections, Strings and Engineering Trade-offs

**Student Name:** [Enter your name]
**Register Number:** [Enter your register number]
**Java Version:** JDK 11 or later (all programs were run on OpenJDK 21)
**Repository name:** `CSA09-<RegisterNumber>-Assignment2`

## Project contents
- `Assignment_Report.md` – answers and evidence for Q1–Q7 (predictions, measurements, analysis, alternatives).
- `Task01/` – Q1: String vs StringBuilder vs StringBuffer (`TicketBuilderBench.java`, `SharedLogDemo.java`).
- `Task02/` – Q2: ArrayList vs LinkedList vs ArrayDeque (`PlaylistListBench.java`).
- `Task03/` – Q3: hashCode–equals contract (`Attendance.java` completed Complete-the-Code program, `ContractTests.java`).
- `Task04/` – Q4: HashMap vs TreeMap vs LinkedHashMap, plus sorted array (`MedicineMapBench.java`).
- `Task05/` – Q5: ranking with Comparator chains (`LeaderboardDemo.java` completed program, `RankingTests.java`).
- `Task06/` – Q6: triage with PriorityQueue (`Triage.java` completed program, `TriageGrowth.java`).
- `Task07/` – Q7: diagnosis and redesign of the Task05 leaderboard (`RankingRedesign.java`).
- Each task folder has a `README.md` and a `results/` folder with the raw output and CSV files from the run used in the report.

## How to compile and run

Open a terminal in the project folder. Each task folder is self-contained.

### Task 1
```bash
cd Task01
javac -d . TicketBuilderBench.java SharedLogDemo.java
java -cp . TicketBuilderBench
java -cp . SharedLogDemo
```

### Task 2
```bash
cd Task02
javac -d . PlaylistListBench.java
java -cp . PlaylistListBench
```

### Task 3
```bash
cd Task03
javac -d . Attendance.java ContractTests.java
java -cp . Attendance
java -cp . ContractTests
```

### Task 4
```bash
cd Task04
javac -d . MedicineMapBench.java
java -cp . MedicineMapBench
```

### Task 5
```bash
cd Task05
javac -d . LeaderboardDemo.java RankingTests.java
java -cp . LeaderboardDemo
java -cp . RankingTests
```

### Task 6
```bash
cd Task06
javac -d . Triage.java TriageGrowth.java
java -cp . Triage
java -cp . TriageGrowth
```

### Task 7
```bash
cd Task07
javac -d . RankingRedesign.java
java -cp . RankingRedesign
```

Benchmarks write CSV files into `results/` relative to the folder you run them from. Run them from the task folder.

## Before submitting
1. Replace the student name and register number in this file and in `Assignment_Report.md`.
2. Re-run every program on your own computer and replace the numbers in the report with your own averages. The numbers here are from a shared 2-CPU sandbox.
3. Save your own screenshots of the program outputs in the corresponding task folders.
4. Create the GitHub repository with the required name, upload the project, and make at least one commit per task.
5. Add the GitHub link to the report cover page.
6. Be ready to explain every design decision and table in the viva.
