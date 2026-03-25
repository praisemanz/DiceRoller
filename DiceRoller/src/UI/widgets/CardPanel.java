package UI.widgets;

import UI.theme.AppTheme;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/**
 * A card-style JPanel with:
 *   - Rounded corners (configurable radius)
 *   - Drop shadow
 *   - Optional colored left accent strip
 *   - Optional styled title drawn at the top
 *
 * Usage:
 *   CardPanel card = new CardPanel("Quick Roll", AppTheme.accent());
 *   card.setLayout(new FlowLayout());
 *   card.add(someButton);
 */
public class CardPanel extends JPanel {

    private final String title;
    private final Color  titleColor;

    private int   arc        = 14;   // corner radius
    private int   shadowSize = 3;
    private int   stripWidth = 0;    // 0 = no strip; > 0 = draw left accent strip

    // Inner content insets (so children don't overlap the title or strip)
    private static final int TITLE_H  = 28;
    private static final int STRIP_W  = 4;
    private static final int PAD      = 10;

    // ── Factory helpers ───────────────────────────────────────────────────────

    public static CardPanel titled(String title, Color accentColor) {
        CardPanel p = new CardPanel(title, accentColor);
        p.stripWidth = STRIP_W;
        return p;
    }

    public static CardPanel plain() {
        return new CardPanel(null, null);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    public CardPanel(String title, Color titleColor) {
        this.title      = title;
        this.titleColor = titleColor;
        setOpaque(false);

        // Reserve space for title + padding at the top, strip on the left
        int top  = (title != null && !title.isEmpty()) ? TITLE_H + PAD : PAD;
        int left = (stripWidth > 0) ? STRIP_W + PAD + 2 : PAD;
        setBorder(BorderFactory.createEmptyBorder(top, left + 4, PAD, PAD));
    }

    public CardPanel arc(int r)         { this.arc = r;        return this; }
    public CardPanel shadow(int s)      { this.shadowSize = s; return this; }
    public CardPanel strip(int w)       { this.stripWidth = w; return this; }

    // ── Paint ─────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth()  - shadowSize;
        int h = getHeight() - shadowSize;

        // ── Shadow ────────────────────────────────────────────────────────────
        for (int i = shadowSize; i > 0; i--) {
            int alpha = (int) (55 * (shadowSize - i + 1.0) / shadowSize);
            g2.setColor(new Color(0, 0, 0, alpha));
            g2.fill(new RoundRectangle2D.Float(i, i, w, h, arc + i, arc + i));
        }

        // ── Card background ───────────────────────────────────────────────────
        g2.setColor(AppTheme.panel());
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));

        // ── Left accent strip ─────────────────────────────────────────────────
        if (stripWidth > 0 && titleColor != null) {
            g2.setColor(titleColor);
            // Clip to rounded card shape so the strip respects the corners
            Shape clip = new RoundRectangle2D.Float(0, 0, w, h, arc, arc);
            g2.setClip(clip);
            g2.fillRect(0, 0, stripWidth, h);
            g2.setClip(null);
        }

        // ── Title ─────────────────────────────────────────────────────────────
        if (title != null && !title.isEmpty()) {
            g2.setFont(AppTheme.fontBold(13));
            g2.setColor(titleColor != null ? titleColor : AppTheme.fg());
            int tx = (stripWidth > 0) ? stripWidth + 10 : 12;
            int ty = TITLE_H / 2 + g2.getFontMetrics().getAscent() / 2 + 2;
            g2.drawString(title, tx, ty);

            // Subtle separator line below title
            g2.setColor(AppTheme.withAlpha(AppTheme.border(), 100));
            g2.setStroke(new BasicStroke(0.8f));
            int lineX = (stripWidth > 0) ? stripWidth : 1;
            g2.drawLine(lineX, TITLE_H, w - 1, TITLE_H);
        }

        g2.dispose();

        // Let Swing paint children on top
        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        // Painted in paintComponent already via stroke; suppress default
    }

    @Override
    public boolean isOpaque() {
        return false;
    }
}
