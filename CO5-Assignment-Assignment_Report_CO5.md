# CO5 Java Assignment 5 – Data Persistence Engineering: Files and Databases

**Student Name:** Brasilla R
**Register Number:** 192511357
**GitHub Link:** [Paste your repository link]
**Java Version:** JDK 11 or later (`Files.writeString` needs Java 11; all results were produced with JDK 21)
**Test database:** MariaDB 10.11 (MySQL-compatible) with MariaDB Connector/J 2.7.6, database `transport_test`

> Part A is design and pseudocode. Part B is implemented in the task folders and every result below is taken from the saved output files in those folders. The database used was MariaDB, not MySQL, so exact timings will differ on MySQL. The screenshots in each `screenshots/` folder are rendered copies of those saved outputs, made in the test environment; retake them on your own machine if your faculty requires.

---

# PART A – Persistence Design & Pseudocode (25 marks)

## A1 – Design a File Format (7 marks)

### A1.1 Record layouts (2 marks)

The transport office record has seven fields: pass ID, student register number, name, route number, boarding stop, validity date and amount paid.

**(a) Delimited text file.** One record per line, UTF-8, line terminated by LF, field order as below.

| # | Field | Type | Size rule | Example |
|---|---|---|---|---|
| 1 | pass_id | text | `P` + 6 digits | `P000001` |
| 2 | reg_no | text | 9 digits | `192311001` |
| 3 | name | text | 1–60 characters, **no comma** | `Student Arun` |
| 4 | route_no | integer | 1–99 | `12` |
| 5 | boarding_stop | text | 1–30 characters, **no comma** | `Gate 2` |
| 6 | validity_date | date | `YYYY-MM-DD` | `2027-06-30` |
| 7 | amount | decimal | 2 decimal places | `4500.00` |

Delimiter: comma. A name containing a comma is **rejected when the record is written** (validation rule). The alternative, CSV quoting (`"Lee, Kumar"`), makes every reader need a quote-aware parser, which is harder to test; the rule is simpler and was chosen. Example record (60 bytes including the newline): `P000001,192311001,Student Arun,12,Gate 2,2027-06-30,4500.00`.

**(b) Binary file written with `DataOutputStream`.** Strings are written with `writeUTF`, which stores a 2-byte length followed by the bytes, so no delimiter is needed and a name may contain any character.

| Order | Call | Bytes | Example value |
|---|---|---|---|
| 1 | `writeUTF(pass_id)` | 2 + length | `P000001` → 9 |
| 2 | `writeUTF(reg_no)` | 2 + length | `192311001` → 11 |
| 3 | `writeUTF(name)` | 2 + length | `Student Arun` → 14 |
| 4 | `writeInt(route_no)` | 4 | 12 → 4 |
| 5 | `writeUTF(boarding_stop)` | 2 + length | `Gate 2` → 8 |
| 6 | `writeInt(yyyymmdd)` | 4 | 20270630 → 4 |
| 7 | `writeDouble(amount)` | 8 | 4500.0 → 8 |

Record size for the example: **58 bytes**. The text record is 60 bytes, so the two layouts are almost the same size, but only the binary layout is safe for any name.

### A1.2 Operations for the text design (3 marks)

Stream choice and reasons:
- **`BufferedWriter` over `FileWriter` in APPEND mode** for `add`: the data is text, and the buffer turns many small writes into few operating-system writes.
- **`BufferedReader` over `FileReader`** for `find` and `listByRoute`: `readLine()` gives one record at a time, and reading in 8 KB chunks avoids one system call per character (Part A2 shows the cost of that).
- **`FileInputStream`/`FileOutputStream` are not used**: they are byte streams, and the text would have to be decoded by hand.

```text
START  add(record)
    IF record fails validation (field count, formats, no comma in name) THEN
        RETURN error "invalid record"
    END IF
    IF find(record.pass_id) is not empty THEN
        RETURN error "duplicate pass ID"
    END IF
    OPEN BufferedWriter on passFile in APPEND mode
    WRITE joined fields with ","
    WRITE newline
    CLOSE writer                              // flushes the buffer
    RETURN success
STOP

START  find(passId)
    IF passFile does not exist THEN RETURN empty
    OPEN BufferedReader on passFile
    lineNo <- 0
    WHILE readLine() gives line
        lineNo <- lineNo + 1
        fields <- SPLIT line on "," keeping empty fields
        IF count(fields) is not 7 THEN
            LOG "damaged line" lineNo
            CONTINUE                          // skip, keep searching
        END IF
        IF fields[1] equals passId THEN
            CLOSE reader
            RETURN fields
        END IF
    END WHILE
    CLOSE reader
    RETURN empty
STOP

START  listByRoute(route)
    OPEN BufferedReader on passFile
    results <- empty list
    WHILE readLine() gives line
        fields <- SPLIT line on ","
        IF count(fields) is 7 AND fields[4] equals route THEN
            ADD fields TO results
        END IF
    END WHILE
    CLOSE reader
    RETURN results
STOP
```

