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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern interactive button component painted using Java AWT Graphics2D.
 * Features rounded geometry, dynamic hover/click feedback, and integrated vector icons.
 */
public class ModernButton extends Canvas {

    public enum ButtonType {
        PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, ACCENT
    }

    public enum IconType {
        NONE, PLUS, SEARCH, REFRESH, ISSUE, RETURN, CHECK, ALERT, TRASH, MONEY
    }

    private String text;
    private String actionCommand;
    private ButtonType type;
    private IconType icon;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private boolean isEnabled = true;

    private final List<ActionListener> listeners = new ArrayList<>();
    private Image offscreenImage;
    private Dimension offscreenSize;

    public ModernButton(String text, ButtonType type, IconType icon) {
        this.text = text;
        this.type = type;
        this.icon = icon;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (isEnabled) {
                    isHovered = false;
                    isPressed = false;
                    repaint();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled && isPressed) {
                    isPressed = false;
                    repaint();
                    fireActionPerformed();
                }
            }
        });
    }

    public ModernButton(String text, ButtonType type) {
        this(text, type, IconType.NONE);
    }

    public ModernButton(String text) {
        this(text, ButtonType.PRIMARY, IconType.NONE);
    }

    public void addActionListener(ActionListener l) {
        listeners.add(l);
    }

    public void removeActionListener(ActionListener l) {
        listeners.remove(l);
    }

    public void setActionCommand(String command) {
        this.actionCommand = command;
    }

    public String getActionCommand() {
        return actionCommand != null ? actionCommand : text;
    }

    private void fireActionPerformed() {
        ActionEvent evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getActionCommand());
        for (ActionListener l : listeners) {
            l.actionPerformed(evt);
        }
    }

    public void setText(String text) {
        this.text = text;
        repaint();
    }

    public void setType(ButtonType type) {
        this.type = type;
        repaint();
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(UITheme.FONT_BODY_BOLD);
        int textWidth = fm.stringWidth(text);
        int iconWidth = (icon != IconType.NONE) ? 22 : 0;
        return new Dimension(Math.max(80, textWidth + iconWidth + 30), 36);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
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

        // Double-buffering to eliminate flickering
        if (offscreenImage == null || offscreenSize == null
                || offscreenSize.width != w || offscreenSize.height != h) {
            offscreenImage = createImage(w, h);
            offscreenSize = new Dimension(w, h);
        }

        Graphics2D g2 = (Graphics2D) offscreenImage.getGraphics();
        UITheme.applyAntialiasing(g2);

        // Fill background of parent canvas
        g2.setColor(getParent() != null ? getParent().getBackground() : UITheme.MAIN_BG);
        g2.fillRect(0, 0, w, h);

        // Determine colors based on state and button type
        Color bg;
        Color border;
        Color fg;

        switch (type) {
            case SECONDARY:
                bg = isPressed ? UITheme.BORDER_COLOR : (isHovered ? UITheme.CARD_HOVER : UITheme.CARD_BG);
                border = isHovered ? UITheme.BORDER_FOCUS : UITheme.BORDER_COLOR;
                fg = UITheme.TEXT_DARK;
                break;
            case SUCCESS:
                bg = isPressed ? UITheme.SUCCESS_HOVER : (isHovered ? UITheme.SUCCESS_GREEN : UITheme.SUCCESS_GREEN);
                border = bg;
                fg = Color.WHITE;
                break;
            case DANGER:
                bg = isPressed ? UITheme.DANGER_HOVER : (isHovered ? UITheme.DANGER_RED : UITheme.DANGER_RED);
                border = bg;
                fg = Color.WHITE;
                break;
            case WARNING:
                bg = isPressed ? UITheme.WARNING_HOVER : (isHovered ? UITheme.WARNING_AMBER : UITheme.WARNING_AMBER);
                border = bg;
                fg = Color.WHITE;
                break;
            case ACCENT:
                bg = isPressed ? UITheme.PURPLE_ACCENT.darker() : (isHovered ? UITheme.PURPLE_ACCENT : UITheme.PURPLE_ACCENT);
                border = bg;
                fg = Color.WHITE;
                break;
            case PRIMARY:
            default:
                bg = isPressed ? UITheme.PRIMARY_HOVER : (isHovered ? UITheme.PRIMARY_BLUE.brighter() : UITheme.PRIMARY_BLUE);
                border = bg;
                fg = Color.WHITE;
                break;
        }

        if (!isEnabled) {
            bg = UITheme.CARD_HOVER;
            border = UITheme.BORDER_COLOR;
            fg = UITheme.TEXT_LIGHT;
        }

        // Draw rounded rectangle background
        int arc = 8;
        g2.setColor(bg);
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

        g2.setColor(border);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

        // Calculate layout for icon and text
        g2.setFont(UITheme.FONT_BODY_BOLD);
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent() - fm.getDescent();

        int iconSize = 14;
        int spacing = 8;
        int totalContentWidth = textWidth + ((icon != IconType.NONE) ? (iconSize + spacing) : 0);
        int startX = (w - totalContentWidth) / 2;
        int contentY = (h + textHeight) / 2;

        if (icon != IconType.NONE) {
            int iconX = startX;
            int iconY = (h - iconSize) / 2;
            paintIcon(g2, iconX, iconY, iconSize, fg);
            startX += iconSize + spacing;
        }

        g2.setColor(fg);
        g2.drawString(text, startX, contentY);

        g.drawImage(offscreenImage, 0, 0, this);
        g2.dispose();
    }

    private void paintIcon(Graphics2D g2, int x, int y, int size, Color c) {
        switch (icon) {
            case PLUS:
                UITheme.drawPlusIcon(g2, x, y, size, c);
                break;
            case SEARCH:
                UITheme.drawSearchIcon(g2, x, y, size, c);
                break;
            case REFRESH:
                UITheme.drawRefreshIcon(g2, x, y, size, c);
                break;
            case ISSUE:
                UITheme.drawIssueIcon(g2, x, y, size, c);
                break;
            case RETURN:
                UITheme.drawReturnIcon(g2, x, y, size, c);
                break;
            case CHECK:
                UITheme.drawCheckIcon(g2, x, y, size, c);
                break;
            case ALERT:
                UITheme.drawAlertIcon(g2, x, y, size, c);
                break;
            case MONEY:
                UITheme.drawMoneyIcon(g2, x, y, size, c);
                break;
            default:
                break;
        }
    }
}
