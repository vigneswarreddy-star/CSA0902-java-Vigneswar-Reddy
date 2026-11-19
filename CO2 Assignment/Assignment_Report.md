# CO2 Java Assignment 2 – Collections, Strings and Engineering Trade-offs

**Student Name:** [Enter your name]
**Register Number:** [Enter your register number]
**GitHub Link:** [Paste your repository link]
**Java Version:** JDK 11 or later (all runs in this report used OpenJDK 21.0.12 on Linux, 2 vCPU)

> **Important before submission:** The numbers below come from one run of the programs in this project on a shared 2-CPU sandbox. Timings change from run to run (the notes in each section show how much). Re-run every program on your own machine, replace the numbers with your own averages, and make sure you can explain every table during the viva. Screenshots must be taken on your own computer.

---

## Q1 – Building Text Under Load: String, StringBuilder or StringBuffer? (6 marks)

**Folder:** `Task01/`

### Prediction (written before running)
- **String `+` in a loop:** each `ticket += field` creates a *new* String and copies everything built so far. Building a ticket of *n* fields copies about L·n(n+1)/2 characters (L = average field length), which is **O(n²)** characters. Each iteration also creates intermediate String objects, roughly 2 per field (`field + "|"` and the running ticket).
- **StringBuilder / StringBuffer:** appends write into a growing char array. Amortised cost is **O(n)** characters, and only the final `toString()` creates a String.
- For a short ticket (10 fields) the difference should be negligible. For a long ticket (1,000 fields) it should be large, roughly proportional to the ratio of copy volumes.
- The threaded booking log will lose data with `StringBuilder` and will not with `StringBuffer`.

### Workloads
| Workload | Description | Why |
|---|---|---|
| W1 short | 1 ticket of 10 fields, built 100,000 times | one short ticket; repeated so the time is measurable |
| W2 many | 10,000 tickets × 50 fields | the sample workload from the brief |
| W3 own | 50 tickets × 1,000 fields | a large group-booking manifest |

### Measurements (`Task01/TicketBuilderBench.java`, 2 warm-up + 5 timed runs, final run)
| Workload | Approach | Mean ms | Std dev | Bytes allocated (MB) | Retained MB | Est. intermediate Strings |
|---|---|---|---|---|---|---|
| W1 | String (+) | 63.11 | 64.29 | 99.95 | 15.65 | 2,000,000 |
| W1 | StringBuilder | 28.27 | 7.24 | 49.59 | 15.64 | 100,000 |
| W1 | StringBuffer | 31.98 | 11.51 | 50.35 | 15.64 | 100,000 |
| W2 | String (+) | 35.50 | 1.44 | 207.10 | 7.86 | 1,000,000 |
| W2 | StringBuilder | 13.79 | 0.37 | 30.92 | 7.85 | 10,000 |
| W2 | StringBuffer | 16.12 | 3.28 | 31.00 | 7.85 | 10,000 |
| W3 | String (+) | 77.86 | 28.48 | 347.90 | 0.70 | 100,000 |
| W3 | StringBuilder | 0.95 | 0.30 | 2.46 | 0.70 | 50 |
| W3 | StringBuffer | 0.85 | 0.03 | 2.46 | 0.70 | 50 |

*Measurement notes:* "Bytes allocated" uses the thread allocation counter (`ThreadMXBean`), so it is precise. "Retained" is the heap held by the output after a GC. Intermediate String counts are estimates from the code (2 per field for `+`, 1 final String per ticket for the builders). The W1 timing varied from about 42 ms to 63 ms across my two runs, so treat small differences as noise.

### Observed versus predicted
- **Long tickets show the quadratic behaviour.** W3 copies 50 × 1000² / 2 fields' worth of text, and W2 copies 10,000 × 50² / 2. The predicted time ratio is (50 × 1000²) / (10,000 × 50²) = 2.0. Observed: 77.86 / 35.50 ≈ 2.2. The `+` approach grows with the square of the field count, while the builders stay flat.
- **Short tickets:** the time difference is small and within noise, which matches the prediction. The allocation difference is still real: 99.95 MB against 49.59 MB for the same output.
- **Memory:** the *retained* memory is the same for all three approaches (7.9 MB for W2, 0.7 MB for W3), because the final strings are identical. The difference is *transient garbage*: W2 allocates 207 MB with `+` but 31 MB with a builder, and W3 allocates 348 MB against 2.5 MB. Garbage costs GC time and memory bandwidth, which is why the gap appears under load.

### Design decision – the multithreaded booking server
`Task01/SharedLogDemo.java`: 4 threads each append 100,000 times to one shared log. Final run:

