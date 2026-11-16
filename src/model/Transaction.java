package model;

import java.time.LocalDate;
import util.CurrencyUtils;
import util.DateUtils;

/**
 * Represents a library circulation transaction.
 * Each transaction has an internally generated unique random Transaction ID (e.g., SHELF-TXN-8F3A92C1).
 */
public class Transaction {

    public static final String ISSUED = "ISSUED";
    public static final String RETURNED = "RETURNED";
    public static final String OVERDUE = "OVERDUE";
    public static final String POSSIBLY_LOST = "POSSIBLY LOST";

    private String transactionId;
    private int itemId;
    private int memberId;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private double fine;
    private boolean finePaid;
    private String status;

    /**
     * Full constructor for storage and persistence restoration.
     */
    public Transaction(String transactionId, int itemId, int memberId, LocalDate issueDate,
                       LocalDate dueDate, LocalDate returnDate, double fine, boolean finePaid, String status) {
        this.transactionId = transactionId;
        this.itemId = itemId;
        this.memberId = memberId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fine = fine;
        this.finePaid = finePaid;
        this.status = status;
    }

    /**
     * Overloaded constructor for newly issued transactions.
     */
    public Transaction(String transactionId, int itemId, int memberId, LocalDate issueDate, LocalDate dueDate) {
        this(transactionId, itemId, memberId, issueDate, dueDate, null, 0.0, false, ISSUED);
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getCode() {
        return transactionId;
    }

    /**
     * Hash code integer representation for backward compatibility.
     */
    public int getTxnId() {
        return Math.abs(transactionId.hashCode());
    }

    public int getItemId() {
        return itemId;
    }

    public int getMemberId() {
        return memberId;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public double getFine() {
        return fine;
    }

    public void setFine(double fine) {
        this.fine = fine;
    }

    public boolean isFinePaid() {
        return finePaid;
    }

    public void setFinePaid(boolean finePaid) {
        this.finePaid = finePaid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isReturned() {
        return RETURNED.equalsIgnoreCase(status) || returnDate != null;
    }

    public boolean isOverdue(LocalDate asOf) {
        if (isReturned()) {
            return false;
        }
        return asOf != null && asOf.isAfter(dueDate);
    }

    public int getOverdueDays(LocalDate asOf) {
        if (isReturned()) {
            if (returnDate != null && returnDate.isAfter(dueDate)) {
                return DateUtils.calculateOverdueDays(dueDate, returnDate);
            }
            return 0;
        }
        return DateUtils.calculateOverdueDays(dueDate, asOf);
    }

    @Override
    public String toString() {
        return transactionId + " [Item: " + itemId + ", Member: " + memberId + ", Issued: "
                + issueDate + ", Due: " + dueDate
                + (returnDate != null ? ", Returned: " + returnDate : "")
                + ", Fine: " + CurrencyUtils.format(fine) + (finePaid ? " (Paid)" : " (Unpaid)")
                + ", Status: " + status + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return transactionId != null ? transactionId.equals(that.transactionId) : that.transactionId == null;
    }

    @Override
    public int hashCode() {
        return transactionId != null ? transactionId.hashCode() : 0;
    }
}
