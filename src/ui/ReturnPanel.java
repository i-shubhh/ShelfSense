package ui;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import exception.LibraryException;
import model.LibraryItem;
import model.Member;
import model.Transaction;
import service.LibraryService;
import service.ReturnService;
import ui.components.ModernButton;
import ui.components.ModernDialog;
import ui.components.ModernTable;
import ui.components.UITheme;
import util.DateUtils;
import util.CurrencyUtils;

/**
 * Return and Late-Fee Intelligence Panel.
 * Supports searching by random Transaction ID (e.g. SHELF-TXN-8F3A92C1),
 * automatic overdue day calculations, item-type-specific fine rate assessment,
 * item stock replenishment, and fine payment collection.
 */
public class ReturnPanel extends Panel {

    private final LibraryService service;
    private final Frame parentFrame;

    private TextField tfTxnSearch;
    private ModernTable openLoansTable;

    // Intelligence card components
    private Label lblSelectedTxnId;
    private Label lblItemInfo;
    private Label lblMemberInfo;
    private Label lblDatesInfo;
    private Label lblOverdueStatus;
    private Label lblFineCalculation;
    private Label lblTotalFine;
    private Checkbox cbPayFineNow;
    private ModernButton btnProcessReturn;
    private boolean operationPending;

    private Transaction currentlySelectedTxn = null;

    public ReturnPanel(LibraryService service, Frame parentFrame) {
        this.service = service;
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout(12, 12));
        setBackground(UITheme.MAIN_BG);

