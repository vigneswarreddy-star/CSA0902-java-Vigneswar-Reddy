import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The window ONLY collects input and shows results. Validation and database
 * work are delegated to FeeService and run on a background thread.
 */
public class FeeWindow extends Frame {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/college";
    private static final String DB_USER = "root";          // change to your MySQL user
    private static final String DB_PASSWORD = "password";  // change to your password

    private final FeeService service;
    private final TextField regNo = new TextField(15);
    private final TextField amount = new TextField(10);
    private final Button pay = new Button("Pay");
    private final Button clear = new Button("Clear");
    private final Button receipt = new Button("Receipt");
    private final Label message = new Label(" ");

    /** A database action that may fail with a PaymentException. */
    @FunctionalInterface
    private interface DbTask {
        void run() throws FeeService.PaymentException;
    }

    public FeeWindow(FeeService service) {
        super("Student Fee Payment");
        this.service = service;

        Panel form = new Panel(new GridLayout(2, 2, 5, 5));
        form.add(new Label("Registration no."));
        form.add(regNo);
        form.add(new Label("Amount (Rs)"));
        form.add(amount);

        Panel buttons = new Panel(new FlowLayout());
        buttons.add(pay);
        buttons.add(clear);
        buttons.add(receipt);

        setLayout(new BorderLayout(5, 5));
        add(form, BorderLayout.NORTH);
        add(buttons, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);

        // One listener per button: no action-command string comparison.
        pay.addActionListener(e -> onPay());
        clear.addActionListener(e -> onClear());
        receipt.addActionListener(e -> onReceipt());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();   // FeeService opens and closes its own connections, so nothing else to release
            }
        });
        setSize(440, 180);
    }

    private void onPay() {
        String reg;
        double amt;
        try {
            reg = FeeService.validateRegNo(regNo.getText());
            amt = FeeService.validateAmount(amount.getText());
        } catch (FeeService.ValidationException ex) {
            showMessage(ex.getMessage(), true);
            return;
        }
        final String regFinal = reg;
        final double amtFinal = amt;
        setBusy(true, "Processing payment...");
        runInBackground(() -> {
            service.payFee(regFinal, amtFinal);
            EventQueue.invokeLater(() -> {
                setBusy(false, "Payment of Rs " + amtFinal + " recorded for " + regFinal + ".");
                regNo.setText("");
                amount.setText("");
            });
        });
    }

    private void onReceipt() {
        String reg;
        try {
            reg = FeeService.validateRegNo(regNo.getText());
        } catch (FeeService.ValidationException ex) {
            showMessage(ex.getMessage(), true);
            return;
        }
        final String regFinal = reg;
        setBusy(true, "Loading receipt...");
        runInBackground(() -> {
            String text = service.buildReceipt(regFinal);
            EventQueue.invokeLater(() -> {
                setBusy(false, " ");
                showReceipt(text);
            });
        });
    }

    private void onClear() {
        regNo.setText("");
        amount.setText("");
        showMessage(" ", false);
    }

    /** Runs database work off the event-dispatch thread and reports failures back on it. */
    private void runInBackground(DbTask task) {
        Thread worker = new Thread(() -> {
            try {
                task.run();
            } catch (FeeService.PaymentException ex) {
                EventQueue.invokeLater(() -> {
                    setBusy(false, ex.getMessage());
                    showMessage(ex.getMessage(), true);
                });
            }
        }, "fee-db-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void setBusy(boolean busy, String text) {
        pay.setEnabled(!busy);
        clear.setEnabled(!busy);
        receipt.setEnabled(!busy);
        showMessage(text, false);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void showMessage(String text, boolean error) {
        message.setText(text);
        message.setForeground(error ? Color.RED : Color.BLACK);
    }

    private void showReceipt(String text) {
        Dialog d = new Dialog(this, "Receipt", false);
        TextArea area = new TextArea(text, 6, 40, TextArea.SCROLLBARS_NONE);
        area.setEditable(false);
        d.add(area);
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                d.dispose();
            }
        });
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    public static void main(String[] args) {
        FeeService service = new FeeService(DB_URL, DB_USER, DB_PASSWORD);
        EventQueue.invokeLater(() -> new FeeWindow(service).setVisible(true));
    }
}
