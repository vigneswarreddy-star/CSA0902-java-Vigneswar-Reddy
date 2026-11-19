# Task 1 (Q1) – String, StringBuilder or StringBuffer?

- `TicketBuilderBench.java` – builds tickets with `+`, `StringBuilder` and `StringBuffer` for three workloads (short, many, long). Prints mean time, standard deviation, bytes allocated, retained memory and estimated intermediate Strings, and writes `results/ticket_bench.csv`.
- `SharedLogDemo.java` – 4 threads append to one shared log using StringBuilder (unsafe) and StringBuffer (synchronised). Shows lost characters.

Run:
```bash
javac -d . TicketBuilderBench.java SharedLogDemo.java
java -cp . TicketBuilderBench
java -cp . SharedLogDemo
```

Results: `results/ticket_bench_output.txt`, `results/ticket_bench.csv`, `results/shared_log_output.txt`.
Expect different numbers on your machine, and different lost-character counts for StringBuilder on each run.
