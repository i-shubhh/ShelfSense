package model;

import java.time.Year;
import java.util.Hashtable;
import exception.ItemNotAvailableException;
import util.IDGenerator;

/**
 * Abstract parent of every catalogue item.
 *
 * Demonstrates:
 *   - Abstraction (abstract methods calculateLateFee, getType)
 *   - Encapsulation (private fields with accessors/mutators)
 *   - Interface implementation (Borrowable)
 *   - Thread synchronization (issueItem, returnItem, waitUntilAvailable)
 */
public abstract class LibraryItem implements Borrowable {

    /** Hashtable of fine rates keyed by item type */
    public static final Hashtable<String, Double> FINE_RATES = new Hashtable<String, Double>();

    static {
        FINE_RATES.put("BOOK", 2.0);
        FINE_RATES.put("REFERENCE", 10.0);
        FINE_RATES.put("MAGAZINE", 1.0);
        FINE_RATES.put("DIGITAL", 3.0);
    }

    private static int counter = 1000;

    private int itemId;
    private String title;
    private int publicationYear;
    private int totalCopies;
    private int availableCopies;

    protected LibraryItem(int itemId, String title, int publicationYear, int totalCopies, int availableCopies) {
        if (!isValidYear(publicationYear)) {
            throw new IllegalArgumentException("Invalid publication year: " + publicationYear);
        }
        if (totalCopies < 0 || availableCopies < 0 || availableCopies > totalCopies) {
            throw new IllegalArgumentException("Available copies must be between 0 and total copies.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Item title cannot be blank.");
        }
        this.itemId = itemId;
        this.title = title.trim();
        this.publicationYear = publicationYear;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    /** Constructor overloading: id is generated and all copies start available. */
    protected LibraryItem(String title, int publicationYear, int copies) {
        this(nextId(), title, publicationYear, copies, copies);
    }

    public static synchronized int nextId() {
        return ++counter;
    }

    public static synchronized void syncCounter(int maxExistingId) {
        if (maxExistingId > counter) {
            counter = maxExistingId;
        }
    }

    static boolean isValidYear(int year) {
        return year >= 1400 && year <= Year.now().getValue() + 5;
    }

    public final String generateItemCode() {
        return IDGenerator.formatItemCode(getType(), itemId);
    }

    // ------------------------------------------------- abstract behaviour

    public abstract double calculateLateFee(int daysLate);

    public abstract String getType();

    // -------------------------------------------------- overridable hooks

    public int getLoanDays() {
        return 14;
    }

    public String getAuthor() {
        return "";
    }

    public String getIsbn() {
        return "";
    }

    public String getExtra() {
        return "";
    }

    protected double feeFor(int daysLate) {
        if (daysLate <= 0) {
            return 0.0;
        }
        double rate = getLateFeeRate();
        return daysLate * rate;
    }

    /** Returns the configured INR late-fee rate for this item category. */
    public double getLateFeeRate() {
        Double rateObj = FINE_RATES.get(getType());
        return rateObj != null ? rateObj : 2.0;
    }

    public String displayDetails() {
        return "Code: " + generateItemCode() + "\nType: " + getType() + "\nTitle: " + title
                + "\nYear: " + publicationYear + "\nCopies: " + getAvailableCopies() + " of " + totalCopies
                + "\nLoan period: " + getLoanDays() + " days";
    }

    // ---------------------------------------------------- Borrowable

    @Override
    public synchronized void issueItem(Member m) throws ItemNotAvailableException {
        if (availableCopies <= 0) {
            throw new ItemNotAvailableException("Item " + generateItemCode() + " (" + title
                    + ") has zero available copies");
        }
        availableCopies--;
    }

    public void issueItemUnsafe(Member m) throws ItemNotAvailableException, InterruptedException {
        if (availableCopies > 0) {
            Thread.sleep(50);
            availableCopies--;
        } else {
            throw new ItemNotAvailableException("Item " + generateItemCode() + " has zero available copies");
        }
    }

    @Override
    public synchronized void returnItem(Member m) {
        if (availableCopies < totalCopies) {
            availableCopies++;
        }
        notifyAll(); // wakes reservation threads that are wait()ing for this item
    }

    @Override
    public synchronized boolean isAvailable() {
        return availableCopies > 0;
    }

    public synchronized void waitUntilAvailable() throws InterruptedException {
        while (availableCopies <= 0) {
            wait();
        }
    }

    // --------------------------------------------------------- getters & setters

    public int getItemId() {
        return itemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        if (!isValidYear(publicationYear)) {
            throw new IllegalArgumentException("Invalid publication year: " + publicationYear);
        }
        this.publicationYear = publicationYear;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public synchronized void setTotalCopies(int totalCopies) {
        if (totalCopies < 0 || totalCopies < availableCopies) {
            throw new IllegalArgumentException("Total copies cannot be less than available copies.");
        }
        this.totalCopies = totalCopies;
    }

    public synchronized int getAvailableCopies() {
        return availableCopies;
    }

    public synchronized void setAvailableCopies(int availableCopies) {
        if (availableCopies < 0 || availableCopies > totalCopies) {
            throw new IllegalArgumentException("Available copies must be between 0 and total copies.");
        }
        this.availableCopies = availableCopies;
    }

    // ------------------------------------------- Object class overrides

    @Override
    public String toString() {
        return generateItemCode() + " - " + title + " [" + getType() + ", " + getAvailableCopies() + "/"
                + totalCopies + " available]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return itemId == ((LibraryItem) o).itemId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(itemId);
    }
}
