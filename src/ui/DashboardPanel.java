package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Panel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import model.LibraryItem;
import model.Member;
import model.Transaction;
import service.LibraryService;
import ui.components.ModernButton;
import ui.components.ModernDialog;
import ui.components.ModernTable;
import ui.components.StatCard;
import ui.components.UITheme;
import util.DateUtils;
import util.CurrencyUtils;

/**
 * Dashboard Panel displaying real-time metrics, quick circulation actions,
 * overdue alert banners, and recent circulation transactions.
 */
public class DashboardPanel extends Panel {

    private final LibraryService service;
    private final Consumer<Sidebar.Section> navigationHandler;

    private StatCard cardItems;
    private StatCard cardMembers;
    private StatCard cardIssued;
    private StatCard cardAvailable;
    private StatCard cardOverdue;
    private StatCard cardFines;

    private ModernTable recentTable;
    private Panel alertBanner;

    public DashboardPanel(LibraryService service, Consumer<Sidebar.Section> navigationHandler) {
        this.service = service;
        this.navigationHandler = navigationHandler;
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.MAIN_BG);

        initUI();
        refresh();
    }

    private void initUI() {
        // North Container: Header + Stat Cards + Actions
        Panel northPanel = new Panel(new BorderLayout(10, 10));
        northPanel.setBackground(UITheme.MAIN_BG);

        // Header Banner
        Panel headerPanel = new Panel(new BorderLayout()) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                UITheme.applyAntialiasing(g2);
                g2.setFont(UITheme.FONT_HERO);
                g2.setColor(UITheme.TEXT_DARK);
                g2.drawString("Circulation Intelligence Dashboard", 4, 26);

                g2.setFont(UITheme.FONT_BODY);
                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString("Real-time overview of library inventory, member loans, and late-fee telemetry", 4, 48);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(800, 58);
            }
        };
        northPanel.add(headerPanel, BorderLayout.NORTH);

        // 6 Metrics Cards Grid (2 rows x 3 columns)
        Panel cardsGrid = new Panel(new GridLayout(2, 3, 12, 12));
        cardsGrid.setBackground(UITheme.MAIN_BG);

        cardItems = new StatCard("Total Items", "0", "Across 4 media categories", StatCard.CardTheme.BLUE, StatCard.IconKind.ITEMS);
        cardMembers = new StatCard("Registered Members", "0", "UG, PG, PhD & Faculty", StatCard.CardTheme.PURPLE, StatCard.IconKind.MEMBERS);
        cardIssued = new StatCard("Currently Issued", "0", "Active member loans", StatCard.CardTheme.BLUE, StatCard.IconKind.ISSUED);
        cardAvailable = new StatCard("Available Copies", "0", "Ready on shelves", StatCard.CardTheme.GREEN, StatCard.IconKind.AVAILABLE);
        cardOverdue = new StatCard("Overdue Loans", "0", "Requiring check-in", StatCard.CardTheme.RED, StatCard.IconKind.OVERDUE);
        cardFines = new StatCard("Fines Collected", CurrencyUtils.format(0.0),
                "Outstanding: " + CurrencyUtils.format(0.0), StatCard.CardTheme.AMBER, StatCard.IconKind.FINES);

        cardsGrid.add(cardItems);
        cardsGrid.add(cardMembers);
        cardsGrid.add(cardIssued);
        cardsGrid.add(cardAvailable);
        cardsGrid.add(cardOverdue);
        cardsGrid.add(cardFines);

        northPanel.add(cardsGrid, BorderLayout.CENTER);

        // Quick Actions Bar
        Panel actionsBar = new Panel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        actionsBar.setBackground(UITheme.MAIN_BG);

        ModernButton btnQuickIssue = new ModernButton("Issue Item", ModernButton.ButtonType.PRIMARY, ModernButton.IconType.ISSUE);
        btnQuickIssue.addActionListener(e -> navigationHandler.accept(Sidebar.Section.ISSUE));

        ModernButton btnQuickReturn = new ModernButton("Return & Check-in", ModernButton.ButtonType.SUCCESS, ModernButton.IconType.RETURN);
        btnQuickReturn.addActionListener(e -> navigationHandler.accept(Sidebar.Section.RETURN));

        ModernButton btnAddItem = new ModernButton("Add Item", ModernButton.ButtonType.SECONDARY, ModernButton.IconType.PLUS);
        btnAddItem.addActionListener(e -> navigationHandler.accept(Sidebar.Section.CATALOGUE));

        ModernButton btnAddMember = new ModernButton("Register Member", ModernButton.ButtonType.SECONDARY, ModernButton.IconType.PLUS);
        btnAddMember.addActionListener(e -> navigationHandler.accept(Sidebar.Section.MEMBERS));

        ModernButton btnAudit = new ModernButton("Run Overdue Audit", ModernButton.ButtonType.WARNING, ModernButton.IconType.REFRESH);
        btnAudit.addActionListener(e -> {
            AsyncUi.run("ShelfSense-OverdueAudit", new AsyncUi.Work() {
                @Override
                public void run() {
                    service.scanOverdue(LocalDate.now());
                }
            }, new Runnable() {
                @Override
                public void run() {
                    refresh();
                }
            }, new java.util.function.Consumer<Exception>() {
                @Override
                public void accept(Exception error) {
                    ModernDialog.showError(null, "Audit Error", error.getMessage());
                }
            });
        });

        actionsBar.add(btnQuickIssue);
        actionsBar.add(btnQuickReturn);
        actionsBar.add(btnAddItem);
        actionsBar.add(btnAddMember);
        actionsBar.add(btnAudit);

        northPanel.add(actionsBar, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);

        // Center: Recent Transactions Table
        Panel centerPanel = new Panel(new BorderLayout(8, 8));
        centerPanel.setBackground(UITheme.MAIN_BG);

        Panel tableTitlePanel = new Panel(new BorderLayout()) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                UITheme.applyAntialiasing(g2);
                g2.setFont(UITheme.FONT_SECTION);
                g2.setColor(UITheme.TEXT_DARK);
                 g2.drawString("Recent & Attention Transactions", 4, 18);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(500, 26);
            }
        };
        centerPanel.add(tableTitlePanel, BorderLayout.NORTH);

        String[] headers = {"Transaction ID", "Member", "Library Item", "Issue Date", "Due Date", "Status", "Fine"};
        float[] weights = {2.2f, 1.8f, 2.5f, 1.2f, 1.2f, 1.2f, 1.0f};
        recentTable = new ModernTable(headers, weights);
        recentTable.setRowsPerPage(8);

        recentTable.setDoubleClickListener((idx, row) -> {
            // Quick jump to Return panel with selected Transaction ID
            navigationHandler.accept(Sidebar.Section.RETURN);
        });

        centerPanel.add(recentTable, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    public synchronized void refresh() {
        int itemsCount = service.getTotalItemsCount();
        int membersCount = service.getTotalMembersCount();
        int issuedCount = service.getCurrentlyIssuedCount();
        int availableCount = service.getAvailableCopiesCount();
        int overdueCount = service.getOverdueCount();
        int possiblyLostCount = service.getPossiblyLostCount();
        double finesCollected = service.getTotalFinesCollected();
        double outstandingFines = service.getTotalOutstandingFines();

        cardItems.setValue(String.valueOf(itemsCount));
        cardMembers.setValue(String.valueOf(membersCount));
        cardIssued.setValue(String.valueOf(issuedCount));
        cardAvailable.setValue(String.valueOf(availableCount));

        cardOverdue.setValue(String.valueOf(overdueCount));
        cardOverdue.setSubtitle(possiblyLostCount > 0
                ? "POSSIBLY LOST: " + possiblyLostCount
                : (overdueCount > 0 ? "Needs Check-in" : "No Overdues"));

        cardFines.setValue(CurrencyUtils.format(finesCollected));
        cardFines.setSubtitle("Unpaid: " + CurrencyUtils.format(outstandingFines));

        // Load recent transactions into table
        List<Transaction> recents = service.getRecentTransactions(20);
        for (Transaction lost : service.getPossiblyLostTransactions()) {
            if (!recents.contains(lost)) {
                recents.add(lost);
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (Transaction t : recents) {
            Member m = service.findMember(t.getMemberId());
            LibraryItem it = service.findItem(t.getItemId());

            String memberStr = (m != null) ? m.getName() + " (" + m.getCode() + ")" : "ID " + t.getMemberId();
            String itemStr = (it != null) ? it.getTitle() + " [" + it.generateItemCode() + "]" : "ID " + t.getItemId();
            String fineStr = CurrencyUtils.format(t.getFine());
            String statusStr = t.getStatus();
            if (!t.isReturned() && t.getOverdueDays(java.time.LocalDate.now()) > 0) {
                statusStr += " (" + t.getOverdueDays(java.time.LocalDate.now()) + " days)";
            }

            rows.add(new String[]{
                t.getTransactionId(),
                memberStr,
                itemStr,
                DateUtils.formatDisplay(t.getIssueDate()),
                DateUtils.formatDisplay(t.getDueDate()),
                statusStr,
                fineStr
            });
        }
        recentTable.setData(rows);
    }
}
