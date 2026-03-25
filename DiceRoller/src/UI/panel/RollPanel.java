package UI.panel;

import PD.*;
import UI.theme.AppTheme;
import UI.widgets.CardPanel;
import UI.widgets.RoundedButton;
import service.HashService;
import service.MacroService;
import service.SoundService;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * Primary dice-rolling panel.
 * Uses CardPanel sections, RoundedButton controls, glowing die chips,
 * and an animated result display with SHA-256 hash verification.
 */
public class RollPanel extends JPanel {

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final RollHistory    rollHistory;
    private final MacroService   macroService;
    private final SoundService   soundService;
    private final Runnable       themeToggleCallback;

    // ── Domain ───────────────────────────────────────────────────────────────
    private DiceBag currentDiceBag;

    // ── Input widgets ────────────────────────────────────────────────────────
    private JTextField notationField;
    private JTextField diceField;
    private JTextField facesField;
    private JTextField modifierField;
    private JCheckBox  advantageCheck;
    private JCheckBox  disadvantageCheck;
    private JCheckBox  explodingCheck;

    // ── Output widgets ───────────────────────────────────────────────────────
    private JLabel     totalLabel;
    private JLabel     rollModeLabel;
    private JPanel     dieValuesPanel;
    private JLabel     statusLabel;
    private JLabel     hashLabel;

    // ── History ──────────────────────────────────────────────────────────────
    private DefaultListModel<String> historyModel;
    private JList<String>            historyList;
    private JLabel                   statsLabel;

    // ── Animation ────────────────────────────────────────────────────────────
    private Timer animTimer;
    private int   finalTotal;

    // ── Constructor ───────────────────────────────────────────────────────────

