import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Presentation layer (Swing). It builds the screens and reacts to events. It never
 * runs SQL on the event-dispatch thread: every database call runs in a SwingWorker.
 */
public class RegistrationApp extends JFrame {

    // Change these to match your MySQL installation.
    private static final String DB_URL = "jdbc:mysql://localhost:3306/campus_events";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password";

    private static final String ALL_EVENTS = "All events";

    private final RegistrationService service;

    // Registration form
    private final JTextField studentIdField = new JTextField(12);
    private final JTextField nameField = new JTextField(18);
    private final JTextField emailField = new JTextField(18);
    private final JComboBox<String> registerEvent = new JComboBox<>();
    private final JButton registerButton = new JButton("Register");

    // Table toolbar
    private final JComboBox<String> filterEvent = new JComboBox<>();
    private final JButton prevButton = new JButton("Previous");
    private final JButton nextButton = new JButton("Next");
    private final JLabel pageLabel = new JLabel("Page 1");

    // Table and status
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Event", "Student ID", "Name", "Email", "Registered at"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel status = new JLabel("Starting...");

    private final Map<String, Integer> eventIds = new LinkedHashMap<>();
    private int pageIndex = 0;
    private boolean hasNextPage = false;
    private boolean busy = false;
    private boolean loadingEvents = false;
    private int loadToken = 0;   // lets a stale page load be ignored

