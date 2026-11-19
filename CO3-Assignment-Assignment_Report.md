# CO3 Java Assignment 3 – Exceptions, Resources, Threads and Concurrency

**Student A – Name:** [Enter name]   **Register Number:** [Enter number]
**Student B – Name:** [Enter name]   **Register Number:** [Enter number]
**GitHub Link (Student A):** [Paste link]
**GitHub Link (Student B):** [Paste link]
**Java Version:** JDK 11 or later (runs used OpenJDK 21.0.12, Linux, 2 vCPU sandbox)

> **Before submission:** This report was prepared from one run of the programs in this repository. Thread interleavings and timings change between runs, and some results are rare (for example, 1 in 10 for the `if` version of the counter). Each student must run every program in their own repository, replace the numbers with their own results, take their own screenshots, and be able to explain every section below in the viva. Sections labelled **Student A** or **Student B** must be written and verified by that student.

---

## Individual Contribution Record

| Question | Student A – Primary Responsibility | Student B – Primary Responsibility | Joint |
|---|---|---|---|
| Q1 | Throwable hierarchy, failure table, catch-order justification, finally guarantee | Five+ test inputs, catch/finally record, stack trace, broad-catch demonstration | Implementation; "single catch (Exception e)" question |
| Q2 | Exception hierarchy diagram, checked/unchecked reasons, propagation path, final handling | Scenarios per exception, chained trace with getCause(), lost-cause demonstration, test table | Complete the code; joint conclusion |
| Q3 | Debugging with breakpoints, leak location, failing input, file counts, logging | Resource list, leak explanation, try-with-resources closing order, suppressed exceptions | Engineering decision on try-with-resources |
| Q4 | Thread vs Runnable, life-cycle diagram, run() vs start(), join() | Ten runs, priority experiments, daemon thread, scheduler explanation | Joint conclusion on output order |
| Q5 | Race design, repeated runs, interleaving, critical section | Timing of 10,000 bookings, method vs block lock, static synchronization, intrinsic lock | Deployment decision for 20 screens |
| Q6 | Producer–consumer design, wait in loop, counter bounds, sleep vs wait | Deadlock construction, four conditions, lock-ordering prevention | Joint conclusion |
| Q7 | Design (Stage 2): ExecutorService, Callable, Future, concurrent collection, class diagram | Implementation and measurement (Stage 3): fault injection, pool sizes, hand-checked counts | Stage 1 (interpretation), Stage 4 (defence) |

---

## Q1 – Handling Failure in a Bank ATM Simulation (6 marks)

**Folder:** `Task01/`

### Joint: failure points and classification
| Failure point | Exception | Type | Caught in |
|---|---|---|---|
| Card-reader file missing | `NoSuchFileException` (from `Files.newBufferedReader`) | Checked (IOException subclass) | `attempt()` – most specific IOException catch |
| Corrupt balance line in card file | `NumberFormatException` (from `Long.parseLong` in `load`) | Unchecked | multi-catch `NumberFormatException \| InvalidAmountException` |
| Letters in account or amount | `NumberFormatException` | Unchecked | same multi-catch |
| Zero or negative amount | `InvalidAmountException` (extends `IllegalArgumentException`) | Unchecked | same multi-catch |
| Unknown account | `AccountNotFoundException` | Checked (custom) | multi-catch `AccountNotFoundException \| InsufficientBalanceException` |
| Amount above balance | `InsufficientBalanceException` | Checked (custom) | same multi-catch |
| Card reader not closed | none (finally always runs) | – | `finally` in `withdraw` |

### Student A – Exception design
**Throwable hierarchy (relevant part):**
```text
Throwable
├── Error                      (not used: e.g., OutOfMemoryError means the JVM is in trouble; the ATM must not catch it)
└── Exception
    ├── IOException            (checked)
    │   └── NoSuchFileException   (checked; caught before IOException)
    ├── InsufficientBalanceException   (checked, custom)
    ├── AccountNotFoundException       (checked, custom)
    └── RuntimeException      (unchecked)
        └── IllegalArgumentException
            ├── NumberFormatException
            └── InvalidAmountException   (unchecked, custom)
```

**Why these checked/unchecked choices.** Insufficient balance and unknown account are expected business outcomes, so the caller is forced to handle them (checked). Negative or zero amounts and non-numeric text are input rule violations that the caller should prevent, so they are unchecked and do not clutter every signature.

**Why the catch blocks are in this order.** Java requires that a subclass is caught before its superclass, otherwise the second catch can never run and the compiler reports an error. `NoSuchFileException` is a subclass of `IOException`, so it must come first; `IOException` comes after it as the general case. The other catches (the custom exceptions and `NumberFormatException`) are not subclasses of `IOException`, so their position does not matter for correctness. Student B's section below shows the compiler error for the wrong order.

**What finally guarantees.** The `finally` block in `withdraw` runs after the try block completes normally, after a `return`, and after any exception, including the ones that are not caught in that method. The card reader is closed in every case. The test output shows `finally: card reader closed` for every test where the reader was opened, and `finally: no card reader was opened` for the missing-file test (where there is nothing to close).

### Student B – Testing and tracing
**Test inputs and observed paths** (from `results/atm_run.txt`):

| Test | Input (account / amount / card) | Path taken | Finally ran |
|---|---|---|---|
| T1 valid | 1001 / 200 / cards.txt | OK, balance 300 | yes (reader closed) |
| T2 letters in amount | 1001 / abc / cards.txt | `NumberFormatException` (multi-catch) | yes |
| T3 negative amount | 1001 / -50 / cards.txt | `InvalidAmountException` (multi-catch) | yes |
| T4 zero amount | 1001 / 0 / cards.txt | `InvalidAmountException` | yes |
| T5 more than balance | 1001 / 900 / cards.txt | `InsufficientBalanceException` (checked catch) | yes |
| T6 unknown account | 9999 / 10 / cards.txt | `AccountNotFoundException` | yes |
| T7 letters in account | 12x / 10 / cards.txt | `NumberFormatException` | yes |
| T8 missing card file | 1001 / 10 / missing file | `NoSuchFileException` (most specific IOException catch) | yes (no reader was open) |
| T9 corrupt card file | 1002 / 10 / corrupt file | `NumberFormatException` raised while loading the file | yes |

