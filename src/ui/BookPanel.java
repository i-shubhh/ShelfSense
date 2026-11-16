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
import java.util.ArrayList;
import java.util.List;

import exception.LibraryException;
import model.Book;
import model.DigitalMedia;
import model.ItemFactory;
import model.LibraryItem;
import model.Magazine;
import model.ReferenceBook;
import service.LibraryService;
import service.Validator;
import ui.components.ModernButton;
import ui.components.ModernDialog;
import ui.components.ModernTable;
import ui.components.UITheme;

/**
 * Library Item Catalogue Management Panel.
 * Supports adding books, reference books, periodicals, and digital media,
 * editing, deleting, searching, and filtering by category or availability.
 */
public class BookPanel extends Panel {

    private final LibraryService service;
    private final Frame parentFrame;

    private TextField tfSearch;
    private Choice chFilterType;
    private Choice chFilterAvail;
    private ModernTable itemTable;

    // Form fields
    private Choice chItemType;
    private TextField tfTitle;
    private TextField tfAuthor;
    private TextField tfIsbn;
    private TextField tfYear;
    private TextField tfCopies;
    private TextField tfExtra;
    private Label lblExtra;

    private LibraryItem currentlySelectedItem = null;
    private boolean operationPending;

    public BookPanel(LibraryService service, Frame parentFrame) {
        this.service = service;
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout(12, 12));
        setBackground(UITheme.MAIN_BG);

