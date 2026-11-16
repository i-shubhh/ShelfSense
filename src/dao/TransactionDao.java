package dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import model.Transaction;
import storage.DataManager;

/**
 * Data Access Object for Transaction entities using Core Java File I/O.
 */
public class TransactionDao {

    private final DataManager dataManager;

    public TransactionDao(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public TransactionDao() {
        this(new DataManager());
    }

    public synchronized void insert(Transaction t) throws IOException {
        List<Transaction> list = findAll();
        list.removeIf(item -> item.getTransactionId().equals(t.getTransactionId()));
        list.add(t);
        dataManager.saveTransactions(list);
    }

    public synchronized List<Transaction> findAll() {
        return dataManager.loadTransactions();
    }

    public synchronized Transaction findById(String transactionId) {
        if (transactionId == null) return null;
        for (Transaction t : findAll()) {
            if (t.getTransactionId().equalsIgnoreCase(transactionId.trim())) {
                return t;
            }
        }
        return null;
    }

    public synchronized void update(Transaction t) throws IOException {
        insert(t);
    }

    public synchronized void updateStatus(String txnId, String status) throws IOException {
        Transaction t = findById(txnId);
        if (t != null) {
            t.setStatus(status);
            update(t);
        }
    }

    public synchronized void delete(String txnId) throws IOException {
        List<Transaction> list = findAll();
        list.removeIf(t -> t.getTransactionId().equalsIgnoreCase(txnId.trim()));
        dataManager.saveTransactions(list);
    }

    public synchronized boolean exists(String txnId) {
        return findById(txnId) != null;
    }

    public synchronized int count() {
        return findAll().size();
    }
}
