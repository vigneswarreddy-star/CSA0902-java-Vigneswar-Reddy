# CO4 Java Assignment 4 – Event-Driven GUI Design

**Student Name:** Brasilla R
**Register Number:** 192511357
**Java Version:** JDK 8 or later (tested with JDK 21)
**Repository name:** `CSA09-192511357-Assignment4`

## Project contents
- `Assignment_Report.md` – answers and explanations for Tasks 1 to 6, including the alternative concept and complexity analysis for each task.
- `Task01/` – repaired fee payment design: `FeeService.java`, `FeeWindow.java`, `FeeServiceTest.java`, `fees_schema.sql`.
- `Task02/` – design task (answers in the report) plus `WindowTrace.java`, which prints the window event order.
- `Task03/` – `LibrarySearchScreen.java`, the library search screen.
- `Task04/` – `DrawPad.java` (repaired) and `Prevention/DrawPadMisspelled.java` with its compiler message.
- `Task05/` – `Converter.java` (lambda, completed) and `ConverterVariants.java` (styles a, b, c).
- `Task06/` – layered Swing and JDBC application, `schema.sql`, console demo and benchmark.

## How to compile and run each task

Open a terminal in the project folder.

### Task 1 (validation tests need no database)
```bash
cd Task01
javac FeeService.java FeeWindow.java FeeServiceTest.java
java FeeServiceTest
```

### Task 2 (optional event trace, needs a display)
```bash
cd Task02
javac WindowTrace.java
java WindowTrace
```

### Task 3
```bash
cd Task03
javac LibrarySearchScreen.java
java LibrarySearchScreen
```

### Task 4
```bash
cd Task04
javac DrawPad.java
java DrawPad
```

### Task 5
```bash
cd Task05
javac Converter.java ConverterVariants.java
java Converter
java ConverterVariants a
```

### Task 6 (needs MySQL and the MySQL Connector/J jar)
```bash
cd Task06
mysql -u root -p < schema.sql
javac -cp ".:mysql-connector-j-8.0.33.jar" *.java
java  -cp ".:mysql-connector-j-8.0.33.jar" RegistrationApp
```

## Before submitting
1. Replace the student name and register number in this file and in the report.
2. Run all programs on your own computer.
3. Screenshots are in each task's `screenshots/` folder (generated in a test environment). Retake them on your own machine if your faculty requires screenshots from your own run.
4. Create the GitHub repository with the required name, share it with the faculty, and upload the project.
5. Make at least one commit for each task, as the assignment requires.
6. Add the GitHub link to the report cover page.

**Note:** The `*_output.txt` files were produced by running the programs in a test environment. Run the programs yourself. The screenshots in the task folders were generated in a test environment and should be retaken if your faculty requires your own.