        initUI();
        refresh();
    }

    private void initUI() {
        // North: Search & Filter Toolbar
        Panel northPanel = new Panel(new BorderLayout(8, 8));
        northPanel.setBackground(UITheme.MAIN_BG);

        Panel headerPanel = new Panel(new BorderLayout()) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                UITheme.applyAntialiasing(g2);
                g2.setFont(UITheme.FONT_TITLE);
                g2.setColor(UITheme.TEXT_DARK);
                g2.drawString("Library Catalogue & Inventory", 4, 22);

                g2.setFont(UITheme.FONT_BODY);
                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString("Manage books, reference volumes, journals, and digital media with OOP polymorphism", 4, 42);
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
        chFilterType = new Choice();
        chFilterType.add("ALL");
        chFilterType.add("BOOK");
        chFilterType.add("REFERENCE");
        chFilterType.add("MAGAZINE");
        chFilterType.add("DIGITAL");
        chFilterType.addItemListener(e -> applyFilter());
        searchBar.add(chFilterType);

        searchBar.add(new Label("Stock:"));
        chFilterAvail = new Choice();
        chFilterAvail.add("ALL");
        chFilterAvail.add("AVAILABLE");
        chFilterAvail.add("ISSUED");
        chFilterAvail.addItemListener(e -> applyFilter());
        searchBar.add(chFilterAvail);

        ModernButton btnSearch = new ModernButton("Filter", ModernButton.ButtonType.PRIMARY, ModernButton.IconType.SEARCH);
        btnSearch.addActionListener(e -> applyFilter());
        searchBar.add(btnSearch);

        ModernButton btnReset = new ModernButton("Reset", ModernButton.ButtonType.SECONDARY);
        btnReset.addActionListener(e -> {
            tfSearch.setText("");
            chFilterType.select("ALL");
            chFilterAvail.select("ALL");
            applyFilter();
        });
        searchBar.add(btnReset);

        northPanel.add(searchBar, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);

        // Center: Item Table
        String[] headers = {"Code", "Type", "Title", "Author / Creator", "Year", "Stock", "Status"};
        float[] weights = {1.0f, 1.3f, 3.2f, 2.2f, 0.9f, 1.2f, 1.4f};
        itemTable = new ModernTable(headers, weights);
        itemTable.setRowsPerPage(11);

        itemTable.setSelectionListener((idx, row) -> {
            if (row != null && row.length > 0) {
                String code = row[0];
                currentlySelectedItem = service.findItemByCode(code);
                populateForm(currentlySelectedItem);
            }
        });

        add(itemTable, BorderLayout.CENTER);

        // East: Add / Edit Form Drawer
        Panel formPanel = createFormPanel();
        add(formPanel, BorderLayout.EAST);
    }

    private Panel createFormPanel() {
        Panel formCard = new Panel(new BorderLayout(10, 10));
        formCard.setBackground(UITheme.CARD_BG);
        formCard.setPreferredSize(new Dimension(340, 500));

        // Form Title
        Panel formHeader = new Panel(new BorderLayout()) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                UITheme.applyAntialiasing(g2);
                g2.setFont(UITheme.FONT_SECTION);
                g2.setColor(UITheme.TEXT_DARK);
                g2.drawString("Item Editor", 12, 22);

                g2.setFont(UITheme.FONT_SMALL);
                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString("Add or update catalogue specifications", 12, 38);

                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawLine(0, 48, getWidth(), 48);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(340, 52);
            }
        };
        formCard.add(formHeader, BorderLayout.NORTH);

        // Form Body with GridBagLayout
        Panel formBody = new Panel(new GridBagLayout());
        formBody.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 12, 4, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        // Item Type Choice
        gbc.gridx = 0; gbc.gridy = row++;
        formBody.add(new Label("Item Classification:"), gbc);
        chItemType = new Choice();
        chItemType.add("BOOK");
        chItemType.add("REFERENCE");
        chItemType.add("MAGAZINE");
        chItemType.add("DIGITAL");
        chItemType.addItemListener(e -> updateExtraLabel());
        gbc.gridy = row++;
        formBody.add(chItemType, gbc);

        // Title
        gbc.gridy = row++;
        formBody.add(new Label("Title:"), gbc);
        tfTitle = new TextField();
        gbc.gridy = row++;
        formBody.add(tfTitle, gbc);

        // Author / Creator / Publisher
        gbc.gridy = row++;
        formBody.add(new Label("Author / Publisher / Creator:"), gbc);
        tfAuthor = new TextField();
        gbc.gridy = row++;
        formBody.add(tfAuthor, gbc);

        // ISBN
        gbc.gridy = row++;
        formBody.add(new Label("ISBN (Books / Reference only):"), gbc);
        tfIsbn = new TextField();
        gbc.gridy = row++;
        formBody.add(tfIsbn, gbc);

        // Year & Total Copies in one row
        Panel twoCol = new Panel(new GridLayout(1, 2, 8, 0));
        twoCol.setBackground(UITheme.CARD_BG);

        Panel col1 = new Panel(new BorderLayout());
        col1.setBackground(UITheme.CARD_BG);
        col1.add(new Label("Year:"), BorderLayout.NORTH);
        tfYear = new TextField("2024");
        col1.add(tfYear, BorderLayout.CENTER);

        Panel col2 = new Panel(new BorderLayout());
        col2.setBackground(UITheme.CARD_BG);
        col2.add(new Label("Copies:"), BorderLayout.NORTH);
        tfCopies = new TextField("2");
        col2.add(tfCopies, BorderLayout.CENTER);

        twoCol.add(col1);
        twoCol.add(col2);

        gbc.gridy = row++;
        formBody.add(twoCol, gbc);

        // Extra Field (Edition / Format)
        lblExtra = new Label("Extra Spec (Edition / Format):");
        gbc.gridy = row++;
        formBody.add(lblExtra, gbc);
        tfExtra = new TextField();
        gbc.gridy = row++;
        formBody.add(tfExtra, gbc);

        formCard.add(formBody, BorderLayout.CENTER);

        // Buttons Bar
        Panel formButtons = new Panel(new GridLayout(2, 2, 8, 8));
        formButtons.setBackground(UITheme.HEADER_BG);

        ModernButton btnSaveNew = new ModernButton("Save New", ModernButton.ButtonType.PRIMARY, ModernButton.IconType.PLUS);
        btnSaveNew.addActionListener(e -> saveNewItem());

        ModernButton btnUpdate = new ModernButton("Update", ModernButton.ButtonType.SUCCESS, ModernButton.IconType.CHECK);
        btnUpdate.addActionListener(e -> updateExistingItem());

        ModernButton btnDelete = new ModernButton("Delete", ModernButton.ButtonType.DANGER, ModernButton.IconType.ALERT);
        btnDelete.addActionListener(e -> deleteSelectedItem());

        ModernButton btnClear = new ModernButton("Clear", ModernButton.ButtonType.SECONDARY);
        btnClear.addActionListener(e -> clearForm());

        formButtons.add(btnSaveNew);
        formButtons.add(btnUpdate);
        formButtons.add(btnDelete);
        formButtons.add(btnClear);

        formCard.add(formButtons, BorderLayout.SOUTH);
        return formCard;
    }

    private void updateExtraLabel() {
        String type = chItemType.getSelectedItem();
        if ("REFERENCE".equalsIgnoreCase(type)) {
            lblExtra.setText("Edition Number (e.g., 2, 3):");
            tfExtra.setEnabled(true);
        } else if ("DIGITAL".equalsIgnoreCase(type)) {
            lblExtra.setText("Format (e.g. MP4, PDF, Audio):");
            tfExtra.setEnabled(true);
        } else {
            lblExtra.setText("Extra Spec (Not required for " + type + "):");
            tfExtra.setText("");
            tfExtra.setEnabled(false);
        }
    }

    private void populateForm(LibraryItem it) {
        if (it == null) return;
        chItemType.select(it.getType());
        tfTitle.setText(it.getTitle());
        tfAuthor.setText(it.getAuthor());
        tfIsbn.setText(it.getIsbn());
        tfYear.setText(String.valueOf(it.getPublicationYear()));
        tfCopies.setText(String.valueOf(it.getTotalCopies()));
        tfExtra.setText(it.getExtra());
        updateExtraLabel();
    }

    private void clearForm() {
        currentlySelectedItem = null;
        tfTitle.setText("");
        tfAuthor.setText("");
        tfIsbn.setText("");
        tfYear.setText("2024");
        tfCopies.setText("2");
        tfExtra.setText("");
        chItemType.select("BOOK");
        updateExtraLabel();
    }

    private void saveNewItem() {
        if (operationPending) return;
        try {
            String title = Validator.requireText(tfTitle.getText(), "Title");
            String type = chItemType.getSelectedItem();
            String author = tfAuthor.getText().trim();
            String isbn = tfIsbn.getText().trim();
            int year = Validator.parseYear(tfYear.getText(), "Publication Year");
            int copies = Validator.parsePositive(tfCopies.getText(), "Copies");
            String extra = tfExtra.getText().trim();

            if ("BOOK".equalsIgnoreCase(type) || "REFERENCE".equalsIgnoreCase(type)) {
                if (!isbn.isEmpty()) {
                    Validator.validateIsbn(isbn);
                }
            }

            int newId = LibraryItem.nextId();
            LibraryItem item = ItemFactory.create(type, newId, title, author, isbn, year, copies, copies, extra);

            operationPending = true;
            AsyncUi.run("ShelfSense-ItemSave", new AsyncUi.Work() {
                @Override
                public void run() throws Exception {
                    service.addItem(item);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    operationPending = false;
                    ModernDialog.showSuccess(parentFrame, "Item Saved", "Library Item Added",
                            "Item \"" + item.getTitle() + "\" was added successfully.\nCode: " + item.generateItemCode(),
                            item.generateItemCode());
                    clearForm();
                    refresh();
                }
            }, new java.util.function.Consumer<Exception>() {
                @Override
                public void accept(Exception error) {
                    operationPending = false;
                    ModernDialog.showError(parentFrame, "Save Error", error.getMessage());
                }
            });
        } catch (Exception ex) {
            ModernDialog.showError(parentFrame, "Validation Error", ex.getMessage());
        }
    }

    private void updateExistingItem() {
        if (operationPending) return;
        if (currentlySelectedItem == null) {
            ModernDialog.showError(parentFrame, "Selection Required", "Please select an item from the table to update.");
            return;
        }

        try {
            String title = Validator.requireText(tfTitle.getText(), "Title");
            String type = chItemType.getSelectedItem();
            String author = tfAuthor.getText().trim();
            String isbn = tfIsbn.getText().trim();
            int year = Validator.parseYear(tfYear.getText(), "Publication Year");
            int totalCopies = Validator.parsePositive(tfCopies.getText(), "Total Copies");
            String extra = tfExtra.getText().trim();

            if ("BOOK".equalsIgnoreCase(type) || "REFERENCE".equalsIgnoreCase(type)) {
                if (!isbn.isEmpty()) {
                    Validator.validateIsbn(isbn);
                }
            }

            // Adjust available copies proportionally
            int diff = totalCopies - currentlySelectedItem.getTotalCopies();
            int newAvail = Math.max(0, currentlySelectedItem.getAvailableCopies() + diff);

            LibraryItem updated = ItemFactory.create(type, currentlySelectedItem.getItemId(), title,
                    author, isbn, year, totalCopies, newAvail, extra);

            operationPending = true;
            AsyncUi.run("ShelfSense-ItemUpdate", new AsyncUi.Work() {
                @Override
                public void run() throws Exception {
                    service.updateItem(updated);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    operationPending = false;
                    currentlySelectedItem = updated;
                    ModernDialog.showSuccess(parentFrame, "Item Updated", "Update Successful",
                            "Item " + updated.generateItemCode() + " has been updated.", null);
                    refresh();
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

    private void deleteSelectedItem() {
        if (operationPending) return;
        if (currentlySelectedItem == null) {
            ModernDialog.showError(parentFrame, "Selection Required", "Please select an item from the table to delete.");
            return;
        }

        boolean confirm = ModernDialog.showConfirm(parentFrame, "Confirm Delete", "Delete Library Item?",
                "Are you sure you want to permanently delete:\n" + currentlySelectedItem.generateItemCode()
                + " - " + currentlySelectedItem.getTitle() + "?");

        if (confirm) {
            try {
                final LibraryItem itemToDelete = currentlySelectedItem;
                operationPending = true;
                AsyncUi.run("ShelfSense-ItemDelete", new AsyncUi.Work() {
                    @Override
                    public void run() throws Exception {
                        service.deleteItem(itemToDelete.getItemId());
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        operationPending = false;
                        ModernDialog.showSuccess(parentFrame, "Item Deleted", "Deletion Completed",
                                "The item has been removed from the library database.", null);
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
        String query = tfSearch.getText();
        String typeFilter = chFilterType.getSelectedItem();
        String availFilter = chFilterAvail.getSelectedItem();

        List<LibraryItem> items = service.searchItems(query, typeFilter, availFilter);
        loadTableData(items);
    }

    public synchronized void refresh() {
        applyFilter();
    }

    private void loadTableData(List<LibraryItem> items) {
        List<String[]> rows = new ArrayList<>();
        for (LibraryItem it : items) {
            String stockStr = it.getAvailableCopies() + " / " + it.getTotalCopies();
            String statusBadge;
            if (service.getReservationCount(it.getItemId()) > 0) {
                statusBadge = "RESERVED";
            } else if (it.getAvailableCopies() <= 0) {
                statusBadge = "ISSUE BLOCKED";
            } else if (it.getAvailableCopies() == 1) {
                statusBadge = "LOW STOCK";
            } else {
                statusBadge = "AVAILABLE";
            }

            rows.add(new String[]{
                it.generateItemCode(),
                it.getType(),
                it.getTitle(),
                it.getAuthor().isEmpty() ? "-" : it.getAuthor(),
                String.valueOf(it.getPublicationYear()),
                stockStr,
                statusBadge
            });
        }
        itemTable.setData(rows);
    }
}
