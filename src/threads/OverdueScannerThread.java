package threads;

import java.time.LocalDate;
import java.util.Random;
import service.LibraryService;

/**
 * Scans issued transactions periodically in a background thread and flags overdue records.
 * Demonstrates thread priority, timed sleep, life cycle states, and cooperative interruption.
 */
public class OverdueScannerThread extends Thread {

    private final LibraryService service;
    private final DataSignal signal; // may be null
    private final int cycles;
    private final long baseDelayMillis;
    private final Random random = new Random();
    private volatile boolean running = true;
    private Runnable onScanComplete;

    public OverdueScannerThread(LibraryService service, DataSignal signal, int cycles, long baseDelayMillis) {
        super("OverdueScannerThread");
        this.service = service;
        this.signal = signal;
        this.cycles = cycles;
        this.baseDelayMillis = baseDelayMillis;
        setPriority(Thread.NORM_PRIORITY);
        setDaemon(true);
    }

    public OverdueScannerThread(LibraryService service) {
        this(service, null, Integer.MAX_VALUE, 15000); // scans every 15s indefinitely
    }

    public void setOnScanComplete(Runnable onScanComplete) {
        this.onScanComplete = onScanComplete;
    }

    public void stopScanning() {
        running = false;
        interrupt();
    }

    @Override
    public void run() {
        service.log("Scanner thread started | Priority: " + getPriority() + " | State: " + getState());
        try {
            for (int i = 0; i < cycles && running; i++) {
                Thread.sleep(baseDelayMillis + random.nextInt(200));
                int overdue = service.scanOverdue(LocalDate.now());
                service.log("Overdue scan #" + (i + 1) + " complete, overdue records found: " + overdue);
                if (signal != null) {
                    service.log("Signaling update complete");
                    signal.signalReady();
                }
                if (onScanComplete != null) {
                    onScanComplete.run();
                }
            }
        } catch (InterruptedException e) {
            service.log("Overdue scanner thread interrupted");
            Thread.currentThread().interrupt();
        }
        service.log("Overdue scanner finished");
    }
}
