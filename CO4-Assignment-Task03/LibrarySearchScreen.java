import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Campus library search screen.
 *
 * Layout structure (see report, Task 3.2):
 *   JFrame (BorderLayout)
 *     NORTH   : JPanel (BorderLayout)     - title bar + search row
 *       NORTH : JLabel (title)
 *       CENTER: JPanel (FlowLayout)       - text field, Choice, Search button
 *     CENTER  : JScrollPane > JTextArea   - results (grows on resize)
 *     SOUTH   : JPanel (BorderLayout)     - buttons + status
 *       CENTER: JPanel (FlowLayout)       - Prev, Next, Reserve
 *       SOUTH : JLabel                    - status message
 */
public class LibrarySearchScreen extends JFrame {

    private static final int PAGE_SIZE = 5;

    // Sample catalogue: {category, title}
    private static final String[][] CATALOGUE = {
        {"Computer Science", "Introduction to Java Programming"},
        {"Computer Science", "Data Structures in Java"},
        {"Computer Science", "Operating System Concepts"},
        {"Computer Science", "Database System Concepts"},
        {"Computer Science", "Computer Networks"},
        {"Mathematics", "Engineering Mathematics Vol 1"},
        {"Mathematics", "Discrete Mathematics"},
        {"Mathematics", "Probability and Statistics"},
        {"Physics", "Engineering Physics"},
        {"Physics", "Modern Physics"},
        {"Literature", "English Grammar and Composition"},
        {"Literature", "A Brief History of Time"}
    };

    private final JTextField searchField = new JTextField(20);
    private final Choice category = new Choice();
    private final JButton searchButton = new JButton("Search");
    private final JTextArea results = new JTextArea();
    private final JButton prevButton = new JButton("Prev");
    private final JButton nextButton = new JButton("Next");
    private final JButton reserveButton = new JButton("Reserve");
    private final JLabel status = new JLabel("Enter a search term and press Search.");

    private String[] currentMatches = new String[0];
    private int page = 0;

    public LibrarySearchScreen() {
        super("Campus Library Search");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // ----- Top: title bar and search row -----
        JLabel title = new JLabel("Campus Library Search", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(18f));
        title.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        category.add("All");
        for (String c : new String[]{"Computer Science", "Mathematics", "Physics", "Literature"}) {
            category.add(c);
        }
        searchRow.add(new JLabel("Search:"));
        searchRow.add(searchField);
        searchRow.add(category);
        searchRow.add(searchButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(title, BorderLayout.NORTH);
        top.add(searchRow, BorderLayout.CENTER);

        // ----- Centre: results area -----
        results.setEditable(false);
        results.setLineWrap(true);
        results.setWrapStyleWord(true);
        results.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JScrollPane resultScroll = new JScrollPane(results);
        resultScroll.setPreferredSize(new Dimension(600, 240));

        // ----- Bottom: button row and status message -----
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonRow.add(prevButton);
        buttonRow.add(nextButton);
        buttonRow.add(reserveButton);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(buttonRow, BorderLayout.CENTER);
        bottom.add(status, BorderLayout.SOUTH);
        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));

        // Frame layout
        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(resultScroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // ----- Listeners: one per control -----
        searchButton.addActionListener(e -> runSearch());
        searchField.addActionListener(e -> runSearch());   // Enter key also searches
        prevButton.addActionListener(e -> changePage(-1));
        nextButton.addActionListener(e -> changePage(1));
        reserveButton.addActionListener(e -> reserveItem());

        prevButton.setEnabled(false);
        nextButton.setEnabled(false);
        reserveButton.setEnabled(false);

        pack();
        setLocationRelativeTo(null);
    }

    private void runSearch() {
        String term = searchField.getText().trim().toLowerCase();
        String chosen = category.getSelectedItem();
        if (term.isEmpty()) {
            status.setText("Please type a search term.");
            return;
        }
        java.util.List<String> found = new java.util.ArrayList<>();
        for (String[] item : CATALOGUE) {
            boolean categoryOk = chosen.equals("All") || item[0].equals(chosen);
            if (categoryOk && item[1].toLowerCase().contains(term)) {
                found.add(item[1]);
            }
        }
        currentMatches = found.toArray(new String[0]);
        page = 0;
        showPage();
        status.setText(currentMatches.length + " item(s) found for \"" + term + "\" in " + chosen + ".");
    }

    private void changePage(int delta) {
        page += delta;
        showPage();
    }

    private void showPage() {
        StringBuilder sb = new StringBuilder();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, currentMatches.length);
        for (int i = start; i < end; i++) {
            sb.append(i + 1).append(". ").append(currentMatches[i]).append('\n');
        }
        if (currentMatches.length == 0) {
            sb.append("No results.");
        }
        results.setText(sb.toString());
        prevButton.setEnabled(page > 0);
        nextButton.setEnabled(end < currentMatches.length);
        reserveButton.setEnabled(currentMatches.length > 0);
    }

    private void reserveItem() {
        String answer = JOptionPane.showInputDialog(this, "Enter the item number to reserve:");
        if (answer == null) {
            return;   // cancelled
        }
        try {
            int n = Integer.parseInt(answer.trim());
            if (n < 1 || n > currentMatches.length) {
                status.setText("Item number must be between 1 and " + currentMatches.length + ".");
                return;
            }
            status.setText("Reserved: " + currentMatches[n - 1]);
        } catch (NumberFormatException ex) {
            status.setText("Please enter a whole number.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibrarySearchScreen().setVisible(true));
    }
}
