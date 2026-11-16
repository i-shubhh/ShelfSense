package ui.components;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern interactive table component implemented in Core Java AWT Canvas.
 * Supports column headers, status badge rendering, selection, double-click actions,
 * and built-in pagination.
 */
public class ModernTable extends Canvas {

    public interface RowDoubleClickListener {
        void onRowDoubleClick(int rowIndex, String[] rowData);
    }

    public interface RowSelectionListener {
        void onRowSelected(int rowIndex, String[] rowData);
    }

    private final String[] columnNames;
    private final float[] columnWidthWeights;
    private final List<String[]> allRows = new ArrayList<>();
    private final List<String[]> filteredRows = new ArrayList<>();

    private int selectedRowIndex = -1;
    private int hoveredRowIndex = -1;
    private int currentPage = 0;
    private int rowsPerPage = 10;

    private RowDoubleClickListener doubleClickListener;
    private RowSelectionListener selectionListener;

    private Image offscreenImage;
    private Dimension offscreenSize;

    private final int headerHeight = 36;
    private final int rowHeight = 32;
    private final int footerHeight = 34;

    public ModernTable(String[] columnNames, float[] columnWidthWeights) {
        this.columnNames = columnNames;
        this.columnWidthWeights = columnWidthWeights;
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int y = e.getY();
                int x = e.getX();

                // Check footer pagination click
                if (y >= getHeight() - footerHeight) {
                    handleFooterClick(x, y);
                    return;
                }

                // Check row click
                if (y >= headerHeight && y < getHeight() - footerHeight) {
                    int clickedRow = (y - headerHeight) / rowHeight;
                    int actualIndex = currentPage * rowsPerPage + clickedRow;

                    if (actualIndex >= 0 && actualIndex < filteredRows.size()) {
                        selectedRowIndex = actualIndex;
                        repaint();

                        String[] row = filteredRows.get(actualIndex);
                        if (selectionListener != null) {
                            selectionListener.onRowSelected(actualIndex, row);
                        }

                        if (e.getClickCount() == 2 && doubleClickListener != null) {
                            doubleClickListener.onRowDoubleClick(actualIndex, row);
                        }
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRowIndex = -1;
                repaint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int y = e.getY();
                if (y >= headerHeight && y < getHeight() - footerHeight) {
                    int row = (y - headerHeight) / rowHeight;
                    int actualIndex = currentPage * rowsPerPage + row;
                    if (actualIndex < filteredRows.size() && actualIndex != hoveredRowIndex) {
                        hoveredRowIndex = actualIndex;
                        repaint();
                    }
                } else if (hoveredRowIndex != -1) {
                    hoveredRowIndex = -1;
                    repaint();
                }
            }
        });
    }

    public void setDoubleClickListener(RowDoubleClickListener l) {
        this.doubleClickListener = l;
    }

    public void setSelectionListener(RowSelectionListener l) {
        this.selectionListener = l;
    }

    public void setData(List<String[]> data) {
        allRows.clear();
        filteredRows.clear();
        if (data != null) {
            allRows.addAll(data);
            filteredRows.addAll(data);
        }
        currentPage = 0;
        selectedRowIndex = -1;
        repaint();
    }

    public void setFilteredData(List<String[]> data) {
        filteredRows.clear();
        if (data != null) {
            filteredRows.addAll(data);
        }
        currentPage = 0;
        selectedRowIndex = -1;
        repaint();
    }

    public int getSelectedRowIndex() {
        return selectedRowIndex;
    }

    public String[] getSelectedRow() {
        if (selectedRowIndex >= 0 && selectedRowIndex < filteredRows.size()) {
            return filteredRows.get(selectedRowIndex);
        }
        return null;
    }

    public void setRowsPerPage(int count) {
        this.rowsPerPage = Math.max(5, count);
        this.currentPage = 0;
        repaint();
    }

    private void handleFooterClick(int x, int y) {
        int w = getWidth();
        int totalPages = getTotalPages();

        // Prev button bounds: x from w - 180 to w - 100
        if (x >= w - 170 && x <= w - 100 && currentPage > 0) {
            currentPage--;
            repaint();
        }
        // Next button bounds: x from w - 90 to w - 20
        else if (x >= w - 90 && x <= w - 20 && currentPage < totalPages - 1) {
            currentPage++;
            repaint();
        }
    }

    private int getTotalPages() {
        if (filteredRows.isEmpty()) return 1;
        return (int) Math.ceil((double) filteredRows.size() / rowsPerPage);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(650, headerHeight + (rowsPerPage * rowHeight) + footerHeight);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(450, 200);
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (offscreenImage == null || offscreenSize == null
                || offscreenSize.width != w || offscreenSize.height != h) {
            offscreenImage = createImage(w, h);
            offscreenSize = new Dimension(w, h);
        }

        Graphics2D g2 = (Graphics2D) offscreenImage.getGraphics();
        UITheme.applyAntialiasing(g2);

        // Background
        g2.setColor(UITheme.CARD_BG);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);

        // Header Background
        g2.setColor(UITheme.HEADER_BG);
        g2.fillRoundRect(0, 0, w - 1, headerHeight, 8, 8);
        g2.fillRect(0, headerHeight - 8, w - 1, 8); // square bottom corners of header

        g2.setColor(UITheme.BORDER_COLOR);
        g2.drawLine(0, headerHeight, w, headerHeight);

        // Calculate Column Widths
        float totalWeight = 0;
        for (float weight : columnWidthWeights) {
            totalWeight += weight;
        }

        int[] colX = new int[columnNames.length];
        int[] colW = new int[columnNames.length];
        int currentX = 14;

        for (int i = 0; i < columnNames.length; i++) {
            colX[i] = currentX;
            int width = (int) ((w - 28) * (columnWidthWeights[i] / totalWeight));
            colW[i] = width;
            currentX += width;
        }

        // Draw Column Headers
        g2.setFont(UITheme.FONT_BODY_BOLD);
        g2.setColor(UITheme.TEXT_MUTED);
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i < columnNames.length; i++) {
            g2.drawString(columnNames[i], colX[i], (headerHeight + fm.getAscent() - fm.getDescent()) / 2);
        }

        // Draw Rows
        int startIndex = currentPage * rowsPerPage;
        int endIndex = Math.min(startIndex + rowsPerPage, filteredRows.size());

        for (int i = 0; i < rowsPerPage; i++) {
            int actualIndex = startIndex + i;
            int rowY = headerHeight + (i * rowHeight);

            if (actualIndex < endIndex) {
                String[] rowData = filteredRows.get(actualIndex);
                boolean isSelected = (actualIndex == selectedRowIndex);
                boolean isHover = (actualIndex == hoveredRowIndex);

                // Row Background
                if (isSelected) {
                    g2.setColor(UITheme.PRIMARY_BG);
                    g2.fillRect(1, rowY, w - 2, rowHeight);
                    // Blue indicator line on left
                    g2.setColor(UITheme.PRIMARY_BLUE);
                    g2.fillRect(1, rowY, 4, rowHeight);
                } else if (isHover) {
                    g2.setColor(UITheme.CARD_HOVER);
                    g2.fillRect(1, rowY, w - 2, rowHeight);
                } else if (i % 2 == 1) {
                    g2.setColor(new Color(250, 252, 254));
                    g2.fillRect(1, rowY, w - 2, rowHeight);
                }

                // Row bottom separator line
                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawLine(0, rowY + rowHeight, w, rowY + rowHeight);

                // Cell content
                g2.setFont(UITheme.FONT_BODY);
                int textY = rowY + (rowHeight + fm.getAscent() - fm.getDescent()) / 2;

                for (int c = 0; c < Math.min(columnNames.length, rowData.length); c++) {
                    String val = rowData[c];
                    if (isStatusOrBadge(val)) {
                        drawStatusBadge(g2, val, colX[c], rowY + 5, colW[c] - 10, rowHeight - 10);
                    } else {
                        g2.setColor(isSelected ? UITheme.PRIMARY_BLUE.darker() : UITheme.TEXT_DARK);
                        String truncated = truncate(val, colW[c] - 8, fm);
                        g2.drawString(truncated, colX[c], textY);
                    }
                }
            } else {
                // Empty row filler
                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawLine(0, rowY + rowHeight, w, rowY + rowHeight);
            }
        }

        // Draw Footer
        int footerY = h - footerHeight;
        g2.setColor(UITheme.CARD_BG);
        g2.fillRect(1, footerY, w - 2, footerHeight);
        g2.setColor(UITheme.BORDER_COLOR);
        g2.drawLine(0, footerY, w, footerY);

        // Record count text
        g2.setFont(UITheme.FONT_SMALL);
        g2.setColor(UITheme.TEXT_MUTED);
        String countStr;
        if (filteredRows.isEmpty()) {
            countStr = "No records found";
        } else {
            countStr = String.format("Showing %d-%d of %d records", startIndex + 1, endIndex, filteredRows.size());
        }
        g2.drawString(countStr, 14, footerY + 21);

        // Pagination buttons
        int totalPages = getTotalPages();
        String pageInfo = String.format("Page %d of %d", currentPage + 1, totalPages);
        g2.drawString(pageInfo, w - 280, footerY + 21);

        // Prev Button
        drawMiniButton(g2, "‹ Prev", w - 170, footerY + 6, 68, 22, currentPage > 0);
        // Next Button
        drawMiniButton(g2, "Next ›", w - 90, footerY + 6, 68, 22, currentPage < totalPages - 1);

        // Outer border
        g2.setColor(UITheme.BORDER_COLOR);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

        g.drawImage(offscreenImage, 0, 0, this);
        g2.dispose();
    }

    private void drawMiniButton(Graphics2D g2, String text, int x, int y, int bw, int bh, boolean enabled) {
        g2.setColor(enabled ? UITheme.CARD_HOVER : UITheme.CARD_BG);
        g2.fillRoundRect(x, y, bw, bh, 6, 6);
        g2.setColor(enabled ? UITheme.BORDER_FOCUS : UITheme.BORDER_COLOR);
        g2.drawRoundRect(x, y, bw, bh, 6, 6);

        g2.setFont(UITheme.FONT_SMALL);
        g2.setColor(enabled ? UITheme.PRIMARY_BLUE : UITheme.TEXT_LIGHT);
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (bw - fm.stringWidth(text)) / 2;
        int ty = y + (bh + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, tx, ty);
    }

    private boolean isStatusOrBadge(String text) {
        if (text == null) return false;
        String t = text.trim().toUpperCase();
        return t.equals("ISSUED") || t.equals("RETURNED") || t.equals("OVERDUE")
                || t.equals("PAID") || t.equals("UNPAID") || t.equals("AVAILABLE")
                || t.equals("LOW STOCK") || t.equals("OUT OF STOCK")
                || t.equals("RESERVED") || t.equals("ISSUE BLOCKED")
                || t.equals("POSSIBLY LOST");
    }

    private void drawStatusBadge(Graphics2D g2, String text, int x, int y, int maxW, int h) {
        String t = text.trim().toUpperCase();
        Color bg;
        Color fg;

        switch (t) {
            case "RETURNED":
            case "AVAILABLE":
            case "PAID":
                bg = UITheme.SUCCESS_BG;
                fg = UITheme.SUCCESS_GREEN;
                break;
            case "OVERDUE":
            case "POSSIBLY LOST":
            case "OUT OF STOCK":
            case "UNPAID":
                bg = UITheme.DANGER_BG;
                fg = UITheme.DANGER_RED;
                break;
            case "RESERVED":
            case "ISSUE BLOCKED":
                bg = UITheme.WARNING_BG;
                fg = UITheme.WARNING_AMBER;
                break;
            case "LOW STOCK":
                bg = UITheme.WARNING_BG;
                fg = UITheme.WARNING_AMBER;
                break;
            case "ISSUED":
            default:
                bg = UITheme.PRIMARY_BG;
                fg = UITheme.PRIMARY_BLUE;
                break;
        }

        g2.setFont(UITheme.FONT_BADGE);
        FontMetrics fm = g2.getFontMetrics();
        int bw = Math.min(maxW, fm.stringWidth(t) + 16);

        g2.setColor(bg);
        g2.fillRoundRect(x, y, bw, h, 12, 12);
        g2.setColor(fg);
        g2.drawString(t, x + (bw - fm.stringWidth(t)) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }

    private String truncate(String text, int maxPixelWidth, FontMetrics fm) {
        if (text == null) return "";
        if (fm.stringWidth(text) <= maxPixelWidth) return text;
        String ellipsis = "...";
        int ellipsisWidth = fm.stringWidth(ellipsis);
        if (maxPixelWidth <= ellipsisWidth) return ellipsis;
        StringBuilder sb = new StringBuilder();
        int currentWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            int charWidth = fm.charWidth(text.charAt(i));
            if (currentWidth + charWidth + ellipsisWidth > maxPixelWidth) {
                break;
            }
            sb.append(text.charAt(i));
            currentWidth += charWidth;
        }
        return sb.toString() + ellipsis;
    }

}
