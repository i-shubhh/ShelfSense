package ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import util.CurrencyUtils;

/**
 * Design system tokens, color palettes, typography, and vector icon painters.
 * Implements a modern SaaS desktop dashboard aesthetic using Core Java AWT Graphics2D.
 */
public final class UITheme {

    private UITheme() {
    }

    // ---------------------------------------------------------------- COLORS

    // Navigation & Sidebar
    public static final Color SIDEBAR_BG = new Color(15, 23, 42);          // #0F172A (Deep Slate Navy)
    public static final Color SIDEBAR_HOVER = new Color(30, 41, 59);       // #1E293B (Slate 800)
    public static final Color SIDEBAR_ACTIVE = new Color(37, 99, 235);     // #2563EB (Vibrant Royal Blue)
    public static final Color SIDEBAR_TEXT = new Color(148, 163, 184);     // #94A3B8 (Slate 400)
    public static final Color SIDEBAR_TEXT_ACTIVE = Color.WHITE;

    // Main Canvas & Cards
    public static final Color MAIN_BG = new Color(248, 250, 252);          // #F8FAFC (Slate 50)
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_HOVER = new Color(241, 245, 249);       // #F1F5F9 (Slate 100)
    public static final Color BORDER_COLOR = new Color(226, 232, 240);     // #E2E8F0 (Slate 200)
    public static final Color BORDER_FOCUS = new Color(59, 130, 246);      // #3B82F6 (Blue 500)
    public static final Color HEADER_BG = new Color(241, 245, 249);

    // Accent Colors
    public static final Color PRIMARY_BLUE = new Color(37, 99, 235);       // #2563EB (Blue 600)
    public static final Color PRIMARY_HOVER = new Color(29, 78, 216);      // #1D4ED8 (Blue 700)
    public static final Color PRIMARY_BG = new Color(239, 246, 255);       // #EFF6FF (Blue 50)

    public static final Color SUCCESS_GREEN = new Color(5, 150, 105);      // #059669 (Emerald 600)
    public static final Color SUCCESS_HOVER = new Color(4, 120, 87);
    public static final Color SUCCESS_BG = new Color(236, 253, 245);       // #ECFDF5 (Emerald 50)

    public static final Color DANGER_RED = new Color(220, 38, 38);         // #DC2626 (Red 600)
    public static final Color DANGER_HOVER = new Color(185, 28, 28);
    public static final Color DANGER_BG = new Color(254, 242, 242);        // #FEF2F2 (Red 50)

    public static final Color WARNING_AMBER = new Color(217, 119, 6);      // #D97706 (Amber 600)
    public static final Color WARNING_HOVER = new Color(180, 83, 9);
    public static final Color WARNING_BG = new Color(254, 243, 199);       // #FEF3C7 (Amber 50)

    public static final Color PURPLE_ACCENT = new Color(124, 58, 237);     // #7C3AED (Violet 600)
    public static final Color PURPLE_BG = new Color(245, 243, 255);        // #F5F3FF (Violet 50)

    // Typography Colors
    public static final Color TEXT_DARK = new Color(15, 23, 42);           // #0F172A
    public static final Color TEXT_MUTED = new Color(100, 116, 139);       // #64748B
    public static final Color TEXT_LIGHT = new Color(148, 163, 184);       // #94A3B8

    // ---------------------------------------------------------------- FONTS