| Run | StringBuilder: lost chars | StringBuilder: time | StringBuffer: lost chars | StringBuffer: time |
|---|---|---|---|---|
| 1 | 112,778 (plus an ArrayIndexOutOfBoundsException in a thread) | 26 ms | 0 | 71 ms |
| 2 | 19,687 | 6 ms | 0 | 8 ms |
| 3 | 41,621 | 5 ms | 0 | 32 ms |

`StringBuilder.append` is not synchronised. Two threads can write into the same internal array at once, so characters are lost or the array index goes out of bounds. The fast times are wrong results, not a speed advantage.

**Recommendation**
- **Per-ticket text:** `StringBuilder`. A ticket is built inside one thread, so no locking is needed, and it is the fastest option (13.8 ms for W2).
- **Shared log:** do not share a `StringBuilder`. `StringBuffer` is correct, because every method is synchronised, but it takes a lock on every append. Better: each booking thread puts a finished ticket line into a thread-safe queue (for example `ConcurrentLinkedQueue` or a `BlockingQueue`), and one writer thread appends the lines to the log file. This gives one lock-free hand-off per ticket instead of one lock per append.

### Alternative Concept and Complexity Analysis
- **Repeated `+`:** the k-th field copies the first k fields, so the total is Σ k·L = L·n(n+1)/2 = **O(n²·L)** characters, with about 2n temporary Strings.
- **StringBuilder / StringBuffer:** each append costs O(length of the field). Capacity doubles when full, so the total copying is at most about twice the final length: **O(n·L)** overall, with 1 final String.
- **`String.join(sep, fields)`:** computes the total length first and copies each field once: **O(n·L)**, one result String. Simple and fast when all fields are already available as an array.
- **`StringJoiner`:** a builder that handles separators and prefixes: **O(n·L)** and one final String. Good when the ticket format is fixed.
- **`String.format(...)`:** parses the format string on every call and creates a Formatter and intermediate Strings for each specifier. It is correct and readable for fixed-layout tickets, but has a large constant factor.
- **Space:** `+` makes about 2n temporary Strings of growing size (garbage). The builder and `String.join` keep only the output plus one buffer.

---

## Q2 – When Is ArrayList an Appropriate Engineering Choice? (4 marks)

**Folder:** `Task02/`

### Prediction (before running)
| Operation | ArrayList | LinkedList | ArrayDeque |
|---|---|---|---|
| Add at end | O(1) amortised | O(1) | O(1) amortised |
| Insert / remove at front | O(n) (shifts all elements) | O(1) | O(1) |
| Insert / remove in middle | O(n) (shift) + O(1) | O(n) to find the position, O(1) to link | not supported |
| Get k-th | O(1) | O(n) (walk from the nearer end) | not supported |
| Iterate all | O(n), cache-friendly | O(n), pointer chasing | O(n) |

### Experiment design
`Task02/PlaylistListBench.java`: playlist sizes 1,000, 10,000 and 100,000. Each operation runs K = 1,000 times on a fresh list (2 warm-up + 5 timed runs). Results are mean nanoseconds per operation. "Iterate" is per element.

### Measurements (final run)
| Size | Operation (ns/op) | ArrayList | LinkedList | ArrayDeque |
|---|---|---|---|---|
| 1,000 | add at end | 506.9 | 84.0 | 73.5 |
| 1,000 | insert at front | 494.4 | 235.5 | 69.6 |
| 1,000 | insert in middle | 126.7 | 1,211.9 | n/a |
| 1,000 | remove at front | 74.0 | 173.0 | 107.2 |
| 1,000 | remove in middle | 99.2 | 550.8 | n/a |
| 1,000 | get k-th | 138.6 | 517.6 | n/a |
| 1,000 | iterate (per element) | 94.6 | 137.6 | 111.8 |
| 1,000 | queue: add end + remove front | 173.9 | 132.3 | 118.0 |
| 10,000 | add at end | 78.2 | 100.6 | 98.8 |
| 10,000 | insert at front | 695.6 | 102.6 | 80.5 |
| 10,000 | insert in middle | 341.6 | 9,605.2 | n/a |
| 10,000 | remove at front | 475.9 | 47.5 | 44.9 |
| 10,000 | remove in middle | 292.2 | 8,940.0 | n/a |
| 10,000 | get k-th | 69.5 | 4,637.6 | n/a |
| 10,000 | iterate (per element) | 69.4 | 72.8 | 66.9 |
| 10,000 | queue: add end + remove front | 416.4 | 127.9 | 102.0 |
| 100,000 | add at end | 68.6 | 62.8 | 63.1 |
| 100,000 | insert at front | 8,967.8 | 69.4 | 64.4 |
| 100,000 | insert in middle | 4,581.8 | 100,490.7 | n/a |
| 100,000 | remove at front | 10,663.2 | 46.9 | 39.1 |
| 100,000 | remove in middle | 4,630.9 | 116,102.3 | n/a |
| 100,000 | get k-th | 105.4 | 50,963.9 | n/a |
| 100,000 | iterate (per element) | 5.6 | 8.9 | 6.8 |
| 100,000 | queue: add end + remove front | 8,989.4 | 97.3 | 97.3 |

