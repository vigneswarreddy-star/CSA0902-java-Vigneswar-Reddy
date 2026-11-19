# Task 6 – Campus Event Registration (Swing + JDBC + MySQL)

Layers
- Presentation: `RegistrationApp.java` (Swing; all database work in SwingWorker).
- Service: `RegistrationService.java` (validation, duplicate rule, paging, CSV export).
- Data access: `RegistrationRepository.java` (JDBC with PreparedStatement only).
- Model: `Registration.java`.
- Database script: `schema.sql`.

Evidence
- `demo_output.txt` – output of `RegistrationDemo` (normal, invalid, duplicate, paging, export).
- `benchmark_output.txt` – output of `LoadBenchmark` (load all rows vs one page).

Setup
1. Install MySQL (or MariaDB) and load the schema:
   `mysql -u root -p < schema.sql`
2. Download MySQL Connector/J (for example `mysql-connector-j-8.0.33.jar`) into this folder.
3. Set `DB_USER` and `DB_PASSWORD` in the Java files to your own login.

Compile and run
```bash
javac -cp ".:mysql-connector-j-8.0.33.jar" *.java
java  -cp ".:mysql-connector-j-8.0.33.jar" RegistrationApp     # the window
java  -cp ".:mysql-connector-j-8.0.33.jar" RegistrationDemo    # console test cases
java  -cp ".:mysql-connector-j-8.0.33.jar" LoadBenchmark       # optional measurement
```
Screenshots: `screenshots/` covers R1, R2, R3 and R5 (generated in a test environment). R4, R6 and R7 still need your own screenshots.
