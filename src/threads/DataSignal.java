package threads;

/** Small monitor used for inter-thread communication: one thread wait()s, another notify()s. */
public class DataSignal {

    private boolean ready = false;

    /** Blocks (state WAITING) until another thread calls signalReady(). */
    public synchronized void waitForData() throws InterruptedException {
        while (!ready) {
            wait();
        }
        ready = false;
    }

    public synchronized void signalReady() {
        ready = true;
        notify();
    }
}
