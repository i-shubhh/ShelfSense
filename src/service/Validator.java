package service;

import java.time.Year;
import exception.InvalidIsbnException;
import model.Member;

/**
 * Robust input validation utilities.
 * Validates ISBNs, dates, numbers, member phone numbers, and categories.
 */
public final class Validator {

    private Validator() {
    }

    public static void validateIsbn(String isbn) throws InvalidIsbnException {
        if (isbn == null || !isbn.replaceAll("[-\\s]", "").matches("\\d{10}|\\d{13}")) {
            throw new InvalidIsbnException("ISBN must contain 10 or 13 digits (got: "
                    + (isbn == null ? "empty" : "'" + isbn + "'") + ")");
        }
    }

    public static int parseInt(String text, String field) {
        if (text == null || text.trim().isEmpty()) {
            throw new NumberFormatException("Please enter a value for " + field);
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Please enter a valid numeric integer for " + field);
        }
    }

    public static int parsePositive(String text, String field) {
        int v = parseInt(text, field);
        if (v <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return v;
    }

    public static int parseYear(String text, String field) {
        int year = parseInt(text, field);
        int currentYear = Year.now().getValue();
        if (year < 1400 || year > currentYear + 5) {
            throw new IllegalArgumentException(field + " must be between 1400 and " + (currentYear + 5));
        }
        return year;
    }

    public static int parseCode(String text, String field) {
        String digits = text == null ? "" : text.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new NumberFormatException("Please enter a valid " + field);
        }
        return parseInt(digits, field);
    }

    public static String requireText(String text, String field) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return text.trim();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String digits = phone.replaceAll("[\\s-]", "");
        return digits.matches("\\d{10}");
    }

    public static String requirePhone(String phone) {
        if (!isValidPhone(phone)) {
            throw new IllegalArgumentException("Phone number must contain exactly 10 digits");
        }
        return phone.replaceAll("[\\s-]", "").trim();
    }

    public static String requireCategory(String category) {
        if (!Member.isValidCategory(category)) {
            throw new IllegalArgumentException("Invalid member category: " + category);
        }
        return category.trim();
    }
}
