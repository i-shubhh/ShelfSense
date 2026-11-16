package exception;

/** Thrown when an ISBN does not contain exactly 13 digits. */
public class InvalidIsbnException extends LibraryException {
    private static final long serialVersionUID = 1L;

    public InvalidIsbnException(String message) {
        super(message);
    }
}
