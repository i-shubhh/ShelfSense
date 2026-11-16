# ShelfSense - Mini Project Report
SRM Apex University, Central Library | Java Programming (Units I-IV)

Team members and contribution (fill in your names):

| Member | Roll no. | Work done |
|---|---|---|
| Member 1 | | model package (LibraryItem, Book, ReferenceBook, Magazine, DigitalMedia, Member, Transaction), final/abstract/interface |
| Member 2 | | storage package (DataManager), File I/O persistence, atomic write, loading & verification, DAO abstraction |
| Member 3 | | threads package, synchronisation, wait/notify, BackupManager (byte streams) |
| Member 4 | | AWT interface (ShelfSenseFrame), events, exceptions, Validator, testing and report |

## 1. Problem and solution
The library had slow circulation, hand-calculated late fees, reference books taken home, no borrow-limit check,
double issue of the last copy, and no backup of the register. ShelfSense is a pure Core Java desktop system that stores records
persistently via structured Java File I/O (items.txt, members.txt, transactions.txt), applies business rules (borrow limits, per-type late fees, reservation queue, overdue flags),
runs background threads, backs up the log in a binary file, generates cryptographically unique SecureRandom Transaction IDs, and offers a modern AWT interface.

## 2. Design
* Hierarchy: `LibraryItem` (abstract, implements `Borrowable`) -> `Book` -> `ReferenceBook` (final); `Magazine` and `DigitalMedia` extend `LibraryItem`.
* Layers: `ui` calls `service`; `service` calls `dao` and `model`; only `dao` contains SQL; `threads` and `io` support the service.
* Exceptions: `LibraryException` is the common parent of `ItemNotAvailableException`, `BorrowLimitExceededException`, `InvalidIsbnException`.
* Business rules: Book Rs. 2/day; Reference Rs. 10/day beyond the 2-day loan; Magazine Rs. 1/day (max Rs. 50); Digital Rs. 3/day (max Rs. 150);
  no fine above `MAX_FINE_PER_ITEM` (Rs. 500); zero copies -> member joins the reservation queue; limit reached -> issue blocked;
  overdue over 30 days -> "POSSIBLY LOST". Borrow limits: UG 3, PG 5, PhD 8, Faculty 10, Staff 4.

## 3. Syllabus topic to class and method map

| Topic | Class | Method / place |
|---|---|---|
| Data types, variables, operators, expressions, control statements | LibraryService, Member | `scanOverdue` (if/else, ternary), `limitFor` (switch), `seedRandomData` (loops) |
| Arrays | LibraryService, Member | `int[12]` in `monthlyIssues`; `String[] Member.CATEGORIES`; `String[] MONTHS` |
| Classes, objects, constructors | model.* | all model classes |
| this reference, this(...) chaining | Book, Member, Transaction | `Book(String)` -> `Book(String,String)` -> `Book(String,String,String,int)` -> main constructor |
| static | LibraryItem, Member, Transaction | `nextId()` counters, `FINE_RATES`, `Validator` static methods |
| final | LibraryItem, ReferenceBook | constant `MAX_FINE_PER_ITEM`; final method `generateItemCode()`; `final class ReferenceBook` |
| Inheritance, super | Book, ReferenceBook | `super(...)` in every constructor; `super.displayDetails()` |
| Multilevel hierarchy | LibraryItem -> Book -> ReferenceBook | model package |
| Abstract class | LibraryItem | `calculateLateFee(int)` and `getType()` are abstract |
| Interface | Borrowable | `issueItem`, `returnItem`, `isAvailable` implemented in LibraryItem |
| Packages and access specifiers | all | private fields; protected constructors/`feeFor`; default `LibraryItem.isValidYear`; public API |
| Method overloading | LibraryService | `searchItem(int)`, `searchItem(String)`, `searchItem(String,int)`; `issueItem(int,int)` and `issueItem(int,int,LocalDate)` |
| Constructor overloading | Book, Member, Transaction | three Book constructors, two Member and two Transaction constructors |
| Overriding, dynamic dispatch | LibraryItem subclasses | `calculateLateFee`, `displayDetails` called through a `LibraryItem` reference (`ConsoleDemo.polymorphism`, `LibraryService.returnItem`) |
| Object class | LibraryItem, Member | `toString`, `equals`, `hashCode` |
| Exception hierarchy | exception package | `LibraryException` and 3 children |
| try, catch, multiple catch, finally | ShelfSenseFrame, BackupManager | `actionPerformed` (7 catch blocks); `export` and `restore` (finally closes streams) |
| Nested try | BackupManager | `restore`: inner try parses the bytes, outer try handles the file |
| throw, throws | LibraryService, Validator | `issueItem` throws and declares the three exceptions; `Validator.validateIsbn` |
| Built-in exceptions | various | NumberFormatException (`Validator.parseInt`), ArithmeticException (`monthlyReport`), ArrayIndexOutOfBounds (`BackupManager.restore`, UncaughtExceptionDemo), NullPointerException (`ConsoleDemo.errors`, ui), IOException, SQLException (dao), InterruptedException (threads) |
| User-defined exceptions | exception | `ItemNotAvailableException`, `BorrowLimitExceededException`, `InvalidIsbnException` |
| Thread class, Runnable | threads | `OverdueScannerThread extends Thread`; `BackupTask implements Runnable` |
| Thread life cycle, priorities | ThreadDemo | `lifecycleAndPriorities` prints `getState()`/`getPriority()` (BackupTask MIN=1, scanner NORM=5) |
| Synchronisation | LibraryItem, ThreadDemo | `synchronized issueItem` vs `issueItemUnsafe`; `runRace(false/true)` |
| Inter-thread communication | LibraryItem, DataSignal, ReservationThread | `waitUntilAvailable()` wait() / `returnItem()` notifyAll(); `DataSignal` wait/notify |
| Byte streams | BackupManager | `FileOutputStream`, `FileInputStream`, manual int/long to byte conversion |
| AWT, delegation event model | ShelfSenseFrame | Frame, Panels, CardLayout, TextField, Choice, List, Button, TextArea |
| ActionEvent / ItemEvent / KeyEvent / MouseEvent / WindowEvent | ShelfSenseFrame | `actionPerformed`, `itemStateChanged`, `attachKeys`, MouseAdapter, WindowAdapter |
| Adapter classes | ShelfSenseFrame | `WindowAdapter.windowClosing` closes the database; `MouseAdapter.mouseClicked` shows details; `KeyAdapter` shortcuts |
| File I/O Persistence & DAO | storage, dao | DataManager (items.txt, members.txt, transactions.txt), atomic write, ItemDao, MemberDao, TransactionDao (CRUD) |
| ArrayList | LibraryService | `catalogue` |
| LinkedList | LibraryService | `reservations` (queue per item) |
| Vector | LibraryService | `activityLog` (several threads write) |
| HashSet | LibraryService | `isbns` (duplicate rejection in `addItem`) |
| TreeSet | LibraryService | `titles` (alphabetical display) |
| HashMap | LibraryService | `members` (lookup by id) |
| Hashtable | LibraryItem | `FINE_RATES` (fine per type) |
| Iterator | LibraryService | `returnItem` removes the returned transaction from `issued`; `scanOverdue` |

