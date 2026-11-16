package ui;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.ArrayList;
import java.util.List;

import exception.LibraryException;
import model.LibraryItem;
import model.Member;
import model.Transaction;
import service.LibraryService;
import threads.BackupTask;
import ui.components.ModernButton;
import ui.components.ModernDialog;
import ui.components.ModernTable;
import ui.components.UITheme;
import util.DateUtils;
import util.CurrencyUtils;

/**
 * Transaction History and Audit Ledger Panel.
 * Displays comprehensive historical records, searchable by generated Transaction ID,
 * filterable by status and fine state, with late fee payment collection and backup export.
 */
public class TransactionPanel extends Panel {

    private final LibraryService service;
    private final Frame parentFrame;

    private TextField tfSearch;
    private Choice chStatus;
    private Choice chFine;
    private ModernTable txnTable;
    private boolean paymentPending;

    private Label lblSummary;

    public TransactionPanel(LibraryService service, Frame parentFrame) {
        this.service = service;
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout(12, 12));
        setBackground(UITheme.MAIN_BG);

        initUI();
        refresh();
    }

    private void initUI() {
        // North: Header and Filter Toolbar
        Panel northPanel = new Panel(new BorderLayout(8, 8));
        northPanel.setBackground(UITheme.MAIN_BG);

        Panel headerPanel = new Panel(new BorderLayout()) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                UITheme.applyAntialiasing(g2);
                g2.setFont(UITheme.FONT_TITLE);
                g2.setColor(UITheme.TEXT_DARK);
                g2.drawString("Circulation Ledger & Transaction History", 4, 22);

                g2.setFont(UITheme.FONT_BODY);
                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString("Complete historical ledger of all loans, check-ins, late fee calculations, and payments", 4, 42);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(700, 50);
            }
        };
        northPanel.add(headerPanel, BorderLayout.NORTH);

        // Filter Bar
        Panel filterBar = new Panel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterBar.setBackground(UITheme.HEADER_BG);

        filterBar.add(new Label("Search:"));
        tfSearch = new TextField(18);
        tfSearch.addActionListener(e -> applyFilter());
        filterBar.add(tfSearch);

        filterBar.add(new Label("Status:"));
        chStatus = new Choice();
        chStatus.add("ALL");
        chStatus.add("ISSUED");
        chStatus.add("RETURNED");
        chStatus.add("OVERDUE");
        chStatus.add("POSSIBLY LOST");
        chStatus.addItemListener(e -> applyFilter());
        filterBar.add(chStatus);

        filterBar.add(new Label("Fine:"));
        chFine = new Choice();
        chFine.add("ALL");
        chFine.add("FINE OWED");
        chFine.add("FINE PAID");
        chFine.addItemListener(e -> applyFilter());
        filterBar.add(chFine);

        ModernButton btnFilter = new ModernButton("Filter", ModernButton.ButtonType.PRIMARY, ModernButton.IconType.SEARCH);
        btnFilter.addActionListener(e -> applyFilter());
        filterBar.add(btnFilter);

        ModernButton btnReset = new ModernButton("Reset", ModernButton.ButtonType.SECONDARY);
        btnReset.addActionListener(e -> {
            tfSearch.setText("");
            chStatus.select("ALL");
            chFine.select("ALL");
            applyFilter();
        });
        filterBar.add(btnReset);

        ModernButton btnPayFine = new ModernButton("Collect Fine", ModernButton.ButtonType.WARNING, ModernButton.IconType.MONEY);
        btnPayFine.addActionListener(e -> collectFineForSelected());
        filterBar.add(btnPayFine);

        ModernButton btnBackup = new ModernButton("Export Backup", ModernButton.ButtonType.SECONDARY, ModernButton.IconType.REFRESH);
        btnBackup.addActionListener(e -> triggerBackup());
        filterBar.add(btnBackup);

        northPanel.add(filterBar, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);

        // Center: Full Ledger Table
        String[] headers = {
            "Transaction ID", "Member", "Library Item", "Issue Date", "Due Date", "Return Date", "Status", "Fine", "Payment"
        };
        float[] weights = {2.2f, 1.8f, 2.8f, 1.2f, 1.2f, 1.2f, 1.2f, 1.0f, 1.1f};
        txnTable = new ModernTable(headers, weights);
        txnTable.setRowsPerPage(12);

        add(txnTable, BorderLayout.CENTER);

        // South: Telemetry Summary Strip
        Panel southPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        southPanel.setBackground(UITheme.HEADER_BG);
        lblSummary = new Label("Ledger telemetry loading...");
        lblSummary.setFont(UITheme.FONT_BODY_BOLD);
        lblSummary.setForeground(UITheme.TEXT_DARK);
        southPanel.add(lblSummary);

        add(southPanel, BorderLayout.SOUTH);
    }

    private void collectFineForSelected() {
        if (paymentPending) return;
        String[] row = txnTable.getSelectedRow();
        if (row == null || row.length == 0) {
            ModernDialog.showError(parentFrame, "Selection Required", "Please select a transaction row from the table first.");
            return;
        }

        String txnId = row[0];
        Transaction txn = service.findTransaction(txnId);
        if (txn == null) {
            ModernDialog.showError(parentFrame, "Transaction Error", "Transaction not found.");
            return;
        }

        if (txn.getFine() <= 0) {
            ModernDialog.showInfo(parentFrame, "No Fine Assessed", "Zero Balance",
                    "Transaction " + txnId + " has no late fees assessed.");
            return;
        }
        if (txn.isFinePaid()) {
            ModernDialog.showInfo(parentFrame, "Already Paid", "Fine Settled",
                    "The fine for transaction " + txnId + " (" + CurrencyUtils.format(txn.getFine())
                    + ") has already been collected.");
            return;
        }

        boolean confirm = ModernDialog.showConfirm(parentFrame, "Collect Fine", "Confirm Payment Collection",
                "Collect late fee payment for:\nTransaction: " + txnId
                + "\nAmount: " + CurrencyUtils.format(txn.getFine())
                + "\n\nMark fine as PAID in ledger?");

        if (confirm) {
            final double amount = txn.getFine();
            paymentPending = true;
            AsyncUi.run("ShelfSense-FinePayment", new AsyncUi.Work() {
                @Override
                public void run() throws Exception {
                    service.payFine(txnId);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    paymentPending = false;
                    ModernDialog.showSuccess(parentFrame, "Fine Collected", "Payment Received",
                            "Late fee payment of " + CurrencyUtils.format(amount) + " collected.", txnId);
                    refresh();
                }
            }, new java.util.function.Consumer<Exception>() {
                @Override
                public void accept(Exception error) {
                    paymentPending = false;
                    ModernDialog.showError(parentFrame, "Payment Error", error.getMessage());
                }
            });
        }
    }

    private void triggerBackup() {
        Thread thread = new Thread(new BackupTask(service, null, LibraryService.BACKUP_FILE, result -> {
            java.awt.EventQueue.invokeLater(() -> {
                ModernDialog.showSuccess(parentFrame, "Backup Successful", "Byte Stream Export Complete",
                        result + "\nFile: " + LibraryService.BACKUP_FILE.getAbsolutePath(), null);
            });
        }), "BackgroundBackupThread");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private void applyFilter() {
        String q = tfSearch.getText();
        String status = chStatus.getSelectedItem();
        String fine = chFine.getSelectedItem();

        List<Transaction> filtered = service.filterTransactions(q, status, fine);
        loadTableData(filtered);
    }

    public synchronized void refresh() {
        applyFilter();

        int totalTxns = service.transactionCount();
        int active = service.getCurrentlyIssuedCount();
        int overdue = service.getOverdueCount();
        double collected = service.getTotalFinesCollected();
        double unpaid = service.getTotalOutstandingFines();

        lblSummary.setText(String.format(
                "Total Transactions: %d  |  Active Loans: %d  |  Overdue: %d  |  Fines Collected: %s  |  Outstanding Fines: %s",
                totalTxns, active, overdue, CurrencyUtils.format(collected), CurrencyUtils.format(unpaid)
        ));
    }

    private void loadTableData(List<Transaction> list) {
        List<String[]> rows = new ArrayList<>();
        for (Transaction t : list) {
            Member m = service.findMember(t.getMemberId());
            LibraryItem it = service.findItem(t.getItemId());

            String memberStr = (m != null) ? m.getName() + " (" + m.getCode() + ")" : "ID " + t.getMemberId();
            String itemStr = (it != null) ? it.getTitle() + " [" + it.generateItemCode() + "]" : "ID " + t.getItemId();
            String returnStr = (t.getReturnDate() != null) ? DateUtils.formatDisplay(t.getReturnDate()) : "-";
            String fineStr = CurrencyUtils.format(t.getFine());
            String paidBadge = (t.getFine() <= 0) ? "N/A" : (t.isFinePaid() ? "PAID" : "UNPAID");

            rows.add(new String[]{
                t.getTransactionId(),
                memberStr,
                itemStr,
                DateUtils.formatDisplay(t.getIssueDate()),
                DateUtils.formatDisplay(t.getDueDate()),
                returnStr,
                t.getStatus(),
                fineStr,
                paidBadge
            });
        }
        txnTable.setData(rows);
    }
}
