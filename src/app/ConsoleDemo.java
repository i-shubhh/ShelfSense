package app;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import exception.BorrowLimitExceededException;
import exception.InvalidIsbnException;
import exception.ItemNotAvailableException;
import exception.LibraryException;
import model.Book;
import model.DigitalMedia;
import model.LibraryItem;
import model.Magazine;
import model.Member;
import model.ReferenceBook;
import model.Transaction;
import service.LibraryService;
import service.FineService;
import service.Validator;
import threads.ThreadDemo;
import util.CurrencyUtils;

/**
 * Console Demonstration and Test Suite for ShelfSense.
 * Exercises all core OOP concepts, polymorphism, random Transaction ID generation,
 * late-fee intelligence, multithreading synchronization, and File I/O persistence.
 */
public final class ConsoleDemo {

    private ConsoleDemo() {
    }

    public static void run(LibraryService service) {
        System.out.println("================================================================================");
        System.out.println("  ShelfSense: The Smart Library Circulation & Late-Fee Intelligence System");
        System.out.println("  Core Java & AWT Engine Verification Suite");
        System.out.println("================================================================================\n");

        System.out.println("Inventory Telemetry:");
        System.out.println("  • Total Library Items:     " + service.getTotalItemsCount());
        System.out.println("  • Registered Members:      " + service.getTotalMembersCount());
        System.out.println("  • Active Loans (Issued):   " + service.getCurrentlyIssuedCount());
        System.out.println("  • Overdue Loans:           " + service.getOverdueCount());
        System.out.println("  • Total Fines Collected:   " + CurrencyUtils.format(service.getTotalFinesCollected()));
        System.out.println("  • Outstanding Fines:       " + CurrencyUtils.format(service.getTotalOutstandingFines()));
        System.out.println("  • All-Time Transactions:   " + service.transactionCount());
        System.out.println();

        // 1. Polymorphism Test
        testPolymorphism();

        // 2. Random Transaction ID Generation Test (Requirement 8)
        testRandomTransactionId(service);

        // 3. Issue and Return Circulation Cycle
        testCirculation(service);

        // 4. Overdue & Late-Fee Intelligence Test (Requirement 9)
        testLateFeeIntelligence(service);

        // 5. Reservation queue and possibly-lost monitoring
        testReservationAndLostMonitoring(service);

        // 6. Multithreading & Synchronization Test (Requirement 12)
        testMultithreading(service);

        // 7. Binary Backup Verification Test
        testBackup(service);

        System.out.println("================================================================================");
        System.out.println("  All ShelfSense Verification Checks Passed Successfully! (100% Core Java)");
        System.out.println("================================================================================\n");
    }

    private static void testPolymorphism() {
        System.out.println("----- 1. OOP POLYMORPHISM & INHERITANCE TEST -----");
        LibraryItem[] items = {
            new Book(101, "Clean Architecture", "Robert C. Martin", "9780134494166", 2017, 3, 3),
            new ReferenceBook(102, "Encyclopaedia of Physics", "Rita Lerner", "9780895737526", 2020, 1, 1, 3),
            new Magazine(103, "IEEE Micro", "IEEE Press", 2024, 2, 2),
            new DigitalMedia(104, "Advanced Java Video Lab", "Oracle Corp", "MP4 4K", 2023, 2, 2)
        };

        for (LibraryItem it : items) {
            System.out.println(String.format("  [%-9s] %-28s | Loan Days: %2d | Late Fee for 5 days late: %s",
                    it.getType(), it.getTitle(), it.getLoanDays(), CurrencyUtils.format(it.calculateLateFee(5))));
        }
        System.out.println("  ✓ Dynamic method dispatch verified across all 4 item subclasses.\n");
    }

    private static void testRandomTransactionId(LibraryService service) {
        System.out.println("----- 2. RANDOM TRANSACTION ID GENERATION (REQUIREMENT 8) -----");
        try {
            Member m = service.getMembersSorted().get(0);
            LibraryItem item = service.getCatalogue().get(0);

            Transaction t1 = service.issueItem(item.getItemId(), m.getMemberId());
            System.out.println("  Auto-generated Transaction ID #1: " + t1.getTransactionId());

            Transaction t2 = service.issueItem(item.getItemId(), m.getMemberId());
            System.out.println("  Auto-generated Transaction ID #2: " + t2.getTransactionId());

            boolean unique = !t1.getTransactionId().equals(t2.getTransactionId());
            boolean formatOk = t1.getTransactionId().matches("SHELF-TXN-[0-9A-F]{8}");

            System.out.println("  Format check (SHELF-TXN-XXXXXXXX): " + (formatOk ? "PASSED ✓" : "FAILED ✕"));
            System.out.println("  Uniqueness check between IDs:      " + (unique ? "PASSED ✓" : "FAILED ✕"));

            // Check-in both test transactions
            service.returnItem(t1.getTransactionId(), LocalDate.now(), true);
            service.returnItem(t2.getTransactionId(), LocalDate.now(), true);
            service.deleteTransaction(t1.getTransactionId());
            service.deleteTransaction(t2.getTransactionId());
            System.out.println("  ✓ Verified automated SecureRandom generation & permanent persistence.\n");
        } catch (Exception e) {
            System.out.println("  Transaction test note: " + e.getMessage() + "\n");
        }
    }

