/** Tests the validation rules. These need no database and no window. */
public class FeeServiceTest {

    private static int passed = 0;
    private static int failed = 0;

    private interface Action { Object run() throws FeeService.ValidationException; }

    public static void main(String[] args) {
        expectRegNo("  tn10eb1234 ", "TN10EB1234");
        expectRegNoError("", "required");
        expectRegNoError("AB-12", "letters and digits");
        expectRegNoError("ABCDEFGHIJKLMNOP", "at most");

        expectAmount("2500", 2500.0);
        expectAmount("10.555", 10.56);
        expectAmountError("", "required");
        expectAmountError("abc", "must be a number");
        expectAmountError("0", "greater than zero");
        expectAmountError("-5", "greater than zero");
        expectAmountError("NaN", "greater than zero");
        expectAmountError("2000000", "limit");

        System.out.println("Passed: " + passed + "  Failed: " + failed);
    }

    private static void expectRegNo(String input, String expected) {
        try {
            String got = FeeService.validateRegNo(input);
            check("regNo '" + input + "'", got.equals(expected), expected + " but got " + got);
        } catch (FeeService.ValidationException e) {
            check("regNo '" + input + "'", false, expected + " but got: " + e.getMessage());
        }
    }

    private static void expectRegNoError(String input, String fragment) {
        expectError("regNo '" + input + "'", fragment, () -> FeeService.validateRegNo(input));
    }

    private static void expectAmount(String input, double expected) {
        try {
            double got = FeeService.validateAmount(input);
            check("amount '" + input + "'", Math.abs(got - expected) < 1e-9, expected + " but got " + got);
        } catch (FeeService.ValidationException e) {
            check("amount '" + input + "'", false, expected + " but got: " + e.getMessage());
        }
    }

    private static void expectAmountError(String input, String fragment) {
        expectError("amount '" + input + "'", fragment, () -> FeeService.validateAmount(input));
    }

    private static void expectError(String label, String fragment, Action action) {
        try {
            action.run();
            check(label, false, "a validation error containing '" + fragment + "'");
        } catch (FeeService.ValidationException e) {
            check(label, e.getMessage().toLowerCase().contains(fragment.toLowerCase()),
                  "message containing '" + fragment + "' but got: " + e.getMessage());
        }
    }

    private static void check(String label, boolean ok, String expectation) {
        if (ok) {
            passed++;
            System.out.println("PASS  " + label);
        } else {
            failed++;
            System.out.println("FAIL  " + label + "  (expected " + expectation + ")");
        }
    }
}
