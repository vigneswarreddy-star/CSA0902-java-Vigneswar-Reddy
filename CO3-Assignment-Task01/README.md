# Task 1 (Q1) – Handling Failure in an ATM

- `ATM.java` – withdrawal with try, catch (specific before general, multi-catch) and finally. Nine test inputs, each printing which catch block handled it and whether finally ran.
- `ATMBroadCatchError.java` – deliberately wrong catch order; the compile error is in `results/broad_catch_compile_error.txt`.
- `ExceptionCostBench.java` – cost of an error code versus an exception, with and without stack trace, at two call depths (used in the alternative analysis for Q1 and Q2).
- `cards.txt` / `cards_corrupt.txt` – card-reader input files.

Run: `javac -d . ATM.java ExceptionCostBench.java` then `java -cp . ATM` (or `ATM uncaught` for the stack trace).
`ATMBroadCatchError.java` is meant to fail compilation; compile it separately to see the error.
