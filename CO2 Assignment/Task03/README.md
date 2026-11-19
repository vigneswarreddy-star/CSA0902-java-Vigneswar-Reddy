# Task 3 (Q3) – hashCode–equals contract

- `Attendance.java` – the Complete-the-Code program. TODO 1 is completed with `hashCode()` based on `regNo`. Expected output: `Unique students: 2`.
- `ContractTests.java` – three designed tests with predictions printed first: distinct students, duplicates (broken and fixed classes), and mutating a hash field after insertion.

Output before completing TODO 1 (`Attendance` without `hashCode`): `Unique students: 3`. Saved in `results/before_todo1_output.txt`.

Run:
```bash
javac -d . Attendance.java ContractTests.java
java -cp . Attendance
java -cp . ContractTests
```

Results: `results/before_todo1_output.txt`, `results/after_todo1_output.txt`, `results/contract_tests_output.txt`.
