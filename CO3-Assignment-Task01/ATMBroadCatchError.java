// Deliberately WRONG order, used to show the compiler error for a broad catch placed before a narrow one.
public class ATMBroadCatchError {
    public static void main(String[] args) {
        try {
            long amount = Long.parseLong("abc");
        } catch (Exception e) {                    // broad catch first
            System.out.println("broad: " + e.getMessage());
        } catch (NumberFormatException e) {        // narrower catch after broader one -> compile error
            System.out.println("narrow: " + e.getMessage());
        }
    }
}
