# Task 7 (Q7) – Concurrent, Fault-Tolerant Result Processor

- `GenerateScores.java` – creates 10,000 score files in `data/` and `manifest.txt`, with known faults (100 missing, 100 corrupt, 50 out of range, 9,750 valid). Writes `expected_summary.txt`.
- `ResultProcessor.java` – the design: fixed pool, Callable tasks that never throw, ConcurrentHashMap keyed by file name, Future.get with timeout. Usage: `java -cp . ResultProcessor <dataDir> <manifest> <pool> [latencyMs] [timeoutSec] [failuresFile]`.
- `data_small/` and `manifest_small.txt` – hand-checked set of 10 files. Expected: OK=5, CORRUPT=3, MISSING=1, OUT_OF_RANGE=1, sum of OK averages 280.0.
- `AltApproaches.java` – alternatives: parallel stream on a ForkJoinPool of a given size, and one platform thread per file.
- `make_fifo_test.sh` – builds a directory with one named pipe, which blocks forever (the failure case described in the report).
- `results/pool_experiment.csv` – pool sizes 1, 2, 4, 8 with 0 ms and 1 ms simulated storage latency (3 runs each).

Run: `javac -d . *.java`, `java -cp . GenerateScores`, then the commands in the root README.