### A1.3 Damaged data (2 marks)

- **Damaged line** (wrong field count, non-numeric route or amount, bad date): detected when the line is read. The line is logged with its line number and skipped, so one bad line does not stop a search. Part B2 showed that the first version of the code skipped lines silently, which is why the log step is required.
- **Missing file**: `find` and `listByRoute` return "no passes" without an error; `add` creates the file on first write. The program reports "file not found" only at start-up if the office expects the file to exist.
- **Incomplete write after a power failure**: the last line may be partial (fewer than seven fields, or no newline). It is treated as a damaged line on read. To reduce the risk, `add` writes the whole record and newline in one call and forces it to disk (`FileChannel.force`) before reporting success. For compaction or repair, the program writes a new file and renames it over the old one (`Files.move` with `ATOMIC_MOVE`), so the old file is kept until the new one is complete.

### Alternative: RandomAccessFile with fixed-length records

With fixed-length records, record *i* starts at byte *i × recordSize*, so finding a pass by ID needs an index lookup or a computed position. Time to find a pass by its position is **O(1)** (one seek), against **O(n)** for the scan of the delimited file. Searching by pass ID still needs an index (for example a sorted file with binary search, O(log n) seeks) because the ID is not the position, so the O(1) advantage applies only to lookup by position.

Space cost of padding: with each field padded to its maximum width (pass_id 10, reg_no 9, name 40, route 4, stop 20, date 4 as an integer, amount 8), each record takes **95 bytes**. The actual example record is 58 bytes, so 37 bytes (39%) are padding. For about 8,000 passes a year, that is about 742 KB against 453 KB with variable-length records. The padding is small in absolute terms, but it grows with the number of records and it forces a maximum name length, which the variable layout does not.

**Conclusion:** the variable-length binary or text layout is preferred for this office. The office needs search by pass ID and by route, not by position, so the seek advantage is not used, and the padding and length limits are real costs.

---

## A2 – Don't Read What You Don't Need (5 marks)

### A2.1 Predict the cost (2 marks)

The program calls `FileInputStream.read()` once per byte. A 50 MB file is 50 × 1,024 × 1,024 = **52,428,800 bytes**, so the program makes **52,428,801 calls** (one more for the end-of-file return). Each call of `read()` on a `FileInputStream` is a request to the operating system, a system call with a fixed overhead of roughly 0.3 to 1 µs. The prediction is therefore 52 million system calls and a run time of tens of seconds, dominated by the call overhead rather than the actual work of counting newlines.

### A2.2 Redesign using buffering (2 marks)

```text
START  countLinesBuffered(file)
    buffer <- NEW byte array of 8192
    lines <- 0
    OPEN FileInputStream on file
    WHILE (n <- read(buffer)) is not -1
        FOR i <- 0 TO n - 1
            IF buffer[i] is newline THEN lines <- lines + 1
        END FOR
    END WHILE
    CLOSE stream
    RETURN lines
STOP
```

A call to `read(buffer)` asks the operating system for up to 8,192 bytes at once. The number of system calls falls from one per byte to one per 8 KB, which is about 8,192 times fewer. Java code is in `Task01/LineCounter.java`.

### A2.3 When buffering does not help (1 mark)

Buffering gives little benefit when the cost is in processing, not in reading. For example, if each line is parsed with a regular expression that costs far more time than the read, the read calls become a small part of the total, and a larger buffer changes almost nothing. Another case is a file that is already in memory, where no system calls are made at all.

### Alternative: `Files.readAllLines()`

`Files.readAllLines()` reads the whole file into a list of strings. For a 50 MB text file, the memory used is **O(file size)**: Java stores each character in 2 bytes, so the strings alone need about 100 MB, plus list and object overhead. The time is O(N) for N bytes, as with buffering, and the number of operating-system reads is small. The buffered streaming approach uses **O(buffer size)** memory (8 KB here), whatever the file size.

**When each is appropriate:** `readAllLines` is appropriate for small files that the program needs in full several times (for example a configuration file) or that it must sort or index. Streaming with a buffer is appropriate for large files read once, such as the 50 MB attendance log. The measured result in Part B1.3 confirms the buffering gain (15,583 ms unbuffered against 26 ms buffered for 50 MB).

---

## A3 – Discover the Data-Access Design Pattern (5 marks)

### A3.1 Identify the common pattern (2 marks)

Each of `StudentDB`, `CourseDB` and `MarksDB` repeats these steps:

1. Open a connection.
2. Build an SQL string.
3. Execute the statement.
4. Read the `ResultSet` and map each row to an object.
5. Close the connection.

**Parts that differ:** the table name, the SQL text, the mapping from columns to object fields, and the object class created. **Parts that are the same:** opening and closing connections, executing, iterating the result set, and error handling. The repeated part is a template; the differences are the variable parts. This is the **Data Access Object (DAO)** pattern with a generic interface.

