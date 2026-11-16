package model;

import exception.ItemNotAvailableException;

/** Contract for everything that can be issued to and returned by a member. */
public interface Borrowable {
    void issueItem(Member m) throws ItemNotAvailableException;

    void returnItem(Member m);

    boolean isAvailable();
}
