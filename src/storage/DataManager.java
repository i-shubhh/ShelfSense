package storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import model.Book;
import model.ItemFactory;
import model.LibraryItem;
import model.Member;
import model.Transaction;
import util.IDGenerator;

/**
 * High-reliability File I/O persistence manager.
 * Stores data in structured text files in the `data/` directory.
 * Fully thread-safe with atomic save strategies and fault-tolerant parsing.
 */
public class DataManager {

    private static final String DATA_DIR = "data";
    private static final String ITEMS_FILE = DATA_DIR + File.separator + "items.txt";
    private static final String MEMBERS_FILE = DATA_DIR + File.separator + "members.txt";
    private static final String TXNS_FILE = DATA_DIR + File.separator + "transactions.txt";
    private static final String RESERVATIONS_FILE = DATA_DIR + File.separator + "reservations.txt";
    private static final String DELIMITER = "\\|";
    private static final String DELIM_CHAR = "|";

    public DataManager() {
        ensureDataDirectory();
    }

    private void ensureDataDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // ------------------------------------------------------------- ITEMS

    public synchronized void saveItems(List<LibraryItem> items) throws IOException {
        ensureDataDirectory();
        File target = new File(ITEMS_FILE);
        File temp = new File(ITEMS_FILE + ".tmp");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
            writer.write("# itemId|type|title|author|isbn|publicationYear|totalCopies|availableCopies|extra");
            writer.newLine();
            for (LibraryItem it : items) {
                String line = String.join(DELIM_CHAR,
                        String.valueOf(it.getItemId()),
                        it.getType(),
                        sanitize(it.getTitle()),
                        sanitize(it.getAuthor()),
                        sanitize(it.getIsbn()),
                        String.valueOf(it.getPublicationYear()),
                        String.valueOf(it.getTotalCopies()),
                        String.valueOf(it.getAvailableCopies()),
                        sanitize(it.getExtra())
                );
                writer.write(line);
                writer.newLine();
            }
        }