### A3.2 Generic data-access interface (2 marks)

```text
INTERFACE Dao<T, K>                          // T = entity type, K = key type
    insert(entity: T)
    findById(key: K) RETURNS T or NULL
    findAll() RETURNS list of T
    update(entity: T)
    delete(key: K)
END INTERFACE
```

```text
CLASS StudentDao IMPLEMENTS Dao<Student, String>

    PRIVATE connectionSource                     // provides connections

    METHOD findById(regNo)
        sql <- "SELECT reg_no, name, email, room_no FROM student WHERE reg_no = ?"
        con <- connectionSource.open()
        ps <- con.prepareStatement(sql)
        ps.setString(1, regNo)
        rs <- ps.executeQuery()
        IF rs.next() THEN
            result <- mapRow(rs)                 // the only student-specific mapping
        ELSE
            result <- NULL
        END IF
        close rs, ps, con                        // always, even on error (try-with-resources)
        RETURN result
    END METHOD

    METHOD mapRow(rs)
        RETURN new Student(rs.getString("reg_no"), rs.getString("name"),
                           rs.getString("email"), rs.getInt("room_no"))
    END METHOD

    // insert, findAll, update and delete follow the same pattern with their own SQL
END CLASS
```

A generic helper, `executeQuery(sql, params, mapper)`, can hold the connection, statement and close steps once for every DAO, and each DAO passes only its SQL and its mapper. That removes the repeated code from all three classes.

### A3.3 Map objects to tables (1 mark)

| Java field (class `Student`) | Java type | Column (table `student`) | SQL type | Constraint |
|---|---|---|---|---|
| `regNo` | `String` | `reg_no` | VARCHAR(20) | **PRIMARY KEY**, NOT NULL |
| `name` | `String` | `name` | VARCHAR(100) | NOT NULL |
| `email` | `String` | `email` | VARCHAR(120) | |
| `roomNo` | `int` | `room_no` | INT | |

The primary key is `reg_no`, because each register number identifies exactly one student and is used to look the student up (`findById`).

### Alternative: SQL written directly in the service or GUI

If the SQL were written inside the service or GUI classes, each class that uses a table would contain its own SQL text. **When a column is renamed**, every class that uses that column must change: with the DAO, the change is confined to one class (`StudentDao`); without it, the change is in every service and window that touches `student`. For a table used in five classes, that is five places to find, edit and test instead of one.

**Time and space:** the SQL is executed by the database in the same way in both designs, so the database work per operation is identical. The DAO adds one method call and one object per operation, which is O(1) time and negligible space compared with the database round trip. The difference is maintenance and risk, not speed.

---

## A4 – What Should Be Serialized? (4 marks)

### A4.1 Decide what to persist (2 marks)

- **`Connection` must be `transient`.** A database connection is tied to a live network socket and to the database server's session. It cannot be restored from a file, and `Connection` is not serializable, so without `transient` serialization fails with `NotSerializableException`.
- **`password` must be `transient`.** Serialized data is readable by anyone who can read the file. The password would be stored in clear text. After loading, the password is `null`, and login must be checked against the database (or the stored value should be a hash, if it must be kept).
- **name, register number and room data** are plain state and are serialized.

**What happens to the object graph.** `ObjectOutputStream.writeObject(hostel)` writes the `Hostel` object, then every object it refers to, recursively: the list of `Room` objects, and for each room the list of `Resident` objects. Each object is written once; if the same object is reached twice, a back-reference is written instead, so shared objects and cycles are handled. Every class in the graph must implement `Serializable`. Transient fields are skipped and are restored to their default values (`null` or 0) when the graph is read back.

The Part A4 measurement (`PartA_Measure/Resident.java`) confirms that a transient password is not written: after serializing a resident with password `secret-pw`, the file contains **0** occurrences of that text.

### A4.2 Plan for change (2 marks)

A new field `phone` is added to `Resident` after files were saved.

- **With `serialVersionUID` declared** (for example `private static final long serialVersionUID = 1L;`) and unchanged: the old file is read successfully. The new field is set to its default (`null`). The compatibility rules allow adding a field, so no exception is thrown.
- **Without `serialVersionUID`:** Java computes one from the class's structure. Adding a field changes that value, and reading the old file throws `InvalidClassException`.

Test result (`PartA_Measure/versions/`, output in `versions_output.txt`): the file written by the version without `phone` was read by the version with `phone` and `serialVersionUID = 1L`, giving `read OK: name=Arun regNo=192311001 phone=null password=null`. The version without a declared UID failed with `local class incompatible: stream classdesc serialVersionUID = 1, local class serialVersionUID = 7661959039145148080`.

Recommendation: always declare `serialVersionUID`, handle `null` for fields added later, and never rename or change the type of a field without a conversion step.

### Alternative: CSV or JSON text

Measured for 1,000, 10,000 and 100,000 residents (`PartA_Measure/SerialVsCsv.java`, output in `parta_alt_output.txt`):

