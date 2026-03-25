package UI.panel;

import PD.DiceNotation;
import UI.theme.AppTheme;
import UI.widgets.CardPanel;
import UI.widgets.RoundedButton;
import service.ProbabilityCalculator;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;

/**
 * Displays the exact probability distribution for any NdF+M notation.
 * Uses a custom-painted bar chart with mean line, gradient fills, and stats.
 */
public class ProbabilityPanel extends JPanel {

    private JTextField notationInput;
    private JTextField atLeastField;
    private JLabel     atLeastResult;
    private JLabel     atMostResult;
    private JLabel     statsLabel;
    private JLabel     statusLabel;
    private BarChart   chartPanel;

    // Last computed distribution (highlighted when a roll result comes in)
    private int lastRolledValue = -1;

    public ProbabilityPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(AppTheme.bg());
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildSouth(),  BorderLayout.SOUTH);
    }

    /** Called externally to highlight a specific outcome on the chart. */
    public void highlightValue(int value) {
        lastRolledValue = value;
        chartPanel.setHighlight(value);
        chartPanel.repaint();
    }

    // ── Build sections ────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel title = new JLabel("\uD83D\uDCCA  Probability Calculator");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.accent2());
        p.add(title, BorderLayout.WEST);

        JLabel sub = new JLabel("Exact distribution \u2014 no simulation");
        sub.setFont(AppTheme.FONT_SMALL);
        sub.setForeground(AppTheme.muted());
        p.add(sub, BorderLayout.EAST);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);

        // Input row
        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        inputRow.setOpaque(false);
        inputRow.add(makeLabel("Notation:"));
        notationInput = textField(12);
        notationInput.setToolTipText("e.g. 3d6, 2d8+3, 1d20");
        notationInput.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) compute();
            }
        });
        inputRow.add(notationInput);
        RoundedButton computeBtn = RoundedButton.accent2("Compute");
        computeBtn.addActionListener(e -> compute());
        inputRow.add(computeBtn);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.FONT_SMALL);
        statusLabel.setForeground(AppTheme.danger());
        inputRow.add(statusLabel);

        p.add(inputRow, BorderLayout.NORTH);

        // Chart
        chartPanel = new BarChart();
        JScrollPane chartScroll = new JScrollPane(chartPanel);
        chartScroll.setBorder(BorderFactory.createLineBorder(AppTheme.border()));
        chartScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        chartScroll.getViewport().setBackground(AppTheme.panel());
        chartScroll.setPreferredSize(new Dimension(0, 320));
        p.add(chartScroll, BorderLayout.CENTER);

        return p;
    }

    private JPanel buildSouth() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);

        // Stats row
        statsLabel = new JLabel("Enter a notation above and click Compute");
        statsLabel.setFont(AppTheme.FONT_BODY);
        statsLabel.setForeground(AppTheme.muted());
        statsLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        p.add(statsLabel, BorderLayout.NORTH);

        // "Chance of >= X" row
        CardPanel chanceRow = CardPanel.titled("Probability Query", AppTheme.accent2());
        chanceRow.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 6));

        chanceRow.add(makeLabel("Roll \u2265"));
        atLeastField = textField(5);
        chanceRow.add(atLeastField);
        RoundedButton calcBtn = RoundedButton.plain("Calculate");
        calcBtn.addActionListener(e -> calcAtLeast());
        chanceRow.add(calcBtn);
        atLeastResult = new JLabel(" ");
        atLeastResult.setFont(AppTheme.FONT_BOLD);
        atLeastResult.setForeground(AppTheme.success());
        chanceRow.add(atLeastResult);

        p.add(chanceRow, BorderLayout.CENTER);
        return p;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void compute() {
        String nota = notationInput.getText().trim();
        if (nota.isEmpty()) { flash("Enter a notation", AppTheme.danger()); return; }

        try {
            DiceNotation dn = new DiceNotation(nota);
            int numDice  = dn.getNumberOfDice();
            int numFaces = dn.getNumberOfFaces();
            int modifier = dn.getModifier();

            Map<Integer, Double> dist = ProbabilityCalculator.computeDistribution(numDice, numFaces, modifier);
            double mean   = ProbabilityCalculator.expectedValue(numDice, numFaces, modifier);
            double stdDev = ProbabilityCalculator.standardDeviation(numDice, numFaces);

            chartPanel.setDistribution(dist, mean, lastRolledValue, AppTheme.accent(), AppTheme.accent2());
            chartPanel.setPreferredSize(new Dimension(Math.max(600, dist.size() * 28 + 60), 300));
            chartPanel.revalidate();
            chartPanel.repaint();

            int minOutcome = dist.isEmpty() ? 0 : dist.keySet().iterator().next();
            int maxOutcome = 0;
            for (int k : dist.keySet()) maxOutcome = k;

            statsLabel.setText(String.format(
                "Outcomes: %d \u2013 %d   |   Mean: %.2f   |   Std Dev: %.2f   |   Notation: %s",
                minOutcome, maxOutcome, mean, stdDev, dn.toString()));
            statsLabel.setForeground(AppTheme.fg());
            flash(" ", AppTheme.muted());
        } catch (IllegalArgumentException ex) {
            flash(ex.getMessage(), AppTheme.danger());
        }
    }

    private void calcAtLeast() {
        Map<Integer, Double> dist = chartPanel.getDistribution();
        if (dist == null || dist.isEmpty()) { flash("Compute a distribution first", AppTheme.danger()); return; }
        String txt = atLeastField.getText().trim();
        if (txt.isEmpty()) { flash("Enter a threshold value", AppTheme.danger()); return; }
        try {
            int threshold = Integer.parseInt(txt);
            double prob   = ProbabilityCalculator.probabilityAtLeast(dist, threshold);
            double probBelow = ProbabilityCalculator.probabilityAtMost(dist, threshold - 1);
            atLeastResult.setText(String.format(
                "\u2265 %d: %.1f%%    < %d: %.1f%%",
                threshold, prob * 100, threshold, probBelow * 100));
            chartPanel.setHighlight(threshold);
            chartPanel.repaint();
        } catch (NumberFormatException ex) {
            flash("Enter a whole number", AppTheme.danger());
        }
    }

    private void flash(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    // ── Widget factories ──────────────────────────────────────────────────────

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(AppTheme.fg());
        l.setFont(AppTheme.FONT_BODY);
        return l;
    }

    private JTextField textField(int cols) {
        JTextField f = new JTextField(cols);
        f.setBackground(AppTheme.field());
        f.setForeground(AppTheme.fg());
        f.setCaretColor(AppTheme.fg());
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.border()),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        return f;
    }

    // ── Inner class: Bar Chart ────────────────────────────────────────────────

    private static class BarChart extends JPanel {

        private Map<Integer, Double> distribution = new LinkedHashMap<>();
        private double mean        = 0;
        private int    highlight   = -1;
        private Color  barColor    = Color.CYAN;
        private Color  meanColor   = Color.MAGENTA;

        private static final int PAD_L  = 50;
        private static final int PAD_R  = 20;
        private static final int PAD_T  = 20;
        private static final int PAD_B  = 50;

        BarChart() {
            setBackground(AppTheme.panel());
        }

        void setDistribution(Map<Integer, Double> dist, double mean, int highlight,
                             Color barColor, Color meanColor) {
            this.distribution = dist;
            this.mean         = mean;
            this.highlight    = highlight;
            this.barColor     = barColor;
            this.meanColor    = meanColor;
        }

        Map<Integer, Double> getDistribution() { return distribution; }

        void setHighlight(int v) { highlight = v; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (distribution == null || distribution.isEmpty()) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.muted());
                g2.setFont(AppTheme.FONT_BODY);
                String msg = "No data — enter a notation and click Compute";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int chartW = w - PAD_L - PAD_R;
            int chartH = h - PAD_T - PAD_B;

            // Find max probability for scaling
            double maxProb = 0;
            for (double v : distribution.values()) maxProb = Math.max(maxProb, v);
            if (maxProb == 0) maxProb = 1;

            // Background grid lines
            g2.setColor(AppTheme.withAlpha(AppTheme.border(), 80));
            g2.setStroke(new BasicStroke(0.5f));
            int gridLines = 5;
            for (int i = 0; i <= gridLines; i++) {
                int y = PAD_T + chartH - (int) (chartH * i / (double) gridLines);
                g2.drawLine(PAD_L, y, PAD_L + chartW, y);
                // Y label (probability %)
                g2.setColor(AppTheme.muted());
                g2.setFont(AppTheme.FONT_SMALL);
                String pct = String.format("%.0f%%", maxProb * i / gridLines * 100);
                g2.drawString(pct, 2, y + 4);
                g2.setColor(AppTheme.withAlpha(AppTheme.border(), 80));
            }

            // Bars
            int n = distribution.size();
            if (n == 0) { g2.dispose(); return; }
            int barW = Math.max(8, chartW / n - 2);
            int gap  = Math.max(2, chartW / n - barW);
            Integer[] outcomes = distribution.keySet().toArray(new Integer[0]);

            for (int i = 0; i < n; i++) {
                int outcome = outcomes[i];
                double prob  = distribution.get(outcome);
                int barH     = (int) (chartH * prob / maxProb);
                int x        = PAD_L + i * (barW + gap);
                int y        = PAD_T + chartH - barH;

                // Bar color: highlighted = success, normal = accent gradient
                if (outcome == highlight) {
                    g2.setColor(AppTheme.success());
                } else {
                    // Gradient: bottom = barColor, top = barColor lighter
                    GradientPaint gp = new GradientPaint(
                        x, y + barH, AppTheme.withAlpha(barColor, 160),
                        x, y,        AppTheme.withAlpha(barColor, 230));
                    g2.setPaint(gp);
                }
                g2.fillRoundRect(x, y, barW, barH, 4, 4);

                // X-axis labels (every tick, but skip some if crowded)
                if (n <= 30 || i % (n / 20 + 1) == 0) {
                    g2.setColor(AppTheme.muted());
                    g2.setFont(AppTheme.FONT_SMALL);
                    String label = String.valueOf(outcome);
                    FontMetrics fm = g2.getFontMetrics();
                    int lx = x + (barW - fm.stringWidth(label)) / 2;
                    g2.drawString(label, lx, h - 30);
                }

                // Probability label on tall bars
                if (barH > 22) {
                    g2.setColor(AppTheme.bg());
                    g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                    String pct = String.format("%.1f", prob * 100);
                    FontMetrics fm = g2.getFontMetrics();
                    int lx = x + (barW - fm.stringWidth(pct)) / 2;
                    g2.drawString(pct, lx, y + barH - 4);
                }
            }

            // Mean vertical line
            double minOut = outcomes[0];
            double maxOut = outcomes[n - 1];
            if (maxOut > minOut) {
                double meanNorm = (mean - minOut) / (maxOut - minOut);
                int mx = PAD_L + (int) (meanNorm * chartW);
                g2.setColor(meanColor);
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10f, new float[]{6, 4}, 0));
                g2.drawLine(mx, PAD_T, mx, PAD_T + chartH);
                g2.setFont(AppTheme.FONT_SMALL);
                g2.setStroke(new BasicStroke(1f));
                g2.drawString(String.format("\u03bc=%.1f", mean), mx + 4, PAD_T + 14);
            }

            // Axes
            g2.setColor(AppTheme.border());
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(PAD_L, PAD_T, PAD_L, PAD_T + chartH);
            g2.drawLine(PAD_L, PAD_T + chartH, PAD_L + chartW, PAD_T + chartH);

            // X axis label
            g2.setColor(AppTheme.muted());
            g2.setFont(AppTheme.FONT_SMALL);
            g2.drawString("Outcome", PAD_L + chartW / 2 - 20, h - 8);

            g2.dispose();
        }
    }
}
