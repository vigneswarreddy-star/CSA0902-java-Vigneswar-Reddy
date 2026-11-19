# Task 6 (Q6) – Priority and Fairness in Triage

- `Triage.java` – the Complete-the-Code program. TODO 1 is completed with a PriorityQueue ordered by severity, then arrival. Also includes the 8-patient hand-checked case and a demonstration that printing the PriorityQueue directly does not show treatment order.
- `TriageGrowth.java` – admit and serve cost at 1,000 / 10,000 / 100,000 / 1,000,000 patients, and a starvation simulation comparing the strict severity rule with the deadline rule.

Run:
```bash
javac -d . Triage.java TriageGrowth.java
java -cp . Triage
java -cp . TriageGrowth
```

Results: `results/triage_output.txt`, `results/triage_growth_output.txt`.