    public RegistrationApp(RegistrationService service) {
        super("Campus Event Registration");
        this.service = service;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApp();
            }
        });
        setJMenuBar(buildMenuBar());

        // Form panel
        JPanel form = new JPanel(new GridLayout(5, 2, 5, 5));
        form.setBorder(BorderFactory.createTitledBorder("New registration"));
        form.add(new JLabel("Student ID"));  form.add(studentIdField);
        form.add(new JLabel("Name"));        form.add(nameField);
        form.add(new JLabel("Email"));       form.add(emailField);
        form.add(new JLabel("Event"));       form.add(registerEvent);
        form.add(new JLabel(""));            form.add(registerButton);
        registerButton.addActionListener(e -> registerStudent());

        // Toolbar above the table
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JLabel("Show event:"));
        toolbar.add(filterEvent);
        toolbar.add(prevButton);
        toolbar.add(pageLabel);
        toolbar.add(nextButton);
        filterEvent.addActionListener(e -> {
            if (!loadingEvents) {
                loadPage(0);
            }
        });
        prevButton.addActionListener(e -> loadPage(pageIndex - 1));
        nextButton.addActionListener(e -> loadPage(pageIndex + 1));

        JPanel north = new JPanel(new BorderLayout());
        north.add(form, BorderLayout.NORTH);
        north.add(toolbar, BorderLayout.SOUTH);

        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        setSize(960, 560);
        setLocationRelativeTo(null);
        setBusy(true);
        loadEvents();
    }

    // ----- Menu bar -----

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem export = new JMenuItem("Export CSV...");
        export.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
        export.addActionListener(e -> exportCsv());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> exitApp());
        fileMenu.add(export);
        fileMenu.addSeparator();
        fileMenu.add(exit);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Campus Event Registration\nJava Swing with JDBC (MySQL)."));
        helpMenu.add(about);

        bar.add(fileMenu);
        bar.add(helpMenu);
        return bar;
    }

    // ----- Background tasks -----

    private void loadEvents() {
        setStatus("Loading events...", false);
        new SwingWorker<Map<String, Integer>, Void>() {
            @Override
            protected Map<String, Integer> doInBackground() throws Exception {
                return service.loadEvents();
            }

            @Override
            protected void done() {
                try {
                    Map<String, Integer> events = get();
                    eventIds.clear();
                    eventIds.putAll(events);
                    loadingEvents = true;
                    registerEvent.removeAllItems();
                    filterEvent.removeAllItems();
                    filterEvent.addItem(ALL_EVENTS);
                    for (String name : events.keySet()) {
                        registerEvent.addItem(name);
                        filterEvent.addItem(name);
                    }
                    loadingEvents = false;
                    loadPage(0);
                } catch (ExecutionException ex) {
                    setBusy(false);
                    showError(ex.getCause());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void loadPage(int page) {
        if (page < 0) {
            return;
        }
        final Integer eventId = selectedFilterEventId();
        final int token = ++loadToken;
        setBusy(true);
        setStatus("Loading registrations...", false);

        new SwingWorker<RegistrationService.Page, Void>() {
            @Override
            protected RegistrationService.Page doInBackground() throws Exception {
                return service.page(eventId, page);
            }

            @Override
            protected void done() {
                if (token != loadToken) {
                    return;   // a newer request has replaced this one
                }
                try {
                    RegistrationService.Page result = get();
                    fillTable(result.getRows());
                    pageIndex = page;
                    hasNextPage = result.hasNext();
                    pageLabel.setText("Page " + (page + 1));
                    setBusy(false);
                    setStatus(result.getRows().size() + " row(s) on this page.", false);
                } catch (ExecutionException ex) {
                    setBusy(false);
                    showError(ex.getCause());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void registerStudent() {
        final String eventName = (String) registerEvent.getSelectedItem();
        if (eventName == null) {
            setStatus("No event is available to register for.", true);
            return;
        }
        final int eventId = eventIds.get(eventName);
        final String sid = studentIdField.getText();
        final String name = nameField.getText();
        final String email = emailField.getText();
        setBusy(true);
        setStatus("Saving registration...", false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                service.register(eventId, sid, name, email);
                return null;
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    get();
                    clearForm();
                    setStatus("Registered for " + eventName + ".", false);
                    loadPage(pageIndex);          // refresh the table so the new row appears
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof RegistrationService.ValidationException
                            || cause instanceof RegistrationService.DuplicateRegistrationException) {
                        setStatus(cause.getMessage(), true);
                        JOptionPane.showMessageDialog(RegistrationApp.this, cause.getMessage(),
                                "Registration rejected", JOptionPane.WARNING_MESSAGE);
                    } else {
                        showError(cause);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("registrations.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final File file = chooser.getSelectedFile();
        final Integer eventId = selectedFilterEventId();
        setBusy(true);
        setStatus("Exporting...", false);

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return service.exportCsv(eventId, file.toPath());
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    int rows = get();
                    setStatus("Exported " + rows + " row(s) to " + file.getName() + ".", false);
                } catch (ExecutionException ex) {
                    showError(ex.getCause());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    // ----- Helpers (run on the event-dispatch thread) -----

    private Integer selectedFilterEventId() {
        String name = (String) filterEvent.getSelectedItem();
        if (name == null || ALL_EVENTS.equals(name)) {
            return null;
        }
        return eventIds.get(name);
    }

    private void fillTable(List<Registration> rows) {
        tableModel.setRowCount(0);
        for (Registration r : rows) {
            tableModel.addRow(new Object[]{
                r.getId(), r.getEventName(), r.getStudentId(),
                r.getStudentName(), r.getEmail(), r.getRegisteredAt()});
        }
    }

    /** Disables input while a background task runs, so the user cannot send a second request. */
    private void setBusy(boolean b) {
        busy = b;
        registerButton.setEnabled(!b);
        filterEvent.setEnabled(!b);
        registerEvent.setEnabled(!b);
        prevButton.setEnabled(!b && pageIndex > 0);
        nextButton.setEnabled(!b && hasNextPage);
        setCursor(Cursor.getPredefinedCursor(b ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void clearForm() {
        studentIdField.setText("");
        nameField.setText("");
        emailField.setText("");
    }

    private void setStatus(String text, boolean error) {
        status.setText(text);
        status.setForeground(error ? Color.RED : Color.BLACK);
    }

    private void showError(Throwable t) {
        String text = t == null ? "Unknown error." : t.getMessage();
        setStatus(text, true);
        JOptionPane.showMessageDialog(this, text, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void exitApp() {
        dispose();
        System.exit(0);
    }

    public static void main(String[] args) {
        RegistrationService service = new RegistrationService(
                new RegistrationRepository(DB_URL, DB_USER, DB_PASSWORD));
        SwingUtilities.invokeLater(() -> new RegistrationApp(service).setVisible(true));
    }
}
