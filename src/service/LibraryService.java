package service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import exception.BorrowLimitExceededException;
import exception.ItemNotAvailableException;
import exception.LibraryException;
import io.BackupManager;
import model.Book;
import model.LibraryItem;
import model.Member;
import model.Transaction;
import storage.DataManager;
import util.CurrencyUtils;

/**
 * Core Facade and Business Service for ShelfSense.
 * Coordinates all circulation, fine intelligence, persistence, and collections.
 * All public mutating methods are synchronized to provide complete thread safety.
 */
public class LibraryService {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final File BACKUP_FILE = new File("library_backup.dat");

    private final DataManager dataManager;
    private final IssueService issueService;
    private final ReturnService returnService;
    private final FineService fineService;
    private final TransactionService transactionService;

    // Collections
    private final List<LibraryItem> catalogue = new ArrayList<>();
    private final Map<Integer, LibraryItem> itemsById = new HashMap<>();
    private List<LibraryItem> catalogueSnapshot;
    private final Set<String> isbns = new HashSet<>();
    private final Set<String> titles = new TreeSet<>();
    private final Map<Integer, Member> members = new HashMap<>();
    private List<Member> membersSnapshot;
    private final Map<Integer, LinkedList<Member>> reservations = new HashMap<>();
    private final Vector<String> activityLog = new Vector<>();
    private final List<Transaction> allTxns = new ArrayList<>();
    private final Map<Integer, Integer> activeLoanCounts = new HashMap<>();
    private int activeLoanTotal;

    public LibraryService(DataManager dataManager) {
        this.dataManager = dataManager;
        this.fineService = new FineService();
        this.issueService = new IssueService();
        this.returnService = new ReturnService(this.fineService);
        this.transactionService = new TransactionService();
    }

    public LibraryService() {
        this(new DataManager());
    }

    // ------------------------------------------------------------ START-UP

    public synchronized void initialize() throws IOException {
        reload();
        if (catalogue.isEmpty() && members.isEmpty() && allTxns.isEmpty()) {
            dataManager.seedDefaultData(catalogue, new ArrayList<Member>(), allTxns);
            reload();
        }
        scanOverdue(LocalDate.now());
        log("ShelfSense initialized successfully. Items: " + catalogue.size() + ", Members: "
                + members.size() + ", Transactions: " + allTxns.size());
    }

    public synchronized void reload() {
        catalogue.clear();
        itemsById.clear();
        catalogueSnapshot = null;
        isbns.clear();
        titles.clear();
        members.clear();
        membersSnapshot = null;
        allTxns.clear();
        activeLoanCounts.clear();
        activeLoanTotal = 0;

        catalogue.addAll(dataManager.loadItems());
        for (LibraryItem it : catalogue) {
            itemsById.put(it.getItemId(), it);
            titles.add(it.getTitle());
            if (!it.getIsbn().isEmpty()) {
                isbns.add(it.getIsbn());
            }
        }

        for (Member m : dataManager.loadMembers()) {
            members.put(m.getMemberId(), m);
        }

        allTxns.addAll(dataManager.loadTransactions());
        for (Transaction txn : allTxns) {
            if (!txn.isReturned()) {
                activeLoanCounts.merge(txn.getMemberId(), 1, Integer::sum);
                activeLoanTotal++;
            }
        }

        reservations.clear();
        for (Map.Entry<Integer, List<Integer>> entry : dataManager.loadReservations().entrySet()) {
            if (findItem(entry.getKey()) == null) {
                continue;
            }
            LinkedList<Member> queue = new LinkedList<>();
            for (Integer memberId : entry.getValue()) {
                Member member = findMember(memberId);
                if (member != null && !queue.contains(member)) {
                    queue.add(member);
                }
            }
            if (!queue.isEmpty()) {
                reservations.put(entry.getKey(), queue);
            }
        }
    }

    public synchronized void persistAll() throws IOException {
        dataManager.saveItems(catalogue);
        dataManager.saveMembers(members.values());
        dataManager.saveTransactions(allTxns);
        dataManager.saveReservations(reservations);
    }

