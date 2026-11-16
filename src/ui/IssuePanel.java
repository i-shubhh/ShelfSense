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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.time.LocalDate;
import java.util.List;

import exception.LibraryException;
import model.LibraryItem;
import model.Member;
import model.Transaction;
import service.LibraryService;
import ui.components.ModernButton;
import ui.components.ModernDialog;
import ui.components.UITheme;
import util.DateUtils;
import util.CurrencyUtils;

/**
 * Issue Circulation Panel.
 * Implements member loan limit checks, inventory verification, dynamic due date calculation,
 * and automated cryptographically unique random Transaction ID generation (Requirement 8).
 */
public class IssuePanel extends Panel {

    private final LibraryService service;
    private final Frame parentFrame;

    private Choice chMembers;
    private Choice chItems;

    private TextField tfMemberSearch;
    private TextField tfItemSearch;

    // Telemetry & Preview cards
    private Label lblMemberEligibility;
    private Label lblItemStock;
    private Label lblIssueDate;
    private Label lblDueDate;
    private Label lblLoanPeriod;
    private Label lblLateFeeRate;
    private ModernButton btnIssue;
    private ModernButton btnReserve;
    private boolean operationPending;

    public IssuePanel(LibraryService service, Frame parentFrame) {
        this.service = service;
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.MAIN_BG);

