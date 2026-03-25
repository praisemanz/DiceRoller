package UI.panel;

import PD.RollMacro;
import UI.theme.AppTheme;
import UI.widgets.CardPanel;
import UI.widgets.RoundedButton;
import service.MacroService;
import service.SoundService;

import java.awt.*;
import java.util.List;
import javax.swing.*;

/**
 * Manages saved roll macros: create, edit, delete, and execute.
 * "Roll Selected" switches to the Roll tab and fires the macro there.
 */
public class MacroPanel extends JPanel {

    private final MacroService  macroService;
    private final RollPanel     rollPanel;
    private final Runnable      switchToRollCallback;
    private final SoundService  soundService;

    // ── Macro list ────────────────────────────────────────────────────────────
    private DefaultListModel<String> listModel;
    private JList<String>            macroList;

    // ── Form fields ───────────────────────────────────────────────────────────
    private JTextField  nameField;
    private JTextField  notationField;
    private JTextArea   descField;
    private JComboBox<String> modeBox;

    // ── Buttons ───────────────────────────────────────────────────────────────
    private RoundedButton saveBtn;
    private RoundedButton deleteBtn;
    private RoundedButton rollBtn;

    private int editingIndex = -1; // -1 = new macro, >= 0 = editing existing

    public MacroPanel(MacroService macroService, RollPanel rollPanel,
                      Runnable switchToRollCallback, SoundService soundService) {
        this.macroService         = macroService;
        this.rollPanel            = rollPanel;
        this.switchToRollCallback = switchToRollCallback;
        this.soundService         = soundService;

        setLayout(new BorderLayout(10, 10));
        setBackground(AppTheme.bg());
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildListPanel(), BorderLayout.WEST);
        add(buildFormPanel(), BorderLayout.CENTER);

