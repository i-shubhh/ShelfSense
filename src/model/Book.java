package model;

import java.time.Year;

/**
 * Represents a circulating Book.
 * Level 2 of the multilevel inheritance hierarchy: LibraryItem -> Book -> ReferenceBook.
 */
public class Book extends LibraryItem {

    private String author;
    private String isbn;

    public Book(int itemId, String title, String author, String isbn, int year, int total, int available) {
        super(itemId, title, year, total, available);
        this.author = author;
        this.isbn = isbn;
    }

    // ---- constructor overloading chained with this(...) ----

    public Book(String title) {
        this(title, "Unknown");
    }

    public Book(String title, String author) {
        this(title, author, "0000000000000", 1);
    }

    public Book(String title, String author, String isbn, int copies) {
        this(nextId(), title, author, isbn, Year.now().getValue(), copies, copies);
    }

    @Override
    public double calculateLateFee(int daysLate) {
        return feeFor(daysLate); // Rs. 2 per overdue day
    }

    @Override
    public String getType() {
        return "BOOK";
    }

    @Override
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @Override
    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    @Override
    public String displayDetails() {
        return super.displayDetails() + "\nAuthor: " + author + "\nISBN: " + isbn;
    }
}