    public static final Font FONT_HERO = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 17);
    public static final Font FONT_CARD_VALUE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_SECTION = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_BADGE = new Font("SansSerif", Font.BOLD, 11);
    public static final Font FONT_CODE = new Font("Monospaced", Font.BOLD, 12);
    public static final Font FONT_NAV = new Font("SansSerif", Font.BOLD, 13);

    // ---------------------------------------------------------- RENDERING

    public static void applyAntialiasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    // ----------------------------------------------------- VECTOR ICON PAINTERS

    public static void drawBookIcon(Graphics2D g2, int x, int y, int size, Color c) {
        Color prev = g2.getColor();
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int w = size;
        int h = (int) (size * 0.85);
        int midX = x + w / 2;

        // Spine
        g2.drawLine(midX, y + 2, midX, y + h);

        // Left Page
        g2.drawArc(x, y + 2, w / 2, 6, 0, 180);
        g2.drawLine(x, y + 5, x, y + h);
        g2.drawArc(x, y + h - 3, w / 2, 6, 180, 180);

        // Right Page
        g2.drawArc(midX, y + 2, w / 2, 6, 0, 180);
        g2.drawLine(x + w, y + 5, x + w, y + h);
        g2.drawArc(midX, y + h - 3, w / 2, 6, 180, 180);

        g2.setColor(prev);
    }

    public static void drawUsersIcon(Graphics2D g2, int x, int y, int size, Color c) {
        Color prev = g2.getColor();
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int headR = size / 3;
        int headX = x + size / 2 - headR / 2;
        int headY = y + 1;

        // Head
        g2.drawOval(headX, headY, headR, headR);

        // Torso arc
        int bodyY = headY + headR + 2;
        int bodyW = (int) (size * 0.8);
        int bodyX = x + (size - bodyW) / 2;
        g2.drawArc(bodyX, bodyY, bodyW, size - (bodyY - y), 0, 180);

        g2.setColor(prev);
    }

    public static void drawCheckIcon(Graphics2D g2, int x, int y, int size, Color c) {
        Color prev = g2.getColor();
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g2.drawLine(x + 2, y + size / 2, x + size / 3 + 1, y + size - 3);
        g2.drawLine(x + size / 3 + 1, y + size - 3, x + size - 2, y + 3);

        g2.setColor(prev);
    }

    public static void drawAlertIcon(Graphics2D g2, int x, int y, int size, Color c) {
        Color prev = g2.getColor();
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        Polygon p = new Polygon();
        p.addPoint(x + size / 2, y + 1);
        p.addPoint(x + 1, y + size - 1);
        p.addPoint(x + size - 1, y + size - 1);
        g2.drawPolygon(p);

        // Exclamation mark
        g2.drawLine(x + size / 2, y + size / 3, x + size / 2, y + (int) (size * 0.65));
        g2.fillOval(x + size / 2 - 1, y + (int) (size * 0.78), 3, 3);

        g2.setColor(prev);
    }

    public static void drawClockIcon(Graphics2D g2, int x, int y, int size, Color c) {
        Color prev = g2.getColor();
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g2.drawOval(x + 1, y + 1, size - 2, size - 2);
        int cx = x + size / 2;
        int cy = y + size / 2;
        g2.drawLine(cx, cy, cx, cy - size / 3);
        g2.drawLine(cx, cy, cx + size / 4, cy);

        g2.setColor(prev);
    }

    public static void drawMoneyIcon(Graphics2D g2, int x, int y, int size, Color c) {
        Color prev = g2.getColor();
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g2.drawOval(x + 1, y + 1, size - 2, size - 2);
        g2.setFont(new Font("SansSerif", Font.BOLD, (int) (size * 0.65)));
        g2.drawString(CurrencyUtils.symbol(), x + size / 3 - 1, y + (int) (size * 0.75));

        g2.setColor(prev);
    }

    public static void drawSearchIcon(Graphics2D g2, int x, int y, int size, Color c) {
        Color prev = g2.getColor();
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int lensSize = (int) (size * 0.65);
        g2.drawOval(x + 1, y + 1, lensSize, lensSize);
        g2.drawLine(x + lensSize - 1, y + lensSize - 1, x + size - 2, y + size - 2);

        g2.setColor(prev);
    }

    public static void drawPlusIcon(Graphics2D g2, int x, int y, int size, Color c) {
        Color prev = g2.getColor();
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int cx = x + size / 2;
        int cy = y + size / 2;
        g2.drawLine(cx, y + 2, cx, y + size - 2);
        g2.drawLine(x + 2, cy, x + size - 2, cy);

        g2.setColor(prev);
    }

    public static void drawRefreshIcon(Graphics2D g2, int x, int y, int size, Color c) {
        Color prev = g2.getColor();
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g2.drawArc(x + 2, y + 2, size - 4, size - 4, 45, 270);
        // Arrow head
        int ax = x + size / 2 + 3;
        int ay = y + 2;
        g2.drawLine(ax, ay, ax + 4, ay + 4);
        g2.drawLine(ax, ay, ax - 4, ay + 2);

        g2.setColor(prev);
    }

    public static void drawIssueIcon(Graphics2D g2, int x, int y, int size, Color c) {
        Color prev = g2.getColor();
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Arrow pointing up-right (outbox)
        g2.drawRect(x + 1, y + size / 3, size - 2, (int) (size * 0.6));
        g2.drawLine(x + size / 2, y + (int) (size * 0.7), x + size / 2, y + 2);
        g2.drawLine(x + size / 2, y + 2, x + size / 2 - 4, y + 6);
        g2.drawLine(x + size / 2, y + 2, x + size / 2 + 4, y + 6);

        g2.setColor(prev);
    }

    public static void drawReturnIcon(Graphics2D g2, int x, int y, int size, Color c) {
        Color prev = g2.getColor();
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Arrow pointing down (inbox)
        g2.drawRect(x + 1, y + size / 3, size - 2, (int) (size * 0.6));
        g2.drawLine(x + size / 2, y + 2, x + size / 2, y + (int) (size * 0.65));
        g2.drawLine(x + size / 2, y + (int) (size * 0.65), x + size / 2 - 4, y + (int) (size * 0.65) - 4);
        g2.drawLine(x + size / 2, y + (int) (size * 0.65), x + size / 2 + 4, y + (int) (size * 0.65) - 4);

        g2.setColor(prev);
    }
}