        if (target.exists()) {
            target.delete();
        }
        if (!temp.renameTo(target)) {
            // fallback if atomic rename fails on some OS
            copyFile(temp, target);
            temp.delete();
        }
    }

    public synchronized List<LibraryItem> loadItems() {
        List<LibraryItem> list = new ArrayList<>();
        File file = new File(ITEMS_FILE);
        if (!file.exists()) {
            return list;
        }

        int maxId = 1000;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length >= 8) {
                    try {
                        int id = Integer.parseInt(parts[0].trim());
                        String type = parts[1].trim();
                        String title = parts[2].trim();
                        String author = parts[3].trim();
                        String isbn = parts[4].trim();
                        int year = Integer.parseInt(parts[5].trim());
                        int total = Integer.parseInt(parts[6].trim());
                        int avail = Integer.parseInt(parts[7].trim());
                        String extra = parts.length > 8 ? parts[8].trim() : "";

                        LibraryItem item = ItemFactory.create(type, id, title, author, isbn, year, total, avail, extra);
                        list.add(item);
                        if (id > maxId) {
                            maxId = id;
                        }
                    } catch (Exception parseEx) {
                        System.err.println("Skipping corrupted item record: " + line + " (" + parseEx.getMessage() + ")");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading items file: " + e.getMessage());
        }

        LibraryItem.syncCounter(maxId);
        return list;
    }

    // ----------------------------------------------------------- MEMBERS

    public synchronized void saveMembers(Collection<Member> members) throws IOException {
        ensureDataDirectory();
        File target = new File(MEMBERS_FILE);
        File temp = new File(MEMBERS_FILE + ".tmp");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
            writer.write("# memberId|name|category|phone|borrowLimit");
            writer.newLine();
            for (Member m : members) {
                String line = String.join(DELIM_CHAR,
                        String.valueOf(m.getMemberId()),
                        sanitize(m.getName()),
                        sanitize(m.getCategory()),
                        sanitize(m.getPhone()),
                        String.valueOf(m.getBorrowLimit())
                );
                writer.write(line);
                writer.newLine();
            }
        }

        if (target.exists()) {
            target.delete();
        }
        if (!temp.renameTo(target)) {
            copyFile(temp, target);
            temp.delete();
        }
    }

    public synchronized List<Member> loadMembers() {
        List<Member> list = new ArrayList<>();
        File file = new File(MEMBERS_FILE);
        if (!file.exists()) {
            return list;
        }

        int maxId = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length >= 5) {
                    try {
                        int id = Integer.parseInt(parts[0].trim());
                        String name = parts[1].trim();
                        String category = parts[2].trim();
                        String phone = parts[3].trim();
                        int limit = Integer.parseInt(parts[4].trim());

                        Member m = new Member(id, name, category, phone, limit);
                        list.add(m);
                        if (id > maxId) {
                            maxId = id;
                        }
                    } catch (Exception parseEx) {
                        System.err.println("Skipping corrupted member record: " + line + " (" + parseEx.getMessage() + ")");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading members file: " + e.getMessage());
        }

        Member.syncCounter(maxId);
        return list;
    }

    // ------------------------------------------------------ TRANSACTIONS

    public synchronized void saveTransactions(List<Transaction> transactions) throws IOException {
        ensureDataDirectory();
        File target = new File(TXNS_FILE);
        File temp = new File(TXNS_FILE + ".tmp");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
            writer.write("# transactionId|itemId|memberId|issueDate|dueDate|returnDate|fine|finePaid|status");
            writer.newLine();
            for (Transaction t : transactions) {
                String line = String.join(DELIM_CHAR,
                        t.getTransactionId(),
                        String.valueOf(t.getItemId()),
                        String.valueOf(t.getMemberId()),
                        t.getIssueDate().toString(),
                        t.getDueDate().toString(),
                        t.getReturnDate() == null ? "" : t.getReturnDate().toString(),
                        String.valueOf(t.getFine()),
                        String.valueOf(t.isFinePaid()),
                        t.getStatus()
                );
                writer.write(line);
                writer.newLine();
            }
        }

        if (target.exists()) {
            target.delete();
        }
        if (!temp.renameTo(target)) {
            copyFile(temp, target);
            temp.delete();
        }
    }

    public synchronized List<Transaction> loadTransactions() {
        List<Transaction> list = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        File file = new File(TXNS_FILE);
        if (!file.exists()) {
            return list;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length >= 9) {
                    try {
                        String txnId = parts[0].trim();
                        int itemId = Integer.parseInt(parts[1].trim());
                        int memberId = Integer.parseInt(parts[2].trim());
                        LocalDate issueDate = LocalDate.parse(parts[3].trim());
                        LocalDate dueDate = LocalDate.parse(parts[4].trim());
                        String retStr = parts[5].trim();
                        LocalDate returnDate = retStr.isEmpty() ? null : LocalDate.parse(retStr);
                        double fine = Double.parseDouble(parts[6].trim());
                        boolean finePaid = Boolean.parseBoolean(parts[7].trim());
                        String status = parts[8].trim();

                        Transaction t = new Transaction(txnId, itemId, memberId, issueDate, dueDate,
                                returnDate, fine, finePaid, status);
                        if (txnId.isEmpty() || !seenIds.add(txnId)) {
                            System.err.println("Skipping duplicate or empty transaction ID: " + txnId);
                        } else {
                            list.add(t);
                        }
                    } catch (Exception parseEx) {
                        System.err.println("Skipping corrupted transaction record: " + line + " (" + parseEx.getMessage() + ")");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading transactions file: " + e.getMessage());
        }

        return list;
    }

    // --------------------------------------------------------- RESERVATIONS

    /** Stores one item/member pair per line; file order is the FIFO queue order. */
    public synchronized void saveReservations(Map<Integer, ? extends Collection<Member>> reservations)
            throws IOException {
        ensureDataDirectory();
        File target = new File(RESERVATIONS_FILE);
        File temp = new File(RESERVATIONS_FILE + ".tmp");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
            writer.write("# itemId|memberId");
            writer.newLine();
            for (Map.Entry<Integer, ? extends Collection<Member>> entry : reservations.entrySet()) {
                for (Member member : entry.getValue()) {
                    writer.write(entry.getKey() + DELIM_CHAR + member.getMemberId());
                    writer.newLine();
                }
            }
        }

        if (target.exists()) {
            target.delete();
        }
        if (!temp.renameTo(target)) {
            copyFile(temp, target);
            temp.delete();
        }
    }

    /** Loads reservation member IDs in file order so each item's queue remains FIFO. */
    public synchronized Map<Integer, List<Integer>> loadReservations() {
        Map<Integer, List<Integer>> result = new LinkedHashMap<>();
        File file = new File(RESERVATIONS_FILE);
        if (!file.exists()) {
            return result;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length >= 2) {
                    try {
                        int itemId = Integer.parseInt(parts[0].trim());
                        int memberId = Integer.parseInt(parts[1].trim());
                        result.computeIfAbsent(itemId, key -> new ArrayList<>()).add(memberId);
                    } catch (NumberFormatException parseEx) {
                        System.err.println("Skipping corrupted reservation record: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading reservations file: " + e.getMessage());
        }
        return result;
    }

    // --------------------------------------------------- SEED TEST DATA

    public synchronized boolean hasExistingData() {
        return new File(ITEMS_FILE).exists() && new File(ITEMS_FILE).length() > 20;
    }

    /**
     * Seeds realistic, varied library data if storage is empty.
     */
    public synchronized void seedDefaultData(List<LibraryItem> outItems, List<Member> outMembers, List<Transaction> outTxns)
            throws IOException {
        Random rnd = new Random(42); // fixed seed for reproducible realistic data
        Set<String> usedTxnIds = new HashSet<>();

        // 1. Books
        String[] titles = {
            "Clean Code", "Introduction to Algorithms", "Design Patterns", "Operating System Concepts",
            "Artificial Intelligence: A Modern Approach", "Java: The Complete Reference",
            "Computer Networks", "Database System Concepts", "Structure and Interpretation of Computer Programs",
            "Effective Java"
        };
        String[] authors = {
            "Robert C. Martin", "T. Cormen, C. Leiserson", "Erich Gamma et al.", "A. Silberschatz, P. Galvin",
            "Stuart Russell, Peter Norvig", "Herbert Schildt", "Andrew S. Tanenbaum", "Abraham Silberschatz",
            "Harold Abelson, G. Sussman", "Joshua Bloch"
        };
        String[] isbns = {
            "9780132350884", "9780262033848", "9780201633610", "9781119800361",
            "9780136042594", "9781260440232", "9780132126953", "9780078022159",
            "9780262510875", "9780134685991"
        };

        for (int i = 0; i < titles.length; i++) {
            int copies = 2 + rnd.nextInt(3);
            outItems.add(new Book(LibraryItem.nextId(), titles[i], authors[i], isbns[i], 2005 + rnd.nextInt(18), copies, copies));
        }

        // 2. Reference Books
        String[] refTitles = {
            "Encyclopaedia of Computer Science", "Oxford Dictionary of Computing",
            "CRC Handbook of Engineering Mathematics", "Standard Handbook for Software Engineers"
        };
        String[] refAuthors = {
            "Anthony Ralston", "John Daintith", "Alan Jeffrey", "Mark Richards"
        };
        for (int i = 0; i < refTitles.length; i++) {
            outItems.add(ItemFactory.create("REFERENCE", LibraryItem.nextId(), refTitles[i], refAuthors[i],
                    "978" + (1000000000L + rnd.nextInt(900000000)), 2018 + rnd.nextInt(5), 1, 1, String.valueOf(1 + i)));
        }

        // 3. Magazines
        String[] magazines = {
            "IEEE Spectrum", "Communications of the ACM", "MIT Technology Review", "Digit Tech Magazine"
        };
        for (String mag : magazines) {
            int copies = 3 + rnd.nextInt(3);
            outItems.add(ItemFactory.create("MAGAZINE", LibraryItem.nextId(), mag, "IEEE / ACM Publishing", "",
                    2024 - rnd.nextInt(2), copies, copies, ""));
        }

        // 4. Digital Media
        String[] mediaTitles = {
            "Java Concurrency Video Masterclass", "Distributed Systems Audio Course",
            "Advanced Algorithms Lecture Series", "Deep Learning Specialization Materials"
        };
        String[] formats = {"MP4 Video", "MP3 Audio", "MP4 1080p", "Interactive ISO"};
        for (int i = 0; i < mediaTitles.length; i++) {
            int copies = 2 + rnd.nextInt(2);
            outItems.add(ItemFactory.create("DIGITAL", LibraryItem.nextId(), mediaTitles[i], "Dr. Martin Odersky", "",
                    2023, copies, copies, formats[i]));
        }

        // 5. Members
        String[] memberNames = {
            "Kavya Reddy", "Arjun Rao", "Meera Nair", "Rohit Verma", "Sneha Iyer",
            "Vikram Singh", "Ananya Das", "Karthik Raj", "Pooja Sharma", "Rahul Menon",
            "Divya Kumar", "Sai Teja", "Nisha Patel", "Aditya Joshi", "Lakshmi Devi",
            "Manoj Kumar", "Ishita Roy", "Harsha Vardhan", "Priya Nair", "Dr. S. Murthy",
            "Prof. L. Rao", "Ramesh Babu"
        };
        String[] categories = {
            "UG", "UG", "UG", "UG", "PG",
            "PG", "PG", "PhD", "PhD", "PhD",
            "UG", "UG", "PG", "PG", "PhD",
            "UG", "UG", "PG", "UG", "Faculty",
            "Faculty", "Staff"
        };

        for (int i = 0; i < memberNames.length; i++) {
            String phone = "98" + String.format("%08d", 10000000 + i * 3791);
            Member m = new Member(Member.nextId(), memberNames[i], categories[i], phone, Member.limitFor(categories[i]));
            outMembers.add(m);
        }

        // 6. Transactions (historical, active, and overdue)
        LocalDate today = LocalDate.now();

        // 8 Returned transactions (some with paid fine, some on time)
        for (int i = 0; i < 8; i++) {
            LibraryItem item = outItems.get(i % outItems.size());
            Member member = outMembers.get(i % outMembers.size());
            LocalDate issueDate = today.minusDays(45 - i * 3);
            LocalDate dueDate = issueDate.plusDays(item.getLoanDays());
            boolean wasOverdue = (i % 2 == 1);
            LocalDate returnDate = wasOverdue ? dueDate.plusDays(3 + i) : dueDate.minusDays(2);
            double fine = wasOverdue ? item.calculateLateFee((int) java.time.temporal.ChronoUnit.DAYS.between(dueDate, returnDate)) : 0.0;
            String txnId = IDGenerator.generateTransactionId(usedTxnIds);
            usedTxnIds.add(txnId);

            outTxns.add(new Transaction(txnId, item.getItemId(), member.getMemberId(),
                    issueDate, dueDate, returnDate, fine, true, Transaction.RETURNED));
        }

        // 8 Active (Issued) transactions (due in the future)
        for (int i = 0; i < 8; i++) {
            LibraryItem item = outItems.get((8 + i) % outItems.size());
            Member member = outMembers.get((4 + i) % outMembers.size());
            if (item.getAvailableCopies() > 0) {
                try {
                    item.issueItem(member);
                } catch (Exception ignored) {
                }
            }
            LocalDate issueDate = today.minusDays(1 + (i % 5));
            LocalDate dueDate = issueDate.plusDays(item.getLoanDays());
            String txnId = IDGenerator.generateTransactionId(usedTxnIds);
            usedTxnIds.add(txnId);

            outTxns.add(new Transaction(txnId, item.getItemId(), member.getMemberId(),
                    issueDate, dueDate, null, 0.0, false, Transaction.ISSUED));
        }

        // 6 Overdue transactions (issued in the past, past due date, not returned yet)
        for (int i = 0; i < 6; i++) {
            LibraryItem item = outItems.get((16 + i) % outItems.size());
            Member member = outMembers.get((10 + i) % outMembers.size());
            if (item.getAvailableCopies() > 0) {
                try {
                    item.issueItem(member);
                } catch (Exception ignored) {
                }
            }
            LocalDate issueDate = today.minusDays(25 + i * 5);
            LocalDate dueDate = issueDate.plusDays(item.getLoanDays());
            int daysLate = (int) java.time.temporal.ChronoUnit.DAYS.between(dueDate, today);
            double accruedFine = item.calculateLateFee(daysLate);
            String txnId = IDGenerator.generateTransactionId(usedTxnIds);
            usedTxnIds.add(txnId);

            outTxns.add(new Transaction(txnId, item.getItemId(), member.getMemberId(),
                    issueDate, dueDate, null, accruedFine, false, Transaction.OVERDUE));
        }

        // Persist the seed data
        saveItems(outItems);
        saveMembers(outMembers);
        saveTransactions(outTxns);
    }

    private String sanitize(String s) {
        if (s == null) return "";
        return s.replace("|", "/").replace("\n", " ").replace("\r", " ").trim();
    }

    private void copyFile(File src, File dest) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(src));
             BufferedWriter out = new BufferedWriter(new FileWriter(dest))) {
            String line;
            while ((line = in.readLine()) != null) {
                out.write(line);
                out.newLine();
            }
        }
    }
}