**Uncaught exception stack trace** (`ATM uncaught`, from `results/atm_uncaught_stacktrace.txt`):
```text
finally: card reader closed
Exception in thread "main" java.lang.NumberFormatException: For input string: "x"
	at java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:67)
	at java.base/java.lang.Long.parseLong(Long.java:709)
	at java.base/java.lang.Long.parseLong(Long.java:832)
	at ATM.withdraw(ATM.java:48)
	at ATM.main(ATM.java:91)
```
Line by line: the exception is created by `forInputString` (the JDK method that builds the message); `Long.parseLong` at line 709 detects the bad character `x`; the `String` overload at line 832 calls it; `ATM.withdraw` line 48 is the parse of the amount text, the program's own line that caused the failure; `ATM.main` line 91 is the call that had no handler. The `finally` message is printed before the trace, because the finally block runs while the exception propagates up the stack.

**Broad catch before narrow catch** (`ATMBroadCatchError.java`, from `results/broad_catch_compile_error.txt`):
```text
ATMBroadCatchError.java:8: error: exception NumberFormatException has already been caught
        } catch (NumberFormatException e) {
          ^
```
A broad `catch (Exception e)` placed first catches everything, so the narrower catch below it can never be reached. The compiler rejects this rather than letting the code silently go unused.

### Joint – the single `catch (Exception e)` question
A single catch is acceptable only at the outermost boundary of the program (for example, `main` or a request handler), where the goal is to log the failure, show a safe message and stop or continue, and where the caller cannot do anything more useful. Even then it should log the full exception and rethrow or report the cause. What is lost by catching everything: the compiler can no longer check that each failure is considered; a programming bug (such as a `NullPointerException`) is treated the same as an expected business case (such as insufficient balance); the message to the customer can become misleading; and the stack trace may be hidden. The ATM handles each case separately so that the customer message, the log and the recovery differ.

### Alternative Concept and Complexity Analysis – no exceptions (error codes and `hasNextInt()`)
**Solution:** check each input before use, `if (!scanner.hasNextLong()) { return ERR_NOT_NUMBER; }`, and return sentinel values such as `-1` for each failure. The caller must test every return value.

**Readability and number of checks.** Each operation needs its own check and branch, and every caller must repeat them. For k failure kinds and n calls, the code contains about k·n explicit tests, and a forgotten test silently passes a `-1` as a balance. Exceptions move the handling to one place per layer, but they hide the path between throw and catch.

**Cost measured** (`ExceptionCostBench.java`, 200,000 calls, all invalid, 3 runs):

| Variant | Depth | ns per call | Bytes allocated per call |
|---|---|---|---|
| error code (-1) | 1 | 5.8 | 0 |
| exception with stack trace | 1 | 726.8 | 728 |
| exception, stack trace off | 1 | 22.6 | 40 |
| error code (-1) | 20 | 26.9 | 0 |
| exception with stack trace | 20 | 1,704.2 | 728 |
| exception, stack trace off | 20 | 832.5 | 40 |

**Analysis.** Capturing a stack trace walks the call stack, so its time grows with the number of frames d; the bytes allocated were the same (728) at both depths, so the extra time at depth 20 is walking and unwinding work, not more memory. Throwing also unwinds frames, so the cost grows with depth even without a stack trace (832 ns at depth 20 against 23 ns at depth 1). An error code costs one comparison and a return. In this benchmark an exception with a stack trace was about 60–130 times more expensive per failure than an error code. For a rare failure (a wrong PIN), that cost is irrelevant; for a hot loop that fails often, error codes or a pre-check are better. Exceptions are preferred here because they cannot be silently ignored by the caller.

---

## Q2 – User-Defined Exceptions and Propagation in an Online Examination Portal (7 marks)

**Folder:** `Task02/`

### Student A – Exception hierarchy and propagation
**Class diagram (text form):**
```text
Exception (checked)
└── ExamException                         (base: every portal failure)
    ├── ExamWindowClosedException         (checked)  – window closed
    ├── NotRegisteredException            (checked)  – student not on the course
    └── SubmissionStorageException        (checked)  – storage failed; wraps the IOException
```
**Why each exception is checked.** Each one is a condition the service and controller must consider: a closed window and an unregistered student must lead to a message for the student, and a storage failure may need to be retried or reported to staff. Checked exceptions make the compiler force each caller to decide. The base class `ExamException` is checked so that a generic `catch (ExamException e)` in the controller covers every portal rule. Using unchecked exceptions here would hide these decisions.

**Propagation path of one exception** (`SubmissionStorageException`, scenario 3 in `PortalDemo`):
1. `Storage.save` (storage layer) catches `IOException("Disk quota exceeded")` and throws `SubmissionStorageException` with the IOException as its cause.
2. `ExamService.submit` (service layer) declares `throws ExamException`, catches the storage exception only to rethrow it unchanged (the cause is preserved), and does not handle it.
3. `ExamController.handle` (controller layer) catches `ExamException`, which is the first place where a decision is made: it shows the student a message and logs the root cause.

**Where it is finally handled and what the student sees:** in the controller. The student sees `Could not store answer03.pdf`. The root cause (`IOException: Disk quota exceeded`) is recorded in the controller's log line, not shown to the student.

### Student B – Failure scenarios and chaining evidence
**Test table** (from `results/portal_output.txt`):

| Scenario | Input | Exception | Caught in | Message shown to student |
|---|---|---|---|---|
| 1 normal submission | S1, CS101, 120 KB | none | – | accepted |
| 2 not registered | S9, CS101 | `NotRegisteredException` | controller (multi-catch) | S9 is not registered for CS101 |
| 3 file too large | S1, CS101, 800 KB | `SubmissionStorageException` (cause: IOException) | controller (catch ExamException) | Could not store answer03.pdf, root cause IOException: Disk quota exceeded |
| 4 window closed | S1, CS101, window closed | `ExamWindowClosedException` | controller (multi-catch) | Exam window closed for CS101 |

