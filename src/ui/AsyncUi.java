package ui;

import java.awt.EventQueue;
import java.util.function.Consumer;

/** Runs file-backed work away from the AWT event thread and returns UI work safely. */
public final class AsyncUi {
    private AsyncUi() {
    }

    public interface Work {
        void run() throws Exception;
    }

    public static void run(String threadName, Work work, Runnable success, Consumer<Exception> failure) {
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    work.run();
                    if (success != null) {
                        EventQueue.invokeLater(success);
                    }
                } catch (Exception error) {
                    if (failure != null) {
                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                failure.accept(error);
                            }
                        });
                    }
                }
            }
        }, threadName);
        worker.setDaemon(true);
        worker.start();
    }
}