    // --------------------------------------------------------------- ITEMS

    public synchronized void addItem(LibraryItem item) throws LibraryException, IOException {
        if (item == null) {
            throw new LibraryException("No item supplied");
        }
        if (findItem(item.getItemId()) != null) {
            throw new LibraryException("Duplicate item ID: " + item.getItemId());
        }
        if (!item.getIsbn().isEmpty() && isbns.contains(item.getIsbn())) {
            throw new LibraryException("An item with ISBN " + item.getIsbn() + " already exists in the catalogue.");
        }

        catalogue.add(item);
        itemsById.put(item.getItemId(), item);
        catalogueSnapshot = null;
        titles.add(item.getTitle());
        if (!item.getIsbn().isEmpty()) {
            isbns.add(item.getIsbn());
        }

        dataManager.saveItems(catalogue);
        log("Added item " + item.generateItemCode() + " - \"" + item.getTitle() + "\" (" + item.getType() + ")");
    }

    public synchronized void updateItem(LibraryItem updated) throws LibraryException, IOException {
        if (updated == null) {
            throw new LibraryException("No item supplied for update");
        }
        LibraryItem existing = findItem(updated.getItemId());
        if (existing == null) {
            throw new LibraryException("Item not found: " + updated.getItemId());
        }

        int index = catalogue.indexOf(existing);
        if (index >= 0) {
            catalogue.set(index, updated);
            itemsById.put(updated.getItemId(), updated);
            catalogueSnapshot = null;
            titles.remove(existing.getTitle());
            titles.add(updated.getTitle());
            if (!existing.getIsbn().isEmpty()) {
                isbns.remove(existing.getIsbn());
            }
            if (!updated.getIsbn().isEmpty()) {
                isbns.add(updated.getIsbn());
            }
            dataManager.saveItems(catalogue);
            log("Updated item " + updated.generateItemCode() + " - \"" + updated.getTitle() + "\"");
        }
    }

    public synchronized void deleteItem(int itemId) throws LibraryException, IOException {
        LibraryItem item = findItem(itemId);
        if (item == null) {
            throw new LibraryException("Item ID " + itemId + " not found.");
        }

        // Check if currently checked out
        for (Transaction t : allTxns) {
            if (t.getItemId() == itemId && !t.isReturned()) {
                throw new LibraryException("Cannot delete item " + item.generateItemCode()
                        + " because it is currently issued in transaction " + t.getTransactionId());
            }
        }

        catalogue.remove(item);
        itemsById.remove(itemId);
        catalogueSnapshot = null;
        titles.remove(item.getTitle());
        if (!item.getIsbn().isEmpty()) {
            isbns.remove(item.getIsbn());
        }

        dataManager.saveItems(catalogue);
        log("Deleted item " + item.generateItemCode() + " - \"" + item.getTitle() + "\"");
    }

    public synchronized LibraryItem findItem(int itemId) {
        return itemsById.get(itemId);
    }

    public synchronized LibraryItem findItemByCode(String code) {
        if (code == null) return null;
        String query = code.trim().toLowerCase();
        for (LibraryItem it : catalogue) {
            if (it.generateItemCode().toLowerCase().equals(query)
                    || String.valueOf(it.getItemId()).equals(query)) {
                return it;
            }
        }
        return null;
    }