**Complete the Code** (`StorageDemo.java`): TODO 1 is `throw new SubmissionStorageException("Could not store " + fileName, e);`, which keeps the IOException as the cause. The output is:
```text
Saved answer01.pdf
Error: Could not store answer02.pdf
Cause: Disk quota exceeded
```
(The expected output in the brief is printed on one line; the program prints one line per statement.)

**Chained stack trace** (scenario 5 in `PortalDemo`, from `results/portal_output.txt`):
```text
SubmissionStorageException: Could not store answer05.pdf
	at Storage.save(StorageDemo.java:21)
	at ExamService.submit(PortalDemo.java:28)
	at PortalDemo.main(PortalDemo.java:75)
Caused by: java.io.IOException: Disk quota exceeded
	at Storage.save(StorageDemo.java:17)
	... 2 more
```
The first block is the wrapper's trace (where the portal's exception was thrown). `Caused by` shows the original IOException with its own trace, which points to the line where the quota check happened (line 17). `... 2 more` means the lower frames are the same as the outer trace and are not repeated.

**What the Cause line would print if the IOException were not passed to the constructor.** `StorageDemoNoCause` shows that `e.getCause()` returns `null`, so the line `Cause: null` is printed; then `e.getCause().getMessage()` throws a `NullPointerException`, and the program crashes with that exception and stops printing. The original reason (the quota) is lost.

**Rethrow without cause** (scenario 6 in `PortalDemo`): the trace has no `Caused by` section and `getCause() = null`. The stack trace shows only the place where the new exception was created, not where the disk quota failed.

### Joint – why throw a `SubmissionStorageException` instead of letting `IOException` travel
- The upper layers should not depend on the details of the storage technology. A controller that catches `IOException` would have to know the storage uses files.
- The message is about the business operation ("Could not store answer02.pdf"), which can be shown to a student, while the IOException is a technical detail that belongs in the log.
- Because the type is a subclass of `ExamException`, one catch in the controller covers every storage failure, and the rule set stays in one hierarchy.
- The cause is preserved, so the technical detail is still available for staff and debugging.

