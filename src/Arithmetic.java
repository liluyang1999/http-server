import java.util.Locale;
import java.util.Set;

/** Pure finite arithmetic: failures are part of the API contract, not server errors. */
public final class Arithmetic {
    public static final Set<String> OPERATIONS = Set.of("add", "subtract", "multiply", "divide");
    private Arithmetic() {}
    public static String operation(String value) {
        if (value == null) throw new IllegalArgumentException("operation is required");
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!OPERATIONS.contains(normalized)) throw new IllegalArgumentException("unknown operation");
        return normalized;
    }
    public static double calculate(String operation, double left, double right) {
        if (!Double.isFinite(left) || !Double.isFinite(right)) throw new IllegalArgumentException("arguments must be finite");
        double value = switch (operation(operation)) {
            case "add" -> left + right;
            case "subtract" -> left - right;
            case "multiply" -> left * right;
            case "divide" -> {
                if (right == 0) throw new IllegalArgumentException("division by zero");
                yield left / right;
            }
            default -> throw new IllegalStateException("unreachable operation");
        };
        if (!Double.isFinite(value)) throw new IllegalArgumentException("result is outside finite double range");
        return value;
    }
}
