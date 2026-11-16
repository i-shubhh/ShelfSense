package ui.components;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern Dashboard Metric Card.
 * Displays key statistical indicators with custom vector glyphs and status badges.
 */
public class StatCard extends Canvas {

    public enum CardTheme {
        BLUE, GREEN, RED, AMBER, PURPLE
    }

    public enum IconKind {
        ITEMS, MEMBERS, ISSUED, AVAILABLE, OVERDUE, FINES, OUTSTANDING
    }

    private String title;
    private String value;
    private String subtitle;
    private CardTheme theme;
    private IconKind iconKind;
    private boolean isHovered = false;

    private Image offscreenImage;
    private Dimension offscreenSize;

    public StatCard(String title, String value, String subtitle, CardTheme theme, IconKind iconKind) {
        this.title = title;
        this.value = value;
        this.subtitle = subtitle;
        this.theme = theme;
        this.iconKind = iconKind;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    public void setValue(String value) {
        if (value == null ? this.value == null : value.equals(this.value)) return;
        this.value = value;
        repaint();
    }

    public void setSubtitle(String subtitle) {
        if (subtitle == null ? this.subtitle == null : subtitle.equals(this.subtitle)) return;
        this.subtitle = subtitle;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(185, 105);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(150, 95);
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

        // Clear parent background
        g2.setColor(getParent() != null ? getParent().getBackground() : UITheme.MAIN_BG);
        g2.fillRect(0, 0, w, h);

        // Card background
        g2.setColor(UITheme.CARD_BG);
        int arc = 12;
        g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);

        // Card border (changes to accent on hover)
        Color accentColor;
        Color badgeBg;
        switch (theme) {
            case GREEN:
                accentColor = UITheme.SUCCESS_GREEN;
                badgeBg = UITheme.SUCCESS_BG;
                break;
            case RED:
                accentColor = UITheme.DANGER_RED;
                badgeBg = UITheme.DANGER_BG;
                break;
            case AMBER:
                accentColor = UITheme.WARNING_AMBER;
                badgeBg = UITheme.WARNING_BG;
                break;
            case PURPLE:
                accentColor = UITheme.PURPLE_ACCENT;
                badgeBg = UITheme.PURPLE_BG;
                break;
            case BLUE:
            default:
                accentColor = UITheme.PRIMARY_BLUE;
                badgeBg = UITheme.PRIMARY_BG;
                break;
        }

        g2.setColor(isHovered ? accentColor : UITheme.BORDER_COLOR);
        g2.setStroke(new BasicStroke(isHovered ? 1.8f : 1.0f));
        g2.drawRoundRect(2, 2, w - 5, h - 5, arc, arc);

        // Draw Icon Circle Badge in top right
        int iconCircleSize = 36;
        int iconCircleX = w - iconCircleSize - 14;
        int iconCircleY = 14;

        g2.setColor(badgeBg);
        g2.fillOval(iconCircleX, iconCircleY, iconCircleSize, iconCircleSize);

        int iconX = iconCircleX + (iconCircleSize - 18) / 2;
        int iconY = iconCircleY + (iconCircleSize - 18) / 2;
        drawIcon(g2, iconX, iconY, 18, accentColor);

        // Draw Title (small uppercase)
        g2.setFont(UITheme.FONT_SMALL);
        g2.setColor(UITheme.TEXT_MUTED);
        g2.drawString(title.toUpperCase(), 14, 24);

        // Draw Metric Value
        g2.setFont(UITheme.FONT_CARD_VALUE);
        g2.setColor(UITheme.TEXT_DARK);
        g2.drawString(value, 14, 56);

        // Draw Subtitle / Status badge
        g2.setFont(UITheme.FONT_BADGE);
        g2.setColor(accentColor);
        g2.drawString(subtitle, 14, 84);

        g.drawImage(offscreenImage, 0, 0, this);
        g2.dispose();
    }

    private void drawIcon(Graphics2D g2, int x, int y, int size, Color c) {
        switch (iconKind) {
            case ITEMS:
            case AVAILABLE:
                UITheme.drawBookIcon(g2, x, y, size, c);
                break;
            case MEMBERS:
                UITheme.drawUsersIcon(g2, x, y, size, c);
                break;
            case ISSUED:
                UITheme.drawIssueIcon(g2, x, y, size, c);
                break;
            case OVERDUE:
                UITheme.drawAlertIcon(g2, x, y, size, c);
                break;
            case FINES:
            case OUTSTANDING:
                UITheme.drawMoneyIcon(g2, x, y, size, c);
                break;
            default:
                UITheme.drawCheckIcon(g2, x, y, size, c);
                break;
        }
    }
}
