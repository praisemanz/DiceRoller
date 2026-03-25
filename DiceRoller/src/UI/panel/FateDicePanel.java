package UI.panel;

import PD.*;
import UI.theme.AppTheme;
import UI.widgets.CardPanel;
import UI.widgets.RoundedButton;
import service.HashService;
import service.SoundService;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/**
 * Fate/Fudge dice panel (4dF + modifier).
 * Displays [+] [-] [ ] symbols, total, and the Fate Ladder adjective.
 */
public class FateDicePanel extends JPanel {

    private final RollHistory  rollHistory;
    private final SoundService soundService;

    private final FateRoll fateRoll;

    // ── Widgets ────────────────────────────────────────────────────────────────
    private FateDieDisplay[] diePanels;
    private JLabel            totalLabel;
    private JLabel            ladderLabel;
    private JLabel            modLabel;
    private JSpinner          modSpinner;
    private JLabel            statusLabel;
    private DefaultListModel<String> histModel;
    private JList<String>     histList;
    private JLabel            statsLabel;

    public FateDicePanel(RollHistory rollHistory, SoundService soundService) {
        this.rollHistory  = rollHistory;
        this.soundService = soundService;
        this.fateRoll     = new FateRoll(0);

        setLayout(new BorderLayout(10, 10));
        setBackground(AppTheme.bg());
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);
        add(buildSouth(),   BorderLayout.SOUTH);

