package service;

import java.time.LocalDate;
import exception.LibraryException;
import model.LibraryItem;
import model.Member;
import model.Transaction;

/**
 * Service managing item check-ins, return processing, and overdue fee calculations.
 */
public class ReturnService {

    private final FineService fineService;

    public ReturnService(FineService fineService) {
        this.fineService = fineService;
    }

    public ReturnService() {
        this(new FineService());
    }

    /**
     * Processes an item return, updating item availability, calculating late fees,
     * and marking the transaction as RETURNED.
     */
    public synchronized ReturnResult processReturn(Transaction txn, LibraryItem item, Member member,
                                                   LocalDate returnDate, boolean payFineNow)
            throws LibraryException {

        if (txn == null) {
            throw new LibraryException("Transaction record not found.");
        }
        if (txn.isReturned()) {
            throw new LibraryException("Transaction " + txn.getTransactionId() + " has already been returned on "
                    + txn.getReturnDate());
        }

        LocalDate actualReturnDate = (returnDate != null) ? returnDate : LocalDate.now();
        if (txn.getIssueDate() != null && actualReturnDate.isBefore(txn.getIssueDate())) {
            throw new LibraryException("Return date cannot be before the issue date.");
        }

        // 1. Calculate overdue days and fine amount
        int overdueDays = fineService.calculateOverdueDays(txn.getDueDate(), actualReturnDate);
        double fine = fineService.calculateFine(item, txn.getDueDate(), actualReturnDate);

        // 2. Update transaction status
        txn.setReturnDate(actualReturnDate);
        txn.setFine(fine);
        txn.setFinePaid(fine <= 0 || payFineNow);
        txn.setStatus(Transaction.RETURNED);

        // 3. Return item to inventory (increments copies & wakes waiting threads)
        if (item != null) {
            item.returnItem(member);
        }

        return new ReturnResult(txn, item, member, overdueDays, fine, txn.isFinePaid());
    }

    /**
     * DTO containing return processing results.
     */
    public static class ReturnResult {
        public final Transaction transaction;
        public final LibraryItem item;
        public final Member member;
        public final int overdueDays;
        public final double fineAssessed;
        public final boolean finePaid;

        public ReturnResult(Transaction transaction, LibraryItem item, Member member,
                            int overdueDays, double fineAssessed, boolean finePaid) {
            this.transaction = transaction;
            this.item = item;
            this.member = member;
            this.overdueDays = overdueDays;
            this.fineAssessed = fineAssessed;
            this.finePaid = finePaid;
        }
    }
}
