# Task 3 (Q3) – Resource Safety and Debugging

- `GenerateStores.java` – creates `data_ok/` (clean) and `data_fail/` (store3 has a corrupt amount on line 7).
- `ReportManualNoFinally.java` – version 1: close() only on the success path. Leaks the reader on failure.
- `ReportManual.java` – version 2: manual close in finally, with close errors caught.
- `ReportTwr.java` – version 3: try-with-resources.
- `ReportCommon.java` – shared parsing and output, so all versions write the same report.
- `make_fail_scripts.sh` – runs all three versions on both inputs and compares the reports.
- `SuppressedAndOrderDemo.java` – closing order and a suppressed exception.
- `results/jdb_session_output.txt` – scripted jdb session: breakpoint at the failure return shows the reader still open. Compiled with `javac -g`.

Run: `javac -g -d . *.java` then `./make_fail_scripts.sh`.