        refreshHistory();
    }

    // ── Build sections ────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JLabel title = new JLabel("\u2721  Fate Dice  (4dF)");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.success());
        p.add(title, BorderLayout.WEST);
        JLabel sub = new JLabel("For Fate Core, Fudge, and narrative RPGs");
        sub.setFont(AppTheme.FONT_SMALL);
        sub.setForeground(AppTheme.muted());
        p.add(sub, BorderLayout.EAST);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        // Die display row
        CardPanel diceRow = CardPanel.titled("Fate Dice", AppTheme.success());
        diceRow.setLayout(new FlowLayout(FlowLayout.CENTER, 16, 16));
        diePanels = new FateDieDisplay[4];
        for (int i = 0; i < 4; i++) {
            diePanels[i] = new FateDieDisplay();
            diceRow.add(diePanels[i]);
        }
        p.add(diceRow);
        p.add(Box.createVerticalStrut(8));

        // Result row
        CardPanel resultPanel = CardPanel.titled("Result", AppTheme.success());
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));

        totalLabel = new JLabel("?");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 60));
        totalLabel.setForeground(AppTheme.success());
        totalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        resultPanel.add(totalLabel);

        ladderLabel = new JLabel("Roll to see your Fate!");
        ladderLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        ladderLabel.setForeground(AppTheme.muted());
        ladderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        resultPanel.add(ladderLabel);
        resultPanel.add(Box.createVerticalStrut(8));

        // Modifier row
        JPanel modRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        modRow.setOpaque(false);
        modLabel = new JLabel("Modifier:");
        modLabel.setForeground(AppTheme.fg());
        modLabel.setFont(AppTheme.FONT_BODY);
        modRow.add(modLabel);

        SpinnerNumberModel spinModel = new SpinnerNumberModel(0, -10, 10, 1);
        modSpinner = new JSpinner(spinModel);
        modSpinner.setFont(AppTheme.FONT_BODY);
        modSpinner.setBackground(AppTheme.field());
        modSpinner.setForeground(AppTheme.fg());
        ((JSpinner.DefaultEditor) modSpinner.getEditor()).getTextField().setBackground(AppTheme.field());
        ((JSpinner.DefaultEditor) modSpinner.getEditor()).getTextField().setForeground(AppTheme.fg());
        ((JSpinner.DefaultEditor) modSpinner.getEditor()).getTextField().setFont(AppTheme.FONT_BODY);
        modRow.add(modSpinner);
        resultPanel.add(modRow);
        resultPanel.add(Box.createVerticalStrut(8));

        // Roll button
        RoundedButton rollBtn = RoundedButton.success("\u2721  Roll Fate Dice");
        rollBtn.large();
        rollBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        rollBtn.addActionListener(e -> rollFate());
        resultPanel.add(rollBtn);
        resultPanel.add(Box.createVerticalStrut(4));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.FONT_SMALL);
        statusLabel.setForeground(AppTheme.muted());
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        resultPanel.add(statusLabel);

        p.add(resultPanel);

        return p;
    }

    private JPanel buildSouth() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);

        CardPanel histPanel = CardPanel.titled("Fate Roll History", AppTheme.success());
        histPanel.setLayout(new BorderLayout(0, 4));

        histModel = new DefaultListModel<>();
        histList  = new JList<>(histModel);
        histList.setBackground(AppTheme.field());
        histList.setForeground(AppTheme.fg());
        histList.setSelectionBackground(AppTheme.hover());
        histList.setSelectionForeground(AppTheme.success());
        histList.setFont(AppTheme.FONT_MONO);
        histList.setFixedCellHeight(22);

        JScrollPane sp = new JScrollPane(histList);
        sp.setPreferredSize(new Dimension(0, 100));
        sp.setBorder(BorderFactory.createLineBorder(AppTheme.border()));
        sp.getViewport().setBackground(AppTheme.field());
        histPanel.add(sp, BorderLayout.CENTER);

        statsLabel = new JLabel("No fate rolls yet");
        statsLabel.setFont(AppTheme.FONT_SMALL);
        statsLabel.setForeground(AppTheme.muted());
        histPanel.add(statsLabel, BorderLayout.SOUTH);

        p.add(histPanel, BorderLayout.CENTER);
        return p;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void rollFate() {
        int mod = (Integer) modSpinner.getValue();
        fateRoll.setModifier(mod);

        // Animate the dice
        Timer animTimer = new Timer(50, null);
        final int[] frames = {0};
        animTimer.addActionListener(e -> {
            frames[0]++;
            if (frames[0] < 8) {
                for (FateDieDisplay d : diePanels) {
                    int rnd = (int)(Math.random() * 3);
                    FateDie.Face[] faces = FateDie.Face.values();
                    d.setFace(faces[rnd]);
                    d.repaint();
                }
            } else {
                animTimer.stop();
                int total = fateRoll.roll();
                for (int i = 0; i < 4; i++) {
                    diePanels[i].setFace(fateRoll.getDice().get(i).getCurrentFace());
                    diePanels[i].repaint();
                }
                showResult(total);
            }
        });
        animTimer.start();
        soundService.play(SoundService.Sound.ROLL);
    }

    private void showResult(int total) {
        totalLabel.setText((total > 0 ? "+" : "") + total);
        String ladder = fateRoll.getLadderResult();
        ladderLabel.setText(ladder);
        totalLabel.setForeground(ladderColor(total));
        ladderLabel.setForeground(ladderColor(total));

        // Record in shared history
        RollResult r = new RollResult("4dF", total, fateRoll.getDieValues(),
            fateRoll.getModifier(), "Fate");
        r.setVerificationHash(HashService.shortHash(r));
        rollHistory.addResult(r);

        int idx = rollHistory.getRollCount();
        String entry = String.format("#%-3d  4dF %s %d  %-12s  %s",
            idx, total >= 0 ? "=" : "=", total, "[" + ladder + "]", r.getTimeString());
        histModel.addElement(entry);
        histList.ensureIndexIsVisible(histModel.getSize() - 1);
        statsLabel.setText("Fate rolls this session: " + histModel.getSize());

        statusLabel.setText("SHA: #" + r.getVerificationHash().substring(0, 8));
        statusLabel.setForeground(AppTheme.muted());
    }

    private Color ladderColor(int total) {
        if (total >= 3) return AppTheme.success();
        if (total >= 1) return AppTheme.accent();
        if (total == 0) return AppTheme.muted();
        if (total >= -2) return AppTheme.warning();
        return AppTheme.danger();
    }

    private void refreshHistory() {
        histModel.clear();
    }

    // ── Inner class: Fate Die Visual ──────────────────────────────────────────

    private static class FateDieDisplay extends JPanel {
        private FateDie.Face face = null;

        FateDieDisplay() {
            setPreferredSize(new Dimension(88, 88));
            setOpaque(false);
        }

        void setFace(FateDie.Face f) { this.face = f; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth() - 4;
            int h = getHeight() - 4;

            // Shadow
            g2.setColor(AppTheme.withAlpha(Color.BLACK, 70));
            g2.fill(new RoundRectangle2D.Float(3, 3, w, h, 16, 16));

            // Die background
            Color bgColor;
            if (face == null) {
                bgColor = AppTheme.field();
            } else if (face == FateDie.Face.PLUS) {
                bgColor = AppTheme.withAlpha(AppTheme.success(), 60);
            } else if (face == FateDie.Face.MINUS) {
                bgColor = AppTheme.withAlpha(AppTheme.danger(), 60);
            } else {
                bgColor = AppTheme.field();
            }
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 16, 16));

            // Border
            Color borderColor;
            if (face == FateDie.Face.PLUS)       borderColor = AppTheme.success();
            else if (face == FateDie.Face.MINUS)  borderColor = AppTheme.danger();
            else                                   borderColor = AppTheme.border();
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(2.0f));
            g2.draw(new RoundRectangle2D.Float(0, 0, w, h, 16, 16));

            // Symbol
            String display;
            if (face == FateDie.Face.PLUS)       { display = "+"; }
            else if (face == FateDie.Face.MINUS)  { display = "\u2212"; } // minus sign
            else if (face == FateDie.Face.BLANK)  { display = ""; }
            else                                   { display = "?"; }

            g2.setFont(new Font("SansSerif", Font.BOLD, 36));
            Color textColor;
            if (face == FateDie.Face.PLUS)       textColor = AppTheme.success();
            else if (face == FateDie.Face.MINUS)  textColor = AppTheme.danger();
            else                                   textColor = AppTheme.muted();
            g2.setColor(textColor);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(display)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(display, tx, ty);

            g2.dispose();
        }
    }
}
