import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Task 5.1 styles (a), (b) and (c). Style (d), the lambda, is in Converter.java.
 * Run with:  java ConverterVariants a|b|c
 *
 * All styles already include the Task 5.2 change: Enter in the amount field also
 * converts, and the result is shown in a dialog box.
 */
public class ConverterVariants {

    static double rate(String currency) {
        return currency.equals("USD") ? 0.012 : 0.011;
    }

    /** Shared UI and conversion logic. Each style differs only in how the listener is attached. */
    abstract static class Base extends JFrame {
        final JTextField amount = new JTextField(10);
        final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
        final JButton convertButton = new JButton("Convert");

        Base(String title) {
            super(title);
            setLayout(new FlowLayout());
            add(new JLabel("INR")); add(amount); add(target); add(convertButton);
            addWindowListener(new WindowAdapter() {          // WindowAdapter: only the method we need
                @Override public void windowClosing(WindowEvent e) { dispose(); }
            });
            pack();
        }

        void convertAndShow() {
            try {
                double inr = Double.parseDouble(amount.getText().trim());
                String currency = (String) target.getSelectedItem();
                String text = String.format("%.2f %s", inr * rate(currency), currency);
                JOptionPane.showMessageDialog(this, text, "Result", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid amount", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** (a) The frame itself implements ActionListener. */
    static class FrameStyle extends Base implements ActionListener {
        FrameStyle() {
            super("Converter - (a) frame implements ActionListener");
            convertButton.addActionListener(this);
            amount.addActionListener(this);
        }
        @Override public void actionPerformed(ActionEvent e) { convertAndShow(); }
    }

    /** (b) A named inner class. */
    static class NamedInnerStyle extends Base {
        NamedInnerStyle() {
            super("Converter - (b) named inner class");
            ConvertHandler handler = new ConvertHandler();
            convertButton.addActionListener(handler);
            amount.addActionListener(handler);
        }
        class ConvertHandler implements ActionListener {
            @Override public void actionPerformed(ActionEvent e) { convertAndShow(); }
        }
    }

    /** (c) An anonymous inner class. */
    static class AnonymousStyle extends Base {
        AnonymousStyle() {
            super("Converter - (c) anonymous inner class");
            ActionListener handler = new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) { convertAndShow(); }
            };
            convertButton.addActionListener(handler);
            amount.addActionListener(handler);
        }
    }

    public static void main(String[] args) {
        String style = args.length > 0 ? args[0].toLowerCase() : "a";
        SwingUtilities.invokeLater(() -> {
            JFrame frame;
            switch (style) {
                case "b": frame = new NamedInnerStyle(); break;
                case "c": frame = new AnonymousStyle(); break;
                default:  frame = new FrameStyle(); break;
            }
            frame.setVisible(true);
        });
    }
}
