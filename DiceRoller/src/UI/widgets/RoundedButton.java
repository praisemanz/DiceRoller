package UI.widgets;

import UI.theme.AppTheme;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/**
 * A fully custom-painted button with rounded corners, drop shadow, and
 * smooth hover/press states. No default L&F borders or fills are used.
 *
 * Use the static factory methods to get themed variants:
 *   RoundedButton.accent("Roll")
 *   RoundedButton.plain("Clear")
 *   RoundedButton.ghost("Copy")
 *   RoundedButton.danger("Delete")
 *   RoundedButton.success("Save")
 */
public class RoundedButton extends JButton {

    // ── Style variants ────────────────────────────────────────────────────────
    public enum Variant { ACCENT, ACCENT2, PLAIN, GHOST, DANGER, SUCCESS, WARNING }

    private Variant  variant;
    private boolean  hovered  = false;
    private boolean  pressed  = false;
    private int      arc      = 10;  // corner radius
    private boolean  showShadow = true;

    // ── Factory methods ───────────────────────────────────────────────────────

    public static RoundedButton accent(String text) {
        return new RoundedButton(text, Variant.ACCENT);
    }

    public static RoundedButton accent2(String text) {
        return new RoundedButton(text, Variant.ACCENT2);
    }

    public static RoundedButton plain(String text) {
        return new RoundedButton(text, Variant.PLAIN);
    }

    public static RoundedButton ghost(String text) {
        return new RoundedButton(text, Variant.GHOST);
        }

    public static RoundedButton danger(String text) {
        return new RoundedButton(text, Variant.DANGER);
    }

    public static RoundedButton success(String text) {
        return new RoundedButton(text, Variant.SUCCESS);
    }

    public static RoundedButton warning(String text) {
        return new RoundedButton(text, Variant.WARNING);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    public RoundedButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(7, 18, 7, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(AppTheme.fontBold(13));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; pressed = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = true;  repaint(); }
            @Override public void mouseReleased(MouseEvent e){ pressed = false; repaint(); }
        });
    }

    public RoundedButton setArc(int arc)             { this.arc = arc; return this; }
    public RoundedButton noShadow()                  { this.showShadow = false; return this; }
    public RoundedButton small()                     { setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12)); setFont(AppTheme.font(12)); return this; }
    public RoundedButton large()                     { setBorder(BorderFactory.createEmptyBorder(10, 28, 10, 28)); setFont(AppTheme.fontBold(16)); return this; }

    // ── Paint ─────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();
        int sh = showShadow ? 2 : 0; // shadow offset

        // ── Shadow ───────────────────────────────────────────────────────────
        if (showShadow && !pressed) {
            g2.setColor(AppTheme.withAlpha(Color.BLACK, 55));
            g2.fill(new RoundRectangle2D.Float(sh, sh, w - sh, h - sh, arc, arc));
        }

        // ── Fill ─────────────────────────────────────────────────────────────
        Color fill   = getFillColor();
        Color border = getBorderColor();

        if (variant == Variant.GHOST) {
            // Ghost: transparent fill, just border
            g2.setColor(hovered ? AppTheme.withAlpha(border, 30) : new Color(0,0,0,0));
            g2.fill(new RoundRectangle2D.Float(0, 0, w - sh, h - sh, arc, arc));
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(0.75f, 0.75f, w - sh - 1.5f, h - sh - 1.5f, arc, arc));
        } else {
            // Gradient fill: slightly lighter at top
            Color fillTop = pressed
                ? fill.darker()
                : (hovered ? lighten(fill, 18) : fill);
            Color fillBot = pressed ? fill.darker() : fill;
            GradientPaint gp = new GradientPaint(0, 0, fillTop, 0, h - sh, fillBot);
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - sh, h - sh, arc, arc));
        }

        // ── Text ─────────────────────────────────────────────────────────────
        g2.setFont(getFont());
        g2.setColor(getTextColor());
        FontMetrics fm = g2.getFontMetrics();
        String txt = getText();
        int tx = (w - sh - fm.stringWidth(txt)) / 2;
        int ty = (h - sh - fm.getHeight()) / 2 + fm.getAscent();
        // Nudge down slightly when pressed
        if (pressed) ty += 1;
        g2.drawString(txt, tx, ty);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        // Ensure minimum width for small buttons
        d.width = Math.max(d.width, 60);
        return d;
    }

    // ── Color resolution ──────────────────────────────────────────────────────

    private Color getFillColor() {
        switch (variant) {
            case ACCENT:  return AppTheme.accent();
            case ACCENT2: return AppTheme.accent2();
            case DANGER:  return AppTheme.danger();
            case SUCCESS: return AppTheme.success();
            case WARNING: return AppTheme.warning();
            case PLAIN:   return hovered ? AppTheme.hover() : AppTheme.field();
            default:      return new Color(0, 0, 0, 0);
        }
    }

    private Color getBorderColor() {
        switch (variant) {
            case DANGER:  return AppTheme.danger();
            case SUCCESS: return AppTheme.success();
            case ACCENT:  return AppTheme.accent();
            case ACCENT2: return AppTheme.accent2();
            default:      return AppTheme.border();
        }
    }

    private Color getTextColor() {
        switch (variant) {
            case ACCENT:
            case ACCENT2:
            case DANGER:
            case SUCCESS:
            case WARNING:
                return AppTheme.bg();
            case GHOST:
                return getBorderColor();
            default:
                return AppTheme.fg();
        }
    }

    private static Color lighten(Color c, int amount) {
        return new Color(
            Math.min(255, c.getRed()   + amount),
            Math.min(255, c.getGreen() + amount),
            Math.min(255, c.getBlue()  + amount)
        );
    }
}
