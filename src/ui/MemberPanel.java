package ui;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.util.ArrayList;
import java.util.List;

import exception.LibraryException;
import model.LibraryItem;
import model.Member;
import model.Transaction;
import service.LibraryService;
import service.Validator;
import ui.components.ModernButton;
import ui.components.ModernDialog;
import ui.components.ModernTable;
import ui.components.UITheme;
import util.IDGenerator;
import util.CurrencyUtils;

/**
 * Member Management Panel.
 * Supports registering new members with auto-generated Member IDs,
 * updating profiles, enforcing category borrowing limits, and inspecting
 * real-time member borrowing history and active loans.
 */
public class MemberPanel extends Panel {

    private final LibraryService service;
    private final Frame parentFrame;

    private TextField tfSearch;
    private Choice chCategoryFilter;
    private ModernTable memberTable;

    // Form fields
    private TextField tfMemberIdPreview;
    private TextField tfName;
    private Choice chCategory;
    private TextField tfPhone;
    private Label lblLimitPreview;

    // Profile inspection card
    private TextArea taMemberProfile;

    private Member currentlySelectedMember = null;
    private boolean operationPending;

    public MemberPanel(LibraryService service, Frame parentFrame) {
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
                g2.drawString("Member Directory & Circulation Quotas", 4, 22);

                g2.setFont(UITheme.FONT_BODY);
                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString("Register members, track academic quotas (UG, PG, PhD, Faculty, Staff) and view borrowing histories", 4, 42);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(700, 50);
            }
        };
        northPanel.add(headerPanel, BorderLayout.NORTH);

        Panel searchBar = new Panel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        searchBar.setBackground(UITheme.HEADER_BG);

        searchBar.add(new Label("Search:"));
        tfSearch = new TextField(20);
        tfSearch.addActionListener(e -> applyFilter());
        searchBar.add(tfSearch);

        searchBar.add(new Label("Category:"));
        chCategoryFilter = new Choice();
        chCategoryFilter.add("ALL");
        for (String cat : Member.CATEGORIES) {
            chCategoryFilter.add(cat);
        }
        chCategoryFilter.addItemListener(e -> applyFilter());
        searchBar.add(chCategoryFilter);

        ModernButton btnFilter = new ModernButton("Filter", ModernButton.ButtonType.PRIMARY, ModernButton.IconType.SEARCH);
        btnFilter.addActionListener(e -> applyFilter());
        searchBar.add(btnFilter);

        ModernButton btnReset = new ModernButton("Reset", ModernButton.ButtonType.SECONDARY);
        btnReset.addActionListener(e -> {
            tfSearch.setText("");
            chCategoryFilter.select("ALL");
            applyFilter();
        });
        searchBar.add(btnReset);

        northPanel.add(searchBar, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);

        // Center: Members Table
        String[] headers = {"Member ID", "Full Name", "Category", "Phone", "Quota Limit", "Active Loans", "Eligibility"};
        float[] weights = {1.2f, 2.5f, 1.2f, 1.6f, 1.2f, 1.4f, 1.5f};
        memberTable = new ModernTable(headers, weights);
        memberTable.setRowsPerPage(11);

        memberTable.setSelectionListener((idx, row) -> {
            if (row != null && row.length > 0) {
                String code = row[0];
                currentlySelectedMember = service.findMemberByCode(code);
                populateForm(currentlySelectedMember);
                updateProfileView(currentlySelectedMember);
            }
        });

        add(memberTable, BorderLayout.CENTER);

        // East: Form Drawer and Member Profile View
        Panel eastPanel = createEastPanel();
        add(eastPanel, BorderLayout.EAST);
    }

    private Panel createEastPanel() {
        Panel eastCard = new Panel(new BorderLayout(8, 8));
        eastCard.setBackground(UITheme.CARD_BG);
        eastCard.setPreferredSize(new Dimension(350, 500));

        // Form Title
        Panel formHeader = new Panel(new BorderLayout()) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                UITheme.applyAntialiasing(g2);
                g2.setFont(UITheme.FONT_SECTION);
                g2.setColor(UITheme.TEXT_DARK);
                g2.drawString("Member Registration & Profile", 12, 22);

                g2.setFont(UITheme.FONT_SMALL);
                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString("Auto-generated IDs and borrowing limit enforcement", 12, 38);

                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawLine(0, 48, getWidth(), 48);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(350, 52);
            }
        };
        eastCard.add(formHeader, BorderLayout.NORTH);

        // Form Fields
        Panel formBody = new Panel(new GridBagLayout());
        formBody.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 12, 3, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        gbc.gridx = 0; gbc.gridy = row++;
        formBody.add(new Label("Member ID (Auto-Generated):"), gbc);
        tfMemberIdPreview = new TextField();
        tfMemberIdPreview.setEditable(false);
        gbc.gridy = row++;
        formBody.add(tfMemberIdPreview, gbc);

        gbc.gridy = row++;
        formBody.add(new Label("Full Name:"), gbc);
        tfName = new TextField();
        gbc.gridy = row++;
        formBody.add(tfName, gbc);

        gbc.gridy = row++;
        formBody.add(new Label("Member Classification:"), gbc);
        chCategory = new Choice();
        for (String cat : Member.CATEGORIES) {
            chCategory.add(cat);
        }
        chCategory.addItemListener(e -> updateLimitLabel());
        gbc.gridy = row++;
        formBody.add(chCategory, gbc);

        gbc.gridy = row++;
        lblLimitPreview = new Label("Quota Limit: 3 books simultaneously");
        lblLimitPreview.setForeground(UITheme.PRIMARY_BLUE);
        formBody.add(lblLimitPreview, gbc);

        gbc.gridy = row++;
        formBody.add(new Label("Phone Number (10 digits):"), gbc);
        tfPhone = new TextField();
        gbc.gridy = row++;
        formBody.add(tfPhone, gbc);

        // Action Buttons Grid
        Panel formButtons = new Panel(new GridLayout(2, 2, 8, 8));
        formButtons.setBackground(UITheme.HEADER_BG);

        ModernButton btnRegister = new ModernButton("Register", ModernButton.ButtonType.PRIMARY, ModernButton.IconType.PLUS);
        btnRegister.addActionListener(e -> registerNewMember());

        ModernButton btnUpdate = new ModernButton("Update", ModernButton.ButtonType.SUCCESS, ModernButton.IconType.CHECK);
        btnUpdate.addActionListener(e -> updateExistingMember());

        ModernButton btnDelete = new ModernButton("Delete", ModernButton.ButtonType.DANGER, ModernButton.IconType.ALERT);
        btnDelete.addActionListener(e -> deleteSelectedMember());

        ModernButton btnClear = new ModernButton("Clear", ModernButton.ButtonType.SECONDARY);
        btnClear.addActionListener(e -> clearForm());

        formButtons.add(btnRegister);
        formButtons.add(btnUpdate);
        formButtons.add(btnDelete);
        formButtons.add(btnClear);

        gbc.gridy = row++;
        gbc.insets = new Insets(8, 12, 8, 12);
        formBody.add(formButtons, gbc);

        // Profile details textarea
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 12, 2, 12);
        formBody.add(new Label("Borrowing History & Active Loans:"), gbc);

        taMemberProfile = new TextArea(7, 30);
        taMemberProfile.setEditable(false);
        taMemberProfile.setText("Select a member from the table to view active loans and complete borrowing history.");
        gbc.gridy = row++;
        formBody.add(taMemberProfile, gbc);

        eastCard.add(formBody, BorderLayout.CENTER);
        return eastCard;
    }

    private void updateLimitLabel() {
        String cat = chCategory.getSelectedItem();
        int limit = Member.limitFor(cat);
        lblLimitPreview.setText("Quota Limit: " + limit + " items simultaneously (" + cat + ")");
    }

    private void populateForm(Member m) {
        if (m == null) return;
        tfMemberIdPreview.setText(m.getCode());
        tfName.setText(m.getName());
        chCategory.select(m.getCategory());
        tfPhone.setText(m.getPhone());
        updateLimitLabel();
    }

    private void updateProfileView(Member m) {
        if (m == null) {
            taMemberProfile.setText("Select a member from the table to view their circulation history.");
            return;
        }

        List<Transaction> active = service.getMemberActiveIssues(m.getMemberId());
        List<Transaction> history = service.getMemberHistory(m.getMemberId());

        StringBuilder sb = new StringBuilder();
        sb.append("MEMBER PROFILE: ").append(m.getCode()).append(" - ").append(m.getName()).append("\n");
        sb.append("Category: ").append(m.getCategory()).append(" | Quota: ").append(active.size())
          .append("/").append(m.getBorrowLimit()).append(" books currently out\n\n");

        sb.append("--- CURRENTLY BORROWED ITEMS (").append(active.size()).append(") ---\n");
        if (active.isEmpty()) {
            sb.append("No items currently borrowed. Full borrowing quota available.\n");
        } else {
            for (Transaction t : active) {
                LibraryItem it = service.findItem(t.getItemId());
                sb.append("• [").append(t.getTransactionId()).append("] ")
                  .append(it != null ? it.getTitle() : "Item " + t.getItemId())
                  .append(" | Due: ").append(t.getDueDate())
                  .append(t.isOverdue(java.time.LocalDate.now()) ? " [OVERDUE!]" : " [Active]")
                  .append(" | Fine: ").append(CurrencyUtils.format(t.getFine()))
                  .append("\n");
            }
        }

        sb.append("\n--- ALL-TIME BORROWING HISTORY (").append(history.size()).append(") ---\n");
        int returnedCount = 0;
        double totalFines = 0.0;
        for (Transaction t : history) {
            if (t.isReturned()) returnedCount++;
            if (t.getFine() > 0) totalFines += t.getFine();
            LibraryItem it = service.findItem(t.getItemId());
            sb.append(t.getTransactionId()).append(" | ")
              .append(it != null ? it.getTitle() : "Item " + t.getItemId())
              .append(" | ").append(t.getStatus())
              .append(" | Fine: ").append(CurrencyUtils.format(t.getFine()))
              .append(t.isFinePaid() ? " (PAID)" : " (UNPAID)").append("\n");
        }
        sb.append("Total loans completed: ").append(returnedCount).append("\n");
        sb.append("Total late fines assessed: ").append(CurrencyUtils.format(totalFines)).append("\n");

        taMemberProfile.setText(sb.toString());
    }

    private void clearForm() {
        currentlySelectedMember = null;
        tfMemberIdPreview.setText(IDGenerator.formatMemberId(Member.nextId()));
        tfName.setText("");
        chCategory.select("UG");
        tfPhone.setText("");
        updateLimitLabel();
        taMemberProfile.setText("Select a member from the table to view active loans and borrowing history.");
    }

    private void registerNewMember() {
        if (operationPending) return;
        try {
            String name = Validator.requireText(tfName.getText(), "Full Name");
            String category = chCategory.getSelectedItem();
            String phone = Validator.requirePhone(tfPhone.getText());

            Member member = new Member(name, category, phone);
            operationPending = true;
            AsyncUi.run("ShelfSense-MemberRegister", new AsyncUi.Work() {
                @Override
                public void run() throws Exception {
                    service.registerMember(member);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    operationPending = false;
                    ModernDialog.showSuccess(parentFrame, "Member Registered", "New Member Profile Created",
                            "Member \"" + member.getName() + "\" registered successfully.\nAssigned Member ID: " + member.getCode(),
                            member.getCode());
                    clearForm();
                    refresh();
                }
            }, new java.util.function.Consumer<Exception>() {
                @Override
                public void accept(Exception error) {
                    operationPending = false;
                    ModernDialog.showError(parentFrame, "Registration Error", error.getMessage());
                }
            });
        } catch (Exception ex) {
            ModernDialog.showError(parentFrame, "Registration Error", ex.getMessage());
        }
    }

    private void updateExistingMember() {
        if (operationPending) return;
        if (currentlySelectedMember == null) {
            ModernDialog.showError(parentFrame, "Selection Required", "Please select a member from the table to update.");
            return;
        }

        try {
            String name = Validator.requireText(tfName.getText(), "Full Name");
            String category = chCategory.getSelectedItem();
            String phone = Validator.requirePhone(tfPhone.getText());

            final Member updatedMember = new Member(currentlySelectedMember.getMemberId(), name, category, phone,
                    Member.limitFor(category));
            operationPending = true;
            AsyncUi.run("ShelfSense-MemberUpdate", new AsyncUi.Work() {
                @Override
                public void run() throws Exception {
                    service.updateMember(updatedMember);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    operationPending = false;
                    currentlySelectedMember = updatedMember;
                    ModernDialog.showSuccess(parentFrame, "Profile Updated", "Update Successful",
                            "Member profile for " + updatedMember.getCode() + " updated successfully.", null);
                    refresh();
                    updateProfileView(updatedMember);
                }
            }, new java.util.function.Consumer<Exception>() {
                @Override
                public void accept(Exception error) {
                    operationPending = false;
                    ModernDialog.showError(parentFrame, "Update Error", error.getMessage());
                }
            });
        } catch (Exception ex) {
            ModernDialog.showError(parentFrame, "Update Error", ex.getMessage());
        }
    }

    private void deleteSelectedMember() {
        if (operationPending) return;
        if (currentlySelectedMember == null) {
            ModernDialog.showError(parentFrame, "Selection Required", "Please select a member from the table to delete.");
            return;
        }

        boolean confirm = ModernDialog.showConfirm(parentFrame, "Confirm Deletion", "Delete Member Account?",
                "Are you sure you want to permanently delete member:\n" + currentlySelectedMember.getCode()
                + " - " + currentlySelectedMember.getName() + "?");

        if (confirm) {
            try {
                final Member memberToDelete = currentlySelectedMember;
                operationPending = true;
                AsyncUi.run("ShelfSense-MemberDelete", new AsyncUi.Work() {
                    @Override
                    public void run() throws Exception {
                        service.deleteMember(memberToDelete.getMemberId());
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        operationPending = false;
                        ModernDialog.showSuccess(parentFrame, "Member Deleted", "Account Removed",
                                "The member account has been deleted.", null);
                        clearForm();
                        refresh();
                    }
                }, new java.util.function.Consumer<Exception>() {
                    @Override
                    public void accept(Exception error) {
                        operationPending = false;
                        ModernDialog.showError(parentFrame, "Deletion Error", error.getMessage());
                    }
                });
            } catch (Exception ex) {
                ModernDialog.showError(parentFrame, "Deletion Error", ex.getMessage());
            }
        }
    }

    private void applyFilter() {
        String q = tfSearch.getText();
        String cat = chCategoryFilter.getSelectedItem();
        List<Member> members = service.searchMembers(q, cat);
        loadTableData(members);
    }

    public synchronized void refresh() {
        applyFilter();
        if (currentlySelectedMember == null) {
            tfMemberIdPreview.setText(IDGenerator.formatMemberId(Member.nextId()));
        }
    }

    private void loadTableData(List<Member> members) {
        List<String[]> rows = new ArrayList<>();
        for (Member m : members) {
            int activeLoans = service.getActiveLoanCount(m.getMemberId());
            String loanProgress = activeLoans + " / " + m.getBorrowLimit();
            String status = (activeLoans >= m.getBorrowLimit()) ? "ISSUE BLOCKED" : "AVAILABLE";

            rows.add(new String[]{
                m.getCode(),
                m.getName(),
                m.getCategory(),
                m.getPhone(),
                String.valueOf(m.getBorrowLimit()),
                loanProgress,
                    status
            });
        }
        memberTable.setData(rows);
    }
}
