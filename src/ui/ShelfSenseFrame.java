package ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.Panel;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalDate;

import service.LibraryService;
import threads.OverdueScannerThread;
import ui.components.ModernDialog;
import ui.components.UITheme;

/**
 * Main Application Window Frame for ShelfSense.
 * Built with 100% Core Java AWT, hosting the navigation Sidebar and CardLayout panels.
 */
public class ShelfSenseFrame extends Frame {

    private static final long serialVersionUID = 1L;

    private final LibraryService service;
    private final CardLayout cards = new CardLayout();
    private final Panel cardContainer = new Panel(cards);

    private Sidebar sidebar;
    private DashboardPanel dashboardPanel;
    private BookPanel bookPanel;
    private MemberPanel memberPanel;
    private IssuePanel issuePanel;
    private ReturnPanel returnPanel;
    private TransactionPanel transactionPanel;

    private OverdueScannerThread overdueScanner;

    public ShelfSenseFrame(LibraryService service) {
        super("ShelfSense — Smart Library Circulation and Late-Fee Intelligence System");
        this.service = service;

        setLayout(new BorderLayout());
        setBackground(UITheme.MAIN_BG);
        setSize(1240, 780);
        setMinimumSize(new Dimension(1020, 680));
        setLocationRelativeTo(null); // center on screen

        initMenuBar();
        initComponents();
        startBackgroundServices();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        setVisible(true);
    }

    private void initMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File Menu
        Menu menuFile = new Menu("File");
        MenuItem miDashboard = new MenuItem("Dashboard", new MenuShortcut(KeyEvent.VK_D));
        miDashboard.addActionListener(e -> navigateTo(Sidebar.Section.DASHBOARD));
        menuFile.add(miDashboard);

        MenuItem miRefresh = new MenuItem("Refresh All (F5)");
        miRefresh.addActionListener(e -> refreshAll());
        menuFile.add(miRefresh);

        MenuItem miBackup = new MenuItem("Export Binary Backup", new MenuShortcut(KeyEvent.VK_B));
        miBackup.addActionListener(e -> {
            try {
                String result = service.backupAndVerify();
                ModernDialog.showSuccess(this, "Backup Verified", "Binary Backup Exported",
                        result + "\nFile: " + LibraryService.BACKUP_FILE.getAbsolutePath(), null);
            } catch (IOException ex) {
                ModernDialog.showError(this, "Backup Error", ex.getMessage());
            }
        });
        menuFile.add(miBackup);

        menuFile.addSeparator();

        MenuItem miExit = new MenuItem("Exit", new MenuShortcut(KeyEvent.VK_Q));
        miExit.addActionListener(e -> shutdown());
        menuFile.add(miExit);
        menuBar.add(menuFile);

        // Circulation Menu
        Menu menuCirc = new Menu("Circulation");
        MenuItem miIssue = new MenuItem("Issue Item", new MenuShortcut(KeyEvent.VK_I));
        miIssue.addActionListener(e -> navigateTo(Sidebar.Section.ISSUE));
        menuCirc.add(miIssue);

        MenuItem miReturn = new MenuItem("Return & Check-in", new MenuShortcut(KeyEvent.VK_R));
        miReturn.addActionListener(e -> navigateTo(Sidebar.Section.RETURN));
        menuCirc.add(miReturn);

        MenuItem miTxn = new MenuItem("Transaction History", new MenuShortcut(KeyEvent.VK_T));
        miTxn.addActionListener(e -> navigateTo(Sidebar.Section.TRANSACTIONS));
        menuCirc.add(miTxn);

        MenuItem miAudit = new MenuItem("Run Overdue Audit Now", new MenuShortcut(KeyEvent.VK_O));
        miAudit.addActionListener(e -> {
            AsyncUi.run("ShelfSense-MenuAudit", new AsyncUi.Work() {
                @Override
                public void run() {
                    service.scanOverdue(LocalDate.now());
                }
            }, new Runnable() {
                @Override
                public void run() {
                    int overdue = service.getOverdueCount();
                    refreshAll();
                    ModernDialog.showInfo(ShelfSenseFrame.this, "Audit Complete", "Overdue Circulation Audit",
                            "Overdue scan complete. Found " + overdue + " overdue records requiring check-in.");
                }
            }, new java.util.function.Consumer<Exception>() {
                @Override
                public void accept(Exception error) {
                    ModernDialog.showError(ShelfSenseFrame.this, "Audit Error", error.getMessage());
                }
            });
        });
        menuCirc.add(miAudit);
        menuBar.add(menuCirc);