    public synchronized List<LibraryItem> searchItems(String query, String typeFilter, String availabilityFilter) {
        List<LibraryItem> results = new ArrayList<>();
        String q = (query != null) ? query.trim().toLowerCase() : "";

        if (q.isEmpty() && (typeFilter == null || typeFilter.equalsIgnoreCase("ALL"))
                && (availabilityFilter == null || availabilityFilter.equalsIgnoreCase("ALL"))) {
            return getCatalogue();
        }

        for (LibraryItem it : catalogue) {
            // Type filter
            if (typeFilter != null && !typeFilter.equalsIgnoreCase("ALL")
                    && !it.getType().equalsIgnoreCase(typeFilter)) {
                continue;
            }

            // Availability filter
            if (availabilityFilter != null && !availabilityFilter.equalsIgnoreCase("ALL")) {
                if (availabilityFilter.equalsIgnoreCase("AVAILABLE") && it.getAvailableCopies() <= 0) {
                    continue;
                } else if (availabilityFilter.equalsIgnoreCase("ISSUED") && it.getAvailableCopies() >= it.getTotalCopies()) {
                    continue;
                }
            }

            // Keyword search
            if (!q.isEmpty()) {
                boolean matches = it.getTitle().toLowerCase().contains(q)
                        || it.generateItemCode().toLowerCase().contains(q)
                        || it.getAuthor().toLowerCase().contains(q)
                        || it.getIsbn().toLowerCase().contains(q);
                if (!matches) {
                    continue;
                }
            }

            results.add(it);
        }
        return results;
    }

    public synchronized List<LibraryItem> getCatalogue() {
        if (catalogueSnapshot == null) {
            catalogueSnapshot = Collections.unmodifiableList(new ArrayList<>(catalogue));
        }
        return catalogueSnapshot;
    }

    // ------------------------------------------------------------- MEMBERS

    public synchronized void registerMember(Member member) throws LibraryException, IOException {
        if (member == null) {
            throw new LibraryException("No member supplied");
        }
        if (members.containsKey(member.getMemberId())) {
            throw new LibraryException("Duplicate member ID: " + member.getMemberId());
        }
        members.put(member.getMemberId(), member);
        membersSnapshot = null;
        dataManager.saveMembers(members.values());
        log("Registered member " + member.getCode() + " - " + member.getName() + " (" + member.getCategory() + ")");
    }

    public synchronized void updateMember(Member updated) throws LibraryException, IOException {
        if (updated == null) {
            throw new LibraryException("No member supplied");
        }
        if (!members.containsKey(updated.getMemberId())) {
            throw new LibraryException("Member ID " + updated.getMemberId() + " not found");
        }
        members.put(updated.getMemberId(), updated);
        membersSnapshot = null;
        dataManager.saveMembers(members.values());
        log("Updated member " + updated.getCode() + " - " + updated.getName());
    }

    public synchronized void deleteMember(int memberId) throws LibraryException, IOException {
        Member m = members.get(memberId);
        if (m == null) {
            throw new LibraryException("Member ID " + memberId + " not found.");
        }

        // Check if member has active loans
        List<Transaction> active = getMemberActiveIssues(memberId);
        if (!active.isEmpty()) {
            throw new LibraryException("Cannot delete member " + m.getCode() + " because they currently have "
                    + active.size() + " unreturned item(s).");
        }

        members.remove(memberId);
        membersSnapshot = null;
        dataManager.saveMembers(members.values());
        log("Deleted member " + m.getCode() + " - " + m.getName());
    }

    public synchronized Member findMember(int memberId) {
        return members.get(memberId);
    }

    public synchronized Member findMemberByCode(String code) {
        if (code == null) return null;
        String query = code.trim().toLowerCase();
        for (Member m : members.values()) {
            if (m.getCode().toLowerCase().equals(query) || String.valueOf(m.getMemberId()).equals(query)) {
                return m;
            }
        }
        return null;
    }

    public synchronized List<Member> searchMembers(String query, String categoryFilter) {
        List<Member> results = new ArrayList<>();
        String q = (query != null) ? query.trim().toLowerCase() : "";

        if (q.isEmpty() && (categoryFilter == null || categoryFilter.equalsIgnoreCase("ALL"))) {
            return getMembersSorted();
        }

        for (Member m : members.values()) {
            if (categoryFilter != null && !categoryFilter.equalsIgnoreCase("ALL")
                    && !m.getCategory().equalsIgnoreCase(categoryFilter)) {
                continue;
            }
            if (!q.isEmpty()) {
                boolean matches = m.getName().toLowerCase().contains(q)
                        || m.getCode().toLowerCase().contains(q)
                        || m.getPhone().contains(q);
                if (!matches) {
                    continue;
                }
            }
            results.add(m);
        }

        results.sort(Comparator.comparingInt(Member::getMemberId));
        return results;
    }