    public RollPanel(RollHistory history, MacroService macroService,
                     SoundService soundService, Runnable themeToggleCallback) {
        this.rollHistory         = history;
        this.macroService        = macroService;
        this.soundService        = soundService;
        this.themeToggleCallback = themeToggleCallback;

        setLayout(new BorderLayout(0, 8));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 14, 10, 14));

        add(buildTopPanel(),    BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        restoreHistory();
    }

    // ── External API ──────────────────────────────────────────────────────────

    public void rollWithMacro(String notation, String mode) {
        notationField.setText(notation);
        advantageCheck.setSelected("Advantage".equals(mode));
        disadvantageCheck.setSelected("Disadvantage".equals(mode));
        explodingCheck.setSelected("Exploding".equals(mode));
        doRoll();
    }

    public String getCurrentNotation() {
        return notationField.getText().trim();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  BUILD SECTIONS
    // ═══════════════════════════════════════════════════════════════════════

    private JPanel buildTopPanel() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        // ── Quick Roll card ──────────────────────────────────────────────────
        CardPanel presets = CardPanel.titled("Quick Roll", AppTheme.accent());
        presets.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 6));

        String[][] presetData = {
            {"d4","1d4"},  {"d6","1d6"},   {"d8","1d8"},   {"d10","1d10"},
            {"d12","1d12"},{"d20","1d20"}, {"d%","1d100"}, {"2d6","2d6"},
            {"3d6","3d6"}, {"4d6","4d6"},  {"2d20","2d20"},{"1d6+2","1d6+2"}
        };
        for (String[] p : presetData) {
            JButton btn = presetPill(p[0]);
            final String nota = p[1];
            btn.addActionListener(e -> { notationField.setText(nota); doRoll(); });
            presets.add(btn);
        }
        top.add(presets);
        top.add(Box.createVerticalStrut(8));

        // ── Custom Roll card ─────────────────────────────────────────────────
        CardPanel custom = CardPanel.titled("Custom Roll", AppTheme.accent());
        custom.setLayout(new BoxLayout(custom, BoxLayout.Y_AXIS));

        // Notation row
        JPanel notRow = row();
        notRow.add(bodyLabel("Notation:"));
        notationField = themedField(14);
        notationField.setToolTipText("e.g. 3d6+2, d20, 2d8-1");
        notationField.addKeyListener(enterKey());
        notRow.add(notationField);
        notRow.add(Box.createHorizontalStrut(6));
        RoundedButton rollBtn = RoundedButton.accent("\uD83C\uDFB2 Roll").large();
        rollBtn.addActionListener(e -> doRoll());
        notRow.add(rollBtn);
        RoundedButton clearBtn = RoundedButton.plain("Clear").small();
        clearBtn.addActionListener(e -> clearInputs());
        notRow.add(clearBtn);
        custom.add(notRow);

        // Divider
        JPanel orRow = row();
        JLabel orLabel = bodyLabel("\u2014 or enter manually \u2014");
        orLabel.setForeground(AppTheme.muted());
        orRow.add(orLabel);
        custom.add(orRow);

        // Manual row
        JPanel manRow = row();
        manRow.add(bodyLabel("Dice:"));
        diceField = themedField(3); diceField.addKeyListener(enterKey());
        manRow.add(diceField);
        manRow.add(bodyLabel("Faces:"));
        facesField = themedField(3); facesField.addKeyListener(enterKey());
        manRow.add(facesField);
        manRow.add(bodyLabel("Mod:"));
        modifierField = themedField(3); modifierField.setText("0"); modifierField.addKeyListener(enterKey());
        manRow.add(modifierField);
        custom.add(manRow);

        // Options row (checkboxes)
        JPanel optRow = row();
        advantageCheck    = themedCheckbox("Advantage",    AppTheme.success());
        disadvantageCheck = themedCheckbox("Disadvantage", AppTheme.danger());
        explodingCheck    = themedCheckbox("Exploding",    AppTheme.warning());
        advantageCheck.addActionListener(e -> {
            if (advantageCheck.isSelected()) disadvantageCheck.setSelected(false);
        });
        disadvantageCheck.addActionListener(e -> {
            if (disadvantageCheck.isSelected()) advantageCheck.setSelected(false);
        });
        optRow.add(advantageCheck);
        optRow.add(disadvantageCheck);
        optRow.add(explodingCheck);
        custom.add(optRow);

        top.add(custom);
        return top;
    }

    private JPanel buildCenterPanel() {
        CardPanel result = CardPanel.titled("Result", AppTheme.accent());
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));

        // Big animated total with glow background
        JPanel totalWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER)) {
            @Override protected void paintComponent(Graphics g) {
                // Radial glow behind the number
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                int r = Math.min(cx, cy) + 20;
                for (int i = r; i > 0; i -= 8) {
                    float alpha = (1f - (float) i / r) * 0.06f;
                    g2.setColor(AppTheme.withAlpha(AppTheme.accent(), (int)(alpha * 255)));
                    g2.fillOval(cx - i, cy - i, i * 2, i * 2);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        totalWrapper.setOpaque(false);
        totalWrapper.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));

        totalLabel = new JLabel("Roll the dice!");
        totalLabel.setFont(AppTheme.FONT_RESULT);
        totalLabel.setForeground(AppTheme.accent());
        totalWrapper.add(totalLabel);
        result.add(totalWrapper);

        // Roll mode hint
        rollModeLabel = new JLabel(" ");
        rollModeLabel.setFont(AppTheme.fontItalic(13));
        rollModeLabel.setForeground(AppTheme.muted());
        result.add(centeredComp(rollModeLabel, 0, 2));

        // Die chips panel
        dieValuesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 7, 4));
        dieValuesPanel.setOpaque(false);
        dieValuesPanel.setPreferredSize(new Dimension(500, 58));
        result.add(dieValuesPanel);

        // Hash + status row
        JPanel hashRow = row();
        hashLabel = new JLabel(" ");
        hashLabel.setFont(AppTheme.fontMono(11));
        hashLabel.setForeground(AppTheme.withAlpha(AppTheme.muted(), 160));
        hashRow.add(hashLabel);
        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.font(12));
        statusLabel.setForeground(AppTheme.danger());
        hashRow.add(statusLabel);
        result.add(hashRow);

        // Action buttons
        JPanel actRow = row();
        RoundedButton copyBtn  = RoundedButton.ghost("\uD83D\uDCCB  Copy Result").small();
        RoundedButton macroBtn = RoundedButton.ghost("\u2B50  Save Macro").small();
        copyBtn.addActionListener(e -> copyResult());
        macroBtn.addActionListener(e -> saveMacroDialog());
        actRow.add(copyBtn);
        actRow.add(macroBtn);
        result.add(actRow);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        wrap.add(result, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildBottomPanel() {
        CardPanel hist = CardPanel.titled("Session History", AppTheme.muted());
        hist.setLayout(new BorderLayout(0, 6));

        historyModel = new DefaultListModel<>();
        historyList  = new JList<>(historyModel);
        historyList.setFont(AppTheme.fontMono(12));
        historyList.setBackground(AppTheme.field());
        historyList.setForeground(AppTheme.fg());
        historyList.setSelectionBackground(AppTheme.hover());
        historyList.setSelectionForeground(AppTheme.accent());
        historyList.setFixedCellHeight(22);

        JScrollPane sp = new JScrollPane(historyList);
        sp.setPreferredSize(new Dimension(0, 110));
        sp.setBorder(BorderFactory.createLineBorder(AppTheme.border()));
        sp.getViewport().setBackground(AppTheme.field());
        hist.add(sp, BorderLayout.CENTER);

        JPanel statsRow = new JPanel(new BorderLayout(8, 0));
        statsRow.setOpaque(false);
        statsLabel = new JLabel("No rolls yet");
        statsLabel.setFont(AppTheme.font(11));
        statsLabel.setForeground(AppTheme.muted());
        statsRow.add(statsLabel, BorderLayout.CENTER);
        RoundedButton clearBtn = RoundedButton.plain("Clear").small();
        clearBtn.addActionListener(e -> { rollHistory.clear(); historyModel.clear(); updateStats(); });
        statsRow.add(clearBtn, BorderLayout.EAST);
        hist.add(statsRow, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(hist, BorderLayout.CENTER);
        return wrap;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ROLL LOGIC
    // ═══════════════════════════════════════════════════════════════════════

    private void doRoll() {
        statusLabel.setText(" ");
        statusLabel.setForeground(AppTheme.danger());

        try { buildDiceBag(); }
        catch (Exception ex) { statusLabel.setText(ex.getMessage()); return; }

        String mode = "Normal";
        int[] advResults = null;

        if (advantageCheck.isSelected()) {
            mode = "Advantage";
            advResults = currentDiceBag.rollWithAdvantage();
        } else if (disadvantageCheck.isSelected()) {
            mode = "Disadvantage";
            advResults = currentDiceBag.rollWithDisadvantage();
        } else if (explodingCheck.isSelected()) {
            mode = "Exploding";
            currentDiceBag.rollDiceExploding();
        } else {
            currentDiceBag.rollDice();
        }

        int    total       = currentDiceBag.getTotal();
        int[]  vals        = currentDiceBag.getDieValues();
        String nota        = currentDiceBag.getNotation();
        int    mod         = currentDiceBag.getModifier();
        int    maxPossible = currentDiceBag.getNumberOfDice() * currentDiceBag.getNumberOfFaces() + mod;
        int    minPossible = currentDiceBag.getNumberOfDice() + mod;

        RollResult result = new RollResult(nota, total, vals, mod, mode);
        if (advResults != null) result.setAltTotal(advResults[1]);
        result.setVerificationHash(HashService.shortHash(result));
        rollHistory.addResult(result);

        // Sound
        if (total == maxPossible)      soundService.play(SoundService.Sound.CRITICAL);
        else if (total == minPossible) soundService.play(SoundService.Sound.FUMBLE);
        else                           soundService.play(SoundService.Sound.ROLL);

        hashLabel.setText("SHA: #" + result.getVerificationHash());
        animateResult(total, vals, mod, mode, advResults);

        int idx = rollHistory.getRollCount();
        historyModel.addElement(String.format("#%-3d %s", idx, result));
        historyList.ensureIndexIsVisible(historyModel.getSize() - 1);
        updateStats();
    }

    private void buildDiceBag() throws DiceRangeException, FaceRangeException {
        String nota = notationField.getText().trim();
        if (!nota.isEmpty()) {
            try {
                DiceNotation dn = new DiceNotation(nota);
                currentDiceBag  = dn.createDiceBag();
                diceField.setText(String.valueOf(dn.getNumberOfDice()));
                facesField.setText(String.valueOf(dn.getNumberOfFaces()));
                modifierField.setText(String.valueOf(dn.getModifier()));
                return;
            } catch (IllegalArgumentException ex) {
                throw new FaceRangeException(ex.getMessage());
            }
        }
        int numDice, numFaces, mod;
        try      { numDice  = Integer.parseInt(diceField.getText().trim()); }
        catch (NumberFormatException ex) { throw new DiceRangeException("Enter a valid number of dice"); }
        try      { numFaces = Integer.parseInt(facesField.getText().trim()); }
        catch (NumberFormatException ex) { throw new FaceRangeException("Enter a valid number of faces"); }
        try      { mod = Integer.parseInt(modifierField.getText().trim()); }
        catch (NumberFormatException ex) { mod = 0; }
        currentDiceBag = new DiceBag(numDice, numFaces, mod);
        notationField.setText(currentDiceBag.getNotation());
    }

    // ── Animation ─────────────────────────────────────────────────────────────

    private void animateResult(int total, int[] vals, int mod,
                               String mode, int[] advResults) {
        if (animTimer != null && animTimer.isRunning()) animTimer.stop();
        finalTotal = total;
        int maxVal = Math.max(total, 6);
        java.util.Random rng = new java.util.Random();
        final int[] step = {0};

        animTimer = new Timer(38, null);
        animTimer.addActionListener(e -> {
            step[0]++;
            if (step[0] < 10) {
                int fontSize = 40 + (step[0] % 3) * 6; // slight size bounce
                totalLabel.setFont(AppTheme.fontBold(fontSize));
                totalLabel.setText(String.valueOf(rng.nextInt(maxVal) + 1));
                totalLabel.setForeground(AppTheme.muted());
            } else {
                animTimer.stop();
                totalLabel.setFont(AppTheme.FONT_RESULT);
                totalLabel.setText(String.valueOf(finalTotal));
                totalLabel.setForeground(AppTheme.accent());
                totalLabel.getParent().repaint(); // repaint glow
                showDieValues(vals, mod, mode, advResults);
            }
        });
        animTimer.start();
    }

    private void showDieValues(int[] values, int mod, String mode, int[] advResults) {
        dieValuesPanel.removeAll();
        for (int v : values) {
            dieValuesPanel.add(dieChip(String.valueOf(v)));
        }
        if (mod != 0) {
            JLabel ml = new JLabel((mod > 0 ? "+" : "") + mod);
            ml.setFont(AppTheme.fontBold(16));
            ml.setForeground(mod > 0 ? AppTheme.success() : AppTheme.danger());
            dieValuesPanel.add(ml);
        }
        if ("Advantage".equals(mode) && advResults != null) {
            rollModeLabel.setText("Advantage \u2014 kept " + advResults[0] + ", dropped " + advResults[1]);
            rollModeLabel.setForeground(AppTheme.success());
        } else if ("Disadvantage".equals(mode) && advResults != null) {
            rollModeLabel.setText("Disadvantage \u2014 kept " + advResults[0] + ", dropped " + advResults[1]);
            rollModeLabel.setForeground(AppTheme.danger());
        } else if ("Exploding".equals(mode)) {
            rollModeLabel.setText("Exploding dice! \uD83D\uDCA5");
            rollModeLabel.setForeground(AppTheme.warning());
        } else {
            rollModeLabel.setText(" ");
        }
        dieValuesPanel.revalidate();
        dieValuesPanel.repaint();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearInputs() {
        notationField.setText(""); diceField.setText(""); facesField.setText("");
        modifierField.setText("0");
        advantageCheck.setSelected(false);
        disadvantageCheck.setSelected(false);
        explodingCheck.setSelected(false);
        totalLabel.setText("Roll the dice!");
        totalLabel.setFont(AppTheme.FONT_RESULT);
        totalLabel.setForeground(AppTheme.accent());
        rollModeLabel.setText(" ");
        hashLabel.setText(" ");
        dieValuesPanel.removeAll();
        dieValuesPanel.revalidate();
        dieValuesPanel.repaint();
        statusLabel.setText(" ");
    }

    private void copyResult() {
        RollResult last = rollHistory.getLastResult();
        if (last == null) return;
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new StringSelection(last.toString()), null);
        flash(statusLabel, "Copied!", AppTheme.success());
    }

    private void saveMacroDialog() {
        RollResult last = rollHistory.getLastResult();
        String defaultNota = last != null ? last.getNotation() : notationField.getText().trim();
        if (defaultNota.isEmpty()) { flash(statusLabel, "Roll something first!", AppTheme.danger()); return; }

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 8));
        form.setBackground(AppTheme.panel());
        JTextField nameField = themedField(16);
        JTextField notaField = themedField(16); notaField.setText(defaultNota);
        JTextField descField = themedField(16);
        String[] modes = {"Normal", "Advantage", "Disadvantage", "Exploding"};
        JComboBox<String> modeBox = new JComboBox<>(modes);

        form.add(bodyLabel("Name:"));        form.add(nameField);
        form.add(bodyLabel("Notation:"));    form.add(notaField);
        form.add(bodyLabel("Mode:"));        form.add(modeBox);
        form.add(bodyLabel("Description:")); form.add(descField);

        int res = JOptionPane.showConfirmDialog(this, form, "Save as Macro",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = notaField.getText().trim();
            macroService.addMacro(new PD.RollMacro(name, notaField.getText().trim(),
                (String) modeBox.getSelectedItem(), descField.getText().trim()));
            soundService.play(SoundService.Sound.SAVE);
            flash(statusLabel, "Macro saved!", AppTheme.success());
        }
    }

    private void updateStats()   { statsLabel.setText(rollHistory.getStatsString()); }

    private void restoreHistory() {
        for (int i = 0; i < rollHistory.getRollCount(); i++) {
            RollResult r = rollHistory.getHistory().get(i);
            historyModel.addElement(String.format("#%-3d %s", i + 1, r));
        }
        updateStats();
    }

    private void flash(JLabel label, String text, Color color) {
        label.setText(text); label.setForeground(color);
        Timer t = new Timer(2200, e -> { label.setText(" "); label.setForeground(AppTheme.danger()); });
        t.setRepeats(false); t.start();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  WIDGET FACTORIES
    // ═══════════════════════════════════════════════════════════════════════

    /** A beautiful custom-painted die face chip with glow border and shadow. */
    private JPanel dieChip(final String text) {
        return new JPanel() {
            private boolean glowing = true;
            private float   glowAlpha = 1.0f;
            private Timer   glowTimer;
            {
                setPreferredSize(new Dimension(50, 46));
                setOpaque(false);
                // Animate the glow fading out after appearance
                glowTimer = new Timer(40, null);
                glowTimer.addActionListener(e -> {
                    glowAlpha -= 0.06f;
                    if (glowAlpha <= 0) { glowAlpha = 0; glowing = false; glowTimer.stop(); }
                    repaint();
                });
                glowTimer.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth() - 3, h = getHeight() - 3;

                // Glow rings (fade out after roll)
                if (glowing && glowAlpha > 0) {
                    for (int r = 6; r > 0; r -= 2) {
                        g2.setColor(AppTheme.withAlpha(AppTheme.accent(),
                            (int)(glowAlpha * 50 * (6 - r + 1) / 6)));
                        g2.setStroke(new BasicStroke(r));
                        g2.draw(new RoundRectangle2D.Float(-r/2f, -r/2f, w + r, h + r, 14+r, 14+r));
                    }
                }

                // Shadow
                g2.setColor(AppTheme.withAlpha(Color.BLACK, 65));
                g2.fill(new RoundRectangle2D.Float(2, 2, w, h, 12, 12));

                // Card gradient background
                GradientPaint bg = new GradientPaint(0, 0, AppTheme.lighten(AppTheme.field(), 8),
                    0, h, AppTheme.field());
                g2.setPaint(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 12, 12));

                // Accent border
                g2.setColor(glowing
                    ? AppTheme.withAlpha(AppTheme.accent(), Math.max(90, (int)(glowAlpha * 255)))
                    : AppTheme.withAlpha(AppTheme.accent(), 120));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.75f, 0.75f, w - 1.5f, h - 1.5f, 11, 11));

                // Number
                g2.setFont(AppTheme.fontBold(16));
                g2.setColor(AppTheme.fg());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, tx, ty);

                g2.dispose();
            }
        };
    }

    /** Pill-shaped quick-roll preset button. */
    private JButton presetPill(String text) {
        JButton b = new JButton(text) {
            boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                Color bg = hov ? AppTheme.accent() : AppTheme.presetBg();
                Color fg = hov ? AppTheme.bg()     : AppTheme.accent();
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
                if (!hov) {
                    g2.setColor(AppTheme.withAlpha(AppTheme.accent(), 80));
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h));
                }
                g2.setFont(AppTheme.fontBold(12));
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2,
                    (h - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.height = 32; d.width = Math.max(d.width + 4, 50);
                return d;
            }
        };
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JLabel bodyLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(AppTheme.fg());
        l.setFont(AppTheme.FONT_BODY);
        return l;
    }

    private JTextField themedField(int cols) {
        JTextField f = new JTextField(cols) {
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color borderColor = hasFocus() ? AppTheme.accent() : AppTheme.border();
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(hasFocus() ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 7, 7);
                g2.dispose();
            }
        };
        f.setBackground(AppTheme.field());
        f.setForeground(AppTheme.fg());
        f.setCaretColor(AppTheme.accent());
        f.setFont(AppTheme.font(14));
        f.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { f.repaint(); }
            public void focusLost(FocusEvent e)   { f.repaint(); }
        });
        return f;
    }

    private JCheckBox themedCheckbox(String text, Color fg) {
        JCheckBox cb = new JCheckBox(text);
        cb.setOpaque(false);
        cb.setForeground(fg);
        cb.setFont(AppTheme.FONT_BODY);
        cb.setFocusPainted(false);
        return cb;
    }

    private JPanel row() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        p.setOpaque(false);
        return p;
    }

    private JPanel centeredComp(JComponent comp, int padTop, int padBot) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(padTop, 0, padBot, 0));
        p.add(comp);
        return p;
    }

    private KeyAdapter enterKey() {
        return new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doRoll();
            }
        };
    }
}