        initUI();
        refresh();
    }

    private void initUI() {
        // North: Header and Transaction ID Search Bar
        Panel northPanel = new Panel(new BorderLayout(8, 8));
        northPanel.setBackground(UITheme.MAIN_BG);

        Panel headerPanel = new Panel(new BorderLayout()) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                UITheme.applyAntialiasing(g2);
                g2.setFont(UITheme.FONT_TITLE);
                g2.setColor(UITheme.TEXT_DARK);
                g2.drawString("Circulation Check-In & Late-Fee Intelligence", 4, 22);

                g2.setFont(UITheme.FONT_BODY);
                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString("Search transactions by ID (SHELF-TXN-XXXXXXXX) or select from active loans to assess overdue fines", 4, 42);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(700, 50);
            }
        };
        northPanel.add(headerPanel, BorderLayout.NORTH);

        Panel searchBar = new Panel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        searchBar.setBackground(UITheme.HEADER_BG);

        searchBar.add(new Label("Transaction ID:"));
        tfTxnSearch = new TextField(22);
        tfTxnSearch.addActionListener(e -> searchByTxnId());
        searchBar.add(tfTxnSearch);

        ModernButton btnFind = new ModernButton("Search ID", ModernButton.ButtonType.PRIMARY, ModernButton.IconType.SEARCH);
        btnFind.addActionListener(e -> searchByTxnId());
        searchBar.add(btnFind);

        ModernButton btnClear = new ModernButton("Clear Search", ModernButton.ButtonType.SECONDARY);
        btnClear.addActionListener(e -> {
            tfTxnSearch.setText("");
            refresh();
        });
        searchBar.add(btnClear);

        northPanel.add(searchBar, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);

        // Center: Active Loans Table
        String[] headers = {"Transaction ID", "Member", "Item Title", "Issue Date", "Due Date", "Days Overdue", "Status"};
        float[] weights = {2.2f, 1.8f, 2.8f, 1.2f, 1.2f, 1.1f, 1.2f};
        openLoansTable = new ModernTable(headers, weights);
        openLoansTable.setRowsPerPage(11);

        openLoansTable.setSelectionListener((idx, row) -> {
            if (row != null && row.length > 0) {
                String txnId = row[0];
                selectTransaction(txnId);
            }
        });

        add(openLoansTable, BorderLayout.CENTER);

        // East: Late-Fee Intelligence and Check-in Drawer
        Panel eastPanel = createIntelligenceCard();
        add(eastPanel, BorderLayout.EAST);
    }

    private Panel createIntelligenceCard() {
        Panel card = new Panel(new BorderLayout(8, 8));
        card.setBackground(UITheme.CARD_BG);
        card.setPreferredSize(new Dimension(360, 500));

        // Card Header
        Panel cardHeader = new Panel(new BorderLayout()) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                UITheme.applyAntialiasing(g2);
                g2.setFont(UITheme.FONT_SECTION);
                g2.setColor(UITheme.TEXT_DARK);
                g2.drawString("Late-Fee Intelligence & Check-In", 12, 22);

                g2.setFont(UITheme.FONT_SMALL);
                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString("Automated penalty assessment and stock return", 12, 38);

                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawLine(0, 48, getWidth(), 48);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(360, 52);
            }
        };
        card.add(cardHeader, BorderLayout.NORTH);

        Panel body = new Panel(new GridBagLayout());
        body.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 14, 4, 14);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        lblSelectedTxnId = new Label("Selected: None");
        lblSelectedTxnId.setFont(new Font("Monospaced", Font.BOLD, 13));
        lblSelectedTxnId.setForeground(UITheme.PRIMARY_BLUE);
        gbc.gridx = 0; gbc.gridy = row++;
        body.add(lblSelectedTxnId, gbc);

        lblItemInfo = new Label("Item: -");
        gbc.gridy = row++;
        body.add(lblItemInfo, gbc);

        lblMemberInfo = new Label("Borrower: -");
        gbc.gridy = row++;
        body.add(lblMemberInfo, gbc);

        lblDatesInfo = new Label("Dates: -");
        gbc.gridy = row++;
        body.add(lblDatesInfo, gbc);

        // Intelligence divider
        gbc.gridy = row++;
        gbc.insets = new Insets(10, 14, 4, 14);
        Label lblDivider = new Label("--- LATE FEE INTELLIGENCE ---");
        lblDivider.setForeground(UITheme.TEXT_MUTED);
        body.add(lblDivider, gbc);

        gbc.insets = new Insets(4, 14, 4, 14);
        lblOverdueStatus = new Label("Overdue Status: -");
        gbc.gridy = row++;
        body.add(lblOverdueStatus, gbc);

        lblFineCalculation = new Label("Rate Assessment: -");
        gbc.gridy = row++;
        body.add(lblFineCalculation, gbc);

        lblTotalFine = new Label("Applicable Fine: " + CurrencyUtils.format(0.0));
        lblTotalFine.setFont(UITheme.FONT_TITLE);
        lblTotalFine.setForeground(UITheme.DANGER_RED);
        gbc.gridy = row++;
        body.add(lblTotalFine, gbc);

        cbPayFineNow = new Checkbox("Collect & Pay Fine Upon Return");
        cbPayFineNow.setState(true);
        gbc.gridy = row++;
        gbc.insets = new Insets(8, 14, 8, 14);
        body.add(cbPayFineNow, gbc);

        btnProcessReturn = new ModernButton("Process Return & Check-in", ModernButton.ButtonType.SUCCESS, ModernButton.IconType.RETURN);
        btnProcessReturn.setEnabled(false);
        btnProcessReturn.addActionListener(e -> processReturn());
        gbc.gridy = row++;
        body.add(btnProcessReturn, gbc);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void searchByTxnId() {
        String query = tfTxnSearch.getText().trim();
        if (query.isEmpty()) {
            refresh();
            return;
        }

        Transaction t = service.findTransaction(query);
        if (t != null) {
            selectTransaction(t.getTransactionId());
        } else {
            ModernDialog.showError(parentFrame, "Transaction Not Found",
                    "No transaction found matching Transaction ID:\n\"" + query + "\"");
        }
    }

    public void selectTransaction(String txnId) {
        if (txnId == null) return;
        currentlySelectedTxn = service.findTransaction(txnId);
        if (currentlySelectedTxn == null) return;

        tfTxnSearch.setText(currentlySelectedTxn.getTransactionId());
        lblSelectedTxnId.setText(currentlySelectedTxn.getTransactionId());

        LibraryItem item = service.findItem(currentlySelectedTxn.getItemId());
        Member member = service.findMember(currentlySelectedTxn.getMemberId());

        lblItemInfo.setText("Item: " + (item != null ? item.getTitle() + " (" + item.getType() + ")" : "ID " + currentlySelectedTxn.getItemId()));
        lblMemberInfo.setText("Borrower: " + (member != null ? member.getName() + " (" + member.getCode() + ")" : "ID " + currentlySelectedTxn.getMemberId()));
        lblDatesInfo.setText("Issued: " + DateUtils.formatDisplay(currentlySelectedTxn.getIssueDate())
                + " | Due: " + DateUtils.formatDisplay(currentlySelectedTxn.getDueDate()));

        if (currentlySelectedTxn.isReturned()) {
            lblOverdueStatus.setText("Status: ALREADY RETURNED on " + currentlySelectedTxn.getReturnDate());
            lblOverdueStatus.setForeground(UITheme.SUCCESS_GREEN);
            lblFineCalculation.setText("Fine: " + CurrencyUtils.format(currentlySelectedTxn.getFine())
                    + (currentlySelectedTxn.isFinePaid() ? " (Paid)" : " (Unpaid)"));
            lblTotalFine.setText("Fine: " + CurrencyUtils.format(currentlySelectedTxn.getFine()));
            btnProcessReturn.setEnabled(false);
            return;
        }

        // Automatic Late-Fee Intelligence calculations
        LocalDate today = LocalDate.now();
        int overdueDays = DateUtils.calculateOverdueDays(currentlySelectedTxn.getDueDate(), today);

        if (overdueDays > 0) {
            boolean possiblyLost = overdueDays > 30;
            lblOverdueStatus.setText((possiblyLost ? "POSSIBLY LOST" : "OVERDUE") + " by " + overdueDays + " days!");
            lblOverdueStatus.setForeground(UITheme.DANGER_RED);

            double fine = (item != null) ? item.calculateLateFee(overdueDays) : (overdueDays * 2.0);
            double rate = (item != null) ? item.getLateFeeRate() : 2.0;
            String policy = item != null && "REFERENCE".equalsIgnoreCase(item.getType())
                    ? "2-day free period; " + overdueDays + " chargeable days"
                    : overdueDays + " overdue days";

            lblFineCalculation.setText(policy + " × " + CurrencyUtils.format(rate) + "/day ("
                    + (item != null ? item.getType() : "BOOK") + ")");
            lblTotalFine.setText("Applicable Fine: " + CurrencyUtils.format(fine));
            lblTotalFine.setForeground(UITheme.DANGER_RED);
        } else {
            lblOverdueStatus.setText("✓ Returned On Time (0 days overdue)");
            lblOverdueStatus.setForeground(UITheme.SUCCESS_GREEN);
            lblFineCalculation.setText("No late penalty applied");
            lblTotalFine.setText("Applicable Fine: " + CurrencyUtils.format(0.0));
            lblTotalFine.setForeground(UITheme.SUCCESS_GREEN);
        }

        btnProcessReturn.setEnabled(true);
    }

    private void processReturn() {
        if (operationPending) return;
        if (currentlySelectedTxn == null) {
            ModernDialog.showError(parentFrame, "Selection Required", "Please select an active transaction to return.");
            return;
        }

        try {
            boolean payFineNow = cbPayFineNow.getState();
            final String transactionId = currentlySelectedTxn.getTransactionId();
            operationPending = true;
            btnProcessReturn.setEnabled(false);
            AsyncUi.run("ShelfSense-Return", new AsyncUi.Work() {
                @Override
                public void run() throws Exception {
                    final ReturnService.ReturnResult result = service.returnItem(transactionId,
                            LocalDate.now(), payFineNow);
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            StringBuilder msg = new StringBuilder();
                            msg.append("Item checked in and returned to library stock successfully!\n\n");
                            msg.append("Transaction: ").append(result.transaction.getTransactionId()).append("\n");
                            msg.append("Item: ").append(result.item != null ? result.item.getTitle() : "Item").append("\n");
                            msg.append("Borrower: ").append(result.member != null ? result.member.getName() : "Borrower").append("\n");
                             msg.append("Days Overdue: ").append(result.overdueDays).append(" days\n");
                             msg.append("Late Fine Assessed: ").append(CurrencyUtils.format(result.fineAssessed)).append("\n");
                             msg.append("Payment Status: ").append(result.finePaid ? "PAID IN FULL ✓" : "UNPAID (Added to ledger) ⚠️");
                             Member next = result.item != null
                                     ? service.getReservationHead(result.item.getItemId()) : null;
                             if (next != null && result.item != null && result.item.getAvailableCopies() > 0) {
                                 msg.append("\n\nRESERVED: Next in FIFO queue is ")
                                         .append(next.getName()).append(" (").append(next.getCode())
                                         .append("). They can now borrow this item.");
                             }
                            ModernDialog.showSuccess(parentFrame, "Check-In Complete", "Item Return Receipt",
                                    msg.toString(), result.transaction.getTransactionId());
                            operationPending = false;
                            clearSelection();
                            refresh();
                        }
                    });
                }
            }, null, new java.util.function.Consumer<Exception>() {
                @Override
                public void accept(Exception error) {
                    operationPending = false;
                    btnProcessReturn.setEnabled(true);
                    ModernDialog.showError(parentFrame, "Check-In Error", error.getMessage());
                }
            });
        } catch (Exception ex) {
            ModernDialog.showError(parentFrame, "Check-In Error", ex.getMessage());
        }
    }

    private void clearSelection() {
        currentlySelectedTxn = null;
        tfTxnSearch.setText("");
        lblSelectedTxnId.setText("Selected: None");
        lblItemInfo.setText("Item: -");
        lblMemberInfo.setText("Borrower: -");
        lblDatesInfo.setText("Dates: -");
        lblOverdueStatus.setText("Overdue Status: -");
        lblFineCalculation.setText("Rate Assessment: -");
        lblTotalFine.setText("Applicable Fine: " + CurrencyUtils.format(0.0));
        lblTotalFine.setForeground(UITheme.TEXT_DARK);
        btnProcessReturn.setEnabled(false);
    }

    public synchronized void refresh() {
        List<Transaction> activeList = service.getActiveIssuedTransactions();
        List<String[]> rows = new ArrayList<>();

        for (Transaction t : activeList) {
            Member m = service.findMember(t.getMemberId());
            LibraryItem it = service.findItem(t.getItemId());

            int overdueDays = t.getOverdueDays(LocalDate.now());
            String overdueStr = (overdueDays > 0) ? (overdueDays + " days late") : "On Time";
            String status = t.getStatus();
            if (overdueDays > 0 && overdueDays > 30) {
                status = Transaction.POSSIBLY_LOST;
            }

            rows.add(new String[]{
                t.getTransactionId(),
                (m != null) ? m.getName() + " (" + m.getCode() + ")" : "ID " + t.getMemberId(),
                (it != null) ? it.getTitle() + " [" + it.generateItemCode() + "]" : "ID " + t.getItemId(),
                DateUtils.formatDisplay(t.getIssueDate()),
                DateUtils.formatDisplay(t.getDueDate()),
                overdueStr,
                 status
            });
        }
        openLoansTable.setData(rows);
    }
}
