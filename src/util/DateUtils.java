package util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility functions for Java Date and Time API operations.
 */
public final class DateUtils {

    public static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter SHORT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DateUtils() {
    }

    public static String formatDisplay(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return date.format(DISPLAY_FMT);
    }

    public static String formatIso(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(ISO_FMT);
    }

    public static LocalDate parseIso(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(text.trim(), ISO_FMT);
    }

    /**
     * Calculates overdue days between due date and target date (defaults to today).
     * Returns 0 if target is before or on due date.
     */
    public static int calculateOverdueDays(LocalDate dueDate, LocalDate asOf) {
        if (dueDate == null || asOf == null) {
            return 0;
        }
        if (asOf.isAfter(dueDate)) {
            return (int) ChronoUnit.DAYS.between(dueDate, asOf);
        }
        return 0;
    }

    public static int calculateOverdueDays(LocalDate dueDate) {
        return calculateOverdueDays(dueDate, LocalDate.now());
    }

    public static boolean isOverdue(LocalDate dueDate, LocalDate asOf) {
        return calculateOverdueDays(dueDate, asOf) > 0;
    }

    public static boolean isOverdue(LocalDate dueDate) {
        return isOverdue(dueDate, LocalDate.now());
    }
}

