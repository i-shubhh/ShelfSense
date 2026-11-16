package app;

import java.awt.GraphicsEnvironment;
import java.awt.EventQueue;
import java.io.IOException;
import service.LibraryService;
import ui.ShelfSenseFrame;

/**
 * Unified launcher for ShelfSense.
 */
public final class ShelfSense {

    private ShelfSense() {
    }

    public static void main(String[] args) {
        LibraryService service = new LibraryService();
        try {
            service.initialize();

            if (GraphicsEnvironment.isHeadless()) {
                ConsoleDemo.run(service);
                return;
            }

            // Launch the AWT Desktop UI on the event dispatch thread.
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new ShelfSenseFrame(service);
                }
            });

        } catch (IOException e) {
            System.err.println("Fatal Storage Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
