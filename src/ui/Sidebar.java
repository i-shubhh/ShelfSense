package ui;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import ui.components.UITheme;

/**
 * Beautiful modern navigation sidebar.
 * Features dark navy theme (#0F172A), vector brand header, dynamic section switching,
 * live overdue notification badge, and background real-time clock.
 */
public class Sidebar extends Canvas {

    public enum Section {
        DASHBOARD("Dashboard"),
        CATALOGUE("Library Catalogue"),
        MEMBERS("Member Directory"),
        ISSUE("Issue Circulation"),
        RETURN("Return & Late Fees"),
        TRANSACTIONS("Transaction History");

        private final String title;

        Section(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    private Section currentSection = Section.DASHBOARD;
    private int hoveredIndex = -1;
    private int overdueCount = 0;
    private String currentTimeStr = "";
    private Consumer<Section> onSectionChanged;
    private volatile boolean clockRunning = true;
    private Thread clockThread;

    private Image offscreenImage;
    private Dimension offscreenSize;

    private final int itemHeight = 46;
    private final int itemGap = 6;
    private final int topHeaderHeight = 92;

    public Sidebar(Consumer<Section> onSectionChanged) {
        this.onSectionChanged = onSectionChanged;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Real-time clock background thread
        clockThread = new Thread(() -> {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, dd MMM • HH:mm:ss");
            while (clockRunning) {
                currentTimeStr = LocalDateTime.now().format(fmt);
                repaint();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    if (clockRunning) {
                        Thread.currentThread().interrupt();
                    }
                    break;
                }
            }
        }, "Sidebar-ClockThread");
        clockThread.setDaemon(true);
        clockThread.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int y = e.getY();
                int relY = y - topHeaderHeight;
                if (relY >= 0) {
                    int slot = itemHeight + itemGap;
                    int idx = relY / slot;
                    int inSlot = relY % slot;
                    Section[] sections = Section.values();
                    if (idx >= 0 && idx < sections.length && inSlot <= itemHeight) {
                        currentSection = sections[idx];
                        repaint();
                        if (onSectionChanged != null) {
                            onSectionChanged.accept(currentSection);
                        }
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoveredIndex = -1;
                repaint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int y = e.getY();
                int relY = y - topHeaderHeight;
                if (relY >= 0) {
                    int slot = itemHeight + itemGap;
                    int idx = relY / slot;
                    int inSlot = relY % slot;
                    Section[] sections = Section.values();
                    if (idx >= 0 && idx < sections.length && inSlot <= itemHeight) {
                        if (idx != hoveredIndex) {
                            hoveredIndex = idx;
                            repaint();
                        }
                    } else if (hoveredIndex != -1) {
                        hoveredIndex = -1;
                        repaint();
                    }
                } else if (hoveredIndex != -1) {
                    hoveredIndex = -1;
                    repaint();
                }
            }
        });
    }

    public void setSection(Section section) {
        this.currentSection = section;
        repaint();
    }

    public Section getCurrentSection() {
        return currentSection;
    }

    public void setOverdueCount(int count) {
        this.overdueCount = count;
        repaint();
    }

