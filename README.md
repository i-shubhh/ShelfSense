# ShelfSense: The Smart Library Circulation & Late-Fee Intelligence System

A desktop library circulation and late-fee intelligence system built **exclusively using 100% Core Java and Java AWT (`java.awt`)**.

Designed with a modern SaaS aesthetic, custom vector component painting via `Graphics2D`, automated cryptographically random Transaction ID generation, academic quota enforcement, polymorphic fine calculation, thread synchronization, and persistent Java File I/O.

---

## 1. Strict Technology Stack Compliance

| Technology Requirement | Status | Implementation Details |
|---|---|---|
| **Programming Language** | **Core Java (JDK 11+)** | Zero external frameworks or libraries. Standard JDK only. |
| **User Interface** | **Java AWT (`java.awt`)** | `Frame`, `Panel`, `Canvas`, `Dialog`, `CardLayout`, `GridBagLayout`, `BorderLayout`, `FlowLayout`, `Graphics2D`. *(Strictly NO JavaFX or Swing)* |
| **Event Handling** | **`java.awt.event`** | Action listeners, mouse hover/pressed listeners, window adapters, key shortcuts. |
| **Data Persistence** | **Java File I/O** | Structured delimited text files (`data/items.txt`, `data/members.txt`, `data/transactions.txt`) and binary byte streams (`io.BackupManager`). *(Strictly NO MySQL or SQLite)* |
| **Date & Time API** | **`java.time`** | `LocalDate`, `LocalDateTime`, `ChronoUnit`, `DateTimeFormatter`. |
| **Collections Framework** | **Java Collections** | `List`, `ArrayList`, `LinkedList`, `Set`, `HashSet`, `TreeSet`, `Map`, `HashMap`, `Hashtable`, `Vector`. |
| **Concurrency** | **Java Multithreading** | Background `OverdueScannerThread`, `BackupTask`, `ReservationThread`, synchronized methods, monitors (`wait()` / `notifyAll()`). |
| **Build & Tooling** | **Direct JDK (`javac` / `java`)** | Ordinary terminal commands. *(Strictly NO Maven, Gradle, or external build tools)* |

---

## 2. Compilation and Execution

The project compiles and runs using standard Terminal commands on macOS, Linux, and Windows.

### Standard Terminal Commands (macOS / Linux):

```bash
# 1. Compile all Java sources into out/ directory
javac -d out $(find src -name "*.java")

# 2. Run the modern AWT desktop application
java -cp out Main

# 3. Or run the verification test suite in console mode:
java -cp out Main demo
```

### Alternatively, using Convenience Scripts:

```bash
./build.sh          # Compiles all Java sources
./run.sh            # Launches the modern AWT desktop UI
./run.sh demo       # Runs the automated verification suite
```

---

## 3. Architecture & Project Layout