    public synchronized List<Member> getMembersSorted() {
        if (membersSnapshot == null) {
            List<Member> list = new ArrayList<>(members.values());
            list.sort(Comparator.comparingInt(Member::getMemberId));
            membersSnapshot = Collections.unmodifiableList(list);
        }
        return membersSnapshot;
    }

    public synchronized List<Transaction> getMemberActiveIssues(int memberId) {
        return transactionService.findActiveByMember(allTxns, memberId);
    }

    public synchronized List<Transaction> getMemberHistory(int memberId) {
        scanOverdue(LocalDate.now());
        return transactionService.findByMember(allTxns, memberId);
    }

    // --------------------------------------------------- ISSUE & RETURN

    public synchronized Transaction issueItem(int itemId, int memberId) throws LibraryException, IOException {
        return issueItem(itemId, memberId, LocalDate.now());
    }

    public synchronized Transaction issueItem(int itemId, int memberId, LocalDate issueDate)
            throws LibraryException, IOException {

        LibraryItem item = findItem(itemId);
        if (item == null) {
            throw new LibraryException("Library item with ID " + itemId + " does not exist.");
        }
        Member member = findMember(memberId);
        if (member == null) {
            throw new LibraryException("Library member with ID " + memberId + " does not exist.");
        }

        LinkedList<Member> queue = reservations.get(itemId);
        if (queue != null && !queue.isEmpty() && !queue.peek().equals(member)) {
            throw new LibraryException("ISSUE BLOCKED: Item is reserved for " + queue.peek().getName()
                    + " (" + queue.peek().getCode() + ").");
        }

        Set<String> existingIds = getAllTxnIds();
        int activeLoans = getActiveLoanCount(memberId);

        Transaction txn = issueService.issue(item, member, activeLoans, issueDate, existingIds);
        allTxns.add(txn);
        activeLoanCounts.merge(memberId, 1, Integer::sum);
        activeLoanTotal++;

        // Persist both item copy count and new transaction
        dataManager.saveItems(catalogue);
        dataManager.saveTransactions(allTxns);

        if (queue != null && !queue.isEmpty() && queue.peek().equals(member)) {
            queue.poll();
            if (queue.isEmpty()) {
                reservations.remove(itemId);
            }
            try {
                dataManager.saveReservations(reservations);
            } catch (IOException e) {
                log("Reservation persistence error after issue: " + e.getMessage());
            }
        }

        log("Issued " + item.generateItemCode() + " to " + member.getCode() + " [" + txn.getTransactionId()
                + "] Due: " + txn.getDueDate());
        return txn;
    }

    public synchronized ReturnService.ReturnResult returnItem(String txnId, LocalDate returnDate, boolean payFineNow)
            throws LibraryException, IOException {

        Transaction txn = findTransaction(txnId);
        if (txn == null) {
            throw new LibraryException("Transaction with ID \"" + txnId + "\" does not exist.");
        }

        LibraryItem item = findItem(txn.getItemId());
        Member member = findMember(txn.getMemberId());

        ReturnService.ReturnResult result = returnService.processReturn(txn, item, member, returnDate, payFineNow);

        int activeCount = activeLoanCounts.getOrDefault(txn.getMemberId(), 0);
        if (activeCount > 0) {
            activeLoanCounts.put(txn.getMemberId(), activeCount - 1);
            activeLoanTotal--;
        }

        // Persist updated records
        dataManager.saveItems(catalogue);
        dataManager.saveTransactions(allTxns);

        log("Returned " + (item != null ? item.generateItemCode() : "item") + " from "
                + (member != null ? member.getCode() : "member") + " [" + txn.getTransactionId()
                + "] Fine: " + CurrencyUtils.format(result.fineAssessed)
                + (result.finePaid ? " (Paid)" : " (Pending)"));

        // Keep the head of the FIFO queue reserved and indicate that the copy is ready.
        checkReservationQueue(item);
        Member nextMember = getReservationHead(item != null ? item.getItemId() : -1);
        if (nextMember != null && item != null && item.getAvailableCopies() > 0) {
            log("Next reservation ready: " + nextMember.getCode() + " - " + nextMember.getName()
                    + " may borrow " + item.generateItemCode() + ".");
        }
        try {
            dataManager.saveReservations(reservations);
        } catch (IOException e) {
            log("Reservation persistence error after return: " + e.getMessage());
        }

        return result;
    }