*Measurement note:* in an earlier run, `ArrayDeque.addLast` at 100,000 measured about 500 ns. The cause was my setup: the deque was created with exactly *n* capacity, so the first extra add triggered a resize inside the timed part. I gave it headroom and re-ran; the figures above are the corrected ones. Lesson for the viva: check that setup work is outside the timed region. The 1,000-element column is also dominated by JIT warm-up (for example ArrayList add at end reads 507 ns at 1,000 but 69 ns at 100,000), so treat small sizes as indicative.

### Analysis
- **Growth matches the complexity:** ArrayList front removal rises from about 0.5 µs (10,000) to about 10.7 µs (100,000), because each removal shifts the whole array. LinkedList `get k-th` rises from 4.6 µs to 51 µs for 10× more elements, which is linear. ArrayList `get` stays flat at about 70–140 ns.
- **Usage pattern where ArrayList is reasonable:** playlists that are mostly built by appending, read by index, and iterated (the common case). Editing at the front or in the middle is fine for small lists (about 10,000 songs or fewer, where it costs under 1 µs per operation).
- **When I would stop recommending it:** frequent front or middle edits on large playlists. At 100,000 songs, one front insert costs about 9 µs, roughly 100 times the cost of LinkedList or ArrayDeque. My operating boundary from this evidence is around 10⁴ elements for frequent front edits.

### Changed Condition – the "Up Next" queue
Songs are added at both ends and removed from the front. ArrayList is now the wrong choice: the queue churn case costs 8,989 ns per add-and-remove at 100,000, against 97 ns for ArrayDeque (about 90 times faster). LinkedList is as fast at the ends (97 ns), but each song needs a node object (about 24 bytes extra per song, with compressed pointers), and its nodes are scattered in memory. **Recommendation:** use `ArrayDeque` for the Up Next queue and keep `ArrayList` for the full playlist, where index access and iteration matter.

### Alternative Concept and Complexity Analysis
- **Space per element (estimated, with compressed pointers; not measured):** ArrayList stores one reference per slot (about 4 bytes) plus up to 50% spare capacity. ArrayDeque stores one reference per slot, with no per-element objects. LinkedList adds a node object (about 24 bytes: header, item, next, previous) for each song.
- **Plain array with manual shifting:** the same asymptotics as ArrayList (O(n) for front or middle edits, O(1) for get), but the programmer must manage size and growth. ArrayList is that array plus bookkeeping, so there is no real advantage in using a raw array here.
- **Summary table:** see the prediction table above, with measured values in the results.

---

## Q3 – Engineer the hashCode–equals Contract (6 marks)

**Folder:** `Task03/`

### Test 1 – distinct students (works correctly)
- **Predicted size: 3.** No duplicates exist, so membership checks never need to match.
- **Observed: 3.**

### Test 2 – deliberate duplicates
- **A** is scanned 3 times and **B** twice (5 scans, 2 students).
- **Broken class** (`equals` without `hashCode`): **predicted 5, observed 5.** The first draft of my prediction said 2, which was wrong. Without `hashCode()`, each object uses its identity hash, so the objects usually land in different buckets. `HashSet` only calls `equals()` on objects in the same bucket with the same stored hash, so it never compares them.
- **Fixed class:** **predicted 2, observed 2.**

### Test 3 – own test: changing a field used by hashCode after insertion
- **Predicted:** the set still has 1 element, but `contains` fails because the object is stored in the bucket for its old hash.
- **Observed:** size 1; `contains(same object)` = false; `contains(new object with the new regNo)` = false; iteration still shows the object with its new regNo.
- **Conclusion:** `HashSet` finds an object by its hash bucket. If that hash changes, the object is effectively lost to lookups, even though it is still in the set. This is why key fields must be `final` (or immutable), as in the corrected `Student`.

