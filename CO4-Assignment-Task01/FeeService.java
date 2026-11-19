import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Business and data logic for fee payment. Contains NO GUI code, so it can be
 * tested without opening a window and reused by any user interface.
 */
public class FeeService {

    private static final int MAX_REG_NO_LENGTH = 15;
    private static final double MAX_AMOUNT = 1_000_000.0;

    /** Raised when user input breaks a rule. The message is safe to show to the user. */
    public static class ValidationException extends Exception {
        public ValidationException(String message) { super(message); }
    }

    /** Raised when the database cannot complete a request. */
    public static class PaymentException extends Exception {
        public PaymentException(String message, Throwable cause) { super(message, cause); }
    }

    private final String url;
    private final String user;
    private final String password;

    public FeeService(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** Returns the cleaned registration number (trimmed, upper case) or throws. */
    public static String validateRegNo(String raw) throws ValidationException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ValidationException("Registration number is required.");
        }
        String regNo = raw.trim().toUpperCase(Locale.ROOT);
        if (regNo.length() > MAX_REG_NO_LENGTH) {
            throw new ValidationException("Registration number must be at most " + MAX_REG_NO_LENGTH + " characters.");
        }
        if (!regNo.matches("[A-Z0-9]+")) {
            throw new ValidationException("Registration number may contain only letters and digits.");
        }
        return regNo;
    }

    /** Returns the amount rounded to paise, or throws if it is not a valid positive amount. */
    public static double validateAmount(String raw) throws ValidationException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ValidationException("Amount is required.");
        }
        double amount;
        try {
            amount = Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Amount must be a number, for example 2500.00.");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new ValidationException("Amount must be greater than zero.");
        }
        if (amount > MAX_AMOUNT) {
            throw new ValidationException("Amount is larger than the allowed limit of " + MAX_AMOUNT + ".");
        }
        return Math.round(amount * 100.0) / 100.0;
    }

    /** Records a payment. Uses a PreparedStatement and a try-with-resources block. */
    public void payFee(String regNo, double amount) throws PaymentException {
        String sql = "INSERT INTO fees (reg_no, amount, paid_at) VALUES (?, ?, NOW())";
        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, regNo);
            ps.setDouble(2, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PaymentException("Payment could not be recorded. Please try again.", e);
        }
    }

    /** Builds the latest receipt text for a registration number. */
    public String buildReceipt(String regNo) throws PaymentException {
        String sql = "SELECT amount, paid_at FROM fees WHERE reg_no = ? ORDER BY paid_at DESC LIMIT 1";
        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, regNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return "No payment found for " + regNo + ".";
                }
                return "Receipt for " + regNo + "\n"
                        + "Amount paid: Rs " + String.format(Locale.ROOT, "%.2f", rs.getDouble("amount")) + "\n"
                        + "Paid at: " + rs.getTimestamp("paid_at");
            }
        } catch (SQLException e) {
            throw new PaymentException("Receipt could not be loaded. Please try again.", e);
        }
    }
}
