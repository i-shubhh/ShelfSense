package threads;

import java.util.concurrent.atomic.AtomicInteger;

import exception.ItemNotAvailableException;
import model.Book;
import model.LibraryItem;
import model.Member;
import service.LibraryService;

/** Console demonstrations of thread priorities, life cycle, synchronisation and wait/notify. */
public final class ThreadDemo {

    private ThreadDemo() {
    }

    /** Priorities and life-cycle states (NEW, RUNNABLE, TIMED_WAITING, WAITING, TERMINATED). */
    public static void lifecycleAndPriorities(LibraryService service) throws InterruptedException {
        System.out.println("----- THREAD ACTIVITY LOG -----");
        DataSignal signal = new DataSignal();
        OverdueScannerThread scanner = new OverdueScannerThread(service, signal, 1, 400);
        Thread backup = new Thread(new BackupTask(service, signal, LibraryService.BACKUP_FILE, null), "BackupTask");
        backup.setPriority(Thread.MIN_PRIORITY);

        System.out.println(scanner.getName() + " | priority " + scanner.getPriority() + " | state: " + scanner.getState());
        System.out.println(backup.getName() + " | priority " + backup.getPriority() + " | state: " + backup.getState());
        backup.start();
        scanner.start();
        Thread.sleep(150);
        System.out.println(scanner.getName() + " | state: " + scanner.getState() + "   (sleeping)");
        System.out.println(backup.getName() + " | waiting for data | state: " + backup.getState());
        scanner.join();
        backup.join();
        System.out.println(scanner.getName() + " | finished | state: " + scanner.getState());
        System.out.println(backup.getName() + " | finished | state: " + backup.getState());
    }

    /** Two librarian counters issue the LAST copy at the same moment. */
    public static void synchronisationDemo() throws InterruptedException {
        System.out.println("----- SYNCHRONISATION DEMO -----");
        runRace(false);
        runRace(true);
    }

    private static void runRace(final boolean safe) throws InterruptedException {
        final Book book = new Book("Last Copy Book", "Test Author", "1234567890123", 1);
        final Member m1 = new Member("Counter One Member", "UG", "9000000001");
        final Member m2 = new Member("Counter Two Member", "UG", "9000000002");
        final AtomicInteger issued = new AtomicInteger();
        final AtomicInteger refused = new AtomicInteger();

        Runnable counter1 = counterTask(book, m1, safe, issued, refused);
        Runnable counter2 = counterTask(book, m2, safe, issued, refused);
        Thread t1 = new Thread(counter1, "Counter-1");
        Thread t2 = new Thread(counter2, "Counter-2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println((safe ? "WITH synchronisation   : " : "WITHOUT synchronisation: ")
                + "copies issued = " + issued.get() + ", refused = " + refused.get()
                + ", available copies = " + book.getAvailableCopies()
                + (book.getAvailableCopies() < 0 ? "  <-- WRONG (negative stock)" : "  <-- correct"));
    }

    private static Runnable counterTask(final LibraryItem book, final Member m, final boolean safe,
                                        final AtomicInteger issued, final AtomicInteger refused) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    if (safe) {
                        book.issueItem(m);
                    } else {
                        book.issueItemUnsafe(m);
                    }
                    issued.incrementAndGet();
                } catch (ItemNotAvailableException e) {
                    refused.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
    }

    /** A reservation thread wait()s until a return calls notifyAll(). */
    public static void waitNotifyDemo(LibraryService service) throws InterruptedException {
        System.out.println("----- WAIT / NOTIFY DEMO -----");
        final Book book = new Book("Wait Notify Book", "Test Author", "1234567890123", 1);
        final Member holder = new Member("Holder", "UG", "9000000003");
        final Member waiter = new Member("Waiter", "UG", "9000000004");
        try {
            book.issueItem(holder); // the only copy is now out
        } catch (ItemNotAvailableException e) {
            System.out.println("unexpected: " + e.getMessage());
        }
        Thread reservation = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Reservation thread: no copy, calling wait()");
                    book.waitUntilAvailable();
                    System.out.println("Reservation thread: notified, copy is available");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "ReservationThread");
        reservation.start();
        Thread.sleep(200);
        System.out.println("Reservation thread state: " + reservation.getState());
        System.out.println("Return operation: returnItem() calls notifyAll()");
        book.returnItem(holder);
        reservation.join();
        System.out.println("Reservation thread state: " + reservation.getState());
    }
}
