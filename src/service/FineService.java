package service;

import java.time.LocalDate;
import java.util.List;
import model.LibraryItem;
import model.Transaction;
import util.DateUtils;

/**
 * Late Fee Intelligence Service.
 * Implements automated fine calculations, overdue analysis, and fine ledger tracking.
 */
public class FineService {

    public FineService() {
    }

    /**
     * Calculates overdue days between a due date and the return or audit date.
     */
    public int calculateOverdueDays(LocalDate dueDate, LocalDate asOf) {
        return DateUtils.calculateOverdueDays(dueDate, asOf);
    }

    /**
     * Calculates late fee using polymorphism on LibraryItem subclasses.
     */
    public double calculateFine(LibraryItem item, LocalDate dueDate, LocalDate asOf) {
        if (item == null || dueDate == null || asOf == null) {
            return 0.0;
        }
        int daysLate = calculateOverdueDays(dueDate, asOf);
        if (daysLate <= 0) {
            return 0.0;
        }
        return item.calculateLateFee(daysLate);
    }

    /**
     * Total revenue collected from paid fines across all transactions.
     */
    public double getTotalFinesCollected(List<Transaction> transactions) {
        double total = 0.0;
        if (transactions == null) return total;
        for (Transaction t : transactions) {
            if (t.isFinePaid() && t.getFine() > 0) {
                total += t.getFine();
            }
        }
        return total;
    }

    /**
     * Total outstanding (unpaid) fines across all transactions (active or returned).
     */
    public double getTotalOutstandingFines(List<Transaction> transactions) {
        double total = 0.0;
        if (transactions == null) return total;
        for (Transaction t : transactions) {
            if (!t.isFinePaid() && t.getFine() > 0) {
                total += t.getFine();
            }
        }
        return total;
    }

    /**
     * Total fines assessed (paid + unpaid).
     */
    public double getTotalFinesAssessed(List<Transaction> transactions) {
        double total = 0.0;
        if (transactions == null) return total;
        for (Transaction t : transactions) {
            if (t.getFine() > 0) {
                total += t.getFine();
            }
        }
        return total;
    }

    /**
     * Marks a transaction's fine as paid.
     */
    public void markFinePaid(Transaction txn) {
        if (txn != null) {
            txn.setFinePaid(true);
        }
    }
}

