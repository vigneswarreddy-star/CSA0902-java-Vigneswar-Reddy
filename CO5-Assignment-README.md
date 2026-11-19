# CO5 Java Assignment 5 – Data Persistence Engineering

**Student Name:** Brasilla R
**Register Number:** 192511357
**Java Version:** JDK 11 or later (tested with JDK 21)
**Repository name:** `CSA09-192511357-Assignment5`
**Database:** MySQL 8 with MySQL Connector/J (tested with MariaDB 10.11 and MariaDB Connector/J 2.7.6)

## Project contents
- `Assignment_Report_CO5.md` – Part A (design and pseudocode) and Part B (results and interpretation).
- `sql/schema.sql` – creates the local test database `transport_test` with tables `users` and `bus_pass`. For this assignment only.
- `Task01/` – B1: `PassStore.java` (completed TODO 1), `LineCounter.java` (A2 unbuffered and buffered), `FileGen.java`, `BufferingBenchmark.java`, `GrowthBenchmark.java`, `TestCases.java`, outputs.
- `Task02/` – B2: `UnsafeLogin.java`, `SafeLogin.java`, `AdversarialTest.java`, `PassStore.java` (copy of Task01), output.
- `Task03/` – B3: `PassLoader.java` (completed TODO 1 to 3), `Passes.java`, `Db.java`, `CorrectnessCheck.java`, `InsertBenchmark.java`, `TransactionDemo.java`, `AltMeasure.java`, outputs.
- `Task04/` – B4: `Migrate.java`, `DbInfo.java`, `MakeMigrationFile.java`, `PassLoader.java` and `Db.java` (copies of Task03), outputs.
- `PartA_Measure/` – A4 measurements: `Resident.java`, `SerialVsCsv.java`, and `versions/` (serialization versioning test), outputs.
- Each folder with a `screenshots/` subfolder holds rendered copies of the saved outputs.

## Setup
1. Install MySQL 8 (or MariaDB 10.11+).
2. Create the test database: `mysql -u root -p < sql/schema.sql`
3. Download MySQL Connector/J (for example `mysql-connector-j-8.0.33.jar`) and put it in each folder you compile, or add it to the classpath.
4. Set `USER` and `PWD` in `Db.java` (Task03 and Task04) and in `AdversarialTest.java` (Task02) to your MySQL login.

## How to compile and run

Task01 (no database needed for B1.1 and B1.2 tests):
```bash
cd Task01
javac *.java
java PassStore
java TestCases
java BufferingBenchmark data
java GrowthBenchmark data
```

Task02 (needs the database):
```bash
cd Task02
javac -cp ".:mysql-connector-j-8.0.33.jar" *.java
java -cp ".:mysql-connector-j-8.0.33.jar" AdversarialTest
```

Task03 (needs the database; `bus_pass` is truncated by the programs):
```bash
cd Task03
javac -cp ".:mysql-connector-j-8.0.33.jar" *.java
java -cp ".:mysql-connector-j-8.0.33.jar" CorrectnessCheck
java -cp ".:mysql-connector-j-8.0.33.jar" TransactionDemo
java -cp ".:mysql-connector-j-8.0.33.jar" InsertBenchmark
java -cp ".:mysql-connector-j-8.0.33.jar" AltMeasure       # needs local_infile enabled: SET GLOBAL local_infile=1;
```

Task04 (needs the database):
```bash
cd Task04
javac -cp ".:mysql-connector-j-8.0.33.jar" *.java
java -cp ".:mysql-connector-j-8.0.33.jar" MakeMigrationFile data/passes_migrate.txt
java -cp ".:mysql-connector-j-8.0.33.jar" Migrate data/passes_migrate.txt
java -cp ".:mysql-connector-j-8.0.33.jar" DbInfo
```

PartA_Measure (no database):
```bash
cd PartA_Measure
javac *.java
java SerialVsCsv
```

**Note:** the saved `*_output.txt` files were produced in the test environment. Run the programs yourself and compare. The timings depend on your machine.
