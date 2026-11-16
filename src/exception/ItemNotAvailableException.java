package exception;

/** Thrown when an item with zero available copies is requested. */
public class ItemNotAvailableException extends LibraryException {
    private static final long serialVersionUID = 1L;

    public ItemNotAvailableException(String message) {
        super(message);
    }
}
