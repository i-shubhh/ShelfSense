package dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import model.LibraryItem;
import storage.DataManager;

/**
 * Data Access Object for LibraryItem entities using Core Java File I/O.
 */
public class ItemDao {

    private final DataManager dataManager;

    public ItemDao(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public ItemDao() {
        this(new DataManager());
    }

    public synchronized void insert(LibraryItem it) throws IOException {
        List<LibraryItem> items = findAll();
        items.removeIf(item -> item.getItemId() == it.getItemId());
        items.add(it);
        dataManager.saveItems(items);
    }

    public synchronized List<LibraryItem> findAll() {
        return dataManager.loadItems();
    }

    public synchronized LibraryItem findById(int id) {
        for (LibraryItem it : findAll()) {
            if (it.getItemId() == id) {
                return it;
            }
        }
        return null;
    }

    public synchronized void update(LibraryItem it) throws IOException {
        insert(it);
    }

    public synchronized void delete(int id) throws IOException {
        List<LibraryItem> items = findAll();
        items.removeIf(item -> item.getItemId() == id);
        dataManager.saveItems(items);
    }

    public synchronized int count() {
        return findAll().size();
    }

    public synchronized int maxId() {
        int max = 1000;
        for (LibraryItem it : findAll()) {
            if (it.getItemId() > max) {
                max = it.getItemId();
            }
        }
        return max;
    }
}