### Complete the Code
- Before completing TODO 1, the program prints `Unique students: 3`. Stored as `results/before_todo1_output.txt`.
- After adding `hashCode() { return regNo.hashCode(); }`, it prints `Unique students: 2`, matching the expected output. Stored as `results/after_todo1_output.txt`.
- **Difference:** without `hashCode`, the two scans of student 192311001 are in different buckets, so `equals` never sees them as the same. The hash must come from the same field as `equals`.

### Explanation of the contract
- `HashSet` computes `hashCode()` to choose a bucket, then compares stored hashes, then calls `equals()` only within the bucket.
- If `a.equals(b)` is true, `a.hashCode()` must equal `b.hashCode()`. Otherwise the two equal objects can be stored separately (the Test 2 result).
- The reverse is not required: equal hashes can belong to unequal objects (a collision), and `equals` resolves that.

### Verification
`Task03/ContractTests.java` reruns the three tests with predictions printed first. `Attendance` checks the corrected class: 2 unique students.

### Alternative Concept and Complexity Analysis
- **`TreeSet<Student>` with a comparator on regNo:** each scan costs O(log n) comparisons (red-black tree search), and memory is O(n) for n unique students. No `hashCode` is needed; duplicates are defined by the comparator, so `compareTo` must agree with `equals`.
- **`HashMap<String, Student>` keyed by regNo:** each scan is O(1) on average (hash the key string, then look up), and memory is O(n) with a hash table overhead. Keys are `String`, which already has a correct `equals` and `hashCode`, so the contract cannot be broken by the student class.
- **Recommendation:** `HashMap<String, Student>` (or a `HashSet<String>` of regNos) when only uniqueness and lookup matter; `TreeSet` when a sorted list of students is also needed.

---

## Q4 – Choosing a Map for a Pharmacy Inventory (6 marks)

**Folder:** `Task04/`

### Prediction (before implementing)
| Map | put | get | Code-order report | Insertion-order report |
|---|---|---|---|---|
| HashMap | O(1) avg | O(1) avg | needs copy + sort: O(n log n) | not available (hash order) |
| TreeMap | O(log n) | O(log n) | iterate directly: O(n) | needs an extra list: O(n) memory |
| LinkedHashMap | O(1) avg | O(1) avg | wrong order: needs copy + sort | iterate directly: O(n) |

### Acceptability criterion
- **One lookup:** average under **1 µs** (1,000 ns), so that a request doing many lookups still responds well within 100 ms.
- **Evening report:** under **1 second** for 100,000 medicines.

### Measurements (`Task04/MedicineMapBench.java`, final run)
Each size uses 100,000 random lookups of existing codes; insertion order is random, so LinkedHashMap's order is not code order.

| Size | Structure | Build ms | 100k lookups ms | ns / lookup | Code-order report ms | Report sorted? | Insertion-order report ms | Matches insertion? |
|---|---|---|---|---|---|---|---|---|
| 1,000 | HashMap | 0.45 | 9.81 | 98.1 | 0.96 | yes | 0.20 | no |
| 1,000 | TreeMap | 1.03 | 28.83 | 288.3 | 0.20 | yes | 0.15 | yes (side list) |
| 1,000 | LinkedHashMap | 0.38 | 7.75 | 77.5 | 0.26 | **no** | 0.26 | yes |
| 10,000 | HashMap | 4.00 | 7.68 | 76.8 | 3.12 | yes | 1.64 | no |
| 10,000 | TreeMap | 4.46 | 28.57 | 285.7 | 0.75 | yes | 0.34 | yes (side list) |
| 10,000 | LinkedHashMap | 2.04 | 5.76 | 57.6 | 0.33 | **no** | 0.28 | yes |
| 100,000 | HashMap | 36.90 | 6.23 | 62.3 | 23.03 | yes | 4.98 | no |
| 100,000 | TreeMap | 79.90 | 71.47 | 714.7 | 14.84 | yes | 2.28 | yes (side list) |
| 100,000 | LinkedHashMap | 28.71 | 5.77 | 57.7 | 12.32 | **no** | 10.52 | yes |

Sorted-array alternative (`Arrays.binarySearch`, same file): 100k lookups take 301.6 ns each at 1,000 medicines, 312.7 ns at 10,000 and 526.4 ns at 100,000. Inserting 1,000 new codes into the sorted array costs 2.28 ms at 100,000 medicines (the "insertion-order" column for that row).