        // Help Menu
        Menu menuHelp = new Menu("Help");
        MenuItem miAbout = new MenuItem("About ShelfSense");
        miAbout.addActionListener(e -> {
            ModernDialog.showInfo(this, "About ShelfSense", "ShelfSense 2.0 Desktop",
                    "ShelfSense: The Smart Library Circulation & Late-Fee Intelligence System\n\n"
                    + "Built exclusively using 100% Core Java & Java AWT.\n"
                    + "Features automated SecureRandom Transaction ID generation,\n"
                    + "multilevel OOP inheritance for library item categories,\n"
                    + "polymorphic late fee intelligence, and persistent File I/O storage.");
        });
        menuHelp.add(miAbout);
        menuBar.add(menuHelp);

        setMenuBar(menuBar);
    }

    private void initComponents() {
        // Navigation Sidebar on West
        sidebar = new Sidebar(section -> {
            navigateTo(section);
        });
        add(sidebar, BorderLayout.WEST);

        // Card Panels in Center
        dashboardPanel = new DashboardPanel(service, this::navigateTo);
        bookPanel = new BookPanel(service, this);
        memberPanel = new MemberPanel(service, this);
        issuePanel = new IssuePanel(service, this);
        returnPanel = new ReturnPanel(service, this);
        transactionPanel = new TransactionPanel(service, this);

        cardContainer.add(dashboardPanel, Sidebar.Section.DASHBOARD.name());
        cardContainer.add(bookPanel, Sidebar.Section.CATALOGUE.name());
        cardContainer.add(memberPanel, Sidebar.Section.MEMBERS.name());
        cardContainer.add(issuePanel, Sidebar.Section.ISSUE.name());
        cardContainer.add(returnPanel, Sidebar.Section.RETURN.name());
        cardContainer.add(transactionPanel, Sidebar.Section.TRANSACTIONS.name());

        add(cardContainer, BorderLayout.CENTER);
        updateSidebarBadge();
    }

    private void startBackgroundServices() {
        // Background thread periodically scanning for overdue items
        overdueScanner = new OverdueScannerThread(service);
        overdueScanner.setOnScanComplete(() -> {
            EventQueue.invokeLater(() -> {
                updateSidebarBadge();
                if (sidebar.getCurrentSection() == Sidebar.Section.DASHBOARD) {
                    dashboardPanel.refresh();
                }
            });
        });
        overdueScanner.start();
    }

    public void navigateTo(Sidebar.Section section) {
        sidebar.setSection(section);
        cards.show(cardContainer, section.name());

        // Refresh the target panel upon navigation
        switch (section) {
            case DASHBOARD:
                dashboardPanel.refresh();
                break;
            case CATALOGUE:
                bookPanel.refresh();
                break;
            case MEMBERS:
                memberPanel.refresh();
                break;
            case ISSUE:
                issuePanel.refresh();
                break;
            case RETURN:
                returnPanel.refresh();
                break;
            case TRANSACTIONS:
                transactionPanel.refresh();
                break;
        }
        updateSidebarBadge();
    }

    public void refreshAll() {
        dashboardPanel.refresh();
        bookPanel.refresh();
        memberPanel.refresh();
        issuePanel.refresh();
        returnPanel.refresh();
        transactionPanel.refresh();
        updateSidebarBadge();
    }

    private void updateSidebarBadge() {
        int overdue = service.getOverdueCount();
        sidebar.setOverdueCount(overdue);
    }

    private void shutdown() {
        if (overdueScanner != null) {
            overdueScanner.stopScanning();
        }
        if (sidebar != null) {
            sidebar.stopClock();
        }
        try {
            service.persistAll();
        } catch (IOException e) {
            System.err.println("Error saving state on exit: " + e.getMessage());
        }
        dispose();
        System.exit(0);
    }

    @Override
    public void dispose() {
        if (overdueScanner != null) {
            overdueScanner.stopScanning();
        }
        if (sidebar != null) {
            sidebar.stopClock();
        }
        super.dispose();
    }
}
