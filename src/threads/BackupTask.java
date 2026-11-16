package threads;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import io.BackupManager;
import model.Transaction;
import service.LibraryService;

/**
 * Writes the transaction log to a binary backup file in the background (implements Runnable).
 */
public class BackupTask implements Runnable {

    private final LibraryService service;
    private final DataSignal signal; // may be null
    private final File file;
    private final Consumer<String> onDone; // may be null

    public BackupTask(LibraryService service, DataSignal signal, File file, Consumer<String> onDone) {
        this.service = service;
        this.signal = signal;
        this.file = file;
        this.onDone = onDone;
    }

    @Override
    public void run() {
        Thread me = Thread.currentThread();
        service.log("BackupTask running on " + me.getName() + " | Priority: " + me.getPriority() + " | State: " + me.getState());
        String result;
        try {
            if (signal != null) {
                service.log("BackupTask waiting for signal...");
                signal.waitForData();
                service.log("BackupTask resumed");
            }
            List<Transaction> snapshot = service.snapshotTransactions();
            long bytes = BackupManager.export(snapshot, file);
            result = "Backup successful: " + snapshot.size() + " transactions exported (" + String.format("%,d", bytes) + " bytes).";
        } catch (IOException e) {
            result = "Backup failed: " + e.getMessage();
        } catch (InterruptedException e) {
            result = "Backup interrupted";
            Thread.currentThread().interrupt();
        }
        service.log(result);
        if (onDone != null) {
            onDone.accept(result);
        }
    }
}