### Interpretation
- **Output order:** LinkedHashMap's code-order output is **not sorted**, so it does not meet the report requirement by itself. HashMap's insertion-order output does not match either. Each wrong choice needs an explicit fix: sorting for HashMap or LinkedHashMap (extra O(n log n) work), or a side list for TreeMap.
- **Acceptability:** all structures are under 1 µs per lookup in this run. The worst is TreeMap at 715 ns at 100,000, roughly 11 times slower than HashMap.
- **Decision:** deploy a **HashMap** for stock lookups, and sort its keys once in the evening (23 ms for 100,000 medicines, far under the 1-second criterion). The sorting cost is paid once a day, not on each of the hundreds of daily lookups.
- **Where the remaining cost comes from:** at 100,000 entries the table and the Medicine objects do not fit in CPU cache, so each lookup is dominated by memory access (a bucket and an object). TreeMap adds about 17 string comparisons per lookup (log₂ 100,000), each touching memory. The String hash is cached after the first computation, so it is not a major cost.
- **Why not Hashtable:** every method is `synchronized` on one lock, so all threads queue behind each other, and it does not allow null keys or values. Use HashMap for single-threaded or externally synchronised code, or ConcurrentHashMap when many threads read and write.

### Alternative Concept and Complexity Analysis
- **Sorted array + `Arrays.binarySearch`:** lookup is O(log n) (about 17 comparisons at 100,000), **insertion is O(n)** (shift up to n elements; measured 2.28 µs per insert at 100,000), and the code-order report is free (the array is already sorted). Space is about one reference per medicine per array (two parallel arrays here), with no per-entry hash nodes.
- **Comparison:** the array is simple and gives sorted output for free, but inserts get slower with size. HashMap gives the fastest lookup and needs a sort for each report. TreeMap is the middle ground: O(log n) on everything, always sorted.

---

## Q5 – Derive a Generic Ranking Component from a Principle (7 marks)

**Folder:** `Task05/`

### Design
- **Comparable or Comparator?** Use **separate Comparator objects** (here, one static `RANK` comparator). A `Comparable` team would fix one ordering inside the class. Teams, players, colleges and quiz teams may need different rules, and a comparator can be swapped, combined and tested without changing the class. Use `Comparable` only when a single natural order exists.
- **Multi-field comparison:** `comparingInt(points).reversed()` gives points high-first. `.thenComparing(comparingInt(goalDiff).reversed())` breaks points ties by goal difference, high-first. `.thenComparing(t -> t.name)` breaks remaining ties A–Z. Note that `reversed()` applies only to the comparator it is called on, so each key needs its own reversal.
- **Generic `Leaderboard<T>`:** the class does not need a bound on `T`, because the comparator is passed in. If the design used `Comparable` instead, the bound would be `<T extends Comparable<? super T>>`, the PECS form, which lets a `Comparable` parent type rank subclasses.
- **Wildcards:** `Comparator<? super T>` lets a comparator for a supertype rank a subtype (consumer position). `List<? extends T>` is useful in a method such as `addAll(List<? extends T> more)`, which only reads from the argument (producer position). This is not in the given code, so it is a design note only.
- **Ranking returns a sorted copy** so that the stored list is never changed (TODO 1).

### Correctness argument (based on reasoning, not output)
A ranking is correct if the comparison is consistent: antisymmetric (`compare(a,b)` and `compare(b,a)` have opposite signs), transitive, and returns 0 only for items that are equal in the ranking.
- Each `comparingInt` key is consistent because integer comparison is.
- `reversed()` negates the sign of a consistent comparator, so it stays consistent.
- `thenComparing` is lexicographic: it uses the second comparator only when the first returns 0. A chain of consistent comparators is consistent.
- Two teams compare as 0 only if points, goal difference and name are all equal, so equal items compare as equal.
Therefore the TimSort inside `List.sort` (stable) gives one well-defined ranking, and teams that tie on all three keys keep arrival order.

### Test data
- **Data set A (ties at every level):** Panthers 9/4, Falcons 6/1, Wolves 9/6, Tigers 3/0, Owls 6/3, Bisons 9/4, Cobras 6/1, Eagles 9/6. Expected order worked out by hand: Eagles, Wolves, Bisons, Panthers, Owls, Cobras, Falcons, Tigers. **Result: PASS.**
- **Data set B (designed to break subtraction):** Alpha 2,000,000,000 points, Gamma 0, Beta −2,000,000,000.
  - Correct: `[Alpha, Gamma, Beta]`.
  - Faulty `b.points - a.points`: `[Gamma, Beta, Alpha]`. The subtraction 2e9 − (−2e9) = 4e9 overflows `int` and becomes negative, so Alpha is ranked as if it were lowest.
