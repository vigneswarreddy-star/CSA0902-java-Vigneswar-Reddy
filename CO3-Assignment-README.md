# CO3 Java Assignment 3 – Exceptions, Resources, Threads and Concurrency

**Mode:** Pair work – 2 students per group
**Student A – Name:** [Enter name]   **Register Number:** [Enter number]
**Student B – Name:** [Enter name]   **Register Number:** [Enter number]
**Java Version:** JDK 11 or later (all runs in this project used OpenJDK 21.0.12)
**Repository name:** `CSA09-<RegisterNumber>-Assignment3`

Both students must run every program in their own repository (the brief requires individual execution). The division of responsibility in the report is shown in the Individual Contribution Record.

## Project contents
- `Assignment_Report.md` – joint report. Sections are labelled Student A, Student B and Joint.
- `Task01/` – Q1: ATM withdrawal (exceptions, catch order, finally), uncaught stack trace, compile error for a broad catch first, cost of exceptions versus error codes.
- `Task02/` – Q2: exam portal exception hierarchy, propagation, chaining (`StorageDemo.java` is the completed Complete-the-Code program).
- `Task03/` – Q3: report generator in three versions (manual, manual with finally, try-with-resources), open-file-descriptor measurements, jdb session, suppressed exceptions.
- `Task04/` – Q4: thread stations as Thread subclasses and Runnable, life-cycle states, run() vs start(), priorities, daemon threads.
- `Task05/` – Q5: cinema seat booking (`SeatBooking.java` completed), unsynchronized version, race reproducer, locking strategies, alternatives.
- `Task06/` – Q6: deadlock detection and lock ordering, producer-consumer counter (`TokenCounter.java` completed), while/if and notify variants, alternatives.
- `Task07/` – Q7: fault-tolerant result processor with ExecutorService, Callable and Future; generated test data; pool-size experiment; blocking-file test.

## How to compile and run
Each task folder is self-contained. Compile with `javac -d . *.java` and run from that folder.

### Task 1
```bash
cd Task01 && javac -d . ATM.java ExceptionCostBench.java
java -cp . ATM                 # nine test inputs
java -cp . ATM uncaught        # unhandled exception -> stack trace
java -cp . ExceptionCostBench  # error code vs exception cost
javac -d . ATMBroadCatchError.java        # expected to FAIL: broad catch before narrow
```

### Task 2
```bash
cd Task02 && javac -d . *.java
java -cp . StorageDemo         # completed Complete-the-Code program
java -cp . StorageDemoNoCause  # variant without the cause
java -cp . PortalDemo          # layered portal, scenarios and chained traces
```

### Task 3
```bash
cd Task03 && javac -d . *.java
./make_fail_scripts.sh         # generates data_ok/ and data_fail/, runs all three versions
java -cp . SuppressedAndOrderDemo
```

### Task 4
```bash
cd Task04 && javac -d . *.java
java -cp . ScanStations            # Thread subclasses
java -cp . ScanStations runnable   # Runnable implementations
java -cp . ThreadDemos             # run vs start, life cycle, priorities, daemon
```

### Task 5
```bash
cd Task05 && javac -d . *.java
java -cp . SeatBooking        # completed synchronized version: expected "Seats sold: 10"
java -cp . SeatBookingUnsync  # unsynchronized version (run 10 times and record)
java -cp . RaceDemo           # reproduces double booking with a start gate
java -cp . BookingStrategies  # synchronized method vs block vs no lock
java -cp . AltBookingBench    # AtomicBoolean CAS, ReentrantLock, ConcurrentHashMap
```

### Task 6
```bash
cd Task06 && javac -d . *.java
java -cp . DeadlockDemo          # deadlock detected, then lock ordering
java -cp . TokenCounter          # completed program: expected 210
java -cp . TokenCounterIf        # while changed to if
java -cp . StressIfVsWhile       # 50,000 tokens, 10 runs (java ... StressIfVsWhile if for the if version)
java -cp . TokenCounterNotify    # notify() instead of notifyAll(); use timeout to detect a hang
java -cp . TokenCounterMonitor   # counter size range
java -cp . SleepVsWait
java -cp . AltConcurrencyBench
```

### Task 7
```bash
cd Task07 && javac -d . *.java
java -cp . GenerateScores                              # creates data/ and manifest.txt
java -cp . ResultProcessor data_small manifest_small.txt 2 0 5 failures_small.txt   # hand-checked set
java -cp . ResultProcessor data manifest.txt 4 0 5     # full run, pool of 4
./make_fifo_test.sh && timeout 20 java -cp . ResultProcessor data_fifo manifest_fifo.txt 2 0 3   # blocking file
java -cp . AltApproaches stream data manifest.txt 4 0
java -cp . AltApproaches thread data manifest.txt 0 0
```

## Notes
- The `results/` folders contain the output from the run used in the report. Re-run on your own machine and replace the numbers with your own averages and screenshots.
- Thread timing and interleavings change between runs. Run each experiment multiple times as the brief requires.
- `Task07/data/` holds the 10,000 generated input files. Regenerate them with `GenerateScores` if needed.
