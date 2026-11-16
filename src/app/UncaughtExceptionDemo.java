package app;

/**
 * Small test class that lets one exception go UNCAUGHT.
 * Run:  java -cp out app.UncaughtExceptionDemo
 *
 * Expected output (the explanation goes in the report):
 *   Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index 12 out of bounds for length 12
 *       at app.UncaughtExceptionDemo.recordIssue(UncaughtExceptionDemo.java:..)
 *       at app.UncaughtExceptionDemo.main(UncaughtExceptionDemo.java:..)
 */
public class UncaughtExceptionDemo {

    static void recordIssue(int[] issuesPerMonth, int month) {
        issuesPerMonth[month]++; // month 12 does not exist: valid indexes are 0..11
    }

    public static void main(String[] args) {
        int[] issuesPerMonth = new int[12];
        System.out.println("Recording an issue in month 3 ...");
        recordIssue(issuesPerMonth, 2);
        System.out.println("Recording an issue in month 13 ...");
        recordIssue(issuesPerMonth, 12); // nobody catches the exception -> the JVM prints the stack trace
        System.out.println("This line is never reached.");
    }
}