- **Faulty comparator that ignores tie-breakers** (points only): on data A, the 9-point teams come out as Panthers, Wolves, Bisons, Eagles, which is input order, not name order. A stable sort keeps the input order for ties, so this is wrong silently.
- **Comparator that never returns 0** (`a.points > b.points ? -1 : 1`) on 200 teams: `IllegalArgumentException: Comparison method violates its general contract!`. Java's sort detects the inconsistency only for some inputs, so the test had to be large enough to trigger it.

### Complete the Code
- TODO 1 `ranking()`: copy the list, sort the copy with `order`, return the copy.
- TODO 2: `Comparator.comparingInt((Team t) -> t.points).reversed().thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed()).thenComparing(t -> t.name)`.
- Output: `[Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]`, as expected.
- **Why Bisons is ranked above Panthers:** both have 9 points and goal difference 4, so the tie is broken by name, and "Bisons" comes before "Panthers" A–Z.

### Alternative Concept and Complexity Analysis
- **`Team implements Comparable<Team>`:** one `compareTo` method with the same three-key chain. A single sort is O(n log n) comparisons. Simple for one ranking, but a second ranking (for example by goal difference only) would need a second method or a comparator anyway, and reusing it for players or colleges means writing a new class.
- **Three stable sorts (name, then goal difference, then points):** each `List.sort` is O(n log n), so three sorts cost 3·O(n log n) comparisons, and each sort uses its own TimSort buffer (O(n) extra space). It works because stable sorts keep the previous order for ties. The passes must be run in the right order (the primary key last), which is easy to get wrong and harder to explain and test than one comparator.
- **Reuse:** the comparator-chain design is the easiest to reuse for players or colleges. Each type needs only its own key functions; `Leaderboard<T>` and the chain pattern stay the same.

---

## Q6 – Priority, Order and Fairness in a Queue (8 marks)

**Folder:** `Task06/`

### Design
- **Why a plain Queue is not enough:** a FIFO queue serves by arrival time only. A severity-1 patient who arrives after a severity-3 patient would wait behind that patient, which breaks the rule.
- **PriorityQueue with a comparator:** `comparingInt(severity)` puts the lowest severity number at the head. `thenComparingLong(arrival)` keeps the earliest arrival first within a severity. Together they give a total order, so `poll()` always returns the next patient to see.
- **Arrival order among equal severities:** each patient receives a strictly increasing `counter` value, so no two patients have the same arrival key. The comparator's second key makes the order deterministic.
- **Removal and display:** `poll()` removes and returns the head in O(log n). The waiting list is shown by copying the queue and polling the copy, so the real queue is not changed.

### Correctness check (8 patients, worked by hand)
Arrivals in order: Ravi S3 [0], Meena S1 [1], John S3 [2], Asha S5 [3], Kumar S1 [4], Lata S2 [5], Dev S2 [6], Nita S4 [7].
By hand: severity 1: Meena, Kumar; severity 2: Lata, Dev; severity 3: Ravi, John; severity 4: Nita; severity 5: Asha.
**Program result: `Meena(S1) Kumar(S1) Lata(S2) Dev(S2) Ravi(S3) John(S3) Nita(S4) Asha(S5)` – PASS.**

**Why printing the PriorityQueue directly is wrong:** `toString()` prints the internal heap array, which is a partial order (each parent is at least as urgent as its children), not a sorted list. The raw output of this run was `[Meena(S1), Kumar(S1), Lata(S2), Nita(S4), Ravi(S3), John(S3), Dev(S2), Asha(S5)]`: Nita (S4) sits before Ravi (S3) and Dev (S2) comes after them, which is not treatment order. Only repeated `poll()` gives the treatment order.

### Complete the Code
- TODO 1: `private final PriorityQueue<Patient> queue = new PriorityQueue<>(Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival));`
- Output matches the expected `Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)`.
- **Arrival counter:** every admission gets a larger `arrival` value, so among equal severities the earlier patient has the smaller key and is polled first.

### Growth Investigation
**Prediction:** admit (`add`) and serve (`poll`) are each O(log n) per operation, so the cost per operation should grow slowly with the queue size (about 17 levels at 100,000, 20 at 1,000,000).

| Queue size n | Admit ns/op | Serve ns/op |
|---|---|---|
| 1,000 | 677.5 | 548.2 |
| 10,000 | 301.2 | 250.7 |
| 100,000 | 88.2 | 382.6 |
| 1,000,000 | 198.1 | 673.1 |

*Notes:* the first rows include JIT warm-up, which is why small sizes look slow. Going from 100,000 to 1,000,000, serve cost rises about 1.8 times while the number of levels rises about 1.2 times; the rest is memory access on a larger heap. Both operations stay below 1 µs at every size tested, so the trend is consistent with O(log n), with cache effects.

