package model;

import java.time.Year;

/**
 * Level 3 of the inheritance chain: LibraryItem -> Book -> ReferenceBook.
 * Declared final so no class can extend it.
 * Reference books have a strict 2-day loan period and higher late-fee rate.
 */
public final class ReferenceBook extends Book {

    private int edition;

    public ReferenceBook(int itemId, String title, String author, String isbn, int year,
                         int total, int available, int edition) {
        super(itemId, title, author, isbn, year, total, available);
        this.edition = edition;
    }

    public ReferenceBook(String title, String author, String isbn, int edition) {
        this(nextId(), title, author, isbn, Year.now().getValue(), 1, 1, edition);
    }

    @Override
    public double calculateLateFee(int daysLate) {
        return feeFor(daysLate); // Rs. 10 per overdue day after the 2-day loan
    }

    @Override
    public int getLoanDays() {
        return 2;
    }

    @Override
    public String getType() {
        return "REFERENCE";
    }

    @Override
    public String getExtra() {
        return String.valueOf(edition);
    }

    public int getEdition() {
        return edition;
    }

    public void setEdition(int edition) {
        this.edition = edition;
    }

    @Override
    public String displayDetails() {
        return super.displayDetails() + "\nEdition: " + edition + "\nIn-library / Short loan: 2 days max";
    }
}