    public synchronized ReturnService.ReturnResult returnItem(String txnId) throws LibraryException, IOException {
        return returnItem(txnId, LocalDate.now(), false);
    }

    /** Legacy int txnId support for tests */
    public synchronized ReturnService.ReturnResult returnItem(int txnHash, LocalDate returnDate)
            throws LibraryException, IOException {
        for (Transaction t : allTxns) {
            if (t.getTxnId() == txnHash) {
                return returnItem(t.getTransactionId(), returnDate, false);
            }
        }
        throw new LibraryException("Transaction with ID hash " + txnHash + " not found.");
    }

    // ---------------------------------------------------- FINES & AUDIT

    public synchronized void payFine(String txnId) throws LibraryException, IOException {
        Transaction txn = findTransaction(txnId);
        if (txn == null) {
            throw new LibraryException("Transaction not found: " + txnId);
        }
        if (txn.getFine() <= 0) {
            throw new LibraryException("Transaction " + txnId + " has no assessed fine.");
        }
        if (txn.isFinePaid()) {
            throw new LibraryException("Fine for transaction " + txnId + " is already marked as paid.");
        }

        txn.setFinePaid(true);
        dataManager.saveTransactions(allTxns);
        log("Collected fine of " + CurrencyUtils.format(txn.getFine()) + " for transaction " + txnId);
    }

    /** Removes a completed test or administrative record without allowing active loans to disappear. */
    public synchronized void deleteTransaction(String txnId) throws LibraryException, IOException {
        Transaction txn = findTransaction(txnId);
        if (txn == null) {
            throw new LibraryException("Transaction not found: " + txnId);
        }
        if (!txn.isReturned()) {
            throw new LibraryException("Active transactions cannot be deleted.");
        }
        allTxns.remove(txn);
        dataManager.saveTransactions(allTxns);
        log("Removed completed transaction " + txnId);
    }

    public synchronized double getTotalFinesCollected() {
        return fineService.getTotalFinesCollected(allTxns);
    }

    public synchronized double getTotalOutstandingFines() {
        scanOverdue(LocalDate.now());
        return fineService.getTotalOutstandingFines(allTxns);
    }

    public synchronized double getTotalFinesAssessed() {
        scanOverdue(LocalDate.now());
        return fineService.getTotalFinesAssessed(allTxns);
    }

