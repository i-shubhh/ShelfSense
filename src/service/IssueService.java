package service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import exception.BorrowLimitExceededException;
import exception.ItemNotAvailableException;
import exception.LibraryException;
import model.LibraryItem;
import model.Member;
import model.Transaction;
import util.IDGenerator;

/**
 * Service managing library item issues and circulation validations.
 * Automatically generates unique cryptographically random Transaction IDs.
 */
public class IssueService {

    public IssueService() {
    }

    /**
     * Issues an item to a member with automated validations and unique ID generation.
     *
     * @param item the library item to issue
     * @param member the borrowing member
     * @param activeMemberLoans current active loan count of the member
     * @param issueDate date of issuance (defaults to today)
     * @param existingTxnIds set of existing transaction IDs to ensure absolute uniqueness
     * @return newly created Transaction object with generated ID
     */
    public synchronized Transaction issue(LibraryItem item, Member member, int activeMemberLoans,
                                          LocalDate issueDate, Set<String> existingTxnIds)
            throws LibraryException {

        if (item == null) {
            throw new LibraryException("Selected library item does not exist.");
        }
        if (member == null) {
            throw new LibraryException("Selected library member does not exist.");
        }

        // The borrowing quota is checked before stock so a member at the limit
        // always receives the actionable policy message.
        if (activeMemberLoans >= member.getBorrowLimit()) {
            throw new BorrowLimitExceededException(
                    "ISSUE BLOCKED: You have reached your maximum borrowing limit.");
        }

        // Availability is checked immediately before the synchronized deduction.
        if (item.getAvailableCopies() <= 0) {
            throw new ItemNotAvailableException("ISSUE BLOCKED: " + item.generateItemCode() + " ("
                    + item.getTitle() + ") has 0 available copies. Join the Reservation Queue.");
        }

        // Thread-safe deduction of available copies
        item.issueItem(member);

        // 4. Generate unique random Transaction ID (never manual!)
        String txnId = IDGenerator.generateTransactionId(existingTxnIds);

        // 5. Calculate due date using polymorphism on item's loan days
        LocalDate date = (issueDate != null) ? issueDate : LocalDate.now();
        LocalDate dueDate = date.plusDays(item.getLoanDays());

        // 6. Return new transaction
        return new Transaction(txnId, item.getItemId(), member.getMemberId(), date, dueDate);
    }
}
