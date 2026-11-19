# CO4 Java Assignment 4 – Event-Driven GUI Design

**Student Name:** Brasilla R
**Register Number:** 192511357
**GitHub Link:** [Paste your repository link]
**Java Version:** JDK 8 or later (tested with JDK 21)
**Course:** Programming in Java (CSA09)

> Read every answer, run every program on your own computer, check the screenshots against your own run (the set in each task's `screenshots/` folder was generated in a test environment; retake them on your own machine if faculty requires), and be ready to explain any part during the viva. Pseudocode is written in a START ... STOP style; adjust symbols to the SIMATS Engineering pseudocode standard if your faculty specifies one.

---

## Task 1 – GUI Design Review (6 marks)

### 1.1 Diagnose the design (2 marks)

The original `FeeWindow` (Task 1 in the question paper) has these deficiencies:

1. **Interface and logic are mixed.** All validation, database work and receipt handling sit inside `actionPerformed`. The user experience: a change to a fee rule can break the window, and the rule cannot be tested without opening the window.

2. **The action command is compared with `==`.** `e.getActionCommand() == "Pay"` compares object references, not text. It only works because both strings happen to be interned literals. If the label is ever translated or built at run time, the test fails silently and the user's payment is not made.

3. **The Receipt button falls into the `else` branch.** Any action that is not "Pay", including Receipt, runs the Clear code. The user presses Receipt and the form is wiped with no receipt shown.

4. **No validation of input.** `Double.parseDouble` throws `NumberFormatException` for text such as "abc" and the empty string. The exception is uncaught, so the window does nothing visible. An empty registration number and a zero or negative amount are accepted and written to the database.

5. **Database work and `Thread.sleep(3000)` run on the event-dispatch thread.** The whole window freezes for three seconds on every payment. Windows shows "Not Responding", and clicks made in that time are queued and may fire later as duplicate payments.

6. **No feedback to the user.** Success is shown only with `System.out.println("Paid")`, which the user never sees. A failed payment gives no message at all.

7. **SQL injection.** The statement is built by concatenating the registration number and amount. Input such as `x'); DELETE FROM fees; --` changes the meaning of the SQL. The user experience: any user can damage the fee table.

8. **Connection is never closed.** `Connection c` is opened for each payment and never closed. Connections leak until the database refuses new ones, and the user sees payments fail after a busy day.

9. **Window closing is not handled.** There is no `WindowListener`, so clicking the close button does nothing in AWT and the window stays open. The user has no clean way to exit and must end the process another way.

10. **Checked exceptions are not handled.** `Thread.sleep` throws `InterruptedException` and `DriverManager.getConnection` throws `SQLException`. Neither is caught or declared, and `actionPerformed` cannot declare them, so the code does not compile as written. The first step of any repair is therefore to catch or wrap these exceptions.

11. **Double-clicking Pay inserts two payments.** Nothing disables the button while a payment is in progress.

### 1.2 Repair the design (3 marks)

**Class structure**

| Class | Layer | Responsibility |
|---|---|---|
| `FeeWindow` (extends `Frame`) | User interface | Collects input, disables buttons while busy, shows results and errors. No SQL and no rules. |
| `FeeService` | Service | Validates the registration number and amount, records a payment, builds a receipt. No GUI code. |
| `FeeService.ValidationException` | Service | Checked exception for bad input. Its message is shown to the user. |
| `FeeService.PaymentException` | Service | Checked exception for database failure. |
| `FeeServiceTest` | Test | Console tests of the validation rules. |

**Rewritten event handling.** Each button has its own lambda listener (`pay.addActionListener(e -> onPay())`), so no action-command string is compared. `onPay` validates first, shows any error at once, then disables the buttons and starts a background thread for the database call. When the thread finishes it calls `EventQueue.invokeLater` to update the window. This removes the freeze, the duplicate-click problem and the silent failure. The `Thread.sleep` is removed; the bank confirmation step is not part of the database and is not simulated.

**Evidence.** The code is in `Task01/FeeService.java` and `Task01/FeeWindow.java`. `FeeServiceTest` ran with the following output:

```text
PASS  regNo '  tn10eb1234 '
PASS  regNo ''
PASS  regNo 'AB-12'
PASS  regNo 'ABCDEFGHIJKLMNOP'
PASS  amount '2500'
PASS  amount '10.555'
PASS  amount ''
PASS  amount 'abc'
PASS  amount '0'
PASS  amount '-5'
PASS  amount 'NaN'
PASS  amount '2000000'
Passed: 12  Failed: 0
```

The database table used by `FeeService` is:

```sql
CREATE TABLE fees (
    reg_no  VARCHAR(15)  NOT NULL,
    amount  DECIMAL(10,2) NOT NULL,
    paid_at TIMESTAMP    NOT NULL
);
```

### 1.3 Design defence (1 mark)

The redesigned FeeWindow only reads the text fields, calls FeeService and displays results, so it contains no SQL, no validation rules and no waiting. FeeService is plain Java with no window, so FeeServiceTest checks its rules in milliseconds without MySQL or a display. Each button has its own listener, so behaviour is easy to find and no string comparison can break. Database work runs on a background thread, keeping the window responsive. Prepared statements prevent SQL injection, and try-with-resources closes every connection. A change to a fee rule touches only one class.

*(92 words)*

### Alternative concept and complexity analysis

With one listener for all buttons, an event is dispatched by comparing its action command with each label in turn. For k buttons, this takes up to k string comparisons, so the worst-case dispatch time is O(k). The listener itself is one object, so the space used is O(1) for the listener.

With one listener object per button, or a Swing `Action` shared by the button, Java calls the correct listener directly. Dispatch is O(1) regardless of k, because the event source already holds a reference to its listener. The space used is k listener objects, which is O(k). Each small listener object costs a few dozen bytes, so for the handful of buttons in this window the extra memory is negligible, while the dispatch and maintenance advantages are real. The per-button design is the one used in the repaired code.

---

## Task 2 – From Requirement to Event-Driven Design (8 marks)

### 2.1 Event table (2 marks)

| Event source | Event object | Listener interface | Action taken |
|---|---|---|---|
| Department combo box (selection changes) | `ItemEvent` | `ItemListener` | Refresh the doctor list for the new department and clear the slot list. |
| Doctor combo box (selection changes) | `ItemEvent` | `ItemListener` | Load the free time slots for the chosen doctor. |
| Time slot list (selection changes) | `ListSelectionEvent` | `ListSelectionListener` | Enable the Book button when a slot is selected. |
| Book button | `ActionEvent` | `ActionListener` | Validate the patient ID and selections, then book the appointment. |
| Patient ID field (Enter key) | `ActionEvent` | `ActionListener` | Same as the Book button. |
| Clear button | `ActionEvent` | `ActionListener` | Reset all fields. |
| Window (close requested) | `WindowEvent` | `WindowListener` (`WindowAdapter`) | If a booking is in progress, ask for confirmation; otherwise dispose the window. |

### 2.2 Component hierarchy (2 marks)

```text
JFrame "Outpatient Appointment Desk"                 (top-level window)
 └─ content pane  (BorderLayout)
     ├─ NORTH  JPanel  (GridLayout 4 x 2)            (form)
     │    ├─ JLabel "Patient ID"  + JTextField       (text input)
     │    ├─ JLabel "Department"  + JComboBox<String>
     │    ├─ JLabel "Doctor"      + JComboBox<Doctor>
     │    └─ JLabel "Time slot"   + JComboBox<String>
     ├─ CENTER JScrollPane > JTextArea               (confirmation or error messages)
     └─ SOUTH  JPanel  (FlowLayout)                  (buttons)
          ├─ JButton "Book"
          └─ JButton "Clear"
```

### 2.3 Handler pseudocode (3 marks)

**(a) Refresh the doctor list when the department changes**

```text
START  onDepartmentChanged(selectedDepartment)
    CLEAR doctorCombo
    CLEAR slotCombo
    IF selectedDepartment is empty THEN
        DISABLE doctorCombo
        RETURN
    END IF
    doctors <- doctorMap.get(selectedDepartment)     // loaded once at start-up
    FOR EACH doctor IN doctors
        ADD doctor TO doctorCombo
    END FOR
    ENABLE doctorCombo
STOP
```

**(b) Validate and book the appointment**

```text
START  onBookClicked()
    patientId <- TRIM(patientIdField.text)
    IF patientId is empty OR patientId has characters other than letters and digits THEN
        SHOW "Enter a valid patient ID" IN messageArea
        RETURN
    END IF
    IF department, doctor or slot is not selected THEN
        SHOW "Select a department, doctor and time slot" IN messageArea
        RETURN
    END IF
    DISABLE bookButton
    result <- appointmentService.book(patientId, doctor, slot)   // runs on a background thread
    IF result is success THEN
        SHOW "Booked: " + doctor + " at " + slot IN messageArea
        CLEAR patientIdField
        CLEAR slotCombo
    ELSE
        SHOW result.errorMessage IN messageArea          // for example, slot already taken
    END IF
    ENABLE bookButton
STOP
```

The booking inside `appointmentService.book` checks that the slot is still free and inserts the booking in one database transaction, so two receptionists cannot take the same slot.

### 2.4 Event trace (1 mark)

The trace below was recorded with a `WindowAdapter` that prints each method, run on JDK 21 (`Task02/WindowTrace.java`, output in `Task02/window_trace_output.txt`). The close was triggered by dispatching a `WINDOW_CLOSING` event, the same event the window manager sends when the user clicks the close button.

```text
A  setVisible(true) called
1  windowOpened
2  windowActivated
   paint()                <- paint() may appear before or after windowActivated
B  user closes window
3  windowClosing          <- confirmation check happens here
5  windowDeactivated
4  windowClosed
```

The position of `paint()` relative to `windowActivated` is decided by the windowing system and can change between runs, so do not rely on it. The order of the close events (`windowClosing`, then `windowDeactivated`, then `windowClosed`) was the same in every run.

Comparison with the applet life cycle: an applet's `init` runs once when it is loaded, like the constructor and layout of the frame. `start` runs each time the applet becomes active, like `windowOpened` and `windowActivated`. `stop` runs when the page is left, and `destroy` runs once at the end, which correspond to `windowDeactivated` and `windowClosed`. The key difference is that a frame is not stopped when it is hidden and restarted; it is opened and closed as a window. The `windowClosing` method is the one place where a window can refuse to close, which applets have no equivalent for.

### Alternative concept and complexity analysis

If the doctor list were loaded from the database each time the department changes, each change would need one query. The time per change is the database round trip, plus O(m_d) to fill the combo box for the m_d doctors in that department. The memory used at any moment is only the list for the current department, O(m_d).

With the list loaded once into `Map<String, List<Doctor>>`, a change needs one hash lookup, O(1) on average, plus the same O(m_d) to fill the combo box, with no database call. The map holds every department and doctor, so the space is O(d + m) for d departments and m doctors in total. For a hospital with d = 20 departments and m = 300 doctors the map is small, so the trade-off favours loading once: the response to each change is immediate, and the database is queried only at start-up. The cost is that a doctor added during the session is not seen until the window is reopened, which should be documented for the receptionists.

---

## Task 3 – From Sketch to Layout (9 marks)

### 3.1 Choose the layout managers (3 marks)

| Container | Layout manager | Justification with respect to resizing |
|---|---|---|
| Frame content pane | `BorderLayout` | It is the only standard layout that can pin the title and search row to the top, the buttons to the bottom and let the results area take all the spare space in the middle. |
| Top panel (title and search row) | `BorderLayout` | The title sits in NORTH and the search row in CENTER, so the top panel keeps its preferred height when the window is resized. |
| Search row | `FlowLayout` (centred) | Its components keep their preferred sizes in a single row. They do not stretch when the window is resized, which the requirement asks for. |
| Results area | `JScrollPane` in the frame's CENTER | The frame's CENTER region is the only one that grows, so the results grow with the window. |
| Bottom panel | `BorderLayout` | The status message goes in SOUTH and the button row in CENTER, so the status line keeps its height while the button row stays fixed. |
| Button row | `FlowLayout` (centred) | Prev, Next and Reserve keep their size and remain centred. |

`GridLayout` was not chosen for the frame because it divides the height into equal cells. Each row would then change height with the window, which breaks the requirement that the search row and buttons keep their size.

### 3.2 Nested container structure (2 marks)

```text
JFrame                                        content pane: BorderLayout
 ├─ NORTH   JPanel                            BorderLayout
 │    ├─ NORTH   JLabel "Campus Library Search"           (title bar)
 │    └─ CENTER  JPanel                       FlowLayout (centre)
 │         ├─ JLabel "Search:"
 │         ├─ JTextField (search field)
 │         ├─ Choice (category)
 │         └─ JButton "Search"
 ├─ CENTER  JScrollPane > JTextArea          results area, grows on resize
 └─ SOUTH   JPanel                            BorderLayout
      ├─ CENTER  JPanel                       FlowLayout (centre)
      │    ├─ JButton "Prev"
      │    ├─ JButton "Next"
      │    └─ JButton "Reserve"
      └─ SOUTH   JLabel (status message)
```

Five containers are used: the frame's content pane, the top panel, the search row, the bottom panel and the button row. The `Choice` is an AWT component, as the question specifies, and it is placed in a Swing panel, which Java allows.

### 3.3 Implementation (3 marks)

The screen is implemented in `Task03/LibrarySearchScreen.java` (Swing). It contains a sample catalogue, a search that filters by text and category, paging with Prev and Next (five results per page), and a Reserve action that asks for an item number.

### 3.4 Resizing test (1 mark)

The program was run and resized to three sizes. The screenshots are in `Task03/screenshots/`: `T3_1_default_size.png` (600 × 370), `T3_2_narrow_560x420.png` and `T3_3_enlarged_1000x700.png`.

| Region | Expected behaviour when the window is resized | What the screenshots show |
|---|---|---|
| Title bar | Keeps its height, stays centred | Same size and centred at 560 × 420 and at 1000 × 700. |
| Search row | Keeps its size, stays centred | Search field, Choice and Search button keep their size and stay centred in both resized windows. |
| Results area | Grows in width and height | Fills all the space below the search row. It is the only region that changes size: it grows at 1000 × 700 and fills the taller 560 × 420 window. |
| Button row | Keeps its size, stays centred | Prev, Next and Reserve keep their size and stay centred in both windows. |
| Status message | Keeps its height at the bottom | Stays at the bottom, showing "2 item(s) found for "java" in All." in both windows. |

**Note:** the results match the behaviour the design predicts. If you retake the screenshots on your own computer, check the table against your own images.

### Alternative concept and complexity analysis

With `GridBagLayout`, every component has a `GridBagConstraints` object and the layout manager works out row and column sizes for the whole grid. The layout time is O(c × g) for c components and g grid cells in each pass, and each component needs its own constraints object, so the extra space is O(c). The result can be made to resize correctly, but the constraints are harder to read and maintain.

With absolute positioning (`setLayout(null)` and `setBounds` for each component), no layout manager runs at all, so the calculation time is O(c) for setting the bounds once. However, nothing resizes: when the window is enlarged the results area stays the same size and the components stay in the same place, so the requirement fails. The number of container objects is 1 for absolute positioning and 5 for the nested design, so the nested design uses a few extra objects, which is negligible at this size. The nested `BorderLayout` and `FlowLayout` design is preferred because it meets the resizing requirement with a simple structure.

---

## Task 4 – Event-Handling Forensics (7 marks)

### 4.1 Locate the defect (2 marks)

The method is named `mouseclicked`, with a lower-case "c", instead of the `mouseClicked` method that `MouseAdapter` defines. Because the name is different, the method is a new method that nobody calls. The `MouseAdapter` version, which does nothing, is the one Java calls on every click. The click is therefore never handled, so `count` never changes and the label stays at "Clicks: 0". The dot is never drawn because the drawing code is inside the misspelled method.

### 4.2 Construct a failing interaction (2 marks)

Once the name is corrected, the dots and the count appear while the window is open. Interaction that reveals the second defect:

1. Start the program and click five times. Five dots appear and the label reads "Clicks: 5".
2. Minimise the window, then restore it.
3. The dots have disappeared, but the label still reads "Clicks: 5".

**Why it happens.** The dots were drawn with `getGraphics()` and `fillOval` in the click handler. That drawing goes straight onto the screen and is not remembered. When the window is restored, the operating system asks the window to repaint, Java calls `paint()`, and `paint()` redraws only what the program stores. Nothing was stored, so the window is cleared and the dots are lost. The count survives because it is stored in a field, which is the difference the fix relies on.

### 4.3 Repair the program (2 marks)

The repaired program is `Task04/DrawPad.java`. The changes are:

- `mouseClicked` is written with the correct name and `@Override`. It adds the point to `points`, updates the label to `points.size()` and calls `repaint()`.
- `paint()` draws every stored point as a 6 × 6 filled circle centred on the point, using `p.x - 3` and `p.y - 3` as the top-left corner.

Compile and run:

```bash
cd Task04
javac DrawPad.java
java DrawPad
```

Screenshots: `Task04/screenshots/T4_1_before_minimise.png` (five dots, "Clicks: 5") and `T4_2_after_minimise_and_restore.png` (the same dots and label after the window was minimised and restored). The test environment has no window manager, so the minimise was confirmed with `getState()` returning ICONIFIED rather than a taskbar; the dots are still shown after restoring.

### 4.4 Prevent recurrence (1 mark)

If `@Override` is placed on the misspelled method, the compiler checks that a method with the same signature exists in a superclass. Here it does not, so compilation fails at the line of the annotation, before the program can run. The defect is therefore exposed as a compile error instead of a silent failure.

The message was produced by compiling `Task04/Prevention/DrawPadMisspelled.java`, which is the same code with `@Override` on the misspelled method:

```text
DrawPadMisspelled.java:9: error: method does not override or implement a method from a supertype
            @Override
            ^
1 error
```

The message is saved in `Task04/compiler_message.txt`. This is the evidence for the question's "Remove @Override from TODO 1, misspell the method as mouseclicked, and record the compiler message" step: without `@Override` the same misspelling compiles silently, which is why the annotation is the safeguard.

### Alternative concept and complexity analysis

The repaired design stores n points and redraws all of them in `paint()`. Each repaint takes O(n) time, and the space is O(n) for the points.

If the dots were drawn once onto an off-screen `BufferedImage` and that image were copied in `paint()`, each click would cost O(1) to draw one dot onto the image. Each repaint would then copy one image, which takes time proportional to the number of pixels, W × H, but not to n. For a 400 × 300 window stored as ARGB (4 bytes per pixel) the image needs 400 × 300 × 4 = 480,000 bytes, about 0.46 MB, whatever the number of clicks. The point list grows with every click, so for very many clicks the image is the better use of memory, while for a few hundred clicks the list is smaller and simpler. The image also loses the ability to redraw or remove individual dots, which the point list keeps.

---

## Task 5 – Same Screen, Different Event-Handling Styles (7 marks)

### 5.1 Four implementations (3 marks)

The four styles are implemented in two files:

| Style | Where it is | How the listener is attached |
|---|---|---|
| (a) Frame implements `ActionListener` | `Task05/ConverterVariants.java`, class `FrameStyle` | `convertButton.addActionListener(this)` |
| (b) Named inner class | `ConverterVariants.java`, class `NamedInnerStyle` | `new ConvertHandler()` passed to the button |
| (c) Anonymous inner class | `ConverterVariants.java`, class `AnonymousStyle` | `new ActionListener() { ... }` passed to the button |
| (d) Lambda expression | `Task05/Converter.java` | `ActionListener convertAction = e -> convert();` |

All four use `WindowAdapter` for window closing: the `Base` class registers `new WindowAdapter() { public void windowClosing(...) { dispose(); } }`, so only the one method that is needed is overridden.

Run them with:

```bash
cd Task05
javac Converter.java ConverterVariants.java
java ConverterVariants a      # or b, or c
java Converter                # style d (lambda)
```

The conversion output was checked by driving the lambda window in a virtual display: 1000 INR gave "12.00 USD", and "abc" gave "Invalid amount".

### 5.2 Change the requirement (2 marks)

The requirement adds Enter-key conversion and a dialog box. I measured the change with `diff` between a pre-change version of `ConverterVariants.java` (button only, result in a label) and the updated version. The pre-change version was reconstructed from the updated file, so treat it as the 5.1 baseline for this comparison; the diff itself is saved in `Task05/change_5_2_diff.txt`.

| Style | Lines changed for the Enter key | Lines changed for the dialog | Notes |
|---|---|---|---|
| (a) Frame implements `ActionListener` | 1 added (`amount.addActionListener(this)`) | 7 diff lines, shared in `Base`, counted once for all styles | The same `actionPerformed` serves both triggers. |
| (b) Named inner class | 1 added (`amount.addActionListener(handler)`) | (shared, as above) | The existing `ConvertHandler` object is reused. |
| (c) Anonymous inner class | 1 added (`amount.addActionListener(handler)`) | (shared, as above) | The anonymous listener is stored in a variable so both components can use it. |
| (d) Lambda | 0 added: TODO 1 already attaches one lambda to the button and the amount field | 2 lines replaced in `convert()` (`result.setText` becomes `JOptionPane`) | Only the dialog change is needed. |

Totals for the variants file: 10 diff lines (7 shared plus 3 Enter-key lines). The lambda version needs 2 replaced lines. The main finding is that the dialog change lives in one shared place in styles (a) to (c), so it costs the same effort in every style, while the Enter key costs one line per style. Adding a new trigger is cheap in every style, but the anonymous and frame versions need a new line each time, which the shared `Action` in the alternative analysis avoids.

### 5.3 Compare the styles (2 marks)

| Criterion | (a) Frame implements | (b) Named inner | (c) Anonymous inner | (d) Lambda |
|---|---|---|---|---|
| Readability | Clear, but the frame carries listener code | Clear: the handler has a name | Compact, but nested and harder to scan | Most compact and easy to read |
| Access to frame fields | Direct | Direct (inner class of the frame) | Direct (inner class of the frame) | Direct (lambda inside the frame) |
| Reuse of the handler | Low: the handler is the frame | High: one named class can be attached to many components | Low: each anonymous class is used once | Medium: the lambda variable can be reused |
| Ease of change | A change to the listener changes the frame class | Change the named class in one place | Each copy must be edited | Change the lambda in one place |

For a large application I would use **named classes or a separate controller class**, not the frame itself. A named class can be tested, reused by several screens and given a meaningful name in stack traces. Lambdas are good for short, single-use handlers. Anonymous inner classes are now mostly replaced by lambdas. Keeping the handler out of the frame also keeps the frame's job limited to layout, which matches the separation principle from Task 1.

### Complete the code (lambda version)

`Task05/Converter.java` contains the completed TODO 1 and TODO 2:

- **TODO 1:** one `ActionListener convertAction = e -> convert();` is registered on the Convert button and on the amount field, so the same lambda serves both the button and the Enter key.
- **TODO 2:** `convert()` parses the amount, multiplies by the rate for the chosen currency and shows the result with two decimals. If parsing fails it shows "Invalid amount".

Screenshots: `Task05/screenshots/T5_1_lambda_valid_1000INR.png` (1000 INR gives "12.00 USD"), `T5_2_lambda_invalid_amount.png` ("abc" gives "Invalid amount"), and `T5_3_style_a_result_dialog.png`, which shows the Task 5.2 result dialog in style (a).

### Alternative concept and complexity analysis

A method reference, `convertButton.addActionListener(this::convert)`, does the same job as the lambda. It creates one listener object when it is evaluated, so the space is O(1) for each place it is used.

A shared `Action` object extends `AbstractAction`. The same object is given to the button with `button.setAction(convertAction)`, to a menu item with `menuItem.setAction(convertAction)`, and to a keyboard shortcut through an `InputMap`. Only one object is created, so the space is O(1) regardless of how many triggers use it. Adding a new trigger means passing the same object to one more component, which is a single line and needs no new listener code. With the four styles above, adding a new trigger means writing a new listener or a new lambda each time, so the effort grows with the number of triggers. The `Action` approach also keeps the enabled state and the text of the menu item and button in step, which the plain listeners do not do.

---

## Task 6 – Design Challenge: Campus Event Registration (13 marks)

### Stage 1 – Interpret the problem (2 marks)

**Users:** students (register for events) and the cultural committee (view, filter and export registrations).

**Screens:** a registration form, a registrations table with an event filter and paging, an export dialog and message dialogs.

**Data:** events (name) and registrations (event, student ID, name, email, time of registration).

**Testable requirements**

| ID | Requirement | Test |
|---|---|---|
| R1 | A student can register with a valid student ID, name and email for an event. | Demo TEST 1 is accepted and appears in the table. |
| R2 | An invalid student ID, name or email is rejected with a message that says what is wrong. | Demo TESTs 2, 3 and 4 are rejected with a validation message. |
| R3 | A student cannot register twice for the same event. | Demo TEST 5 is rejected as a duplicate. |
| R4 | The same student can register for different events. | Demo TEST 6 is accepted. |
| R5 | The table can be filtered by event and loaded one page at a time. | Demo TEST 7 shows 50 rows on page 1 and 2 rows on page 2. |
| R6 | The registrations can be exported to CSV. | Demo TEST 8 writes 52 rows with a header line. |
| R7 | The window stays responsive while data is loading. | All database calls run in `SwingWorker`; the buttons are disabled while work is in progress. |

### Stage 2 – Screen and event flow (2 marks)

```text
+------------------------------------------------------------------+
| File (Export CSV, Exit)  Help (About)                 <- JMenuBar |
+------------------------------------------------------------------+
| New registration                                                  |
|   Student ID [________]   Name [________]   Email [________]      |
|   Event [v Music Night]                       [ Register ]        |
+------------------------------------------------------------------+
| Show event: [v All events]  [Previous]  Page 1  [Next]           |
+------------------------------------------------------------------+
| ID | Event | Student ID | Name | Email | Registered at           |
| ...                                                               |
+------------------------------------------------------------------+
| status message                                                    |
+------------------------------------------------------------------+
```

**Event flow**

```text
Window opens -> load events (worker) -> fill Event and Filter lists -> load page 1 (worker) -> table shown
Register button -> validate and save (worker) -> success dialog -> clear form -> reload current page
                                              -> validation or duplicate dialog -> form kept for correction
Filter change -> load page 1 for the chosen event (worker)
Previous / Next -> load the previous or next page (worker)
File > Export CSV -> file chooser -> write all rows for the filter (worker) -> status message
File > Exit or window close -> dispose and exit
```

### Stage 3 – Critical design decisions (2 marks)

- **Input validation** happens in `RegistrationService.register` before any database call, so a bad value never reaches the database. The rules are plain Java and can be tested without a window.
- **Duplicate checking** happens in two places. The service first checks with `exists`, which gives a clear message. The database `UNIQUE (event_id, student_id)` constraint is the second line of defence: if two submissions race, the second insert fails with `SQLIntegrityConstraintViolationException`, which the service also reports as a duplicate.
- **Database access** happens only in `RegistrationRepository`, using `PreparedStatement` throughout. No SQL appears in the window or the service.
- **Long-running database work** runs in `SwingWorker.doInBackground`, which executes on a worker thread. Results come back to the event-dispatch thread in `done()`, where the table and controls are updated. Buttons are disabled while work is running, so a second request cannot be sent.
- **Stale results are ignored.** Each page load takes a token number, and a result whose token is no longer current is discarded. This stops an old page from overwriting a newer filter choice.
- **Paging** with `LIMIT` and `OFFSET` keeps memory and load time bounded. The service fetches 51 rows to learn whether a Next page exists without a second query.

### Stage 4 – Classes and handlers (4 marks)

**Class diagram**

```text
+---------------------+        +-------------------------+        +----------------------------+
| RegistrationApp     |  uses  | RegistrationService     |  uses  | RegistrationRepository     |
| (JFrame - UI layer) | -----> | (service layer)         | -----> | (data-access layer, JDBC)  |
| JTable, JComboBox,  |        | register(), page(),     |        | findEvents(), exists(),    |
| JMenuBar, SwingWorker |      | exportCsv(), validation |        | insert(), findPage()       |
+---------------------+        +-------------------------+        +----------------------------+
                                          |  creates / returns                  |
                                          v                                     v
                               +-------------------+                 +-------------------+
                               | Registration      |                 | MySQL: events,    |
                               | (model)           |                 | registrations     |
                               +-------------------+                 +-------------------+
```

Each arrow points one way only: the window knows the service, the service knows the repository, and the repository knows the database. The window never touches JDBC.

**Pseudocode: Register action**

```text
START  onRegisterClicked()
    eventName <- registerEvent.selectedItem
    IF eventName is empty THEN
        SHOW "No event is available" IN status
        RETURN
    END IF
    eventId <- eventIds.get(eventName)
    DISABLE all controls
    START worker:
        // service.register runs here, off the event-dispatch thread
        sid <- TRIM(studentId)
        nm  <- TRIM(name)
        em  <- TRIM(email)
        IF sid does not match 1..20 letters or digits THEN THROW ValidationException
        IF nm does not start with a letter or has illegal characters THEN THROW ValidationException
        IF em is not a valid address THEN THROW ValidationException
        IF repository.exists(eventId, sid) THEN THROW DuplicateRegistrationException
        repository.insert(eventId, sid, nm, em)
        // a UNIQUE violation during insert is also reported as a duplicate
    END WORKER
    ON WORKER DONE (on event-dispatch thread):
        ENABLE controls
        IF success THEN
            CLEAR form fields
            SHOW "Registered for " + eventName IN status
            RELOAD current page
        ELSE IF ValidationException or DuplicateRegistrationException THEN
            SHOW the exception message IN status and in a warning dialog
            (keep the form so the student can correct it)
        ELSE
            SHOW "Database error" dialog
        END IF
STOP
```

**Pseudocode: Filter action**

```text
START  onFilterChanged()
    IF the event list is being loaded THEN RETURN     // ignore programmatic changes
    eventId <- eventIdFor(filterEvent.selectedItem)   // null for "All events"
    pageIndex <- 0
    token <- NEW token
    START worker:
        // LIMIT 51 OFFSET 0: the 51st row tells us whether a Next page exists
        rows <- repository.findPage(eventId, 51, 0)
        hasNext <- rows.size > 50
        IF hasNext THEN REMOVE last row
    END WORKER
    ON WORKER DONE:
        IF token is no longer current THEN RETURN      // a newer request has replaced this one
        FILL table WITH rows
        SET page label to "Page 1"
        ENABLE Next IF hasNext
        ENABLE Previous = FALSE
STOP
```

### Stage 5 – Implement and demonstrate (3 marks)

The implementation is in `Task06`:

| File | Layer | Purpose |
|---|---|---|
| `schema.sql` | Database | Creates the `events` and `registrations` tables, the foreign key and the duplicate-blocking unique key. Required SQL script. |
| `Registration.java` | Model | One registration row. |
| `RegistrationRepository.java` | Data access (JDBC) | All SQL. Uses prepared statements only. |
| `RegistrationService.java` | Service | Validation, duplicate rule, paging, CSV export. |
| `RegistrationApp.java` | User interface (Swing) | Form, table, filter, paging, menu bar, `SwingWorker` for every database call. |
| `RegistrationDemo.java` | Console test | Runs the normal, invalid and duplicate test cases against the database. |
| `LoadBenchmark.java` | Measurement | Times loading all rows against loading one page (see the alternative analysis). |

**Setup**

```bash
# 1. Create the database (MySQL or MariaDB)
mysql -u root -p < schema.sql

# 2. Put the JDBC driver (mysql-connector-j) in this folder, then compile
javac -cp ".:mysql-connector-j-8.0.33.jar" *.java

# 3. Run the application window
java -cp ".:mysql-connector-j-8.0.33.jar" RegistrationApp

# 4. Run the console demonstration
java -cp ".:mysql-connector-j-8.0.33.jar" RegistrationDemo
```

Change `DB_USER` and `DB_PASSWORD` in the Java files to your own MySQL login first.

**Demonstration output** (`Task06/demo_output.txt`, from a real run against the database):

```text
TEST 1 normal registration
  ACCEPTED: TEST01
TEST 2 invalid student ID
  REJECTED (validation): Student ID must be 1 to 20 letters or digits.
TEST 3 invalid name
  REJECTED (validation): Name must start with a letter and may contain only letters, spaces, dots, apostrophes and hyphens.
TEST 4 invalid email
  REJECTED (validation): Enter a valid email address, for example name@campus.edu.
TEST 5 duplicate for the same event
  REJECTED (duplicate): Student TEST01 is already registered for this event.
TEST 6 same student, different event (allowed)
  ACCEPTED: TEST01
TEST 7 paging (51 students, page size 50)
  page 1 rows = 50, hasNext = true
  page 2 rows = 2, hasNext = false
TEST 8 CSV export for Music Night
  rows written = 52 (file .../registrations_export_test.csv)
  first two lines of file:
    ID,Event,Student ID,Name,Email,Registered at
    "100056","Music Night","TEST01","Asha Kumar","asha@campus.edu","2026-11-17 08:04:44.0"
```

The windows were also started against the live database under a virtual display. The window loaded all four events plus "All events" into the filter, showed the first page of 50 rows, and filtered to Music Night when the filter was changed.

**Screenshots:** the set in `Task06/screenshots/` covers these requirements:

| Requirement | Screenshot file |
|---|---|
| R1 valid registration | `T6_R1_valid_registration_accepted.png` (form cleared, table reloaded) and `T6_R1_registration_visible_in_table.png` (the new TEST77 row shown under Dance Battle) |
| R2 invalid input | `T6_R2_invalid_student_id_dialog.png` (warning: "Student ID must be 1 to 20 letters or digits.") |
| R3 duplicate | `T6_R3_duplicate_registration_dialog.png` (warning that TEST01 is already registered for Music Night) |
| R5 filter and paging | `T6_R5_main_window_first_page.png`, `T6_R5_filter_music_night_page1.png`, `T6_R5_filter_music_night_page2.png` |

Not captured: R4 (same student for a different event), R6 (CSV export, which opens a file chooser that I did not automate) and R7 (responsiveness during loading). Add screenshots for these yourself if your faculty requires them. The database test data was loaded for the test run, so the row counts in the screenshots reflect that data.

### Alternative concept and complexity analysis

**Measured on the test database** (`LoadBenchmark`, MariaDB 10.11 and Java 21, 100,053 rows, median of 5 runs; output in `Task06/benchmark_output.txt`; your numbers will differ):

| Approach | Time (ms, median) | Rows held in memory |
|---|---|---|
| Load all 100,053 rows into the JTable model | 395 | 100,053 |
| Load one page of 50 (first page) | 161 | 50 (plus 1 look-ahead row) |
| Load one page of 50 near the end of the table | 177 | 50 |

Heap added by the full table model: about 34 MB. The table model for one page holds 50 rows, so its memory is a few kilobytes.

**Analysis**

- **Loading time.** Loading all rows takes time proportional to N, the total number of registrations, O(N), for both the database to produce the rows and the table model to add them. For N = 100,053 this measured a median of about 395 ms, plus the time to draw the table.
- **Memory.** Holding all rows uses O(N) memory: about 34 MB measured for 100,000 rows. A page uses O(page size), which is constant, so memory does not grow with the number of registrations.
- **Responsiveness.** The full load runs on the event-dispatch thread in the naive design, so for the 395 ms measured, and more on a slower machine or a larger table, the window cannot repaint, and it feels frozen. In the design used here, the load runs in a `SwingWorker`, so the window stays responsive whatever the size; the paged design also keeps each database wait short.
- **Paging cost.** `LIMIT` and `OFFSET` do not make the database fast for deep pages. MySQL still reads and skips the rows before the offset, so the last page measured 177 ms against 161 ms for the first page. For very large tables a keyset (seek) query, which filters on the last registration ID already shown, keeps each page cost constant. That is a possible refinement, not required for this assignment.
- **Connection cost.** The repository opens a new connection for each call, which accounts for part of the 161 ms page time. A connection pool would reduce this, but it adds a dependency and is outside the scope of this assignment.

---

## Summary of Evidence Files

| Task | Code or evidence file |
|---|---|
| 1.2 | `Task01/FeeService.java`, `Task01/FeeWindow.java`, `Task01/FeeServiceTest.java`, `Task01/test_output.txt` |
| 2 | `Task02/window_trace_output.txt` (event trace), tables and pseudocode in this report |
| 3 | `Task03/LibrarySearchScreen.java` |
| 4 | `Task04/DrawPad.java`, `Task04/Prevention/DrawPadMisspelled.java`, `Task04/compiler_message.txt` |
| 5 | `Task05/Converter.java`, `Task05/ConverterVariants.java`, `Task05/change_5_2_diff.txt` |
| 6 | `Task06/schema.sql`, `Task06/*.java`, `Task06/demo_output.txt`, `Task06/benchmark_output.txt` |

## Final submission checklist

- [ ] Replace the student name, register number and GitHub link in this report and in `README.md`.
- [ ] Run every program on your own computer and compare the output with this report.
- [ ] Optionally re-measure Task 5.2 on your own before-and-after files; the counts in the report come from `Task05/change_5_2_diff.txt`.
- [ ] Check the generated screenshots in each `screenshots/` folder, and retake them on your own machine if your faculty requires screenshots from your own run. Add the missing R4, R6 and R7 screenshots if required.
- [ ] Commit each task folder separately to the GitHub repository `CSA09-192511357-Assignment4`.
- [ ] Be ready to explain any part of the design, trace an event through a handler and modify a handler during the viva.