    public synchronized int scanOverdue(LocalDate asOf) {
        LocalDate scanDate = asOf != null ? asOf : LocalDate.now();
        int overdueCount = 0;
        boolean changed = false;

        for (Transaction t : allTxns) {
            if (!t.isReturned()) {
                if (t.isOverdue(scanDate)) {
                    overdueCount++;
                    LibraryItem it = findItem(t.getItemId());
                    double fine = fineService.calculateFine(it, t.getDueDate(), scanDate);
                    String nextStatus = t.getOverdueDays(scanDate) > 30 ? Transaction.POSSIBLY_LOST : Transaction.OVERDUE;
                    if (!nextStatus.equals(t.getStatus())) {
                        changed = true;
                        t.setStatus(nextStatus);
                    }
                    if (t.getFine() != fine) {
                        t.setFine(fine);
                        changed = true;
                    }
                } else {
                    if (!Transaction.ISSUED.equals(t.getStatus())) {
                        t.setStatus(Transaction.ISSUED);
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            try {
                dataManager.saveTransactions(allTxns);
            } catch (IOException e) {
                System.err.println("Could not save overdue updates: " + e.getMessage());
            }
        }

        return overdueCount;
    }

    // ----------------------------------------------------- TRANSACTIONS

    public synchronized Transaction findTransaction(String txnId) {
        return transactionService.findById(allTxns, txnId);
    }

    public synchronized List<Transaction> filterTransactions(String query, String statusFilter, String fineFilter) {
        scanOverdue(LocalDate.now());
        return transactionService.filter(allTxns, query, statusFilter, fineFilter, members, itemsById);
    }

    public synchronized List<Transaction> getAllTransactions() {
        return Collections.unmodifiableList(new ArrayList<>(allTxns));
    }

    public synchronized List<Transaction> getRecentTransactions(int limit) {
        int safeLimit = Math.max(0, limit);
        List<Transaction> recent = new ArrayList<>(Math.min(safeLimit, allTxns.size()));
        for (int i = allTxns.size() - 1; i >= 0 && recent.size() < safeLimit; i--) {
            recent.add(allTxns.get(i));
        }
        return recent;
    }

    public synchronized List<Transaction> getActiveIssuedTransactions() {
        List<Transaction> active = new ArrayList<>();
        for (Transaction t : allTxns) {
            if (!t.isReturned()) {
                active.add(t);
            }
        }
        return active;
    }

    /** Returns active loans that have crossed the possibly-lost threshold. */
    public synchronized List<Transaction> getPossiblyLostTransactions() {
        scanOverdue(LocalDate.now());
        List<Transaction> lost = new ArrayList<>();
        for (Transaction t : allTxns) {
            if (!t.isReturned() && Transaction.POSSIBLY_LOST.equals(t.getStatus())) {
                lost.add(t);
            }
        }
        return lost;
    }

    public synchronized Set<String> getAllTxnIds() {
        Set<String> set = new HashSet<>();
        for (Transaction t : allTxns) {
            set.add(t.getTransactionId());
        }
        return set;
    }

    public synchronized List<Transaction> snapshotTransactions() {
        return new ArrayList<>(allTxns);
    }

    // ----------------------------------------------------- DASHBOARD STATS

    public synchronized int getTotalItemsCount() {
        return catalogue.size();
    }

    public synchronized int getTotalMembersCount() {
        return members.size();
    }

    public synchronized int getCurrentlyIssuedCount() {
        return activeLoanTotal;
    }

    /** O(1) active-loan lookup used by the member and issue screens. */
    public synchronized int getActiveLoanCount(int memberId) {
        return activeLoanCounts.getOrDefault(memberId, 0);
    }

    public synchronized int getAvailableCopiesCount() {
        int count = 0;
        for (LibraryItem it : catalogue) {
            count += it.getAvailableCopies();
        }
        return count;
    }

    public synchronized int getOverdueCount() {
        int count = 0;
        for (Transaction t : allTxns) {
            if (!t.isReturned() && t.isOverdue(LocalDate.now())) {
                count++;
            }
        }
        return count;
    }

    public synchronized int getPossiblyLostCount() {
        scanOverdue(LocalDate.now());
        int count = 0;
        LocalDate today = LocalDate.now();
        for (Transaction t : allTxns) {
            if (!t.isReturned() && t.isOverdue(today) && t.getOverdueDays(today) > 30) {
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------- RESERVATIONS & LOGS

    /** Legacy reservation entry point retained for existing callers. */
    public synchronized void reserveItem(int itemId, Member member) {
        try {
            joinReservationQueue(itemId, member);
        } catch (Exception e) {
            log("Reservation failed: " + e.getMessage());
        }
    }

    public synchronized void joinReservationQueue(int itemId, Member member) throws LibraryException, IOException {
        LibraryItem item = findItem(itemId);
        if (item == null) {
            throw new LibraryException("Library item with ID " + itemId + " does not exist.");
        }
        if (member == null || findMember(member.getMemberId()) == null) {
            throw new LibraryException("Library member does not exist.");
        }
        if (item.getAvailableCopies() > 0) {
            throw new LibraryException("RESERVATION NOT REQUIRED: " + item.getTitle() + " is currently AVAILABLE.");
        }

        LinkedList<Member> queue = reservations.computeIfAbsent(itemId, k -> new LinkedList<>());
        if (queue.contains(member)) {
            throw new LibraryException("RESERVATION EXISTS: " + member.getName()
                    + " is already in position " + (queue.indexOf(member) + 1) + ".");
        }
        queue.add(member);
        dataManager.saveReservations(reservations);
        log("Member " + member.getCode() + " queued reservation for item ID " + itemId
                + " at position " + queue.size());
    }

    public synchronized int getReservationCount(int itemId) {
        LinkedList<Member> queue = reservations.get(itemId);
        return queue == null ? 0 : queue.size();
    }

    public synchronized int getReservationPosition(int itemId, int memberId) {
        LinkedList<Member> queue = reservations.get(itemId);
        if (queue == null) return 0;
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getMemberId() == memberId) return i + 1;
        }
        return 0;
    }

    public synchronized Member getReservationHead(int itemId) {
        LinkedList<Member> queue = reservations.get(itemId);
        return queue == null || queue.isEmpty() ? null : queue.peek();
    }

    private synchronized void checkReservationQueue(LibraryItem item) {
        if (item == null || item.getAvailableCopies() <= 0) return;
        LinkedList<Member> queue = reservations.get(item.getItemId());
        if (queue != null && !queue.isEmpty()) {
            Member next = queue.peek();
            log("Reservation ready: Member " + next.getCode() + " may now borrow item "
                    + item.generateItemCode() + " (FIFO position 1).");
        }
    }

    public synchronized void log(String message) {
        activityLog.add("[" + LocalDate.now() + "] " + message);
        if (activityLog.size() > 500) {
            activityLog.remove(0);
        }
    }

    public synchronized List<String> getActivityLog() {
        return new ArrayList<>(activityLog);
    }

    public synchronized String backupAndVerify() throws IOException {
        long bytes = BackupManager.export(allTxns, BACKUP_FILE);
        List<Transaction> restored = BackupManager.restore(BACKUP_FILE);
        return "Backup file verified: " + restored.size() + " records restored (" + bytes + " bytes).";
    }

    public synchronized int catalogueSize() {
        return catalogue.size();
    }

    public synchronized int transactionCount() {
        return allTxns.size();
    }

    public synchronized String monthlyReport() {
        int[] issuesByMonth = new int[12];
        for (Transaction t : allTxns) {
            int month = t.getIssueDate().getMonthValue() - 1;
            issuesByMonth[month]++;
        }
        StringBuilder sb = new StringBuilder("=== Circulation Summary by Month ===\n");
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int i = 0; i < 12; i++) {
            if (issuesByMonth[i] > 0) {
                sb.append(String.format("%-4s: %d issues\n", months[i], issuesByMonth[i]));
            }
        }
        return sb.toString();
    }

    public synchronized String overdueReport() {
        scanOverdue(LocalDate.now());
        StringBuilder sb = new StringBuilder("=== Overdue Circulation Report ===\n");
        int count = 0;
        for (Transaction t : allTxns) {
            if (!t.isReturned() && t.isOverdue(LocalDate.now())) {
                count++;
                LibraryItem it = findItem(t.getItemId());
                Member m = findMember(t.getMemberId());
                sb.append(String.format("[%s] Status: %s | Item: %s (%s) | Borrower: %s (%s) | Issued: %s | Due: %s | Overdue: %d days | Fine: %s\n",
                        t.getTransactionId(),
                        t.getStatus(),
                        it != null ? it.getTitle() : "ID " + t.getItemId(),
                        it != null ? it.generateItemCode() : "ID " + t.getItemId(),
                        m != null ? m.getName() : "ID " + t.getMemberId(),
                        m != null ? m.getCode() : "ID " + t.getMemberId(),
                        t.getIssueDate(),
                        t.getDueDate(),
                        t.getOverdueDays(LocalDate.now()),
                        CurrencyUtils.format(t.getFine())));
            }
        }
        if (count == 0) {
            sb.append("No items currently overdue. Great circulation status!\n");
        } else {
            sb.append("Total overdue items requiring attention: ").append(count).append("\n");
        }
        return sb.toString();
    }
}
