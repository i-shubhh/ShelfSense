import app.ConsoleDemo;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import service.LibraryService;
import ui.ShelfSenseFrame;

/**
 * Root Entry Point for ShelfSense.
 *
 * Compilation & Execution Commands (Standard JDK):
 *   javac -d out $(find src -name "*.java")
 *   java -cp out Main
 *
 * Console test suite:
 *   java -cp out Main demo
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
                new ShelfSenseFrame(service);
            }
        } catch (IOException e) {
            System.err.println("Fatal Storage / I/O Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

