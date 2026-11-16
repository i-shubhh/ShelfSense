package dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import model.Member;
import storage.DataManager;

/**
 * Data Access Object for Member entities using Core Java File I/O.
 */
public class MemberDao {

    private final DataManager dataManager;

    public MemberDao(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public MemberDao() {
        this(new DataManager());
    }

    public synchronized void insert(Member m) throws IOException {
        List<Member> list = findAll();
        list.removeIf(item -> item.getMemberId() == m.getMemberId());
        list.add(m);
        dataManager.saveMembers(list);
    }

    public synchronized List<Member> findAll() {
        return dataManager.loadMembers();
    }

    public synchronized Member findById(int id) {
        for (Member m : findAll()) {
            if (m.getMemberId() == id) {
                return m;
            }
        }
        return null;
    }

    public synchronized void update(Member m) throws IOException {
        insert(m);
    }

    public synchronized void delete(int id) throws IOException {
        List<Member> list = findAll();
        list.removeIf(m -> m.getMemberId() == id);
        dataManager.saveMembers(list);
    }

    public synchronized int count() {
        return findAll().size();
    }

    public synchronized int maxId() {
        int max = 0;
        for (Member m : findAll()) {
            if (m.getMemberId() > max) {
                max = m.getMemberId();
            }
        }
        return max;
    }
}