### Inquiry – starvation
- **Why severity 5 can wait indefinitely:** the strict rule always serves a lower severity number first. While at least one severity 1–4 patient is waiting, a severity-5 patient is never polled. A steady flow of severe arrivals keeps the queue non-empty of higher-priority patients.
- **Simulation (`TriageGrowth.java`):** one S5 patient arrives at t = 0, and from t = 0 to t = 999 two S1 patients arrive every tick and one patient is served per tick.
  - Strict severity rule: S5 is served at **tick 2000**. The backlog of 1,000 S1 patients has to clear first.
  - **Deadline rule** (`priority = arrival + severity × 30 ticks`): S5 is served at **tick 240**. The S5 deadline is 150 ticks, so S1 patients arriving after tick 120 have later deadlines and wait behind it.
- **Why it is fair:** every patient gets a deadline, and the earliest deadline is served first. A severity-5 patient cannot be overtaken by patients whose deadlines are later than its own, so its wait is bounded by its deadline plus the backlog of earlier deadlines. The rule is still a static key, so the PriorityQueue is unchanged. Cost: a severe patient can be served after some less severe ones whose deadlines are earlier, which is the intended trade-off.

### Deployment decision
At 5,000 patients a day, the queue does at most 5,000 admits and 5,000 serves, each about 1 µs. That is about 10 ms of CPU per day. Even at 1,000,000 waiting patients the cost per operation stays under 1 µs. **The design can serve a city hospital receiving 5,000 patients a day.** The limit is the rule, not the speed: the strict rule needs the fairness fix above.

### Alternative Concept and Complexity Analysis
- **Sorted LinkedList:** inserting at the correct position is O(n) to find the spot (walk the list), O(1) to link. Serving the head is O(1). At 5,000 patients a day this is also fast enough, but it degrades linearly.
- **Array of five FIFO queues, one per severity:** admit is O(1) (append to the right queue), serve is O(k) with k = 5 levels, which is effectively O(1). Space is O(n) plus five queues. It is the fastest and clearest design for a fixed set of 5 levels, but it cannot express finer or time-based rules (like the deadline) without changing the structure.
- **Summary:** PriorityQueue O(log n) admit/serve, sorted LinkedList O(n) admit, five-queue array O(1) for both with a fixed number of levels.

---

## Q7 – Diagnose, Redesign and Validate (13 marks)

**Folder:** `Task07/` (`RankingRedesign.java`)

**Chosen solution:** the Q5 `Leaderboard`, specifically its `ranking()` method.

### A. Diagnosis (2 marks)
- **Where the time goes:** `ranking()` copies the list and sorts the copy on every call. The sort uses about n·log₂n comparisons, and most of the time is spent in the comparator.
- **Evidence (original, read-heavy):** 200 `ranking()` calls on 20,000 teams made **52,177,400 comparisons**, about **261,000 per call**, which is close to n·log₂n ≈ 286,000. It took 1,448 ms.
- **Evidence (original, live):** after each of 500 new results, `ranking()` was called again. That made **132,175,476 comparisons** (about 264,000 per update) and took 3,198 ms.
- **Repeated work:** nothing changed between most calls, so the sort repeats work that was already done. The cost is linear in the number of queries multiplied by n·log n.
- **Characteristic causing it:** the design re-derives the order from scratch instead of storing it. The class has no record that the list is already sorted.

### B. Design objective (2 marks)
- Keep the teams in ranking order at all times, so `ranking()` does **no comparisons**.
- Measurable objective: comparisons per `ranking()` call from about n·log n down to **0**, and comparisons per new result from about n·log n down to about **log n**, with identical rankings (including ties).

### C. Revised design (4 marks)
- **Change:** `SortedLeaderboard` inserts each new item at its correct position using **binary search**, in the same upper-bound style (an equal item goes after existing equal items, so arrival order is kept for ties).
- **Property exploited:** the list is sorted before the query, so a query is only a read. The insert needs about log₂n comparisons to find the position, plus an O(n) array shift that moves references but calls no comparator.
- **`ranking()`** returns an unmodifiable view of the stored list, which is O(1) and makes no copy.
- **Operations avoided:** the sort on every query. Operations reduced: comparisons on insert, from a full sort to a binary search.
- **Extra memory and preprocessing:** no extra structure beyond the list itself. No preprocessing is needed.
- **New trade-offs:**
  1. Each add shifts up to n references (memmove). This is fast in practice, but it is O(n) per add. For a bulk load of many items, one sort is cheaper, so the original design is better there.
  2. The view reflects later changes, so a caller who needs a snapshot must copy it.
  3. Team scores are assumed immutable. If points changed in place, the stored order would be wrong, so an update would need remove-and-reinsert (a search plus a shift).