## 4. Collection choices (interface type and implementation)
* `List<LibraryItem>` as ArrayList: the catalogue is read often by index and sorted; few inserts in the middle.
* `Map<Integer, LinkedList<Member>>`: LinkedList gives cheap `addLast` and `peekFirst`, which is exactly a waiting queue.
* `Vector<String>`: already synchronized, so several threads can append log lines safely.
* `Set<String>` as HashSet: constant-time duplicate ISBN check; order is not needed.
* `Set<String>` as TreeSet: titles stay sorted without extra sorting code.
* `Map<Integer, Member>` as HashMap: constant-time member lookup by id.
* `Hashtable<String, Double>`: small shared table of fine rates read by many threads (synchronized).

## 5. Uncaught exception (test class `app.UncaughtExceptionDemo`)
Output:
```
Recording an issue in month 3 ...
Recording an issue in month 13 ...
Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index 12 out of bounds for length 12
	at app.UncaughtExceptionDemo.recordIssue(UncaughtExceptionDemo.java:15)
	at app.UncaughtExceptionDemo.main(UncaughtExceptionDemo.java:23)
```
Explanation: the array has 12 elements, so valid indexes are 0 to 11. Month 13 gives index 12. No `catch` block exists, so the
exception travels up from `recordIssue` to `main` and then to the JVM, which prints the exception name, the message and the
stack trace (the method where it happened first, its caller below it) and ends the `main` thread. The last `println` never runs.

## 6. Sample outputs (from `./run.sh demo`)
Issue slip, return slip, error messages, synchronisation comparison, wait/notify, thread activity log, backup and restore,
overdue report and yearly report are printed by the console demo. Example:
```
WITHOUT synchronisation: copies issued = 2, refused = 0, available copies = -1  <-- WRONG (negative stock)
WITH synchronisation   : copies issued = 1, refused = 1, available copies = 0  <-- correct
```
Without synchronisation both counters pass the check `availableCopies > 0` before either one subtracts, so one copy is issued twice.
With `synchronized`, the check and the update happen as one step; the second counter sees zero copies and is refused.

## 7. Testing
* `./run.sh demo` runs every module; the backup verification compares each restored record with the database.
* Tables hold at least 20 records (24 items, 22 members, 25+ transactions).
* Validation tested: 12-digit ISBN, text in numeric fields, empty names, 9-digit phone, duplicate ISBN, delete of an issued item.

## 8. Conclusion
ShelfSense follows the structure and rules of real circulation software: persistent storage, enforced business rules,
safe concurrent updates and a recoverable binary backup.
