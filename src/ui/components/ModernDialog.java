package ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Modern modal dialog for confirmations, alerts, and transaction receipts.
 */
public class ModernDialog extends Dialog {

    public enum DialogType {
        SUCCESS, WARNING, INFO, CONFIRM
    }

    private boolean confirmed = false;

    public ModernDialog(Frame parent, String title, String heading, String message,
                        String highlightId, DialogType type, boolean showCancel) {
        super(parent, title, true);
        setLayout(new BorderLayout());
        setBackground(UITheme.MAIN_BG);
        setSize(480, 320);
        setLocationRelativeTo(parent);

        // Header Panel
        Panel headerPanel = new Panel() {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                UITheme.applyAntialiasing(g2);
                g2.setColor(UITheme.SIDEBAR_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Draw Dialog Icon
                Color iconColor = (type == DialogType.SUCCESS) ? UITheme.SUCCESS_GREEN
                        : (type == DialogType.WARNING ? UITheme.DANGER_RED : UITheme.PRIMARY_BLUE);
                int iconSize = 22;
                int iconY = (getHeight() - iconSize) / 2;

                if (type == DialogType.SUCCESS) {
                    UITheme.drawCheckIcon(g2, 20, iconY, iconSize, iconColor);
                } else if (type == DialogType.WARNING) {
                    UITheme.drawAlertIcon(g2, 20, iconY, iconSize, iconColor);
                } else {
                    UITheme.drawBookIcon(g2, 20, iconY, iconSize, iconColor);
                }

                g2.setFont(UITheme.FONT_TITLE);
                g2.setColor(Color.WHITE);
                g2.drawString(title, 52, (getHeight() + 12) / 2);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(480, 50);
            }
        };
        add(headerPanel, BorderLayout.NORTH);

        // Content Panel
        Panel contentPanel = new Panel() {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                UITheme.applyAntialiasing(g2);
                int w = getWidth();

                g2.setColor(UITheme.CARD_BG);
                g2.fillRect(0, 0, w, getHeight());

                // Heading
                g2.setFont(UITheme.FONT_SECTION);
                g2.setColor(UITheme.TEXT_DARK);
                g2.drawString(heading, 24, 30);

                // Message
                g2.setFont(UITheme.FONT_BODY);
                g2.setColor(UITheme.TEXT_MUTED);

                // Word wrap message
                String[] lines = message.split("\n");
                int lineY = 55;
                for (String l : lines) {
                    g2.drawString(l, 24, lineY);
                    lineY += 20;
                }

                // Draw prominent Highlight Box if highlightId is present
                if (highlightId != null && !highlightId.trim().isEmpty()) {
                    int boxY = lineY + 10;
                    int boxH = 46;
                    int boxW = w - 48;

                    g2.setColor(UITheme.PRIMARY_BG);
                    g2.fillRoundRect(24, boxY, boxW, boxH, 8, 8);
                    g2.setColor(UITheme.BORDER_FOCUS);
                    g2.drawRoundRect(24, boxY, boxW, boxH, 8, 8);

                    g2.setFont(UITheme.FONT_SMALL);
                    g2.setColor(UITheme.TEXT_MUTED);
                    g2.drawString("TRANSACTION IDENTIFIER (SAVE FOR CHECK-IN)", 36, boxY + 16);

                    g2.setFont(new Font("Monospaced", Font.BOLD, 15));
                    g2.setColor(UITheme.PRIMARY_BLUE);
                    g2.drawString(highlightId, 36, boxY + 36);
                }
            }
        };
        add(contentPanel, BorderLayout.CENTER);

        // Button Bar
        Panel buttonPanel = new Panel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        buttonPanel.setBackground(UITheme.HEADER_BG);

        if (showCancel) {
            ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);
            btnCancel.addActionListener(e -> {
                confirmed = false;
                dispose();
            });
            buttonPanel.add(btnCancel);
        }

        ModernButton btnOk = new ModernButton("OK", ModernButton.ButtonType.PRIMARY, ModernButton.IconType.CHECK);
        btnOk.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        buttonPanel.add(btnOk);

        add(buttonPanel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmed = false;
                dispose();
            }
        });
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public static void showSuccess(Frame parent, String title, String heading, String message, String highlightId) {
        ModernDialog dlg = new ModernDialog(parent, title, heading, message, highlightId, DialogType.SUCCESS, false);
        dlg.setVisible(true);
    }

    public static void showError(Frame parent, String title, String message) {
        ModernDialog dlg = new ModernDialog(parent, title, "Action Failed", message, null, DialogType.WARNING, false);
        dlg.setVisible(true);
    }

    public static void showInfo(Frame parent, String title, String heading, String message) {
        ModernDialog dlg = new ModernDialog(parent, title, heading, message, null, DialogType.INFO, false);
        dlg.setVisible(true);
    }

    public static boolean showConfirm(Frame parent, String title, String heading, String message) {
        ModernDialog dlg = new ModernDialog(parent, title, heading, message, null, DialogType.CONFIRM, true);
        dlg.setVisible(true);
        return dlg.isConfirmed();
    }
}

