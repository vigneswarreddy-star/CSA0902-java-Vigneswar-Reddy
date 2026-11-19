# Task 2 (Q2) – User-Defined Exceptions and Propagation

- `StorageDemo.java` – Complete-the-Code program (TODO 1 completed: the IOException is wrapped and kept as the cause). Expected output: `Saved answer01.pdf`, `Error: Could not store answer02.pdf`, `Cause: Disk quota exceeded`.
- `StorageDemoNoCause.java` – variant without the cause (shows `Cause: null` and then a NullPointerException).
- `PortalDemo.java` – controller → service → storage layers, four scenarios, a chained stack trace and a trace where the cause was dropped.

Run: `javac -d . StorageDemo.java StorageDemoNoCause.java PortalDemo.java` then run any main class.
