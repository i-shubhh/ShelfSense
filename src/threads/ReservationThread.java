package threads;

import java.io.IOException;
import exception.LibraryException;
import model.LibraryItem;
import model.Member;
import service.LibraryService;

/**
 * A member waiting for an item: wait()s inside the item until a return operation calls notifyAll(),
 * then issues the item to that member.
 * Demonstrates thread inter-communication with wait() / notifyAll().
 */
public class ReservationThread extends Thread {

    private final LibraryService service;
    private final LibraryItem item;
    private final Member member;

    public ReservationThread(LibraryService service, LibraryItem item, Member member) {
        super("Reservation-" + member.getCode());
        this.service = service;
        this.item = item;
        this.member = member;
    }

    @Override
    public void run() {
        try {
            service.log(member.getCode() + " is waiting for " + item.generateItemCode());
            item.waitUntilAvailable(); // wait() ... released by notifyAll() in returnItem()
            service.log(member.getCode() + " was notified, issuing " + item.generateItemCode());
            service.issueItem(item.getItemId(), member.getMemberId());
        } catch (InterruptedException e) {
            service.log("Reservation cancelled for " + member.getCode());
            Thread.currentThread().interrupt();
        } catch (LibraryException e) {
            service.log("Reservation failed: " + e.getMessage());
        } catch (IOException e) {
            service.log("Persistence error during reservation: " + e.getMessage());
        }
    }
}
