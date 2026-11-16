package exception;

/** Thrown when a member tries to borrow more items than the category allows. */
public class BorrowLimitExceededException extends LibraryException {
    private static final long serialVersionUID = 1L;

    public BorrowLimitExceededException(String message) {
        super(message);
    }
}