        initUI();
        refresh();
    }

    private void initUI() {
        // North: Header
        Panel headerPanel = new Panel(new BorderLayout()) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                UITheme.applyAntialiasing(g2);
                g2.setFont(UITheme.FONT_TITLE);
                g2.setColor(UITheme.TEXT_DARK);
                g2.drawString("Circulation Desk • Issue Library Item", 4, 22);

                g2.setFont(UITheme.FONT_BODY);
                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString("Automated limit enforcement, policy compliance, and cryptographically secure Transaction ID generation", 4, 42);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(700, 52);
            }
        };
        add(headerPanel, BorderLayout.NORTH);

        // Center: Two Main Workflow Columns
        Panel centerGrid = new Panel(new GridLayout(1, 2, 16, 16));
        centerGrid.setBackground(UITheme.MAIN_BG);

        // Column 1: Member & Item Selection Card
        Panel col1 = createSelectionCard();
        centerGrid.add(col1);

        // Column 2: Loan Preview & Confirmation Card
        Panel col2 = createPreviewCard();
        centerGrid.add(col2);

        add(centerGrid, BorderLayout.CENTER);
    }

    private Panel createSelectionCard() {
        Panel card = new Panel(new BorderLayout(10, 10));
        card.setBackground(UITheme.CARD_BG);

        Panel cardHeader = new Panel(new BorderLayout()) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                UITheme.applyAntialiasing(g2);
                g2.setFont(UITheme.FONT_SECTION);
                g2.setColor(UITheme.TEXT_DARK);
                g2.drawString("Step 1: Select Borrower & Item", 12, 22);

                g2.setFont(UITheme.FONT_SMALL);
                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString("Verify borrower quota and inventory availability", 12, 38);

                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawLine(0, 48, getWidth(), 48);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(350, 52);
            }
        };
        card.add(cardHeader, BorderLayout.NORTH);

        Panel body = new Panel(new GridBagLayout());
        body.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 16, 6, 16);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        // Member selection
        gbc.gridx = 0; gbc.gridy = row++;
        body.add(new Label("Select Member:"), gbc);

        chMembers = new Choice();
        chMembers.addItemListener(e -> updateTelemetry());
        gbc.gridy = row++;
        body.add(chMembers, gbc);

        lblMemberEligibility = new Label("Member Quota: Ready");
        lblMemberEligibility.setForeground(UITheme.PRIMARY_BLUE);
        gbc.gridy = row++;
        body.add(lblMemberEligibility, gbc);

        // Item selection
        gbc.gridy = row++;
        gbc.insets = new Insets(16, 16, 6, 16);
        body.add(new Label("Select Library Item:"), gbc);

        chItems = new Choice();
        chItems.addItemListener(e -> updateTelemetry());
        gbc.gridy = row++;
        gbc.insets = new Insets(6, 16, 6, 16);
        body.add(chItems, gbc);

        lblItemStock = new Label("Stock Availability: Ready");
        lblItemStock.setForeground(UITheme.SUCCESS_GREEN);
        gbc.gridy = row++;
        body.add(lblItemStock, gbc);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private Panel createPreviewCard() {
        Panel card = new Panel(new BorderLayout(10, 10));
        card.setBackground(UITheme.CARD_BG);

        Panel cardHeader = new Panel(new BorderLayout()) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                UITheme.applyAntialiasing(g2);
                g2.setFont(UITheme.FONT_SECTION);
                g2.setColor(UITheme.TEXT_DARK);
                g2.drawString("Step 2: Policy Verification & Issuance", 12, 22);

                g2.setFont(UITheme.FONT_SMALL);
                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString("Review loan terms and confirm automated transaction", 12, 38);

                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawLine(0, 48, getWidth(), 48);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(350, 52);
            }
        };
        card.add(cardHeader, BorderLayout.NORTH);

        Panel body = new Panel(new GridBagLayout());
        body.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 16, 6, 16);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        lblIssueDate = new Label("Issue Date: " + DateUtils.formatDisplay(LocalDate.now()));
        gbc.gridx = 0; gbc.gridy = row++;
        body.add(lblIssueDate, gbc);

        lblLoanPeriod = new Label("Loan Period: 14 Days standard");
        gbc.gridy = row++;
        body.add(lblLoanPeriod, gbc);

        lblDueDate = new Label("Calculated Due Date: " + DateUtils.formatDisplay(LocalDate.now().plusDays(14)));
        lblDueDate.setForeground(UITheme.PRIMARY_BLUE);
        gbc.gridy = row++;
        body.add(lblDueDate, gbc);

        lblLateFeeRate = new Label("Overdue Fine Rate: " + CurrencyUtils.format(2.0) + " / day");
        gbc.gridy = row++;
        body.add(lblLateFeeRate, gbc);

        // Security notice regarding Requirement 8
        Panel noticePanel = new Panel(new BorderLayout()) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                UITheme.applyAntialiasing(g2);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(UITheme.PRIMARY_BG);
                g2.fillRoundRect(0, 0, w, h, 8, 8);
                g2.setColor(UITheme.BORDER_FOCUS);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                g2.setFont(UITheme.FONT_SMALL);
                g2.setColor(UITheme.PRIMARY_BLUE);
                g2.drawString("AUTO TRANSACTION ID GENERATION", 12, 18);

                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString("The system automatically generates a unique random ID", 12, 34);
                g2.drawString("(e.g., SHELF-TXN-XXXXXXXX) via SecureRandom.", 12, 48);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(300, 60);
            }
        };
        gbc.gridy = row++;
        gbc.insets = new Insets(12, 16, 12, 16);
        body.add(noticePanel, gbc);

        // Confirm Issue Button
        btnIssue = new ModernButton("Confirm & Issue Item", ModernButton.ButtonType.PRIMARY, ModernButton.IconType.ISSUE);
        btnIssue.addActionListener(e -> processIssue());
        gbc.gridy = row++;
        body.add(btnIssue, gbc);

        btnReserve = new ModernButton("Join Reservation Queue", ModernButton.ButtonType.WARNING);
        btnReserve.addActionListener(e -> processReservation());
        btnReserve.setEnabled(false);
        gbc.gridy = row++;
        body.add(btnReserve, gbc);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void updateTelemetry() {
        Member m = getSelectedMember();
        LibraryItem it = getSelectedItem();

        if (m != null) {
            int active = service.getActiveLoanCount(m.getMemberId());
            boolean eligible = active < m.getBorrowLimit();
            lblMemberEligibility.setText("Borrowing Quota: " + active + " of " + m.getBorrowLimit() + " used"
                    + (eligible ? " (AVAILABLE)" : " (ISSUE BLOCKED: You have reached your maximum borrowing limit.)"));
            lblMemberEligibility.setForeground(eligible ? UITheme.SUCCESS_GREEN : UITheme.DANGER_RED);
        }

        if (it != null) {
            boolean available = it.getAvailableCopies() > 0;
            int reservationCount = service.getReservationCount(it.getItemId());
            Member reservationHead = service.getReservationHead(it.getItemId());
            boolean memberIsHead = reservationHead != null && m != null && reservationHead.equals(m);
            String stockStatus;
            if (reservationCount > 0) {
                stockStatus = "RESERVED (" + reservationCount + " waiting)";
                if (available && memberIsHead) {
                    stockStatus = "RESERVED: READY FOR YOU (FIFO position 1)";
                }
                lblItemStock.setForeground(UITheme.WARNING_AMBER);
            } else if (available) {
                stockStatus = "AVAILABLE";
                lblItemStock.setForeground(UITheme.SUCCESS_GREEN);
            } else {
                stockStatus = "ISSUE BLOCKED: NO COPIES";
                lblItemStock.setForeground(UITheme.DANGER_RED);
            }
            lblItemStock.setText("Inventory: " + it.getAvailableCopies() + " of " + it.getTotalCopies()
                    + " available [" + stockStatus + "]");

            int loanDays = it.getLoanDays();
            LocalDate due = LocalDate.now().plusDays(loanDays);
            lblLoanPeriod.setText("Loan Policy: " + loanDays + " days for " + it.getType());
            lblDueDate.setText("Calculated Due Date: " + DateUtils.formatDisplay(due));

            lblLateFeeRate.setText("Overdue Fine Rate: " + CurrencyUtils.format(it.getLateFeeRate())
                    + " / day (" + it.getType() + " policy)");

            boolean canIssue = available && m != null && service.getActiveLoanCount(m.getMemberId()) < m.getBorrowLimit()
                    && (reservationHead == null || memberIsHead);
            boolean canReserve = !available && m != null && service.getReservationPosition(it.getItemId(), m.getMemberId()) == 0;
            btnIssue.setEnabled(canIssue && !operationPending);
            btnReserve.setEnabled(canReserve && !operationPending);
        } else {
            btnIssue.setEnabled(false);
            btnReserve.setEnabled(false);
        }
    }

    private Member getSelectedMember() {
        int idx = chMembers.getSelectedIndex();
        if (idx < 0) return null;
            List<Member> list = service.getMembersSorted();
        if (idx < list.size()) {
            return list.get(idx);
        }
        return null;
    }

    private LibraryItem getSelectedItem() {
        int idx = chItems.getSelectedIndex();
        if (idx < 0) return null;
            List<LibraryItem> list = service.getCatalogue();
        if (idx < list.size()) {
            return list.get(idx);
        }
        return null;
    }

    private void processIssue() {
        if (operationPending) return;
        Member m = getSelectedMember();
        LibraryItem it = getSelectedItem();

        if (m == null) {
            ModernDialog.showError(parentFrame, "Selection Required", "Please select a member to issue the item to.");
            return;
        }
        if (it == null) {
            ModernDialog.showError(parentFrame, "Selection Required", "Please select a library item to issue.");
            return;
        }

        try {
            operationPending = true;
            btnIssue.setEnabled(false);
            // Service handles borrow limit check, copy reduction, and SecureRandom Transaction ID generation
            AsyncUi.run("ShelfSense-Issue", new AsyncUi.Work() {
                @Override
                public void run() throws Exception {
                    final Transaction txn = service.issueItem(it.getItemId(), m.getMemberId());
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            String msg = "Item has been successfully checked out!\n"
                                    + "Item: " + it.getTitle() + " (" + it.generateItemCode() + ")\n"
                                    + "Borrower: " + m.getName() + " (" + m.getCode() + ")\n"
                                    + "Due Date: " + DateUtils.formatDisplay(txn.getDueDate()) + "\n\n"
                                    + "Unique Transaction ID has been generated and stored permanently:";
                            ModernDialog.showSuccess(parentFrame, "Transaction Successful", "Item Issued Successfully",
                                    msg, txn.getTransactionId());
                            operationPending = false;
                            btnIssue.setEnabled(true);
                            refresh();
                        }
                    });
                }
            }, null, new java.util.function.Consumer<Exception>() {
                @Override
                public void accept(Exception error) {
                    operationPending = false;
                    btnIssue.setEnabled(true);
                    ModernDialog.showError(parentFrame, "Issue Error", error.getMessage());
                }
            });
        } catch (Exception ex) {
            ModernDialog.showError(parentFrame, "Issue Error", ex.getMessage());
        }
    }

    private void processReservation() {
        if (operationPending) return;
        Member m = getSelectedMember();
        LibraryItem it = getSelectedItem();
        if (m == null || it == null) {
            ModernDialog.showError(parentFrame, "Selection Required", "Select both a member and an item before joining a reservation queue.");
            return;
        }

        operationPending = true;
        btnReserve.setEnabled(false);
        AsyncUi.run("ShelfSense-Reservation", new AsyncUi.Work() {
            @Override
            public void run() throws Exception {
                service.joinReservationQueue(it.getItemId(), m);
            }
        }, new Runnable() {
            @Override
            public void run() {
                operationPending = false;
                ModernDialog.showSuccess(parentFrame, "Reservation Added", "FIFO Reservation Queue",
                        m.getName() + " is now reserved for " + it.getTitle() + ".\nPosition: "
                                + service.getReservationPosition(it.getItemId(), m.getMemberId()), null);
                refresh();
            }
        }, new java.util.function.Consumer<Exception>() {
            @Override
            public void accept(Exception error) {
                operationPending = false;
                ModernDialog.showError(parentFrame, "Reservation Error", error.getMessage());
                updateTelemetry();
            }
        });
    }

    public synchronized void refresh() {
        // Refresh Member Choice
        chMembers.removeAll();
        for (Member m : service.getMembersSorted()) {
            int active = service.getActiveLoanCount(m.getMemberId());
            chMembers.add(String.format("%s - %s [%s: %d/%d used]",
                    m.getCode(), m.getName(), m.getCategory(), active, m.getBorrowLimit()));
        }

        // Refresh Item Choice
        chItems.removeAll();
        for (LibraryItem it : service.getCatalogue()) {
            String queueStatus = service.getReservationCount(it.getItemId()) > 0
                    ? ", RESERVED: " + service.getReservationCount(it.getItemId()) : "";
            chItems.add(String.format("%s - %s [%s: %d avail%s]",
                    it.generateItemCode(), it.getTitle(), it.getType(), it.getAvailableCopies(), queueStatus));
        }

        updateTelemetry();
    }
}
