import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

public class WindowTrace extends Frame {
    static void log(String s) { System.out.println(s); }
    public WindowTrace() {
        super("trace");
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e)      { log("1 windowOpened"); }
            @Override public void windowActivated(WindowEvent e)   { log("2 windowActivated"); }
            @Override public void windowClosing(WindowEvent e)     { log("3 windowClosing (confirm here)"); dispose(); }
            @Override public void windowClosed(WindowEvent e)      { log("4 windowClosed"); }
            @Override public void windowDeactivated(WindowEvent e) { log("5 windowDeactivated"); }
        });
        setSize(300, 200);
    }
    @Override public void paint(java.awt.Graphics g) { log("   paint()"); }
    public static void main(String[] args) throws Exception {
        WindowTrace w = new WindowTrace();
        log("A setVisible(true) called");
        w.setVisible(true);
        Thread.sleep(800);
        log("B user closes window: simulate close event");
        w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
        Thread.sleep(500);
        System.exit(0);
    }
}