**Design representation (pseudocode):**
```text
class SortedLeaderboard<T>
    items: list (kept sorted by order)
    view: read-only wrapper of items

    add(item):
        lo = 0; hi = items.size
        while lo < hi:
            mid = (lo + hi) / 2
            if order(items[mid], item) <= 0: lo = mid + 1   // equal goes after
            else: hi = mid
        items.insert(lo, item)     // O(n) shift, no comparisons

    ranking():
        return view                // O(1), no comparisons
```

### D. Prediction (1 mark, written before implementing)
- **Read-heavy:** `ranking()` costs **0** comparisons and building the board costs about n·log₂n (with fewer comparisons than the original's per-query sorts once there are many queries).
- **Live:** each add costs about **log₂n** comparisons, so 500 updates at n = 20,000 should cost about 500 × 14.3 ≈ 7,100, against about 132 million for the original.
- **Time:** the live workload should drop by thousands of times, because the work per update changes from n·log n to log n plus a memmove.

### E. Implementation and validation (2 marks)
Same test conditions for both designs: n = 1,000, 5,000, 20,000; 200 queries (read-heavy); 500 live updates; 3 repeats; the same comparator (with a counting wrapper). Correctness check: both designs gave **identical rankings for 2,000 teams with many ties**.

| Workload | n | Design | Comparisons | Time (ms, mean) |
|---|---|---|---|---|
| read-heavy | 1,000 | Original | 1,738,400 | 54.5 |
| read-heavy | 1,000 | Redesign | 8,581 | 0.4 |
| live | 1,000 | Original | 5,653,366 | 122.6 |
| live | 1,000 | Redesign | 5,175 | 0.2 |
| read-heavy | 5,000 | Original | 11,055,600 | 299.5 |
| read-heavy | 5,000 | Redesign | 54,506 | 2.2 |
| live | 5,000 | Original | 29,164,981 | 700.0 |
| live | 5,000 | Redesign | 6,214 | 0.3 |
| read-heavy | 20,000 | Original | 52,177,400 | 1,447.7 |
| read-heavy | 20,000 | Redesign | 257,987 | 11.5 |
| live | 20,000 | Original | 132,175,476 | 3,197.8 |
| live | 20,000 | Redesign | 7,192 | 0.6 |

### F. Defence (2 marks)
- **Prediction supported:** the redesign made **0** comparisons per query, as predicted. Live updates cost 7,192 comparisons for 500 adds at n = 20,000, about 14.4 per add, matching log₂ 20,000 ≈ 14.3.
- **Magnitude:** in the live workload at n = 20,000, the time fell from 3,198 ms to 0.6 ms, about **5,000 times**. The read-heavy case at 20,000 fell from 1,448 ms to 11.5 ms, about **125 times**, and the redesign's remaining cost is the one-off build (n log n comparisons). The gain grows with n and with the number of queries.
- **New costs:** the O(n) shift on each insert, and the immutability assumption (trade-off 3 above). The shift is not visible at 20,000 items, but it would matter for millions of items with frequent inserts.
- **When the original is still preferable:** when many teams are loaded at once (one sort is cheaper than many inserts), when the list is rarely read, or when items change in place and the order must be recomputed anyway. In those cases the simpler design is fine.

---

## Limitations of these experiments
- All timings are from a shared 2-CPU sandbox with JIT compilation and garbage collection in play. Small-size results (1,000 items) are dominated by warm-up. Repeat each program on your own machine and report your own means and spread.
- Comparison counts (Q5, Q7) are exact and do not depend on the machine. Use them as the primary evidence where timing is noisy.
- Space estimates for object headers and pointers (Q2, Q4) assume compressed pointers on a 64-bit JVM and were not measured directly.

## Viva preparation (key points)
- Q1: why `+` is O(n²) and why the retained memory is the same while allocation differs.
- Q2: the boundary of ArrayList and why ArrayDeque suits the Up Next queue.
- Q3: why `hashCode` must use the same fields as `equals`, and what a mutable key does.
- Q4: why a HashMap plus one sort is chosen over a TreeMap, and why Hashtable is avoided.
- Q5: why a Comparator chain is used, and the overflow failure in data set B.
- Q6: why printing a PriorityQueue does not show treatment order, and the deadline fix for starvation.
- Q7: the diagnosis (comparisons per query), the binary-insertion redesign, and its trade-offs.
