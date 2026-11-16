package service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import model.LibraryItem;
import model.Member;
import model.Transaction;

/**
 * Service managing transaction queries, filtering, and reporting.
 */
public class TransactionService {

    public TransactionService() {
    }

    public Transaction findById(List<Transaction> transactions, String txnId) {
        if (txnId == null || transactions == null) return null;
        String query = txnId.trim();
        for (Transaction t : transactions) {
            if (t.getTransactionId().equalsIgnoreCase(query)) {
                return t;
            }
        }
        return null;
    }

    public List<Transaction> findByMember(List<Transaction> transactions, int memberId) {
        List<Transaction> result = new ArrayList<>();
        if (transactions == null) return result;
        for (Transaction t : transactions) {
            if (t.getMemberId() == memberId) {
                result.add(t);
            }
        }
        return result;
    }

    public List<Transaction> findActiveByMember(List<Transaction> transactions, int memberId) {
        List<Transaction> result = new ArrayList<>();
        if (transactions == null) return result;
        for (Transaction t : transactions) {
            if (t.getMemberId() == memberId && !t.isReturned()) {
                result.add(t);
            }
        }
        return result;
    }

    public List<Transaction> filter(List<Transaction> transactions, String query, String statusFilter,
                                   String fineFilter, Map<Integer, Member> memberMap,
                                   Map<Integer, LibraryItem> itemMap) {
        List<Transaction> result = new ArrayList<>();
        if (transactions == null) return result;

        String q = (query != null) ? query.trim().toLowerCase() : "";

        for (Transaction t : transactions) {
            // 1. Status Filter
            if (statusFilter != null && !statusFilter.equalsIgnoreCase("ALL")) {
                if (statusFilter.equalsIgnoreCase("ISSUED") && t.isReturned()) {
                    continue;
                } else if (statusFilter.equalsIgnoreCase("RETURNED") && !t.isReturned()) {
                    continue;
                } else if (statusFilter.equalsIgnoreCase("OVERDUE")
                        && (!Transaction.OVERDUE.equalsIgnoreCase(t.getStatus())
                        && !Transaction.POSSIBLY_LOST.equalsIgnoreCase(t.getStatus()))) {
                    continue;
                } else if (statusFilter.equalsIgnoreCase("POSSIBLY LOST")
                        && !Transaction.POSSIBLY_LOST.equalsIgnoreCase(t.getStatus())) {
                    continue;
                }
            }

            // 2. Fine Filter
            if (fineFilter != null && !fineFilter.equalsIgnoreCase("ALL")) {
                if (fineFilter.equalsIgnoreCase("FINE OWED") && (t.getFine() <= 0 || t.isFinePaid())) {
                    continue;
                } else if (fineFilter.equalsIgnoreCase("FINE PAID") && (t.getFine() <= 0 || !t.isFinePaid())) {
                    continue;
                }
            }

            // 3. Search Query
            if (!q.isEmpty()) {
                Member m = memberMap != null ? memberMap.get(t.getMemberId()) : null;
                LibraryItem it = itemMap != null ? itemMap.get(t.getItemId()) : null;

                boolean matches = t.getTransactionId().toLowerCase().contains(q)
                        || (m != null && (m.getName().toLowerCase().contains(q) || m.getCode().toLowerCase().contains(q)))
                        || (it != null && (it.getTitle().toLowerCase().contains(q) || it.generateItemCode().toLowerCase().contains(q)));

                if (!matches) {
                    continue;
                }
            }

            result.add(t);
        }
        return result;
    }

    public List<Transaction> getRecentTransactions(List<Transaction> transactions, int limit) {
        List<Transaction> copy = new ArrayList<>(transactions);
        Collections.reverse(copy);
        if (copy.size() <= limit) {
            return copy;
        }
        return copy.subList(0, limit);
    }
}
