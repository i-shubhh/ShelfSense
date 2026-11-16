package model;

import util.IDGenerator;

/**
 * Represents a registered library member.
 */
public class Member {

    public static final String[] CATEGORIES = {"UG", "PG", "PhD", "Faculty", "Staff"};

    private static int counter = 0;

    private int memberId;
    private String name;
    private String category;
    private String phone;
    private int borrowLimit;

    public Member(int memberId, String name, String category, String phone, int borrowLimit) {
        this.memberId = memberId;
        this.name = name;
        this.category = category;
        this.phone = phone;
        this.borrowLimit = borrowLimit;
    }

    /** Overloaded constructor: id and limit are worked out automatically. */
    public Member(String name, String category, String phone) {
        this(nextId(), name, category, phone, limitFor(category));
    }

    public static synchronized int nextId() {
        return ++counter;
    }

    public static synchronized void syncCounter(int maxExistingId) {
        if (maxExistingId > counter) {
            counter = maxExistingId;
        }
    }

    public static int limitFor(String category) {
        if (category == null) return 2;
        switch (category) {
            case "UG":
                return 3;
            case "PG":
                return 5;
            case "PhD":
                return 8;
            case "Faculty":
                return 10;
            case "Staff":
                return 4;
            default:
                return 2;
        }
    }

    public static boolean isValidCategory(String category) {
        if (category == null) return false;
        for (String c : CATEGORIES) {
            if (c.equalsIgnoreCase(category)) {
                return true;
            }
        }
        return false;
    }

    public String getCode() {
        return IDGenerator.formatMemberId(memberId);
    }

    public int getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
        this.borrowLimit = limitFor(category);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getBorrowLimit() {
        return borrowLimit;
    }

    public void setBorrowLimit(int borrowLimit) {
        this.borrowLimit = borrowLimit;
    }

    @Override
    public String toString() {
        return getCode() + " - " + name + " (" + category + ")";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Member && ((Member) o).memberId == memberId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(memberId);
    }
}
