import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Checked: the caller must handle it. */
class InsufficientBalanceException extends Exception {
    InsufficientBalanceException(String message) { super(message); }
}

/** Checked: unknown account. */
class AccountNotFoundException extends Exception {
    AccountNotFoundException(String message) { super(message); }
}

/** Unchecked (IllegalArgumentException subclass): a programming/input rule violation. */
class InvalidAmountException extends IllegalArgumentException {
    InvalidAmountException(String message) { super(message); }
}

public class ATM {

    /** Reads "account balance" lines. A corrupt balance throws NumberFormatException (unchecked). */
    static Map<String, Long> load(BufferedReader reader) throws IOException {
        Map<String, Long> balances = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.trim().split("\\s+");
            balances.put(parts[0], Long.parseLong(parts[1]));
        }
        return balances;
    }

    /**
     * Returns the new balance. Every failure point is listed in the report.
     * finally always runs: it closes the card reader whether or not the withdrawal succeeds.
     */
    static long withdraw(Path cardFile, String accountText, String amountText)
            throws IOException, AccountNotFoundException, InsufficientBalanceException {
        BufferedReader reader = null;
        try {
            reader = Files.newBufferedReader(cardFile);            // NoSuchFileException if missing
            Map<String, Long> balances = load(reader);              // NumberFormatException if corrupt
            long account = Long.parseLong(accountText.trim());      // NumberFormatException if letters
            long amount = Long.parseLong(amountText.trim());        // NumberFormatException if letters
            if (amount <= 0) {
                throw new InvalidAmountException("Amount must be positive: " + amount);
            }
            Long balance = balances.get(String.valueOf(account));
            if (balance == null) {
                throw new AccountNotFoundException("No account " + account);
            }
            if (amount > balance) {
                throw new InsufficientBalanceException("Balance " + balance + " is less than " + amount);
            }
            return balance - amount;
        } finally {
            if (reader != null) {
                reader.close();
                System.out.println("    finally: card reader closed");
            } else {
                System.out.println("    finally: no card reader was opened");
            }
        }
    }

    /** Runs one attempt and reports which catch block handled it. */
    static void attempt(String label, Path card, String account, String amount) {
        System.out.println("TEST " + label + "  input account=\"" + account + "\" amount=\"" + amount + "\" card=" + card.getFileName());
        try {
            long left = withdraw(card, account, amount);
            System.out.println("    OK: dispensed " + amount.trim() + ", balance now " + left);
        } catch (NoSuchFileException e) {                 // most specific IOException first
            System.out.println("    caught NoSuchFileException (checked): card file missing: " + e.getFile());
        } catch (IOException e) {                          // broader IOException after its subclass
            System.out.println("    caught IOException (checked): " + e.getMessage());
        } catch (NumberFormatException | InvalidAmountException e) {   // multi-catch, unchecked
            System.out.println("    caught " + e.getClass().getSimpleName() + " (unchecked): " + e.getMessage());
        } catch (AccountNotFoundException | InsufficientBalanceException e) {  // multi-catch, checked
            System.out.println("    caught " + e.getClass().getSimpleName() + " (checked): " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("uncaught")) {
            // Deliberately unhandled: shows a stack trace.
            try {
                withdraw(Path.of("cards.txt"), "1001", "x");
            } catch (IOException | AccountNotFoundException | InsufficientBalanceException e) {
                System.out.println("not reached");
            }
            return;
        }
        Path good = Path.of("cards.txt");
        Path corrupt = Path.of("cards_corrupt.txt");
        Path missing = Path.of("no_such_card_reader_file.txt");
        attempt("T1 valid", good, "1001", "200");
        attempt("T2 letters in amount", good, "1001", "abc");
        attempt("T3 negative amount", good, "1001", "-50");
        attempt("T4 zero amount", good, "1001", "0");
        attempt("T5 more than balance", good, "1001", "900");
        attempt("T6 unknown account", good, "9999", "10");
        attempt("T7 letters in account", good, "12x", "10");
        attempt("T8 missing card-reader file", missing, "1001", "10");
        attempt("T9 corrupt card file", corrupt, "1002", "10");
    }
}