| n | Format | Save ms | Load ms | File bytes |
|---|---|---|---|---|
| 1,000 | serialization | 74 | 78 | 35,020 |
| 1,000 | CSV | 26 | 6 | 24,455 |
| 10,000 | serialization | 50 | 78 | 359,021 |
| 10,000 | CSV | 26 | 10 | 255,156 |
| 100,000 | serialization | 291 | 350 | 3,689,022 |
| 100,000 | CSV | 112 | 54 | 2,652,157 |

Both formats are O(n) in time and space. The CSV file is about 28% smaller at 100,000 records, and loading is about 6 times faster, because serialization writes class and field descriptions and object headers. The trade-off is that CSV has no types and no object graph, so the program must convert every field itself. With a new field, CSV needs a column-count check (old lines have fewer columns), and JSON tolerates a missing key; serialization handles it through `serialVersionUID`, as shown above, but only for Java classes.

---

## A5 – Design for Correctness and Safety (4 marks)

### A5.1 Transaction design (2 marks)

```text
START  transfer(fromWallet, toWallet, amount = 500)
    con <- open connection
    con.setAutoCommit(FALSE)                     // start a transaction
    TRY
        balance <- SELECT balance FROM wallet WHERE id = fromWallet FOR UPDATE
        IF balance < amount THEN
            THROW "insufficient balance"
        END IF
        UPDATE wallet SET balance = balance - amount WHERE id = fromWallet
        UPDATE wallet SET balance = balance + amount WHERE id = toWallet
        con.commit()                             // both updates become permanent together
        RETURN success
    CATCH any error
        con.rollback()                           // undo both updates
        RETURN failure with the error message
    FINALLY
        con.setAutoCommit(TRUE)
        close con
    END TRY
STOP
```

If the program fails after the first update and before `commit`, the database discards the uncommitted change, so the money is neither lost nor created. `FOR UPDATE` locks the source row, so two transfers from the same wallet cannot both read the same balance. The Part B3.3 test shows the same property for a multi-row load: the failed transaction left **0** rows, while auto-commit left **5,000**.

### A5.2 Injection analysis (2 marks)

The login query is built by joining the user's text into the SQL:

```sql
SELECT * FROM users WHERE id='<id>' AND pwd='<pwd>'
```

A crafted `id` changes the meaning of the SQL, because the quote characters in the input close the string literal:

- **Comment attack:** `id = admin'-- ` (note the trailing space). The SQL becomes `... WHERE id='admin'-- ' AND pwd='...'`. Everything after `--` is a comment, so the password check is removed and the query matches the `admin` row for any password.
- **Tautology attack:** `id = x' OR '1'='1' -- ` with an empty password. The WHERE clause becomes `id='x' OR '1'='1' -- ' AND pwd=''`, which is always true.

**Observed (Part B2, `Task02/adversarial_output.txt`):** the comment attack was **accepted** by the unsafe check for any password; the tautology with an empty password was **accepted**. The PreparedStatement version rejected both.

**Redesign with PreparedStatement:**

```text
START  login(id, pwd)
    sql <- "SELECT 1 FROM users WHERE id = ? AND pwd = ?"
    ps <- con.prepareStatement(sql)
    ps.setString(1, id)                          // the input is sent as DATA, never as SQL text
    ps.setString(2, pwd)
    rs <- ps.executeQuery()
    RETURN rs.next()                             // TRUE only if a row matches the two values
STOP
```

The SQL text is fixed before the input arrives. The database never parses the input as SQL, so a quote character is just a character in the password.

### Alternative: two separate auto-commit updates

With auto-commit on, each `UPDATE` is its own transaction. If the program fails between them, the first update is kept and the second is never done: **money is lost** (debited from the hostel wallet, not credited to the canteen). If the order were reversed, the failure would **create money** (credited twice on a retry). A transaction costs one commit (one disk flush) for both updates, while two independent updates cost two commits, roughly twice the flush time. For the wallet example that is two flushes instead of one, which is small per transfer, but it is the only way to make the two updates all-or-nothing. The data-correctness risk outweighs the time cost.

---

# PART B – Implementation & Experimental Investigation (25 marks)

All Part B programs are in the task folders. The test database is created by `sql/schema.sql` and is for this assignment only. Each folder's README gives the compile and run commands.

## B1 – Implement and Instrument the File Store (7 marks)

### B1.1 Validate the implementation (2 marks)

`Task01/TestCases.java` runs the required cases on the text store:

| Test | What it verifies | Result |
|---|---|---|
| Normal case: find `P1003` | A valid record is found and its five fields are returned in order | PASS `[P1003, 192311003, Divya, 7, 3800.0]` |
| Pass after a damaged line: find `P1004` | A damaged line in the middle does not stop the search | PASS `[P1004, 192311004, Kiran, 3, 2900.0]` |
| Non-existent ID: find `P9999` | An unknown ID gives null, not an exception | PASS `null` |
| Damaged line: find `P1002` | The damaged line is never returned as a pass | PASS `null` |
| Missing file | A missing file gives null, not a crash | PASS `null` |

