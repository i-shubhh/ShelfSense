package util;

import java.awt.Font;
import java.text.NumberFormat;
import java.util.Locale;

/** Formats all monetary values using Indian Rupees and Indian grouping rules. */
public final class CurrencyUtils {

    private static final Locale INDIA = new Locale("en", "IN");
    private static final String RUPEE_SYMBOL = "₹";
    private static final String CURRENCY_PREFIX = supportsRupeeSymbol() ? RUPEE_SYMBOL : "Rs. ";

    private CurrencyUtils() {
    }

    /** Uses an ASCII-compatible prefix when the runtime font cannot display ₹. */
    public static String symbol() {
        return CURRENCY_PREFIX;
    }

    public static String format(double amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(INDIA);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return CURRENCY_PREFIX + formatter.format(amount);
    }

    private static boolean supportsRupeeSymbol() {
        return new Font("Dialog", Font.PLAIN, 12).canDisplay(RUPEE_SYMBOL.charAt(0));
    }
}