    public void stopClock() {
        clockRunning = false;
        if (clockThread != null) {
            clockThread.interrupt();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(225, 700);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(200, 550);
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

        // Sidebar Background
        g2.setColor(UITheme.SIDEBAR_BG);
        g2.fillRect(0, 0, w, h);

        // Brand Logo & Title Area
        int logoX = 18;
        int logoY = 22;
        int logoSize = 28;

        // Draw modern library logo badge
        g2.setColor(UITheme.PRIMARY_BLUE);
        g2.fillRoundRect(logoX, logoY, logoSize, logoSize, 8, 8);
        UITheme.drawBookIcon(g2, logoX + 5, logoY + 5, 18, Color.WHITE);

        // Brand Title
        g2.setFont(UITheme.FONT_TITLE);
        g2.setColor(Color.WHITE);
        g2.drawString("ShelfSense", logoX + logoSize + 10, logoY + 18);

        // Tagline
        g2.setFont(UITheme.FONT_SMALL);
        g2.setColor(new Color(100, 116, 139));
        g2.drawString("Library Intelligence", logoX + logoSize + 10, logoY + 31);

        // Header Divider
        g2.setColor(new Color(30, 41, 59));
        g2.drawLine(14, topHeaderHeight - 12, w - 14, topHeaderHeight - 12);

        // Navigation Items
        Section[] sections = Section.values();
        FontMetrics fm = g2.getFontMetrics(UITheme.FONT_NAV);

        for (int i = 0; i < sections.length; i++) {
            Section s = sections[i];
            int itemY = topHeaderHeight + i * (itemHeight + itemGap);
            boolean isActive = (s == currentSection);
            boolean isHover = (i == hoveredIndex && !isActive);

            int padX = 12;
            int itemW = w - (padX * 2);

            // Background pill
            if (isActive) {
                g2.setColor(UITheme.SIDEBAR_ACTIVE);
                g2.fillRoundRect(padX, itemY, itemW, itemHeight, 8, 8);
            } else if (isHover) {
                g2.setColor(UITheme.SIDEBAR_HOVER);
                g2.fillRoundRect(padX, itemY, itemW, itemHeight, 8, 8);
            }

            // Draw Icon
            int iconX = padX + 12;
            int iconY = itemY + (itemHeight - 16) / 2;
            Color iconColor = isActive ? Color.WHITE : (isHover ? Color.WHITE : UITheme.SIDEBAR_TEXT);
            paintSectionIcon(g2, s, iconX, iconY, 16, iconColor);

            // Draw Section Title
            g2.setFont(UITheme.FONT_NAV);
            g2.setColor(isActive ? Color.WHITE : (isHover ? Color.WHITE : UITheme.SIDEBAR_TEXT));
            g2.drawString(s.getTitle(), iconX + 26, itemY + (itemHeight + fm.getAscent() - fm.getDescent()) / 2);

            // Draw Overdue Badge on "Return & Late Fees"
            if (s == Section.RETURN && overdueCount > 0) {
                String badgeText = String.valueOf(overdueCount);
                g2.setFont(UITheme.FONT_BADGE);
                FontMetrics bfm = g2.getFontMetrics();
                int bw = Math.max(20, bfm.stringWidth(badgeText) + 10);
                int bh = 18;
                int bx = w - padX - bw - 8;
                int by = itemY + (itemHeight - bh) / 2;

                g2.setColor(UITheme.DANGER_RED);
                g2.fillRoundRect(bx, by, bw, bh, 9, 9);
                g2.setColor(Color.WHITE);
                g2.drawString(badgeText, bx + (bw - bfm.stringWidth(badgeText)) / 2, by + 13);
            }
        }

        // Bottom Footer (Live Clock & System Status)
        int footerH = 70;
        int footerY = h - footerH;

        g2.setColor(new Color(30, 41, 59));
        g2.drawLine(14, footerY, w - 14, footerY);

        // System Online Indicator
        g2.setColor(UITheme.SUCCESS_GREEN);
        g2.fillOval(18, footerY + 16, 8, 8);

        g2.setFont(UITheme.FONT_SMALL);
        g2.setColor(new Color(203, 213, 225));
        g2.drawString("Core Engine Active", 32, footerY + 24);

        // Real-Time Clock String
        g2.setColor(new Color(100, 116, 139));
        g2.drawString(currentTimeStr, 18, footerY + 46);

        g.drawImage(offscreenImage, 0, 0, this);
        g2.dispose();
    }

    private void paintSectionIcon(Graphics2D g2, Section s, int x, int y, int size, Color c) {
        switch (s) {
            case DASHBOARD:
                // Home/Grid icon
                g2.setColor(c);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(x, y, size / 2 - 1, size / 2 - 1, 3, 3);
                g2.drawRoundRect(x + size / 2 + 1, y, size / 2 - 1, size / 2 - 1, 3, 3);
                g2.drawRoundRect(x, y + size / 2 + 1, size / 2 - 1, size / 2 - 1, 3, 3);
                g2.drawRoundRect(x + size / 2 + 1, y + size / 2 + 1, size / 2 - 1, size / 2 - 1, 3, 3);
                break;
            case CATALOGUE:
                UITheme.drawBookIcon(g2, x, y, size, c);
                break;
            case MEMBERS:
                UITheme.drawUsersIcon(g2, x, y, size, c);
                break;
            case ISSUE:
                UITheme.drawIssueIcon(g2, x, y, size, c);
                break;
            case RETURN:
                UITheme.drawReturnIcon(g2, x, y, size, c);
                break;
            case TRANSACTIONS:
                UITheme.drawMoneyIcon(g2, x, y, size, c);
                break;
        }
    }
}
