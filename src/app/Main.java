package app;

import java.awt.GraphicsEnvironment;
import java.awt.EventQueue;
import java.io.IOException;
import service.LibraryService;
import ui.ShelfSenseFrame;

/**
 * Entry point for ShelfSense.
 * Supports launching the graphical AWT desktop interface or the console demonstration test suite.
 *
 * Terminal usage:
 *   java -cp out app.Main          -> Opens the modern AWT desktop application
 *   java -cp out app.Main demo     -> Executes the comprehensive verification test suite
 */
public class Main {

    public static void main(String[] args) {
        LibraryService service = new LibraryService();
        try {
            service.initialize();

            boolean demoRequested = args.length > 0 && (args[0].equalsIgnoreCase("demo")
                    || args[0].equalsIgnoreCase("--demo")
                    || args[0].equalsIgnoreCase("-d"));

            if (demoRequested || GraphicsEnvironment.isHeadless()) {
                ConsoleDemo.run(service);
            } else {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        new ShelfSenseFrame(service);
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("Fatal Storage / I/O Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
