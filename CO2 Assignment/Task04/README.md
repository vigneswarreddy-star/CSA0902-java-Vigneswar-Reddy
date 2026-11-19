# Task 4 (Q4) – Choosing a Map for a Pharmacy Inventory

- `MedicineMapBench.java` – compares HashMap, TreeMap and LinkedHashMap at 1,000 / 10,000 / 100,000 medicines: build time, 100,000 lookups, code-order report, insertion-order report, and a sorted array with `Arrays.binarySearch`. Writes `results/map_bench.csv`.

Run:
```bash
javac -d . MedicineMapBench.java
java -cp . MedicineMapBench
```

Results: `results/map_bench_output.txt`, `results/map_bench.csv`.