Result: **ALL TESTS PASSED**. The screenshot `Task01/screenshots/B1_1_test_cases.png` shows this output.

### B1.2 Compare with the Part A design (1 mark)

The implementation differs from the Part A design in three places, each for a reason:

1. **Five fields, not seven.** The supplied `PassStore` (and its expected output) uses five fields: pass ID, register number, name, route and amount. Boarding stop and validity date were left out to match the given program. Adding them would change the field count check from 5 to 7 and the output format. The given code also writes the amount as `4500.0` (Java's default number format), not the two-decimal form of the Part A design; the program keeps that format because the expected output requires it.
2. **Missing-file check added inside `find`.** The Part A design says a missing file gives "no passes". Without the check, `Files.newBufferedReader` throws `NoSuchFileException`.
3. **Split keeps empty fields** (`split(",", -1)`). Without `-1`, a line ending in a comma loses its last field and is wrongly treated as damaged.

The comma rule from Part A (reject names with commas) is **not** in this implementation. Part B2 shows the cost of that omission.

### B1.3 Measure the effect of buffering (2 marks)

Output: `Task01/buffering_output.txt` (`BufferingBenchmark.java`; unbuffered 50 MB run once, others median of runs).

| File size | Lines | Unbuffered time (ms) | Unbuffered `read()` calls | Buffered time (ms) | Buffered `read(buf)` calls |
|---|---|---|---|---|---|
| 1 MB | 25,989 | 662 | 1,048,595 | 6 | 129 |
| 10 MB | 253,666 | 3,148 | 10,485,791 | 5 | 1,281 |
| 50 MB | 1,245,915 | 15,583 | 52,428,827 | 26 | 6,401 |

**Comparison with the A2.1 prediction:** the prediction was about 52 million system calls. The 50 MB file was 52,428,826 bytes and the unbuffered run made 52,428,827 calls (one per byte plus the end-of-file call), matching the prediction exactly. The run took 15.6 s, somewhat below the "tens of seconds" estimate. Buffering reduced the calls to 6,401 (about 8,190 times fewer) and the time to 26 ms, a speed-up of about 600 times. The unbuffered time grows in proportion to the file size, while the buffered time is close to the cost of reading the data once.

### B1.4 Measure growth (2 marks)

Output: `Task01/growth_output.txt` (`GrowthBenchmark.java`, median of 5 runs, microseconds converted to ms).

| Records | File bytes | Find first record (ms) | Find last record (ms) | Find missing ID (ms) |
|---|---|---|---|---|
| 1,000 | 38,668 | 0.13 | 2.33 | 1.87 |
| 10,000 | 396,644 | 0.12 | 14.71 | 14.58 |
| 100,000 | 4,066,395 | 0.15 | 32.64 | 26.59 |

**Trend:** finding the first record is constant, because the search stops at line 1. Finding the last record or a missing ID scans the whole file. From 1,000 to 100,000 records (100 times more data), the last-record search grew from 2.33 ms to 32.64 ms, about 14 times. The theoretical growth is **O(n)**, i.e. 100 times for 100 times more records; the smaller measured ratio at the low end is fixed overhead (opening the file and JIT warm-up) being a larger share of the small searches. The time keeps rising in proportion to the number of records once the file is large.

### Complete the code (TODO 1)

`Task01/PassStore.java` has TODO 1 completed. Running `java PassStore` gives the expected output:

```text
[P1003, 192311003, Divya, 7, 3800.0]
null
```

Why the damaged line for P1002 does not stop the search: `find` reads the file one line at a time in a loop. A line with the wrong number of fields is skipped by the `continue` statement, and the loop goes on to the next line. The search only stops when a matching line is found or the file ends. So `P1002` is not found (it is damaged), and the lines after it, including `P1003`, are still searched and found.

### Alternative: read once into a HashMap

`GrowthBenchmark` measured 1,000 lookups on 100,000 passes:

| Approach | Time |
|---|---|
| Scan the file for each search (q × n = 1,000 × 100,000) | 6,876 ms |
| Read the file once into `HashMap<String, String[]>` (n) | 124 ms |
| 1,000 HashMap lookups (q) | 1 ms |
| Total for the HashMap approach | 125 ms |

Analysis: scanning costs **O(q × n)** time, here 100 million line reads. The HashMap costs **O(n + q)** on average (one load of n lines, then one constant-time lookup per search), a reduction of about 55 times for this q. The memory is **O(n)**: the HashMap holds all 100,000 entries, about 4 MB of file data plus map and array overhead. The trade-off is that the file must be re-read or updated for new passes, so the map must be kept in step with the file.

---

## B2 – Engineer an Adversarial Input (5 marks)

Output: `Task02/adversarial_output.txt`. Code: `UnsafeLogin.java`, `SafeLogin.java`, `AdversarialTest.java`.

### Part 1 – Crafted login input

| Input `id` | Input `pwd` | Unsafe check | PreparedStatement check |
|---|---|---|---|
| `admin` | `secret123` | ACCEPTED | ACCEPTED |
| `admin` | `wrong` | rejected | rejected |
| `admin'-- ` | `anything` | **ACCEPTED** | rejected |
| `x' OR '1'='1` | `anything` | rejected | rejected |
| `x' OR '1'='1' -- ` | (empty) | **ACCEPTED** | rejected |

**Explanation of the fourth row.** The unsafe query is `WHERE id='x' OR '1'='1' AND pwd='anything'`. In SQL, `AND` binds before `OR`, so the query means `id='x' OR ('1'='1' AND pwd='anything')`. The password must still match, so the row is rejected. The attack works only when the password part is also removed, which the fifth row does with `-- `. This is why the tautology needs the comment as well.

### Part 2 – Damaged bus-pass file

| Record | Damage | Result from `find` | What it reveals |
|---|---|---|---|
| `P2001` | none | found | baseline |
| `P2002,only,three` | wrong field count | null (skipped) | correct, but **silent**: nothing tells the user a record was lost |
| `P2003,...,abc` | non-numeric amount | **returned as a valid pass** with amount `abc` | the store checks the field count only, not the types |
| empty line | blank | skipped | correct, silent |
| `P2005,...,Lee, Kumar,...` | comma inside name | null (6 fields) | a valid pass is **lost** because its name contains a comma |
| `P2006` | none | found | later records are still reached |

### What each input reveals about the original design

1. **String concatenation is the root cause of the login failure.** The user's text is part of the SQL, so the database cannot tell data from code. The fix is the parameter placeholder, not a better filter.
2. **The file store has no validation at read time and no error reporting.** It skips damaged lines silently, and it accepts a non-numeric amount as a valid pass. Both are design gaps. The Part A3 rule (reject commas in names) is also missing from the code, which is why a legitimate name with a comma loses a pass.

### Alternative: escaping or regex validation before building the SQL

Escaping quote characters (replacing `'` with `''`) or checking the input with a regular expression before building the SQL is **weaker**. Escaping depends on remembering to escape every value in every query, and it can be defeated by encoding tricks and by inputs where escaping is not applied (for example a number that is put into SQL without quotes). A regex can reject bad inputs, but it cannot tell which characters are dangerous in every context, and a rule that is too loose lets attacks through. A PreparedStatement sends the input as data, so the risk does not depend on the rule being right.

**Time cost:** validating with a regex costs O(L) per query, where L is the input length, and it is repeated for every query. A PreparedStatement also reads the input once (O(L)) when it is bound, and the server parses the fixed SQL once and reuses the plan (with server-side prepared statements). The validation step adds cost on every query and gives no guarantee; the PreparedStatement costs about the same time and is safe by construction.

---

## B3 – Statement vs PreparedStatement vs Batch (8 marks)

Code: `Task03/`. Test database: `transport_test`, table `bus_pass` (created by `sql/schema.sql`).

### B3.1 Correctness on small data (2 marks)

`CorrectnessCheck.java` inserts 10 records with each approach, then checks the rows with a `SELECT` and reads the column types from `ResultSetMetaData`.

Column metadata (read from the table): `pass_id VARCHAR`, `reg_no VARCHAR`, `name VARCHAR`, `route_no INTEGER`, `amount DECIMAL`, all NOT NULL.

| Method | Rows in table | Rows matching input | Result |
|---|---|---|---|
| Statement | 10 | 10 of 10 | CORRECT |
| PreparedStatement | 10 | 10 of 10 | CORRECT |
| Batch | 10 | 10 of 10 | CORRECT |

### B3.2 Increase the data size (2 marks)

`InsertBenchmark.java`. All three methods run inside **one transaction** (auto-commit off), so the times compare the statement types, not the cost of one disk flush per row. B3.3 measures auto-commit separately.

| Rows | Statement (ms) | PreparedStatement (ms) | PreparedStatement + batch (ms) |
|---|---|---|---|
| 1,000 | 147 | 114 | 90 |
| 10,000 | 719 | 466 | 335 |
| 100,000 | 2,170 | 2,170 | 2,072 |

Reading the results: the PreparedStatement is faster than Statement at 1,000 and 10,000 rows because the server parses the SQL once and reuses it. At 100,000 rows the difference disappears in this measurement. Batching reduces the number of round trips to the database (one per 1,000 rows instead of one per row); on a local server the round trip is cheap, so the gain is small (about 5 to 30%). Over a network, where each round trip costs more, the batch gain would be much larger.

### B3.3 Auto-commit vs transaction (2 marks)

`TransactionDemo.java`: 10,000 rows batched, with an invalid amount forced at row 5,001 (`not-a-number`).

| Mode | Result after the failure | Rows left in table |
|---|---|---|
| Auto-commit ON | failed at row 5,001 after 945 ms | **5,000** (partial load, cannot be undone) |
| One transaction | failed (`NumberFormatException`) after 332 ms | **0** (all rows rolled back) |
| Control: same data without the bad row, one transaction | loaded 10,000 in 476 ms | 10,000 |

With auto-commit, each batch of 1,000 was committed as it ran, so the first 5,000 rows stayed. A transaction keeps every insert uncommitted until `commit`, and the rollback removes them. The transaction also reached the failure faster (332 ms against 945 ms for the same rows). We did not isolate the cause; the likely reason is that the auto-commit run flushes to disk after every batch, but this was not measured separately.

### B3.4 Engineering trade-off (2 marks)

| Case | Recommended approach | Reason |
|---|---|---|
| (a) Nightly import of 100,000 passes | PreparedStatement batch in one transaction (`PassLoader`), validated first (B4) | Batching cuts round trips; a single transaction makes the import all-or-nothing, so a failed night leaves no partial data. Measured: 2,072 ms for 100,000 rows; the import can run in the background. |
| (b) Single pass issued at the counter | One PreparedStatement insert with auto-commit on | One row, one commit; the counter needs the result immediately and the row must be saved as soon as it is issued. Batching would add delay for no benefit. |

### Complete the code (TODO 1 to TODO 3)

`Task03/PassLoader.java`, all three TODOs completed:
- **TODO 1:** set the five parameters (route as int, amount as double), add the row to the batch, and execute the batch after every 1,000 rows.
- **TODO 2:** `commit()` makes all inserted rows permanent, then return the count.
- **TODO 3:** `rollback()` undoes every insert in this transaction, then re-throw the exception.

`CorrectnessCheck` and `TransactionDemo` call `loadAll`. The failure case (`TransactionDemo`) shows **0 rows** remain after the invalid amount, which is the required evidence.

Load test at the three sizes (`InsertBenchmark` and the `loadAll` path in `CorrectnessCheck`/`TransactionDemo`) is recorded in the B3.2 table above.

### Alternative: multi-row INSERT and LOAD DATA

Measured with `AltMeasure.java` for 100,000 rows, each run from an empty table in one transaction (`Task03/b3alt_output.txt`):

| Method | Time (ms) | Memory used by one batch |
|---|---|---|
| PreparedStatement batch, 1,000 rows per batch (`PassLoader`) | 2,491 | 1,000 parameter sets |
| Multi-row INSERT, 1,000 rows per statement (one SQL string with 5,000 placeholders) | 634 | one statement with 5,000 parameters |
| `LOAD DATA LOCAL INFILE` from a CSV file | 226 | the server reads the file in its own buffers |

**Analysis:** all three are O(n) in total time, but the constant factor differs by about 11 times between the batch method and `LOAD DATA`. Multi-row INSERT removes most of the per-statement parsing; `LOAD DATA` avoids the SQL layer for each row. Memory per batch is O(b) for a batch of b rows in the first two; `LOAD DATA` streams the file, so its client-side memory does not grow with the file size. **When to use each:** `LOAD DATA` for a trusted, already-clean file of millions of rows (fastest, but it bypasses row-by-row validation, so it fits only after validation); the batch method when each row must be checked and a failure must roll everything back, as in the B4 migration.

---

## B4 – Final Engineering Decision (5 marks)

### B4.1 Implement a practical strategy (2 marks)

`Task04/Migrate.java` reads the existing pass file and loads it in two steps:
1. **Validate every line first.** Each line is checked for field count, pass ID format, register number (9 digits), non-empty name, route (1–99) and amount (a positive number). Valid lines are kept. Each rejected line is recorded with its line number and reason.
2. **Load the valid rows in one transaction** with `PassLoader.loadAll`. If any row fails in the database, the whole load is rolled back.

Test file: `MakeMigrationFile.java` creates 20,006 lines, of which six are deliberately damaged (wrong field count, non-numeric amount, short register number, empty name, empty line, route out of range).

Result (`Task04/b41_output.txt`):

```text
Migration of data/passes_migrate.txt: read 20006 lines, valid 20000, rejected 6
  REJECTED line 3000: wrong number of fields (4)  | P003000,192300001,Damaged,7
  REJECTED line 6001: amount is not a number  | P006000,192300002,Name,7,abc
  REJECTED line 9002: register number must be 9 digits  | P009000,1923,Short Reg,7,3000.0
  REJECTED line 12003: name is empty  | P012000,192300003,,7,3000.0
  REJECTED line 15004: wrong number of fields (1)  |
  REJECTED line 18005: route number out of range (1-99)  | P018000,192300004,Route,150,3000.0
Loaded 20000 rows in one transaction in 892 ms. Rows in bus_pass = 20000
```

Second run of the same file (`b41b_output.txt`): `Load FAILED and was rolled back: Duplicate entry 'P000001' for key 'PRIMARY'`, with `Rows in bus_pass = 20000 (nothing half-migrated)`. The duplicate key stopped the load, and the transaction left the 20,000 existing rows untouched.

### B4.2 Inspect the database (1 mark)

`Task04/DbInfo.java` uses `DatabaseMetaData` (`Task04/b42_output.txt`):

```text
Database product : MariaDB 10.11.14-MariaDB-0ubuntu0.24.04.1
Driver           : MariaDB Connector/J 2.7.6 (JDBC 4.2)
Connected URL    : jdbc:mysql://localhost:3306/transport_test
User             : root
Identifier quote : [`]
Tables:
  bus_pass
      pass_id    VARCHAR        size=10 nullable=NO
      reg_no     VARCHAR        size=20 nullable=NO
      name       VARCHAR        size=100 nullable=NO
      route_no   INT            size=10 nullable=NO
      amount     DECIMAL        size=10 nullable=NO
      primary key: pass_id
  users