        refreshList();
        updateButtonStates();
    }

    // ── Build sections ────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JLabel title = new JLabel("\u2B50  Roll Macros");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.accent2());
        p.add(title, BorderLayout.WEST);
        JLabel hint = new JLabel("Save & reuse your favourite rolls instantly");
        hint.setFont(AppTheme.FONT_SMALL);
        hint.setForeground(AppTheme.muted());
        p.add(hint, BorderLayout.EAST);
        return p;
    }

    private JPanel buildListPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(240, 0));

        JLabel title = new JLabel("Saved Macros (" + macroService.getCount() + ")");
        title.setFont(AppTheme.FONT_BOLD);
        title.setForeground(AppTheme.fg());
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        p.add(title, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        macroList = new JList<>(listModel);
        macroList.setBackground(AppTheme.field());
        macroList.setForeground(AppTheme.fg());
        macroList.setSelectionBackground(AppTheme.hover());
        macroList.setSelectionForeground(AppTheme.accent());
        macroList.setFont(AppTheme.FONT_BODY);
        macroList.setFixedCellHeight(30);
        macroList.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        macroList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedMacro();
        });

        JScrollPane sp = new JScrollPane(macroList);
        sp.setBorder(BorderFactory.createLineBorder(AppTheme.border()));
        sp.getViewport().setBackground(AppTheme.field());
        p.add(sp, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        btnRow.setOpaque(false);

        rollBtn   = RoundedButton.accent("\uD83C\uDFB2 Roll");
        deleteBtn = RoundedButton.danger("\uD83D\uDDD1 Delete");
        RoundedButton newBtn = RoundedButton.plain("+ New");

        rollBtn.addActionListener(e -> rollSelected());
        deleteBtn.addActionListener(e -> deleteSelected());
        newBtn.addActionListener(e -> clearForm());

        btnRow.add(rollBtn);
        btnRow.add(deleteBtn);
        btnRow.add(newBtn);
        p.add(btnRow, BorderLayout.SOUTH);

        return p;
    }

    private JPanel buildFormPanel() {
        CardPanel p = CardPanel.titled("Macro Details", AppTheme.accent2());
        p.setLayout(new GridBagLayout());

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.EAST;
        lc.insets = new Insets(5, 8, 5, 6);
        lc.gridx = 0;

        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(5, 0, 5, 8);
        fc.gridx = 1;

        // Name
        lc.gridy = 0; fc.gridy = 0;
        p.add(label("Name:"), lc);
        nameField = textField();
        p.add(nameField, fc);

        // Notation
        lc.gridy = 1; fc.gridy = 1;
        p.add(label("Notation:"), lc);
        notationField = textField();
        notationField.setToolTipText("e.g. 2d6+3, 1d20, 4d6");
        p.add(notationField, fc);

        // Mode
        lc.gridy = 2; fc.gridy = 2;
        p.add(label("Roll Mode:"), lc);
        modeBox = new JComboBox<>(new String[]{"Normal", "Advantage", "Disadvantage", "Exploding"});
        modeBox.setBackground(AppTheme.field());
        modeBox.setForeground(AppTheme.fg());
        modeBox.setFont(AppTheme.FONT_BODY);
        p.add(modeBox, fc);

        // Description
        lc.gridy = 3; lc.anchor = GridBagConstraints.NORTHEAST;
        fc.gridy = 3;
        p.add(label("Notes:"), lc);
        descField = new JTextArea(3, 20);
        descField.setBackground(AppTheme.field());
        descField.setForeground(AppTheme.fg());
        descField.setCaretColor(AppTheme.fg());
        descField.setFont(AppTheme.FONT_BODY);
        descField.setLineWrap(true);
        descField.setWrapStyleWord(true);
        descField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.border()),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        JScrollPane descScroll = new JScrollPane(descField);
        descScroll.setBorder(null);
        p.add(descScroll, fc);

        // Save button
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridy = 4;
        bc.gridx = 0;
        bc.gridwidth = 2;
        bc.anchor = GridBagConstraints.CENTER;
        bc.insets = new Insets(12, 0, 4, 0);
        saveBtn = RoundedButton.accent("\u2714 Save Macro");
        saveBtn.addActionListener(e -> saveMacro());
        p.add(saveBtn, bc);

        return p;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void refreshList() {
        listModel.clear();
        List<RollMacro> macros = macroService.getMacros();
        for (RollMacro m : macros) {
            listModel.addElement(m.getName() + "  [" + m.getNotation() + "]");
        }
        // Header count is shown in list model size; list refreshes automatically
    }

    private void loadSelectedMacro() {
        int idx = macroList.getSelectedIndex();
        if (idx < 0) { updateButtonStates(); return; }
        editingIndex = idx;
        RollMacro m = macroService.getMacros().get(idx);
        nameField.setText(m.getName());
        notationField.setText(m.getNotation());
        modeBox.setSelectedItem(m.getRollMode());
        descField.setText(m.getDescription());
        updateButtonStates();
    }

    private void saveMacro() {
        String name = nameField.getText().trim();
        String nota = notationField.getText().trim();
        if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Please enter a name.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
        if (nota.isEmpty()) { JOptionPane.showMessageDialog(this, "Please enter a notation (e.g. 1d20+5).", "Validation", JOptionPane.WARNING_MESSAGE); return; }

        RollMacro m = new RollMacro(
            name, nota,
            (String) modeBox.getSelectedItem(),
            descField.getText().trim()
        );

        if (editingIndex >= 0) {
            macroService.updateMacro(editingIndex, m);
        } else {
            macroService.addMacro(m);
            editingIndex = macroService.getCount() - 1;
        }
        soundService.play(SoundService.Sound.SAVE);
        refreshList();
        macroList.setSelectedIndex(editingIndex);
    }

    private void deleteSelected() {
        int idx = macroList.getSelectedIndex();
        if (idx < 0) return;
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete macro \"" + macroService.getMacros().get(idx).getName() + "\"?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            macroService.removeMacro(idx);
            clearForm();
            refreshList();
        }
    }

    private void rollSelected() {
        int idx = macroList.getSelectedIndex();
        if (idx < 0) return;
        RollMacro m = macroService.getMacros().get(idx);
        // Switch to Roll tab and execute
        if (switchToRollCallback != null) switchToRollCallback.run();
        rollPanel.rollWithMacro(m.getNotation(), m.getRollMode());
        soundService.play(SoundService.Sound.MACRO);
    }

    private void clearForm() {
        editingIndex = -1;
        nameField.setText("");
        notationField.setText("");
        modeBox.setSelectedIndex(0);
        descField.setText("");
        macroList.clearSelection();
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean selected = macroList.getSelectedIndex() >= 0;
        deleteBtn.setEnabled(selected);
        rollBtn.setEnabled(selected);
    }

    // ── Widget factories ──────────────────────────────────────────────────────

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(AppTheme.fg());
        l.setFont(AppTheme.FONT_BODY);
        return l;
    }

    private JTextField textField() {
        JTextField f = new JTextField();
        f.setBackground(AppTheme.field());
        f.setForeground(AppTheme.fg());
        f.setCaretColor(AppTheme.fg());
        f.setFont(AppTheme.FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.border()),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        return f;
    }

}
