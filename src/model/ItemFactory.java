package model;

/**
 * Builds the right LibraryItem subclass from raw parameters (Factory Pattern).
 */
public final class ItemFactory {

    private ItemFactory() {
    }

    public static LibraryItem create(String type, int id, String title, String author, String isbn,
                                     int year, int total, int available, String extra) {
        if (type == null) {
            throw new IllegalArgumentException("Item type cannot be null");
        }
        switch (type.trim().toUpperCase()) {
            case "BOOK":
                return new Book(id, title, author, isbn, year, total, available);
            case "REFERENCE":
                int edition = 1;
                try {
                    if (extra != null && !extra.trim().isEmpty()) {
                        edition = Integer.parseInt(extra.trim());
                    }
                } catch (NumberFormatException ignored) {
                    edition = 1;
                }
                return new ReferenceBook(id, title, author, isbn, year, total, available, edition);
            case "MAGAZINE":
                return new Magazine(id, title, author, year, total, available);
            case "DIGITAL":
                String format = (extra != null && !extra.trim().isEmpty()) ? extra.trim() : "Digital";
                return new DigitalMedia(id, title, author, format, year, total, available);
            default:
                throw new IllegalArgumentException("Unknown item type: " + type);
        }
    }
}
