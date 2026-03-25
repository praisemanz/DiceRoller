package UI.panel;

import PD.RollHistory;
import PD.RollResult;
import UI.theme.AppTheme;
import UI.widgets.RoundedButton;
import service.HistoryPersistence;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.swing.*;

/**
 * Full-featured history panel with session save/load, CSV export,
 * and detailed per-roll display including verification hashes.
 */
public class HistoryPanel extends JPanel {

    private final RollHistory        rollHistory;
    private final HistoryPersistence persistence;

    private DefaultListModel<String> listModel;
    private JList<String>            histList;
    private JLabel                   statsLabel;
    private JLabel                   statusLabel;
    private JTextArea                detailArea;

    public HistoryPanel(RollHistory rollHistory, HistoryPersistence persistence) {
        this.rollHistory = rollHistory;
        this.persistence = persistence;

        setLayout(new BorderLayout(10, 10));
        setBackground(AppTheme.bg());
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);
        add(buildSouth(),   BorderLayout.SOUTH);

        refreshList();
    }

    /** Called by frame whenever this tab becomes visible to refresh data. */
    public void refresh() {
        refreshList();
    }

    // ── Build sections ────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel title = new JLabel("\uD83D\uDCDC  Roll History");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.accent());
        p.add(title, BorderLayout.WEST);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(AppTheme.FONT_SMALL);
        statusLabel.setForeground(AppTheme.success());
        p.add(statusLabel, BorderLayout.EAST);

        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setOpaque(false);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        RoundedButton saveBtn = RoundedButton.accent("\uD83D\uDCBE Save Session");
        saveBtn.addActionListener(e -> saveSession());
        toolbar.add(saveBtn);

        RoundedButton loadBtn = RoundedButton.plain("\uD83D\uDCC2 Load Session");
        loadBtn.addActionListener(e -> loadSession());
        toolbar.add(loadBtn);

        RoundedButton csvBtn = RoundedButton.plain("\uD83D\uDCCA Export CSV");
        csvBtn.addActionListener(e -> exportCsv());
        toolbar.add(csvBtn);

        RoundedButton clearBtn = RoundedButton.danger("\uD83D\uDDD1 Clear");
        clearBtn.addActionListener(e -> clearHistory());
        toolbar.add(clearBtn);
        p.add(toolbar, BorderLayout.NORTH);

        // History list (left)
        listModel = new DefaultListModel<>();
        histList  = new JList<>(listModel);
        histList.setBackground(AppTheme.field());
        histList.setForeground(AppTheme.fg());
        histList.setSelectionBackground(AppTheme.hover());
        histList.setSelectionForeground(AppTheme.accent());
        histList.setFont(AppTheme.FONT_MONO);
        histList.setFixedCellHeight(22);
        histList.addListSelectionListener(e -> showDetail());

        JScrollPane sp = new JScrollPane(histList);
        sp.setBorder(BorderFactory.createLineBorder(AppTheme.border()));
        sp.getViewport().setBackground(AppTheme.field());
        sp.setPreferredSize(new Dimension(0, 0));

        // Detail pane (right)
        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setBackground(AppTheme.panel());
        detailArea.setForeground(AppTheme.fg());
        detailArea.setFont(AppTheme.FONT_MONO);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        detailArea.setText("Select a roll to see details");
        detailArea.setForeground(AppTheme.muted());

        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setBorder(BorderFactory.createLineBorder(AppTheme.border()));
        detailScroll.getViewport().setBackground(AppTheme.panel());
        detailScroll.setPreferredSize(new Dimension(300, 0));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sp, detailScroll);
        split.setDividerLocation(480);
        split.setBackground(AppTheme.bg());
        split.setBorder(null);
        split.setDividerSize(5);
        p.add(split, BorderLayout.CENTER);

        return p;
    }

    private JPanel buildSouth() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        statsLabel = new JLabel(rollHistory.getStatsString());
        statsLabel.setFont(AppTheme.FONT_SMALL);
        statsLabel.setForeground(AppTheme.muted());
        p.add(statsLabel, BorderLayout.CENTER);

        JLabel hintLabel = new JLabel("Hashes verify rolls were not altered");
        hintLabel.setFont(AppTheme.FONT_SMALL);
        hintLabel.setForeground(AppTheme.muted());
        p.add(hintLabel, BorderLayout.EAST);

        return p;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void refreshList() {
        listModel.clear();
        List<RollResult> results = rollHistory.getHistory();
        for (int i = 0; i < results.size(); i++) {
            listModel.addElement(String.format("#%-3d  %s", i + 1, results.get(i).toString()));
        }
        statsLabel.setText(rollHistory.getStatsString());
    }

    private void showDetail() {
        int idx = histList.getSelectedIndex();
        if (idx < 0) return;
        List<RollResult> results = rollHistory.getHistory();
        if (idx >= results.size()) return;
        RollResult r = results.get(idx);
        StringBuilder sb = new StringBuilder();
        sb.append("Roll #").append(idx + 1).append("\n");
        sb.append("─".repeat(30)).append("\n");
        sb.append("Notation :  ").append(r.getNotation()).append("\n");
        sb.append("Total    :  ").append(r.getTotal()).append("\n");
        sb.append("Dice     :  ").append(r.getDieValuesString()).append("\n");
        sb.append("Mode     :  ").append(r.getRollMode()).append("\n");
        if (r.getAltTotal() >= 0) {
            sb.append("Alt Total:  ").append(r.getAltTotal()).append("\n");
        }
        sb.append("Time     :  ").append(r.getTimeString()).append("\n");
        String hash = r.getVerificationHash();
        if (hash != null && !hash.isEmpty()) {
            sb.append("\nVerification Hash\n");
            sb.append("─".repeat(30)).append("\n");
            // Print full hash in 16-char blocks for readability
            for (int i = 0; i < hash.length(); i += 16) {
                sb.append(hash, i, Math.min(i + 16, hash.length())).append("\n");
            }
        }
        detailArea.setText(sb.toString());
        detailArea.setForeground(AppTheme.fg());
        detailArea.setCaretPosition(0);
    }

    private void saveSession() {
        persistence.saveSession(rollHistory);
        flash("Session saved to ~/.diceroller/", AppTheme.success());
    }

    private void loadSession() {
        JFileChooser fc = new JFileChooser(
            System.getProperty("user.home") + "/.diceroller");
        fc.setDialogTitle("Load Session");
        int res = fc.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            Path chosen = fc.getSelectedFile().toPath();
            RollHistory loaded = persistence.loadFromFile(chosen);
            rollHistory.setHistory(loaded.getHistory());
            refreshList();
            flash("Loaded " + loaded.getRollCount() + " rolls", AppTheme.success());
        }
    }

    private void exportCsv() {
        if (rollHistory.getRollCount() == 0) {
            flash("Nothing to export", AppTheme.warning());
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("dice_rolls.csv"));
        fc.setDialogTitle("Export CSV");
        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            try {
                persistence.exportCsv(rollHistory, fc.getSelectedFile().toPath());
                flash("Exported " + rollHistory.getRollCount() + " rolls", AppTheme.success());
            } catch (IOException ex) {
                flash("Export failed: " + ex.getMessage(), AppTheme.danger());
            }
        }
    }

    private void clearHistory() {
        if (rollHistory.getRollCount() == 0) return;
        int confirm = JOptionPane.showConfirmDialog(this,
            "Clear all " + rollHistory.getRollCount() + " roll records?",
            "Clear History", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            rollHistory.clear();
            refreshList();
            detailArea.setText("Select a roll to see details");
            detailArea.setForeground(AppTheme.muted());
        }
    }

    private void flash(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
        Timer t = new Timer(3000, e -> { statusLabel.setText(" "); });
        t.setRepeats(false);
        t.start();
    }

}
