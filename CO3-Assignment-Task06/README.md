# Task 6 (Q6) – Deadlock and Inter-Thread Communication

- `DeadlockDemo.java` – Part A: opposite transfers deadlock (detected with ThreadMXBean), lock-ordered version, 20-thread stress.
- `TokenCounter.java` – Complete-the-Code program: TODO 1 and TODO 2 completed with wait/notifyAll in while loops. Expected: `Sum of tokens collected: 210`.
- `TokenCounterIf.java` – while changed to if (both methods).
- `StressIfVsWhile.java` – 10 runs of 50,000 tokens; `java -cp . StressIfVsWhile if` for the if version.
- `TokenCounterNotify.java` – notify() instead of notifyAll().
- `TokenCounterMonitor.java` – records the minimum and maximum counter size under the lock.
- `SleepVsWait.java` – sleep keeps the monitor; wait releases it.
- `AltConcurrencyBench.java` – ArrayBlockingQueue vs wait/notifyAll; tryLock with timeout vs lock ordering.

Run: `javac -d . *.java` then run the main class you need.