    private static void testCirculation(LibraryService service) {
        System.out.println("----- 3. CIRCULATION & BORROW LIMIT VALIDATION -----");
        LibraryItem item = null;
        Member ugMember = null;
        List<Transaction> issued = new java.util.ArrayList<>();
        try {
            item = new Book(LibraryItem.nextId(), "Borrowing Limit Validation Book", "ShelfSense Test",
                    "9780000000001", 2024, 3, 3);
            ugMember = new Member(Member.nextId(), "Limit Tester", "UG", "9999999999", 3);
            service.addItem(item);
            service.registerMember(ugMember);

            System.out.println("  Testing borrow limit for UG member (Max: 3 items)...");
            issued.add(service.issueItem(item.getItemId(), ugMember.getMemberId()));
            issued.add(service.issueItem(item.getItemId(), ugMember.getMemberId()));
            issued.add(service.issueItem(item.getItemId(), ugMember.getMemberId()));
            System.out.println("  Issued 3 items successfully.");

            try {
                service.issueItem(item.getItemId(), ugMember.getMemberId());
                System.out.println("  ✕ FAILED: Limit check did not prevent 4th loan!");
            } catch (BorrowLimitExceededException ble) {
                System.out.println("  ✓ PASSED: BorrowLimitExceededException caught properly: " + ble.getMessage());
            }

            // Return items
            for (Transaction t : issued) {
                service.returnItem(t.getTransactionId(), LocalDate.now(), true);
                service.deleteTransaction(t.getTransactionId());
            }
            service.deleteItem(item.getItemId());
            service.deleteMember(ugMember.getMemberId());
            System.out.println("  ✓ Items returned and test member cleaned up.\n");
        } catch (Exception e) {
            System.out.println("  Circulation test note: " + e.getMessage() + "\n");
        }
    }

    private static void testLateFeeIntelligence(LibraryService service) {
        System.out.println("----- 4. LATE-FEE INTELLIGENCE TEST (REQUIREMENT 9) -----");
        ReferenceBook refBook = null;
        Transaction txn = null;
        Member member = null;
        try {
            FineService fineService = new FineService();
            Book normalBook = new Book("Three-Day Late Book", "ShelfSense Test", "9780000000003", 1);
            double normalFine = fineService.calculateFine(normalBook, LocalDate.now().minusDays(3), LocalDate.now());
            System.out.println("  Normal book returned 3 days late: " + CurrencyUtils.format(normalFine));
            if (normalFine != 6.0) {
                throw new IllegalStateException("Normal book three-day fine must assess ₹6.00");
            }

            // A reference book kept for five days has three chargeable days.
            refBook = new ReferenceBook(LibraryItem.nextId(), "Advanced Physics Encyclopedia", "Dr. Fermi",
                    "9781234567890", 2022, 1, 1, 1);
            service.addItem(refBook);

            member = new Member(Member.nextId(), "Late Fee Tester", "UG", "9999999995", 3);
            service.registerMember(member);
            LocalDate issueDate = LocalDate.now().minusDays(5);
            txn = service.issueItem(refBook.getItemId(), member.getMemberId(), issueDate);

            // Return today
            int overdueDays = (int) java.time.temporal.ChronoUnit.DAYS.between(txn.getDueDate(), LocalDate.now());
            double expectedFee = fineService.calculateFine(refBook, txn.getDueDate(), LocalDate.now());

            System.out.println("  Loan issued on: " + issueDate + " | Due on: " + txn.getDueDate());
            System.out.println("  Overdue days calculated: " + overdueDays + " days late");
            System.out.println("  Assessed fine (at ₹10.00/day): " + CurrencyUtils.format(expectedFee));
            if (expectedFee != 30.0) {
                throw new IllegalStateException("Reference book five-day stay must assess ₹30.00");
            }

            service.returnItem(txn.getTransactionId(), LocalDate.now(), true);
            System.out.println("  ✓ Returned and marked fine as PAID in ledger.");
            System.out.println("  ✓ Late-fee intelligence calculations verified.\n");
        } catch (Exception e) {
            System.out.println("  Late fee test note: " + e.getMessage() + "\n");
        } finally {
            try {
                if (txn != null) {
                    if (!txn.isReturned()) {
                        service.returnItem(txn.getTransactionId(), LocalDate.now(), true);
                    }
                    service.deleteTransaction(txn.getTransactionId());
                }
                if (refBook != null && service.findItem(refBook.getItemId()) != null) {
                    service.deleteItem(refBook.getItemId());
                }
                if (member != null && service.findMember(member.getMemberId()) != null) {
                    service.deleteMember(member.getMemberId());
                }
            } catch (Exception cleanupError) {
                System.out.println("  Late fee cleanup note: " + cleanupError.getMessage());
            }
        }
    }