```
ShelfSense/
├── src/
│   ├── Main.java                          # Root entry point for standard terminal execution
│   │
│   ├── app/
│   │   ├── Main.java                      # Application launcher (Desktop UI or Demo mode)
│   │   ├── ShelfSense.java                # Unified desktop launcher
│   │   ├── ConsoleDemo.java               # Comprehensive verification test suite
│   │   └── UncaughtExceptionDemo.java     # Demonstration of uncaught exception handling
│   │
│   ├── model/
│   │   ├── Borrowable.java                # Interface for circulating entities
│   │   ├── LibraryItem.java               # Abstract parent class with synchronized loan methods
│   │   ├── Book.java                      # Level 2 inheritance: circulating books
│   │   ├── ReferenceBook.java             # Level 3 inheritance (final class): 2-day short loan
│   │   ├── Magazine.java                  # Periodicals and journals
│   │   ├── DigitalMedia.java              # Multimedia, audiobooks, and video lectures
│   │   ├── Member.java                    # Library patrons with academic borrowing limits
│   │   ├── Transaction.java               # Circulation record with random String Transaction ID
│   │   └── ItemFactory.java               # Factory Pattern constructing items from raw specs
│   │
│   ├── service/
│   │   ├── LibraryService.java            # Main Facade coordinating collections & persistence
│   │   ├── IssueService.java              # Validates limits and generates random Transaction IDs
│   │   ├── ReturnService.java             # Processes check-ins and late-fee calculations
│   │   ├── FineService.java               # Late-fee intelligence and telemetry ledger
│   │   ├── TransactionService.java        # Multi-attribute search and filtering
│   │   └── Validator.java                 # Static validation (ISBN, phone, year, text)
│   │
│   ├── ui/
│   │   ├── ShelfSenseFrame.java           # Main AWT Frame with MenuBar and CardLayout
│   │   ├── Sidebar.java                   # Dark navy navigation sidebar with live clock & badges
│   │   ├── DashboardPanel.java            # Dashboard with 6 StatCards and recent activity
│   │   ├── BookPanel.java                 # Item catalogue manager and form drawer
│   │   ├── MemberPanel.java               # Member directory and borrowing quota inspector
│   │   ├── IssuePanel.java                # Circulation issue workflow with auto ID generation
│   │   ├── ReturnPanel.java               # Check-in panel with automatic late-fee intelligence
│   │   ├── TransactionPanel.java          # Full ledger with search, filtering, and fine collection
│   │   └── components/
│   │       ├── UITheme.java               # Design tokens, color palette, and vector icon painters
│   │       ├── ModernButton.java          # Custom AWT Canvas button with hover and click effects
│   │       ├── StatCard.java              # Interactive metric card with circular vector glyphs
│   │       ├── ModernTable.java           # Custom AWT table with pagination and status badges
│   │       └── ModernDialog.java          # Styled modal confirmation & receipt dialogs
│   │
│   ├── storage/
│   │   └── DataManager.java               # File I/O persistence manager with atomic save
│   │
│   ├── threads/
│   │   ├── OverdueScannerThread.java      # Daemon thread scanning for overdue items periodically
│   │   ├── BackupTask.java                # Background Runnable exporting binary byte backups
│   │   ├── ReservationThread.java         # Inter-thread communication via wait() and notifyAll()
│   │   ├── DataSignal.java                # Thread monitor for cooperative handshakes
│   │   └── ThreadDemo.java                # Concurrency race condition & state demos
│   │
│   ├── exception/
│   │   ├── LibraryException.java          # Base checked library exception
│   │   ├── ItemNotAvailableException.java # Thrown when 0 copies remain in stock
│   │   ├── BorrowLimitExceededException.java # Thrown when member exceeds category quota
│   │   └── InvalidIsbnException.java      # Thrown when ISBN does not match 10/13 digits
│   │
│   └── util/
│       ├── IDGenerator.java               # Cryptographically unique SecureRandom ID generator
│       └── DateUtils.java                 # Java LocalDate calculations and formatters
│
├── data/
│   ├── items.txt                          # Persistent library inventory
│   ├── members.txt                        # Persistent member database
│   └── transactions.txt                   # Persistent circulation transactions
│
├── build.sh                               # macOS / Linux build script
├── run.sh                                 # macOS / Linux execution script
└── README.md                              # Technical documentation
```

---

## 4. Key Features & Implementation Highlights

### 1. Modern AWT Desktop UI
* **No outdated 1990s default components**: All primary visual elements (buttons, metric cards, data tables, navigation sidebar, and confirmation dialogs) are custom-painted using standard `java.awt.Graphics2D`.
* **Antialiasing & Typography**: `RenderingHints.KEY_ANTIALIASING` and `KEY_TEXT_ANTIALIASING` are applied to eliminate jagged edges and render crisp typography.
* **Vector Iconography**: Vector glyphs (books, patrons, outbox, inbox, clocks, currency, search lens, alert triangles) are rendered natively using geometric 2D paths.
* **Double Buffering**: Prevents screen flickering across all custom Canvas components.

### 2. Random Transaction ID Generation (Requirement 8)
* Every newly issued transaction is automatically assigned a cryptographically random unique Transaction ID using `java.security.SecureRandom`.
* Format: `SHELF-TXN-XXXXXXXX` (e.g., `SHELF-TXN-8F3A92C1`).
* **Zero manual input**: The user never types an ID when issuing an item.
* **Uniqueness guarantee**: Generated IDs are verified against existing active and historical records to guarantee zero collisions.
* **Permanent persistence**: The ID is permanently stored in `data/transactions.txt` and is searchable across the application.

### 3. Late Fee Intelligence (Requirement 9)
* Automatically calculates overdue days using Java's `LocalDate` API (`ChronoUnit.DAYS.between(dueDate, returnOrCurrentDate)`).
* **Polymorphic fine rates**:
   * **Books**: ₹2.00 / overdue day (standard 14-day loan)
   * **Reference Books**: ₹10.00 / additional overdue day after the 2-day free period
   * **Magazines / Periodicals**: ₹1.00 / day (capped at ₹50.00)
   * **Digital Media**: ₹3.00 / day (capped at ₹150.00)
* Tracks **paid vs. unpaid fines** and displays total outstanding revenue on the dashboard and ledger.

### 4. Member Management & Quota Enforcement (Requirement 6)
* Members belong to academic categories with strictly enforced borrowing limits:
  * **UG (Undergraduate)**: 3 items
  * **PG (Postgraduate)**: 5 items
  * **PhD (Doctoral Scholar)**: 8 items
  * **Faculty**: 10 items
  * **Staff**: 4 items
* Automatically generates unique Member IDs (`M001`, `M002`, ...).
* Prevents issuing books once a member reaches their borrowing quota.

### 5. Data Persistence (Requirement 11)
* **Zero external databases**: No MySQL, SQLite, or external drivers required.
* All data is preserved across application restarts in human-readable, structured text files under `data/`:
  * `data/items.txt`
  * `data/members.txt`
  * `data/transactions.txt`
