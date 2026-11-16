package exception;

/**
 * Common parent of every user-defined exception in ShelfSense.
 * It is a checked exception, so callers must catch it or declare it with throws.
 */
public class LibraryException extends Exception {
    private static final long serialVersionUID = 1L;

    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
