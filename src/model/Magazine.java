package model;

/**
 * Represents a library Magazine or Periodical.
 */
public class Magazine extends LibraryItem {

    private String publisher;

    public Magazine(int itemId, String title, String publisher, int year, int total, int available) {
        super(itemId, title, year, total, available);
        this.publisher = publisher;
    }

    @Override
    public double calculateLateFee(int daysLate) {
        return Math.min(feeFor(daysLate), 50.0); // Rs. 1 per day, max Rs. 50
    }

    @Override
    public int getLoanDays() {
        return 7;
    }

    @Override
    public String getType() {
        return "MAGAZINE";
    }

    @Override
    public String getAuthor() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    @Override
    public String displayDetails() {
        return super.displayDetails() + "\nPublisher: " + publisher;
    }
}