* **Atomic file writes**: Writes to a `.tmp` file before renaming, preventing file corruption in case of unexpected termination.
* **Fault-tolerant loading**: Gracefully skips corrupted lines without crashing the application.
* **Automatic seeding**: If the files are missing, the system seeds 22+ items, 22+ members, and 28+ realistic transactions.

### 6. Multithreading & Synchronization (Requirement 12)
* **Thread Safety**: All public mutating methods on `LibraryService` are `synchronized`.
* **OverdueScannerThread**: Periodically scans active transactions in the background and flags overdue records.
* **BackupTask**: Exports a binary byte-stream backup (`library_backup.dat`) asynchronously.
* **ReservationThread**: Demonstrates inter-thread communication using `wait()` and `notifyAll()` when a member waits for an out-of-stock item.
* **Race Condition Prevention**: Prevents negative stock when two librarian counters issue the last copy simultaneously.

---

## 5. Verification Test Suite Output

Running `java -cp out Main demo` executes the test suite:

```
================================================================================
  ShelfSense: The Smart Library Circulation & Late-Fee Intelligence System
  Core Java & AWT Engine Verification Suite
================================================================================

Inventory Telemetry:
  • Total Library Items:     22
  • Registered Members:      22
  • Active Loans (Issued):   14
  • Overdue Loans:           9
  • Total Fines Collected:   ₹136.00
  • Outstanding Fines:       ₹527.00
  • All-Time Transactions:   28

----- 1. OOP POLYMORPHISM & INHERITANCE TEST -----
  [BOOK     ] Clean Architecture           | Loan Days: 14 | Late Fee for 5 days late: ₹10.00
  [REFERENCE] Encyclopaedia of Physics     | Loan Days:  2 | Late Fee for 5 overdue days: ₹50.00
  [MAGAZINE ] IEEE Micro                   | Loan Days:  7 | Late Fee for 5 days late: ₹5.00
  [DIGITAL  ] Advanced Java Video Lab      | Loan Days:  7 | Late Fee for 5 days late: ₹15.00
  ✓ Dynamic method dispatch verified across all 4 item subclasses.

----- 2. RANDOM TRANSACTION ID GENERATION (REQUIREMENT 8) -----
  Auto-generated Transaction ID #1: SHELF-TXN-C267C1C8
  Auto-generated Transaction ID #2: SHELF-TXN-B5D0D8DB
  Format check (SHELF-TXN-XXXXXXXX): PASSED ✓
  Uniqueness check between IDs:      PASSED ✓
  ✓ Verified automated SecureRandom generation & permanent persistence.

----- 3. CIRCULATION & BORROW LIMIT VALIDATION -----
  Testing borrow limit for UG member (Max: 3 items)...
  Issued 3 items successfully.
  ✓ PASSED: BorrowLimitExceededException caught properly: Member M9999 (Limit Tester) has reached their borrowing limit of 3 items (UG quota).
  ✓ Items returned and test member cleaned up.

----- 4. LATE-FEE INTELLIGENCE TEST (REQUIREMENT 9) -----
  Loan issued on: 2026-11-05 | Due on: 2026-11-07
  Overdue days calculated: 8 days late
  Assessed fine (at ₹10.00/day): ₹80.00
  ✓ Returned and marked fine as PAID in ledger.
  ✓ Late-fee intelligence calculations verified.

----- 5. MULTITHREADING & SYNCHRONIZATION DEMO -----
----- SYNCHRONISATION DEMO -----
WITHOUT synchronisation: copies issued = 2, refused = 0, available copies = -1  <-- WRONG (negative stock)
WITH synchronisation   : copies issued = 1, refused = 1, available copies = 0  <-- correct

----- WAIT / NOTIFY DEMO -----
Reservation thread: no copy, calling wait()
Reservation thread state: WAITING
Return operation: returnItem() calls notifyAll()
Reservation thread: notified, copy is available
Reservation thread state: TERMINATED
  ✓ Multithreading synchronization & wait/notify verified.

----- 6. BINARY BYTE-STREAM BACKUP VERIFICATION -----
  Backup file verified: 34 records restored (2407 bytes).
  ✓ Byte stream persistence verified.

================================================================================
  All ShelfSense Verification Checks Passed Successfully! (100% Core Java)
================================================================================
```

---

## 6. Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| **Ctrl + D** | Open Dashboard |
| **Ctrl + I** | Open Issue Circulation Desk |
| **Ctrl + R** | Open Return & Check-in Desk |
| **Ctrl + T** | Open Transaction History |
| **Ctrl + O** | Run Overdue Circulation Audit |
| **Ctrl + B** | Export Binary Backup |
| **F5** | Refresh All Data & Telemetry |
| **Ctrl + Q** | Exit Application |