### Alternative Concept and Complexity Analysis – unchecked exceptions
If the custom exceptions extended `RuntimeException`:
- **Method signatures** would drop `throws ExamException`. `submit` would compile without a declaration, and the callers would compile without any `try`.
- **Calling code** would no longer be forced to handle failures. A forgotten catch would let the exception reach the top of the program, which is the main risk. The controller would still catch the base class, but the compiler would not remind any other caller.
- **Cost.** Throwing costs the same for checked and unchecked exceptions; the difference is compile-time checking, not run time. Each exception still captures a stack trace (Q1's measurement: about 700 ns and 728 bytes at depth 1, growing with depth). Chaining holds one extra reference to the cause, so a chain of k exceptions keeps k exception objects and k stack-trace arrays alive until the outermost exception is dropped. The memory cost is small for a few levels, but a long chain of re-wrapping in a loop keeps all of them.

---

## Q3 – Resource Safety, Debugging and Robust Code (7 marks)

**Folder:** `Task03/`

### Joint – the three versions
All three read five store files and write one summary. On clean input they produce identical reports (`results/fd_experiment_output.txt`: "ReportManual identical to version 1" and "ReportTwr identical to version 1"). On `data_fail` (store3 has a bad amount on line 7), each stops with status 1 and logs the failing file.

### Student A – Debugging and experimental analysis
**Where the leak happens.** In `ReportManualNoFinally` the reader is closed only after successful processing (`reader.close()` on line 29). When `readStore` throws on store3 (line 7 is bad), control jumps to the `catch`, which returns 1 on line 32. The reader for store3 is never closed. Inside the debugger this is directly visible (see the jdb session below).

**Breakpoint debugging** (`results/jdb_session_output.txt`, compiled with `javac -g`): a breakpoint at line 32 (`return 1;`) was hit after the failure was logged. `locals` showed `f = instance of UnixPath` and `reader = instance of BufferedReader`, and `print f` gave `data_fail/store3.csv`. The reader is still referenced at the return, so it is still open.

**Failing input and file counts** (`ReportX` prints the open file descriptors before and after; counts from `results/fd_experiment_output.txt`):

| Version | Input | Status | Open FDs before | after | Leaked |
|---|---|---|---|---|---|
| 1 – manual, close only on success | data_fail | 1 | 4 | 5 | **1** |
| 2 – manual, close in finally | data_fail | 1 | 4 | 4 | 0 |
| 3 – try-with-resources | data_fail | 1 | 4 | 4 | 0 |
| 1, 2, 3 | data_ok | 0 | 4 | 4 | 0 |

**Logging** records which file caused the failure. The SEVERE message is:
```text
SEVERE: FAILED on store3.csv: bad amount at data_fail/store3.csv line 7: "12O0"
```
This is printed by `java.util.logging` through `ReportCommon.LOG` in each version.

### Student B – Design and correctness
**Resources in the program.** (1) Each store file's `BufferedReader` (one per file, opened in the loop). (2) The directory stream from `Files.list`, which must be closed too (it is in a try-with-resources in all versions). (3) The output `BufferedWriter` in `writeReport`, which is opened in try-with-resources. Each must be closed on every path.

**Why the version 1 manual code leaks.** A `close()` call that is not in `finally` (or in a construct that always runs) is skipped as soon as an exception leaves the try block. The leak grows by one file per failure: the failing reader stays open until the JVM exits. Running many reports in one JVM would exhaust the file-descriptor limit, which produces the "Too many open files" error in the brief.

**How try-with-resources guarantees closing.** The resource is declared in the parentheses. The compiler generates a `finally` that calls `close()` on the resource whenever the try block ends, normally or with an exception. Resources close in the reverse order of declaration. The output in `results/suppressed_order_output.txt` shows `open A`, `open B`, body, `close B`, `close A`.

**Suppressed exceptions.** If the body throws and `close()` also throws, Java keeps the body's exception as the primary one and attaches the close exception to it as a suppressed exception. The output shows `primary: body failed` and `suppressed: close failed for C`. Without this rule, the close failure would hide the real error.

### Joint – when try-with-resources is not sufficient
1. **The resource is not `AutoCloseable`** (for example, a native handle or a socket managed by a library), so it still needs a manual finally.
2. **The business logic must continue after a failure.** Try-with-resources only closes; it does not decide whether to skip a bad file and carry on. The report here stops at the first bad file, so a corrupt store means no report at all. A robust generator needs a per-file catch that logs, records the failure and continues.
3. **Partial output.** If the writer is closed after a failure, the file may contain an incomplete summary. The program must write to a temporary file and rename it only on success, otherwise a reader may see a partial report.
4. **Resources that are not files** (database connections, locks) need the same discipline plus transaction rollback, which try-with-resources does not provide.

### Alternative Concept and Complexity Analysis – explicit close in finally, or no closing
For a report over f store files, each with n_i lines, processing takes O(Σ n_i) time in every version, because each line is read once. The closing approach does not change the time; it changes the space (number of open handles).
- **No closing:** each file's descriptor stays open until the JVM exits. Under normal success, the handles accumulate to f; a failure in the middle of the run keeps all of the handles open that were not yet closed. The maximum number of open handles grows to f, and the process can run out of descriptors (the "Too many open files" failure).
- **Explicit close in finally (version 2):** at most 1 store reader is open at a time, plus the directory stream and writer. Maximum open handles is constant (3 here), regardless of f.
- **Try-with-resources (version 3):** the same bound as version 2, with less code and no chance of forgetting the close.

---

## Q4 – Thread Creation and the Thread Life Cycle (7 marks)

**Folder:** `Task04/`

### Student A – Thread design
**Two ways to create a thread.** Extending `Thread` (in `ScanStations.Station`) makes the class a thread, which cannot extend any other class, and it couples the work to the thread API. Implementing `Runnable` (in `StationTask`) separates the task from the thread object: the class can extend another class, the same task can run in different threads or an executor, and it is easier to test. Prefer `Runnable` (or a lambda) in most code; extend `Thread` only when a custom thread behaviour is needed.

**Life-cycle state diagram** (states observed in `results/thread_demos_output.txt`):
```text
        new Thread()                 start()                  run() ends
NEW  ───────────────►  RUNNABLE  ◄──────────────────────►  TERMINATED
                          │  ▲
             sleep()/wait()│  │ wakes after sleep / notify / join
                          ▼  │
                   TIMED_WAITING / WAITING / BLOCKED (on a monitor)
```
Observed: `after new Thread: NEW alive=false`, `while sleeping: TIMED_WAITING alive=true`, `after join: TERMINATED alive=false`. The station threads pass through NEW, RUNNABLE (ready to run and running), TIMED_WAITING (during `Thread.sleep` between batches), and TERMINATED.

**Why `run()` does not create a new thread.** `r.run()` is an ordinary method call: it executes on the calling thread. The evidence in `results/thread_demos_output.txt`: `body runs in: main` for `r.run()` and `body runs in: worker` for `r.start()`. Only `start()` asks the JVM to create a new thread and then calls `run()` on it.

**How `join()` ensures the total is printed last.** `join()` makes the calling thread (main) wait until the target thread is TERMINATED. In `ScanStations.main`, the loop `for (Thread t : stations) t.join();` completes only after all four stations finish, so `TOTAL scanned = 200` is printed after every station's own message. Without `join()`, `main` would print the total while stations were still running.

### Student B – Behavioural experiments
**Ten runs, finishing order** (`results/finish_order_10_runs.txt`):

| Run | Order of finishing |
|---|---|
| 1 | Station-3, Station-4, Station-2, Station-1 |
| 2 | Station-3, Station-4, Station-2, Station-1 |
| 3 | Station-4, Station-1, Station-2, Station-3 |
| 4 | Station-2, Station-3, Station-1, Station-4 |
| 5 | Station-2, Station-1, Station-4, Station-3 |
| 6 | Station-4, Station-1, Station-2, Station-3 |
| 7 | Station-3, Station-4, Station-1, Station-2 |
| 8 | Station-3, Station-1, Station-4, Station-2 |
| 9 | Station-4, Station-1, Station-2, Station-3 |
| 10 | Station-4, Station-2, Station-3, Station-1 |

The order changes every run, because each batch uses a random sleep (5–24 ms), and the scheduler decides which thread wakes first. Each station always prints `scanned 50`, and the total is always 200.

**Priority experiment** (6 runs each, S1 set to MAX_PRIORITY and others to MIN_PRIORITY, compared with default priorities; four CPU-bound threads):
- Default: first to finish S2, S4, S4, S1, S2, S1 (varied).
- With priorities: S1 first in all 6 runs.
This is an observation from one machine. Java thread priorities are only hints to the OS scheduler, and on many systems (including Linux without special privileges) they have little or no effect. With 6 runs, I cannot claim the effect is reliable; a larger experiment on the target machine is needed before relying on priorities.

**Daemon thread** (`ThreadDemos` part 4): the daemon ticker printed `daemon tick 4` while the non-daemon worker was still running; when the worker finished, `main` ended. The JVM did not wait for the daemon, which was still alive (`isAlive=true`) and was stopped with the JVM. A non-daemon thread would have kept the program running.

**Explanation using the scheduler.** The scheduler decides when each runnable thread gets a CPU. Sleeping stations are TIMED_WAITING and do not compete for the CPU. When the sleep ends, a station becomes RUNNABLE, and the order in which stations are scheduled depends on timing that the program does not control. A correct program must therefore not depend on this order, as the Joint section explains.

### Joint – why the output order differs, and why the program must not depend on it
Thread scheduling is decided by the operating system and the JVM, and it depends on load, timing, CPU count and background activity. The Java memory model does not promise any ordering between threads unless synchronization (join, locks, volatile, executors with futures) is used. A correct program therefore computes results that do not depend on the order, and it uses join or futures to decide when results may be read. A test that compares printed order is a test of the scheduler, not of the program.

### Alternative Concept and Complexity Analysis – ExecutorService with a fixed pool
**Solution:** `ExecutorService pool = Executors.newFixedThreadPool(4); for each station: pool.submit(task);` then `pool.shutdown(); pool.awaitTermination(...)`. The tasks run on 4 reused threads and the total comes from futures.

**Time.** For n scripts and p threads, with time t per script, the expected completion time is about ⌈n/p⌉·t, assuming no contention. Four station threads each do the work of one station (5 batches), so the time is the same as the pool version with p = 4 for four stations. A pool helps when there are many more tasks than threads, because the tasks reuse threads instead of paying the creation cost each time.

**Memory per thread.** Each platform thread reserves a stack (1 MB by default on Linux x64; the measured value is `ThreadStackSize = 1024 KB`, `results/thread_stack_size.txt`). Four threads reserve about 4 MB, and a pool of p threads reserves about p MB, independent of the number of tasks. Creating a new thread per task (10,000 tasks) would reserve about 10 GB of virtual memory at once if all were alive; the pool avoids that by queueing tasks. Virtual threads (Java 21) use much less memory and are an alternative when tasks mostly wait.

---

## Q5 – Race Conditions in a Cinema Seat-Booking System (6 marks)

**Folder:** `Task05/`

### Student A – Producing and explaining the race
**The critical section.** The check `if (!booked[seat])` and the write `booked[seat] = true; bookingsMade++;` together form the critical section. Another thread can run between the check and the write; then two threads both see the seat as free and both sell it.

**Interleaving of two threads for seat 3:**
```text
Time  Thread 1                          Thread 2
 t1   reads booked[3] → false
 t2                                     reads booked[3] → false
 t3   booked[3] = true; sold++  (sold=1)
 t4                                     booked[3] = true; sold++  (sold=2)  ← double sale
```
Both threads passed the check before either write was visible.

**Why the plain program did not reproduce the race on this machine.** `SeatBookingUnsync` (the unsynchronized version of the given program) sold exactly 10 seats in all 10 runs (`results/seatbooking_unsync_10_runs.txt`). The reason: `Thread.start()` launches the 100 users one after another, and each booking is so fast that a thread finishes before the next one starts, so the check and the write never interleave.

**Reliable reproducer** (`RaceDemo.java`): a `CountDownLatch` start gate makes all users wait and then start together. A `Thread.yield()` between the check and the write widens the window.

| Mode | Trials | Trials with double booking | Max seats sold in one trial |
|---|---|---|---|
| gate only | 200 | 0 | 10 |
| gate + yield between check and write | 200 | **46** | 16 |

So the race is real and is produced by the check-then-act window; the gate alone is not enough to show it on this machine.

**Critical section and fix.** The check and the write must happen as one atomic step, so the fix is to put them in one synchronized method (`SeatBooking.book`, TODO 1): only one thread can hold the object's monitor at a time, so no other thread can pass the check during the write.

### Student B – Comparing synchronization strategies
**Measured** (`results/strategies_output.txt`, 10,000 bookings, 100 seats, 4 threads, about 2 µs of preparation work per booking, mean of 5 runs):

| Strategy | Time (ms) | Seats sold | Correct? |
|---|---|---|---|
| synchronized method (work inside the lock) | 27.0 | 100 | yes |
| synchronized block (only check-and-book in the lock) | 16.6 | 100 | yes |
| no lock (baseline, incorrect in principle) | 16.0 | 100 | not guaranteed |

Locking the whole method made the preparation work run one thread at a time, so the method version took about 60% longer. Locking only the critical section lets the work run in parallel and keeps the seat check safe. The no-lock run showed no double sale in this experiment, because the race window is very small when no pause is inserted; the RaceDemo result above shows the same code is unsafe under a wider window. Correctness must not depend on a lucky run.

**Intrinsic lock.** Each `synchronized` method or block uses the intrinsic lock (monitor) of an object: `this` for the instance method, and the object named in `synchronized (x)` for a block. Only one thread at a time may hold a given monitor; other threads block until it is released.

**Static synchronization.** A `static synchronized` method locks the `Class` object, not an instance. It is needed when the shared state is static (one counter for all cinemas, for example) or when many instances must not run a critical section at the same time. Here each screen has its own `SeatBooking` object, so the instance lock is enough: screens do not share seats and should not block each other.

### Joint – which strategy for 20 screens
Use one **instance** lock per screen (`synchronized` on the screen's booking object, or a lock per seat if the screen is very busy), with only the check-and-book inside the lock. Reasons: each screen's seats are independent, so 20 separate locks allow 20 screens to run in parallel, while one static lock would serialize all of them; the critical section is small, so the lock is held for a very short time; the lock is per screen, so the code stays simple.

Measured alternatives for a single screen (`results/alternatives_output.txt`, ns per booking attempt, 4 threads):

| Strategy | 100 seats | 10,000 seats |
|---|---|---|
| synchronized method | 398.3 | 148.6 |
| AtomicBoolean[] compareAndSet | 254.5 | 168.9 |
| ReentrantLock | 532.1 | 330.2 |
| ConcurrentHashMap putIfAbsent | 475.3 | 1,035.8 |

The synchronized method is simple and fast at both sizes here; the CAS array is fastest at 100 seats under contention. For the cinema I recommend the synchronized method because it is the simplest correct code, and the difference is far below the time a customer takes to choose a seat.

### Alternative Concept and Complexity Analysis
- **`AtomicBoolean[]` with `compareAndSet(false, true)`:** each booking is one atomic hardware operation, O(1) per booking. Space is s objects of about 16–24 bytes each (object header plus a reference) instead of 1 byte per seat in `boolean[]`, so it uses roughly 20 times more memory for s seats.
- **`ReentrantLock`:** same O(1) per booking when uncontended; the lock object adds overhead and must be released in `finally`. Space: one lock object plus the boolean array. It offers tryLock and fairness options that the synchronized keyword does not.
- **`ConcurrentHashMap.putIfAbsent`:** O(1) average per booking, but it stores only booked seats, so space is O(booked seats) with about 40–60 bytes per entry. It is the best fit when the seat set is very large and sparse (10,000 seats with few bookings), but in the measurements it was the slowest at 10,000 seats.
- **Summary:** all four are O(1) per booking. The synchronized method is the simplest, the CAS array is fastest under low contention, and the concurrent map trades speed for sparse storage.

---

## Q6 – Deadlock and Inter-Thread Communication (7 marks)

**Folder:** `Task06/`

### Student B – Deadlock analysis and prevention (Part A)
**A transfer pair that reliably deadlocks** (`DeadlockDemo.java`): thread 1 transfers from account 1 to account 2 and thread 2 transfers from account 2 to account 1. Each takes its first lock, then sleeps 100 ms, so both hold one lock before either asks for the other.

Output (`results/deadlock_output.txt`):
```text
== Part A1: naive transfers (A->B and B->A at the same time)
  DEADLOCK: 2 threads are waiting for each other
  lock names: DeadlockDemo$Account@3b9a45b3 = account-1, DeadlockDemo$Account@7699a589 = account-2
  transfer-A-to-B is BLOCKED waiting for DeadlockDemo$Account@7699a589 held by transfer-B-to-A
  transfer-B-to-A is BLOCKED waiting for DeadlockDemo$Account@3b9a45b3 held by transfer-A-to-B
```
`ThreadMXBean.findDeadlockedThreads()` reports the cycle. (The hash values change every run; the mapping line shows which account each lock is.)

**The four conditions for deadlock**, all present here:
1. **Mutual exclusion:** a monitor can be held by only one thread.
2. **Hold and wait:** each thread holds one account lock while waiting for the other.
3. **No preemption:** a thread cannot be forced to release a lock it holds.
4. **Circular wait:** thread 1 waits for account 2 held by thread 2, which waits for account 1 held by thread 1.

Removing any one condition prevents deadlock. Lock ordering removes circular wait.

**Prevention with lock ordering** (`transferOrdered`): always lock the account with the lower id first. Both transfers then lock account 1 before account 2, so neither can hold account 2 while waiting for account 1. Results: `both finished; balances 950 + 1050 = 2000` (Part A2), and 20 threads with 20,000 random transfers finished in 9 ms with the total still 50,000 (Part A3).

**Why the rule works.** A global order on the locks means every thread acquires locks in the same sequence, so no cycle of waiting can form: a waiting thread always waits for a lock with a higher id than any it holds.

### Student A – Producer–consumer design (Part B)
**Complete the Code** (`TokenCounter.java`): TODO 1 is `while (count == buffer.length) wait();` then store at `in`, advance `in`, `count++`, `notifyAll()`. TODO 2 is `while (count == 0) wait();` then read from `out`, advance `out`, `count--`, `notifyAll()`, and return the token. Output: `Sum of tokens collected: 210 (expected 210)`.

**Why `wait()` must be in a loop.** A waiting thread can wake up for reasons other than the condition becoming true (spurious wakeups are allowed by the JVM specification), and with several waiting threads another thread may have changed the state between the notification and the wake-up. The loop re-checks the condition after every wake-up, so the thread proceeds only when the buffer really has room or a token.

**Why `wait()` must be inside a synchronized block.** `wait()` must be called by the thread that holds the monitor, otherwise Java throws `IllegalMonitorStateException`. Waiting releases the monitor, so other threads can enter, change the state and notify. The `synchronized` methods provide the monitor.

**`while` versus `if`** (`TokenCounterIf.java`, `StressIfVsWhile.java`):
- Simple run (2 students, 20 tokens): 10 of 10 runs printed 210, with exit code 0.
- Stress run (4 students, 50,000 tokens, 10 runs each):
  - `while` version: 0 of 10 runs wrong (sum always 50,000).
  - `if` version: **1 of 10 runs wrong** (sum 49,999, one token lost).

With `if`, a thread that wakes up does not re-check the condition. If another thread has already consumed the token or filled the buffer, the woken thread proceeds anyway. It takes from an empty buffer (or puts into a full one), which corrupts `count` and the indexes. The failure is rare because the bad interleaving needs two threads to wake for the same change, so the simple run was not enough to show it; the stress test was.

**Sleep versus wait** (`SleepVsWait.java`, `results/sleep_vs_wait_output.txt`):
- `Thread.sleep()` inside `synchronized` keeps the monitor: another thread waited **449 ms** to enter the block.
- `wait()` releases the monitor: the notifier entered the block at once (0 ms) while the waiter was waiting.

**Notify versus notifyAll** (`TokenCounterNotify.java`): with `notify()`, only one waiting thread is woken, and it may be a thread waiting for the opposite condition (a consumer when the producer should wake). That thread re-checks its loop condition, finds it false, and waits again, so the intended thread may stay asleep and the program can stall. In this design a producer and two consumers all wait on the same monitor, so `notify()` is unsafe. Three runs with a 10-second limit all finished with 210 (`results/notify_variant_output.txt`), so the risk did not show in these runs. `notifyAll()` is the safe choice here: every waiting thread wakes and re-checks its condition.

**Counter size over time** (`TokenCounterMonitor`, `results/token_counter_monitor_output.txt`): `counter size range seen: min=0 max=5`. The size never exceeded 5 and never dropped below 0, because each change is inside a synchronized method and checked by the loop.

### Joint – what deadlock and producer–consumer have in common
Both are failures of waiting. In deadlock, each thread waits for a lock that will never be released. In producer–consumer, a thread waits for a condition that will never become true unless someone notifies it. Both can be prevented by a rule that threads must follow: lock ordering (a global order) for deadlock, and a condition loop plus notification for the counter. Progress is guaranteed when every waiting thread has a path to be released: with lock ordering a holder can always finish, and with the loop and `notifyAll` a waiting thread is woken whenever its condition may have changed.

### Alternative Concept and Complexity Analysis
- **`ArrayBlockingQueue<Integer>(5)`:** `put` and `take` are O(1) under the queue's internal lock and condition variables, and the queue does the waiting for us. Measured (`results/alt_concurrency_output.txt`, 200,000 items, 1 producer and 2 consumers): wait/notifyAll **5,737 ns per item**; ArrayBlockingQueue **8,573 ns per item**. Space: a fixed array of 5 references plus the lock. The queue is less error-prone because the loop, the notify and the bounds are written once and tested by the JDK.
- **`tryLock` with timeout and retry instead of lock ordering:** a transfer tries both locks with a timeout and releases the first lock if the second is not available, then retries. This avoids deadlock without an ordering rule, but it can livelock and wastes work. Measured (100,000 transfers, 4 threads): lock ordering **245 ns per transfer**; tryLock with retry **1,724 ns per transfer** with about 317 retries per run. Space: one lock per account in both cases.
- **Which is less error-prone:** ArrayBlockingQueue for the producer–consumer (no loops, no notify to get wrong) and lock ordering for transfers (simple rule, no retry logic). tryLock is useful when the lock order cannot be known in advance.

---

## Q7 – Design a Concurrent, Fault-Tolerant Processor from First Principles (10 marks)

**Folder:** `Task07/`

### Stage 1 – Interpret the problem (Joint)
**Parallel tasks and shared data.** Each score file can be read and validated independently, so each file is a task. The shared data is the results collection (one result per file) and the counters for the summary. Reading files is I/O-bound (waiting on storage); validating and averaging is CPU-bound but tiny.

**Failures and how each must be handled:**

| Failure | Detected by | Handling |
|---|---|---|
| File missing | `Files.exists` returns false | status MISSING, run continues |
| Non-numeric or wrong number of scores (including bad encoding) | parse, line count; `IOException` (for example `MalformedInputException`) | status CORRUPT with detail |
| Score outside 0–100 | range check | status OUT_OF_RANGE |
| Unreadable (I/O error) | `IOException` | CORRUPT with exception name |
| Unexpected runtime error in a task | `RuntimeException` in the task | UNEXPECTED, run continues |
| Task never finishes (blocked read) | `Future.get` timeout | TIMEOUT, run continues, worker cancelled (see the limitation below) |
| Worker thread crashes | `ExecutionException` from `Future.get` | UNEXPECTED |

**Measurable requirements:**
1. Every file in the manifest produces exactly one outcome (completeness = 10,000 of 10,000).
2. Counts must equal the expected summary (OK 9,750, CORRUPT 100, MISSING 100, OUT_OF_RANGE 50 for the generated data).
3. One failing file must not stop or delay the others beyond its own timeout.
4. Total time for 10,000 files should fall as the pool grows while the work is I/O-bound (measured below).

### Stage 2 – Design (Student A)
**Class diagram:**
```text
ResultProcessor (main)
 ├── ExecutorService (fixed pool, size p)
 │     └── submits ─► ScoreReader implements Callable<Outcome>
 │                      call(): read → validate → Outcome   (never throws)
 ├── List<Future<Outcome>>          one future per file
 ├── ConcurrentHashMap<String, Outcome>   results keyed by file name
 └── Outcome { file, Status, average, detail }
       Status: OK | MISSING | CORRUPT | OUT_OF_RANGE | TIMEOUT | UNEXPECTED
```

**Pseudocode:**
```text
pool = newFixedThreadPool(p)
for each name in manifest:
    futures.add(pool.submit(new ScoreReader(name, dir/name, latency)))
for i in 0..n-1:
    try:   outcome = futures[i].get(timeout)
    catch TimeoutException:  cancel(futures[i]); outcome = TIMEOUT
    catch ExecutionException: outcome = UNEXPECTED
    results.putIfAbsent(name_i, outcome)          // exactly once per file
pool.shutdownNow(); awaitTermination(2 s)
summary = count statuses over results.values()
print summary, completeness = (results.size() == manifest.size())
```

**Concurrent collection and why.** `ConcurrentHashMap` keyed by file name: each file has exactly one entry, so duplicates are visible (`putIfAbsent` returns the old value), and concurrent writes are safe without a global lock. A plain `HashMap` would be unsafe if workers wrote to it directly; a `ConcurrentLinkedQueue` would not detect duplicates or give a key for each file. In this design the main thread is the only writer, so the map's guarantees are a safety margin for later changes where workers write directly.

**Exception handling so one failure does not stop the run.** Each `ScoreReader.call()` catches every failure it can produce and returns an Outcome, so the future completes normally. Exceptions that escape anyway are caught by `Future.get` as `ExecutionException`. The main loop never stops early.

### Stage 3 – Implement and measure (Student B)
**Data.** `GenerateScores` creates 10,000 files with deterministic faults: every 100th file missing (100), every 50th (not 100th) corrupt (100), files with `i % 200 == 7` out of range (50), and 9,750 valid. `expected_summary.txt` states the expected counts.

**Hand-checked small set** (`data_small/`, `results/hand_check_small_output.txt`). Expected results worked out by hand: OK 5 (s01 averages 80.0, s02 30.0, s04 50.0, s08 20.0, s09 100.0, sum 280.0); CORRUPT 3 (s03 `n/a`, s07 only 3 lines, s10 `8x`); MISSING 1 (s06); OUT_OF_RANGE 1 (s05, value 101). Program output: `counts {OK=5, MISSING=1, CORRUPT=3, OUT_OF_RANGE=1} total=10 sumOfOkAverages=280.0`, and the failure lines match the hand expectations exactly.

**Full run, pool of 4:** `complete=true`, counts `{OK=9750, MISSING=100, CORRUPT=100, OUT_OF_RANGE=50}`, total 10,000, matching the expected summary.

**Pool-size experiment** (`results/pool_experiment.csv`, 3 runs each, mean of runs):

| Simulated latency per file | Pool | Mean elapsed (ms) | Speed-up vs pool 1 |
|---|---|---|---|
| 0 ms (CPU/disk-cache only) | 1 | 425 | 1.00 |
| 0 ms | 2 | 403 | 1.06 |
| 0 ms | 4 | 395 | 1.08 |
| 0 ms | 8 | 435 | 0.98 |
| 1 ms (slow storage) | 1 | 11,835 | 1.00 |
| 1 ms | 2 | 5,888 | 2.01 |
| 1 ms | 4 | 2,974 | 3.98 |
| 1 ms | 8 | 1,528 | 7.75 |

**Observations.** With no latency, the work is CPU-bound and very short, so extra threads do not help: the machine has 2 CPUs and the work fits in them, and extra threads only add switching. With 1 ms latency per file, the threads spend most of their time waiting, so the speed-up is nearly linear; a speed-up of 7.75 with 8 threads is possible on 2 CPUs because waiting threads use no CPU. Real storage limits this: a disk or network share may not serve 8 concurrent reads well.

**Failures not handled correctly (found by testing).** The blocked-file test (`Task07/make_fifo_test.sh`): one of the four files is a named pipe (FIFO) with no writer, so reading it blocks forever. Output (`results/fifo_hang_output.txt`):
```text
pool=2 latency=0ms elapsed=3023 ms terminated=false complete=true
counts {OK=3, TIMEOUT=1} total=4 sumOfOkAverages=120.0
exit code=124 (124 = still running when killed: JVM never exited)
```
The design handled the timeout: the run reported TIMEOUT for the blocked file after 3 s and still completed the other three. But two things were not handled: (1) `Future.cancel(true)` and `shutdownNow()` could not interrupt the blocked file open, so `awaitTermination` returned `false` and the worker thread stayed alive in `open0` (confirmed by a thread dump); (2) the worker is a non-daemon thread, so the JVM never exited and the process had to be killed. Fix: read files with a method that honours interruption (for example `FileChannel` with a timeout, or a separate process), run the work in daemon threads, or call `System.exit` after the summary (acceptable for a batch job, but it hides the leak). Also check file types before reading (`Files.isRegularFile`).

### Joint – Stage 4: defence
**Recommended pool size.** For this workload, use a pool of about the number of CPUs (2–4) when the work is CPU-bound, and up to 8–16 when reads are slow and the storage supports concurrent reads. The measurements show no gain from more threads when there is no latency, and a large gain with latency. The right size depends on the bottleneck, so the pool size should be a configuration value, measured on the real storage.

**Why more threads did not always help.** A thread costs memory (its stack) and time to switch. When the task is short and CPU-bound, the CPU is already busy with the first few threads, so more threads add switching without more work done. When the task waits on I/O, threads overlap their waits, but only up to what the storage can serve.

**How the design guarantees a complete and correct summary.** (1) Every manifest name is submitted once and each future is collected once, so every file gets one outcome; the completeness flag checks `results.size() == manifest.size()`. (2) Each task returns a status, never an exception, so failures are counted, not lost. (3) Counts are computed from the map after all futures are collected, so no count is taken while a worker is still writing. (4) The hand-checked set and the expected summary of the generated data confirm the counts.

**One remaining limitation.** A file read that blocks forever (the FIFO case) is not stopped: the Java file APIs do not guarantee that a blocked open can be interrupted, so the pool thread is lost and the JVM cannot exit normally. The timeout protects the summary, not the thread.

### Alternative Concept and Complexity Analysis
**Parallel stream** (`AltApproaches stream`, `results/alt_stream_output.txt`): `names.parallelStream().map(read).filter(OK).count()` on a ForkJoinPool of size p (10,000 files):

| Parallelism | Latency 0 ms (ms) | Latency 1 ms (ms) |
|---|---|---|
| 1 | 293 | 11,587 |
| 2 | 267 | 5,858 |
| 4 | 287 | 2,936 |
| 8 | 358 | 1,692 |

The results match the ExecutorService (same counts, similar timings). Failure handling is the weak point: each task still returns an outcome, so one bad file does not stop the stream, but there is no per-file timeout. A blocked file blocks the whole stream, because the calling thread waits for every element. Time complexity is the same, O(n·t/p), and the stream uses the common ForkJoin pool shared with the rest of the JVM unless a custom pool is supplied.

**One thread per file** (`AltApproaches thread`, `results/alt_thread_per_file_output.txt`): 10,000 platform threads were created and finished without error, in about 2.3 s (both with 0 ms and 1 ms latency). The peak number of live threads was only 19 (0 ms) and 37 (1 ms), because each thread finishes quickly and thread creation is slow relative to the work. Memory: each platform thread reserves a stack (1 MB by default, `ThreadStackSize = 1024 KB`). If all 10,000 threads were alive together, the reservation would be about 10 GB of virtual memory and a large number of OS threads; the measurement avoided this only because the work finished quickly. Failure handling: each thread must catch its own exceptions, and a thread that blocks forever cannot be stopped at all. The pool gives a bounded number of threads (p), a queue for tasks, and futures with timeouts, which the thread-per-file design lacks.

**Summary:** the ExecutorService design is preferable: bounded memory (p stacks), timeouts per file, and a single place to count results. Parallel streams are simplest for pure computation, and thread-per-file suits only small numbers of tasks.

---

## Limitations of these experiments
- All timings are from a shared 2-CPU sandbox. Results that depend on timing (the 1-in-10 `if` failure, the priority experiment, the notify variant) need more runs on your own machine.
- Thread interleavings are not controlled; the race reproducer uses a start gate and a `yield` to make the race visible, which is a demonstration, not production code.
- Memory figures for stacks and object headers are the JVM defaults and typical object sizes; they were not all measured directly.

## Viva preparation (key points for both students)
- Q1: the Throwable hierarchy, why catch order matters, what finally guarantees, and why exceptions cost more than error codes.
- Q2: why the storage layer throws a domain exception and keeps the cause; what breaks when the cause is dropped.
- Q3: why the manual version leaked one file, how try-with-resources closes in reverse order, and when it is not enough.
- Q4: `run()` versus `start()`, life-cycle states, why output order varies, and why join is needed.
- Q5: the check-then-act race and the critical section; instance versus static locks; why 20 screens need separate locks.
- Q6: the four deadlock conditions and lock ordering; why `wait()` is in a loop; why `if` fails under stress and `notify()` is risky.
- Q7: the ExecutorService design, the concurrent collection, the pool-size results, and the blocked-file limitation.
