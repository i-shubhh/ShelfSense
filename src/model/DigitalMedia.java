package model;

/**
 * Represents digital media (DVD, Audiobooks, Video lectures, E-Resources).
 */
public class DigitalMedia extends LibraryItem {

    private String format;
    private String creator;

    public DigitalMedia(int itemId, String title, String creator, String format, int year, int total, int available) {
        super(itemId, title, year, total, available);
        this.creator = creator;
        this.format = format;
    }

    @Override
    public double calculateLateFee(int daysLate) {
        return Math.min(feeFor(daysLate), 150.0); // Rs. 3 per day, max Rs. 150
    }

    @Override
    public int getLoanDays() {
        return 7;
    }

    @Override
    public String getType() {
        return "DIGITAL";
    }

    @Override
    public String getAuthor() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    @Override
    public String getExtra() {
        return format;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String displayDetails() {
        return super.displayDetails() + "\nCreator: " + creator + "\nFormat: " + format;
    }
}