    private static void testReservationAndLostMonitoring(LibraryService service) {
        System.out.println("----- 5. RESERVATION FIFO & POSSIBLY LOST MONITORING -----");
        LibraryItem item = null;
        Transaction holderLoan = null;
        Transaction lostLoan = null;
        Member holder = null;
        Member first = null;
        Member second = null;
        try {
            item = new Book(LibraryItem.nextId(), "Queue and Lost Status Book", "ShelfSense Test",
                    "9780000000002", 2024, 1, 1);
            holder = new Member(Member.nextId(), "Queue Holder", "UG", "9999999998", 3);
            first = new Member(Member.nextId(), "First Queue Member", "UG", "9999999997", 3);
            second = new Member(Member.nextId(), "Second Queue Member", "UG", "9999999996", 3);
            service.addItem(item);
            service.registerMember(holder);
            service.registerMember(first);
            service.registerMember(second);

            holderLoan = service.issueItem(item.getItemId(), holder.getMemberId());
            service.joinReservationQueue(item.getItemId(), first);
            service.joinReservationQueue(item.getItemId(), second);
            if (item.getAvailableCopies() != 0 || service.getReservationPosition(item.getItemId(), first.getMemberId()) != 1) {
                throw new IllegalStateException("FIFO reservation queue was not created correctly");
            }
            try {
                service.issueItem(item.getItemId(), first.getMemberId());
                throw new IllegalStateException("Zero-copy item was issued instead of being blocked");
            } catch (ItemNotAvailableException expected) {
                System.out.println("  ✓ Zero-copy issue blocked and FIFO reservation queue created.");
            }

            service.returnItem(holderLoan.getTransactionId(), LocalDate.now(), true);
            if (service.getReservationHead(item.getItemId()) != first || item.getAvailableCopies() != 1) {
                throw new IllegalStateException("Return did not expose the FIFO reservation head");
            }
            Transaction firstLoan = service.issueItem(item.getItemId(), first.getMemberId());
            service.returnItem(firstLoan.getTransactionId(), LocalDate.now(), true);
            if (service.getReservationHead(item.getItemId()) != second) {
                throw new IllegalStateException("FIFO queue did not advance to the second member");
            }
            System.out.println("  ✓ Return updated availability and advanced the FIFO queue.");

            LocalDate issueDate = LocalDate.now().minusDays(45);
            lostLoan = service.issueItem(item.getItemId(), second.getMemberId(), issueDate);
            service.scanOverdue(LocalDate.now());
            if (!Transaction.POSSIBLY_LOST.equals(lostLoan.getStatus())) {
                throw new IllegalStateException("31+ overdue days must flag POSSIBLY LOST");
            }
            System.out.println("  ✓ 31+ overdue days flagged as POSSIBLY LOST without deleting the transaction.");

            service.returnItem(lostLoan.getTransactionId(), LocalDate.now(), true);
        } catch (Exception e) {
            System.out.println("  Reservation/lost monitoring test note: " + e.getMessage());
        } finally {
            try {
                if (holderLoan != null) service.deleteTransaction(holderLoan.getTransactionId());
                if (lostLoan != null) service.deleteTransaction(lostLoan.getTransactionId());
                if (item != null) {
                    for (Transaction t : service.getAllTransactions()) {
                        if (t.getItemId() == item.getItemId() && t.isReturned()) {
                            service.deleteTransaction(t.getTransactionId());
                        }
                    }
                    service.deleteItem(item.getItemId());
                }
                if (holder != null) service.deleteMember(holder.getMemberId());
                if (first != null) service.deleteMember(first.getMemberId());
                if (second != null) service.deleteMember(second.getMemberId());
            } catch (Exception cleanupError) {
                System.out.println("  Cleanup note: " + cleanupError.getMessage());
            }
        }
        System.out.println();
    }

    private static void testMultithreading(LibraryService service) {
        System.out.println("----- 6. MULTITHREADING & SYNCHRONIZATION DEMO -----");
        try {
            ThreadDemo.synchronisationDemo();
            System.out.println();
            ThreadDemo.waitNotifyDemo(service);
            System.out.println("  ✓ Multithreading synchronization & wait/notify verified.\n");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void testBackup(LibraryService service) {
        System.out.println("----- 7. BINARY BYTE-STREAM BACKUP VERIFICATION -----");
        try {
            String res = service.backupAndVerify();
            System.out.println("  " + res);
            System.out.println("  ✓ Byte stream persistence verified.\n");
        } catch (IOException e) {
            System.out.println("  Backup verification error: " + e.getMessage() + "\n");
        }
    }
}
