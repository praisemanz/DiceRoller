package UI.panel;

import PD.*;
import UI.theme.AppTheme;
import UI.widgets.CardPanel;
import UI.widgets.RoundedButton;
import service.HashService;
import service.SoundService;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

/**
 * Rolls multiple dice formulas at once — ideal for "attack + damage" style combat rounds.
 * Each line in the input is one formula, optionally prefixed with "Label: notation".
 */
public class MultiRollPanel extends JPanel {

    private final RollHistory rollHistory;
    private final SoundService soundService;

    private JTextArea    inputArea;
    private JLabel       grandTotalLabel;
    private JLabel       statusLabel;
    private DefaultTableModel tableModel;
    private JTable       resultsTable;

    private static final String EXAMPLE =
        "Attack:  1d20+5\nDamage:  2d6+3\nBonus:   1d4";

    public MultiRollPanel(RollHistory rollHistory, SoundService soundService) {
        this.rollHistory  = rollHistory;
        this.soundService = soundService;

        setLayout(new BorderLayout(10, 10));
        setBackground(AppTheme.bg());
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);
        add(buildSouth(),   BorderLayout.SOUTH);
    }

    // ── Build sections ────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JLabel title = new JLabel("\uD83C\uDFB2  Multi-Roll");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.warning());
        p.add(title, BorderLayout.WEST);
        JLabel sub = new JLabel("Roll multiple formulas in one click");
        sub.setFont(AppTheme.FONT_SMALL);
        sub.setForeground(AppTheme.muted());
        p.add(sub, BorderLayout.EAST);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);

        // ── Left: input ──
        CardPanel left = CardPanel.titled("Formulas  (one per line, Label: notation)", AppTheme.warning());
        left.setLayout(new BorderLayout(0, 6));
        left.setPreferredSize(new Dimension(280, 0));

        inputArea = new JTextArea(EXAMPLE);
        inputArea.setBackground(AppTheme.field());
        inputArea.setForeground(AppTheme.fg());
        inputArea.setCaretColor(AppTheme.fg());
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        inputArea.setLineWrap(false);
        inputArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        inputArea.setTabSize(4);

        JScrollPane sp = new JScrollPane(inputArea);
        sp.setBorder(BorderFactory.createLineBorder(AppTheme.border()));
        sp.getViewport().setBackground(AppTheme.field());
        left.add(sp, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btnRow.setOpaque(false);
        RoundedButton rollAllBtn = RoundedButton.warning("\uD83C\uDFB2 Roll All");
        rollAllBtn.addActionListener(e -> rollAll());
        btnRow.add(rollAllBtn);

        RoundedButton clearBtn = RoundedButton.plain("Clear Results");
        clearBtn.addActionListener(e -> clearResults());
        btnRow.add(clearBtn);

        RoundedButton exampleBtn = RoundedButton.plain("Load Example");
        exampleBtn.addActionListener(e -> inputArea.setText(EXAMPLE));
        btnRow.add(exampleBtn);
        left.add(btnRow, BorderLayout.SOUTH);

        p.add(left, BorderLayout.WEST);

        // ── Right: results table ──
        CardPanel right = CardPanel.titled("Results", AppTheme.warning());
        right.setLayout(new BorderLayout(0, 4));

        String[] cols = {"Label", "Notation", "Dice", "Total"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        resultsTable = new JTable(tableModel);
        resultsTable.setBackground(AppTheme.field());
        resultsTable.setForeground(AppTheme.fg());
        resultsTable.setSelectionBackground(AppTheme.hover());
        resultsTable.setSelectionForeground(AppTheme.accent());
        resultsTable.setFont(AppTheme.FONT_BODY);
        resultsTable.setRowHeight(26);
        resultsTable.setShowGrid(false);
        resultsTable.setIntercellSpacing(new Dimension(0, 1));
        resultsTable.getTableHeader().setBackground(AppTheme.panel());
        resultsTable.getTableHeader().setForeground(AppTheme.accent());
        resultsTable.getTableHeader().setFont(AppTheme.FONT_BOLD);
        resultsTable.getTableHeader().setBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.border()));

        // Column widths
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        resultsTable.getColumnModel().getColumn(3).setPreferredWidth(60);

        // Center-align the Total column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        resultsTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        JScrollPane tableSp = new JScrollPane(resultsTable);
        tableSp.setBorder(BorderFactory.createLineBorder(AppTheme.border()));
        tableSp.getViewport().setBackground(AppTheme.field());
        right.add(tableSp, BorderLayout.CENTER);

        // Grand total row
        JPanel grandRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        grandRow.setOpaque(false);
        grandTotalLabel = new JLabel("Grand Total: —");
        grandTotalLabel.setFont(AppTheme.FONT_LARGE);
        grandTotalLabel.setForeground(AppTheme.warning());
        grandRow.add(grandTotalLabel);
        right.add(grandRow, BorderLayout.SOUTH);

        p.add(right, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSouth() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.FONT_SMALL);
        statusLabel.setForeground(AppTheme.muted());
        p.add(statusLabel, BorderLayout.CENTER);

        JLabel hint = new JLabel("All rolls also appear in History tab");
        hint.setFont(AppTheme.FONT_SMALL);
        hint.setForeground(AppTheme.muted());
        p.add(hint, BorderLayout.EAST);
        return p;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void rollAll() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) {
            flash("Enter at least one formula", AppTheme.danger());
            return;
        }

        RollGroup group;
        try {
            group = RollGroup.fromText("Session", text);
        } catch (Exception ex) {
            flash("Parse error: " + ex.getMessage(), AppTheme.danger());
            return;
        }

        if (group.getLineCount() == 0) {
            flash("No valid formulas found", AppTheme.danger());
            return;
        }

        try {
            group.rollAll();
        } catch (Exception ex) {
            flash("Roll error: " + ex.getMessage(), AppTheme.danger());
            return;
        }

        // Populate table
        tableModel.setRowCount(0);
        List<RollGroup.GroupLine> lines = group.getLines();
        for (RollGroup.GroupLine gl : lines) {
            RollResult r = gl.result;
            r.setVerificationHash(HashService.shortHash(r));
            rollHistory.addResult(r);

            Object[] row = {
                gl.label,
                r.getNotation(),
                r.getDieValuesString(),
                String.valueOf(r.getTotal())
            };
            tableModel.addRow(row);
        }

        grandTotalLabel.setText("Grand Total: " + group.getGrandTotal());
        soundService.play(SoundService.Sound.ROLL);
        flash("Rolled " + lines.size() + " formula(s)  — grand total: " + group.getGrandTotal(), AppTheme.success());
    }

    private void clearResults() {
        tableModel.setRowCount(0);
        grandTotalLabel.setText("Grand Total: —");
        statusLabel.setText(" ");
    }

    private void flash(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
        Timer t = new Timer(3500, e -> statusLabel.setText(" "));
        t.setRepeats(false);
        t.start();
    }

}
