package io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.Transaction;

/**
 * Binary backup of transactions using Java byte streams (DataOutputStream / DataInputStream).
 * Demonstrates low-level byte stream persistence and format verification.
 */
public final class BackupManager {

    private static final int MAGIC = 0x53534E53; // "SSNS" (ShelfSense)
    private static final int VERSION = 2;

    private BackupManager() {
    }

    /**
     * Exports transactions to binary file and returns total byte length written.
     */
    public static long export(List<Transaction> list, File file) throws IOException {
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(list.size());
            for (Transaction t : list) {
                out.writeUTF(t.getTransactionId());
                out.writeInt(t.getItemId());
                out.writeInt(t.getMemberId());
                out.writeLong(t.getIssueDate().toEpochDay());
                out.writeLong(t.getDueDate().toEpochDay());
                out.writeLong(t.getReturnDate() == null ? -1L : t.getReturnDate().toEpochDay());
                out.writeDouble(t.getFine());
                out.writeBoolean(t.isFinePaid());
                out.writeUTF(t.getStatus());
            }
            out.flush();
        }
        return file.length();
    }

    /**
     * Restores transactions from binary file.
     */
    public static List<Transaction> restore(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("Backup file does not exist: " + file.getAbsolutePath());
        }
        List<Transaction> list = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new IOException("Invalid file signature; not a ShelfSense backup file.");
            }
            int version = in.readInt();
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                String txnId = in.readUTF();
                int itemId = in.readInt();
                int memberId = in.readInt();
                long issueEpoch = in.readLong();
                long dueEpoch = in.readLong();
                long returnEpoch = in.readLong();
                double fine = in.readDouble();
                boolean finePaid = (version >= 2) ? in.readBoolean() : (fine <= 0);
                String status = in.readUTF();

                LocalDate issueDate = LocalDate.ofEpochDay(issueEpoch);
                LocalDate dueDate = LocalDate.ofEpochDay(dueEpoch);
                LocalDate returnDate = (returnEpoch < 0) ? null : LocalDate.ofEpochDay(returnEpoch);

                list.add(new Transaction(txnId, itemId, memberId, issueDate, dueDate,
                        returnDate, fine, finePaid, status));
            }
        }
        return list;
    }
}
