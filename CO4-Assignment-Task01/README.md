# Task 1.2 – Repaired fee payment design (FeeService + FeeWindow)

Files
- `FeeService.java` – validation, payment and receipt logic (no GUI).
- `FeeWindow.java` – AWT window that collects input and shows results.
- `FeeServiceTest.java` – console tests of the validation rules (no database needed).
- `fees_schema.sql` – creates the `fees` table (needed only for the database features).
- `test_output.txt` – output of `FeeServiceTest` from a real run.

Compile and run
```bash
cd Task01
javac FeeService.java FeeWindow.java FeeServiceTest.java
java FeeServiceTest          # runs without MySQL
```
To use the window, run `fees_schema.sql` in MySQL, add the MySQL JDBC driver (mysql-connector-j) to the classpath, set DB_USER and DB_PASSWORD in `FeeWindow.java`, then:
```bash
javac -cp ".:mysql-connector-j-8.0.33.jar" *.java
java  -cp ".:mysql-connector-j-8.0.33.jar" FeeWindow
```
Result: invalid input is rejected with a message on the window; a valid payment is recorded on a background thread and the window stays responsive.