```

**Why this is useful when the application moves to another server:** the program can report which product and driver version it is talking to, confirm that the expected tables and columns exist before doing any work, and read the identifier quote character, which differs between databases (here a backtick). A startup check on the metadata turns a confusing SQL error at 2 a.m. into a clear message such as "table bus_pass missing on this server".

### B4.3 Production decision (2 marks)

**Decision: use a database.** The office should move from the text file to a database such as MySQL or MariaDB.

| Requirement | Text file | Serialization | Database (measured) |
|---|---|---|---|
| Several counters writing at once | Needs file locking across processes; a crash can corrupt the file | Whole file is rewritten on each change; no concurrent writes | Transactions and row locks; a counter's write does not block others |
| Search by pass ID | O(n) scan: 32.6 ms per search at 100,000 records (B1.4), about 260 ms at 800,000 | O(n) load, then fast in memory, but the whole object graph is loaded into memory | Index on the primary key: lookup is O(log n) |
| Safety against partial writes | Relies on careful file handling | Relies on the write being complete | Commit and rollback (B3.3): 0 rows left after a failure |
| Growth | 100× the current pass count: 800,000 passes a year | Memory grows with all passes | Scales with indexes and disk, and is built for this |

The measurement that decides it is the search time. At 100,000 records a text scan takes about 33 ms; at 100 times the current volume (about 800,000 records a year, accumulating), the same linear scan would take roughly 260 ms per search, with a whole-file rewrite on every new pass. The database answers a search in a fixed number of index steps and handles concurrent counters with transactions. The bulk load measured in B3 (100,000 rows in about 2 s, and 226 ms with `LOAD DATA`) shows that the database also handles the nightly import comfortably.

### Alternative: one serialized file per campus

With 12 campus files, a search across all passes must read every file: total time **O(N)** over all campuses, and the same space as one file, but the work is split. Each campus file is written whole on each new pass, so the cost of a write is O(n_campus), not O(1). Concurrent counters at the same campus still need locking, and there is no uniqueness check across campuses (the same pass ID could be issued twice at two campuses). Serialization also loads the whole campus into memory and is not safe against a crash in the middle of writing a file. A single shared database gives one uniqueness rule, one index for search and transactional writes for every counter.

---

## Summary of evidence files

| Task | Files |
|---|---|
| A1 | Layouts and pseudocode in this report; no Java (Part A) |
| A2 | Predictions in this report; `Task01/LineCounter.java`; measured in `Task01/buffering_output.txt` |
| A3 | Pattern and DAO design in this report |
| A4 | `PartA_Measure/Resident.java`, `PartA_Measure/versions/`, `PartA_Measure/versions_output.txt`, `PartA_Measure/SerialVsCsv.java`, `PartA_Measure/parta_alt_output.txt` |
| A5 | Pseudocode in this report; injection result in `Task02/adversarial_output.txt` |
| B1 | `Task01/PassStore.java`, `TestCases.java`, `LineCounter.java`, `FileGen.java`, `BufferingBenchmark.java`, `GrowthBenchmark.java`, outputs |
| B2 | `Task02/UnsafeLogin.java`, `SafeLogin.java`, `AdversarialTest.java`, `adversarial_output.txt` |
| B3 | `Task03/PassLoader.java`, `Passes.java`, `Db.java`, `CorrectnessCheck.java`, `InsertBenchmark.java`, `TransactionDemo.java`, `AltMeasure.java`, outputs |
| B4 | `Task04/Migrate.java`, `DbInfo.java`, `MakeMigrationFile.java`, outputs |
| Schema | `sql/schema.sql` |

## Final submission checklist

- [ ] Replace the GitHub link on the cover page.
- [ ] Run every program on your own machine (MySQL 8 with Connector/J, or the same MariaDB) and compare with these outputs.
- [ ] Retake the screenshots on your own machine if your faculty requires them. The rendered screenshots in the `screenshots/` folders are copies of the saved outputs.
- [ ] Commit each task folder separately to the repository `CSA09-192511357-Assignment5`.
- [ ] Be ready to explain the DAO pattern, the transaction pseudocode and the injection analysis, and to modify a method during the viva.
