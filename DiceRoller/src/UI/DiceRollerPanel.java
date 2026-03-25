package UI;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;

import PD.*;

/**
 * Main panel for the Dice Roller application.
 * Features: preset buttons, RPG notation, manual input, modifiers,
 * advantage/disadvantage, exploding dice, roll animation,
 * per-die breakdown, copy-to-clipboard, history, session stats,
 * and dark/light theme toggle.
 */
public class DiceRollerPanel extends JPanel {

	// ─── Theme colors (mutable for theme toggle) ────────────────

	private Color bgColor;
	private Color panelColor;
	private Color fieldColor;
	private Color fgColor;
	private Color accentColor;
	private Color accentHover;
	private Color mutedColor;
	private Color successColor;
	private Color dangerColor;
	private Color borderColor;
	private Color hoverColor;
	private Color presetBg;

	private boolean isDarkTheme;

	// ─── Domain objects ─────────────────────────────────────────

	private RollHistory rollHistory;
	private DiceBag currentDiceBag;

	// ─── Input widgets ──────────────────────────────────────────

	private JTextField notationField;
	private JTextField diceField;
	private JTextField facesField;
	private JTextField modifierField;
	private JCheckBox advantageCheck;
	private JCheckBox disadvantageCheck;
	private JCheckBox explodingCheck;

	// ─── Output widgets ─────────────────────────────────────────

	private JLabel totalLabel;
	private JPanel dieValuesPanel;
	private JLabel rollModeLabel;
	private JLabel errorLabel;

	// ─── History widgets ────────────────────────────────────────

	private DefaultListModel<String> historyModel;
	private JList<String> historyList;
	private JLabel statsLabel;

	// ─── Animation ──────────────────────────────────────────────

	private Timer animationTimer;
	private int finalTotal;

	// ─── Constructors ───────────────────────────────────────────

	public DiceRollerPanel() {
		this(true, new RollHistory());
	}

	public DiceRollerPanel(boolean darkTheme, RollHistory history) {
		this.isDarkTheme = darkTheme;
		this.rollHistory = history;
		applyThemeColors();

		setLayout(new BorderLayout(0, 0));
		setBackground(bgColor);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		add(buildTopPanel(), BorderLayout.NORTH);
		add(buildCenterPanel(), BorderLayout.CENTER);
		add(buildBottomPanel(), BorderLayout.SOUTH);

		restoreHistory();
	}

	// ─── Theme ──────────────────────────────────────────────────

	private void applyThemeColors() {
		if (isDarkTheme) {
			bgColor      = new Color(30, 30, 36);
			panelColor   = new Color(40, 42, 54);
			fieldColor   = new Color(55, 58, 72);
			fgColor      = new Color(230, 230, 230);
			accentColor  = new Color(80, 200, 200);
			accentHover  = new Color(100, 220, 220);
			mutedColor   = new Color(150, 150, 160);
			successColor = new Color(80, 200, 120);
			dangerColor  = new Color(255, 85, 85);
			borderColor  = new Color(60, 63, 80);
			hoverColor   = new Color(70, 73, 90);
			presetBg     = new Color(50, 52, 68);
		} else {
			bgColor      = new Color(240, 242, 248);
			panelColor   = new Color(255, 255, 255);
			fieldColor   = new Color(232, 235, 242);
			fgColor      = new Color(30, 30, 36);
			accentColor  = new Color(0, 140, 160);
			accentHover  = new Color(0, 160, 180);
			mutedColor   = new Color(120, 120, 130);
			successColor = new Color(40, 160, 80);
			dangerColor  = new Color(220, 50, 50);
			borderColor  = new Color(200, 205, 215);
			hoverColor   = new Color(220, 222, 230);
			presetBg     = new Color(220, 232, 248);
		}
	}

	private void toggleTheme() {
		isDarkTheme = !isDarkTheme;
		JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
		if (frame == null) return;
		DiceRollerPanel fresh = new DiceRollerPanel(isDarkTheme, rollHistory);
		frame.getContentPane().removeAll();
		frame.getContentPane().add(fresh);
		frame.revalidate();
		frame.repaint();
	}

	private void restoreHistory() {
		for (int i = 0; i < rollHistory.getRollCount(); i++) {
			RollResult r = rollHistory.getHistory().get(i);
			historyModel.addElement(String.format("#%-3d %s", i + 1, r.toString()));
		}
		updateStats();
	}

	// ═══════════════════════════════════════════════════════════
	//  BUILD UI SECTIONS
	// ═══════════════════════════════════════════════════════════

	private JPanel buildTopPanel() {
		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setOpaque(false);

		// ── header row ──
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

		JLabel title = new JLabel("\uD83C\uDFB2  Dice Roller");
		title.setFont(new Font("SansSerif", Font.BOLD, 22));
		title.setForeground(accentColor);
		header.add(title, BorderLayout.WEST);

		JButton themeBtn = styledButton(isDarkTheme ? "\u2600 Light" : "\u263D Dark");
		themeBtn.addActionListener(e -> toggleTheme());
		header.add(themeBtn, BorderLayout.EAST);

		top.add(header);

		// ── presets ──
		JPanel presets = titledPanel("Quick Roll");
		presets.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		String[][] presetData = {
			{"d4","1d4"}, {"d6","1d6"}, {"d8","1d8"},
			{"d10","1d10"}, {"d12","1d12"}, {"d20","1d20"},
			{"d100","1d100"}, {"2d6","2d6"}, {"3d6","3d6"},
			{"4d6","4d6"}, {"2d20","2d20"}, {"1d6+2","1d6+2"}
		};
		for (String[] p : presetData) {
			JButton btn = presetButton(p[0]);
			final String notation = p[1];
			btn.addActionListener(e -> {
				notationField.setText(notation);
				doRoll();
			});
			presets.add(btn);
		}
		top.add(presets);
		top.add(Box.createVerticalStrut(6));

		// ── custom roll ──
		JPanel custom = titledPanel("Custom Roll");
		custom.setLayout(new BoxLayout(custom, BoxLayout.Y_AXIS));

		// notation row
		JPanel notRow = transparentFlow();
		notRow.add(label("Notation:"));
		notationField = textField(10);
		notationField.setToolTipText("e.g. 3d6+2, d20, 2d8-1");
		notationField.addKeyListener(enterKey());
		notRow.add(notationField);
		notRow.add(accentButton("\uD83C\uDFB2 Roll", e -> doRoll()));
		notRow.add(styledButton("Clear", e -> clearInputs()));
		custom.add(notRow);

		// "or" separator
		JPanel orRow = transparentFlow();
		JLabel orLabel = label("\u2014 or enter manually \u2014");
		orLabel.setForeground(mutedColor);
		orRow.add(orLabel);
		custom.add(orRow);

		// manual row
		JPanel manRow = transparentFlow();
		manRow.add(label("Dice:"));
		diceField = textField(3);  diceField.addKeyListener(enterKey());
		manRow.add(diceField);
		manRow.add(label("Faces:"));
		facesField = textField(3); facesField.addKeyListener(enterKey());
		manRow.add(facesField);
		manRow.add(label("Mod:"));
		modifierField = textField(3);
		modifierField.setText("0");
		modifierField.addKeyListener(enterKey());
		manRow.add(modifierField);
		custom.add(manRow);

		// options row
		JPanel optRow = transparentFlow();
		advantageCheck    = checkbox("Advantage",    successColor);
		disadvantageCheck = checkbox("Disadvantage", dangerColor);
		explodingCheck    = checkbox("Exploding",    accentColor);
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
		JPanel center = new JPanel();
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
		center.setOpaque(false);
		center.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

		JPanel result = titledPanel("Result");
		result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));

		// big total
		totalLabel = new JLabel("Roll the dice!");
		totalLabel.setFont(new Font("SansSerif", Font.BOLD, 40));
		totalLabel.setForeground(accentColor);
		result.add(centered(totalLabel, 8, 4));

		// roll-mode hint
		rollModeLabel = new JLabel(" ");
		rollModeLabel.setFont(new Font("SansSerif", Font.ITALIC, 13));
		rollModeLabel.setForeground(mutedColor);
		result.add(centered(rollModeLabel, 0, 0));

		// individual die faces
		dieValuesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
		dieValuesPanel.setOpaque(false);
		dieValuesPanel.setPreferredSize(new Dimension(480, 50));
		result.add(dieValuesPanel);

		// error
		errorLabel = new JLabel(" ");
		errorLabel.setForeground(dangerColor);
		errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
		result.add(centered(errorLabel, 0, 0));

		// copy button
		result.add(centered(styledButton("\uD83D\uDCCB Copy Result", e -> copyResult()), 0, 4));

		center.add(result);
		return center;
	}

	private JPanel buildBottomPanel() {
		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
		bottom.setOpaque(false);

		JPanel hist = titledPanel("History & Stats");
		hist.setLayout(new BorderLayout(0, 4));

		historyModel = new DefaultListModel<>();
		historyList = new JList<>(historyModel);
		historyList.setFont(new Font("Monospaced", Font.PLAIN, 12));
		historyList.setBackground(fieldColor);
		historyList.setForeground(fgColor);
		historyList.setSelectionBackground(hoverColor);
		historyList.setFixedCellHeight(20);

		JScrollPane sp = new JScrollPane(historyList);
		sp.setPreferredSize(new Dimension(480, 120));
		sp.setBorder(BorderFactory.createLineBorder(fieldColor));
		sp.getViewport().setBackground(fieldColor);
		hist.add(sp, BorderLayout.CENTER);

		JPanel statsRow = new JPanel(new BorderLayout());
		statsRow.setOpaque(false);
		statsLabel = new JLabel("No rolls yet");
		statsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
		statsLabel.setForeground(mutedColor);
		statsRow.add(statsLabel, BorderLayout.CENTER);
		statsRow.add(styledButton("Clear History", e -> {
			rollHistory.clear();
			historyModel.clear();
			updateStats();
		}), BorderLayout.EAST);
		hist.add(statsRow, BorderLayout.SOUTH);

		bottom.add(hist);
		return bottom;
	}

	// ═══════════════════════════════════════════════════════════
	//  ROLL LOGIC
	// ═══════════════════════════════════════════════════════════

	private void doRoll() {
		errorLabel.setText(" ");
		errorLabel.setForeground(dangerColor);

		try {
			buildDiceBag();
		} catch (Exception ex) {
			errorLabel.setText(ex.getMessage());
			return;
		}

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

		int total   = currentDiceBag.getTotal();
		int[] vals  = currentDiceBag.getDieValues();
		String nota = currentDiceBag.getNotation();
		int mod     = currentDiceBag.getModifier();

		RollResult result = new RollResult(nota, total, vals, mod, mode);
		if (advResults != null) result.setAltTotal(advResults[1]);
		rollHistory.addResult(result);

		animateResult(total, vals, mod, mode, advResults);

		int idx = rollHistory.getRollCount();
		historyModel.addElement(String.format("#%-3d %s", idx, result.toString()));
		historyList.ensureIndexIsVisible(historyModel.getSize() - 1);
		updateStats();
	}

	private void buildDiceBag() throws DiceRangeException, FaceRangeException {
		String nota = notationField.getText().trim();

		if (!nota.isEmpty()) {
			try {
				DiceNotation dn = new DiceNotation(nota);
				currentDiceBag = dn.createDiceBag();
				diceField.setText(String.valueOf(dn.getNumberOfDice()));
				facesField.setText(String.valueOf(dn.getNumberOfFaces()));
				modifierField.setText(String.valueOf(dn.getModifier()));
				return;
			} catch (IllegalArgumentException ex) {
				throw new FaceRangeException(ex.getMessage());
			}
		}

		int numDice, numFaces, mod;
		try {
			numDice = Integer.parseInt(diceField.getText().trim());
		} catch (NumberFormatException ex) {
			throw new DiceRangeException("Enter a valid number of dice");
		}
		try {
			numFaces = Integer.parseInt(facesField.getText().trim());
		} catch (NumberFormatException ex) {
			throw new FaceRangeException("Enter a valid number of faces");
		}
		try {
			mod = Integer.parseInt(modifierField.getText().trim());
		} catch (NumberFormatException ex) {
			mod = 0;
		}

		currentDiceBag = new DiceBag(numDice, numFaces, mod);
		notationField.setText(currentDiceBag.getNotation());
	}

	// ─── Animation ──────────────────────────────────────────────

	private void animateResult(int total, int[] vals, int mod,
	                           String mode, int[] advResults) {
		if (animationTimer != null && animationTimer.isRunning()) {
			animationTimer.stop();
		}
		finalTotal = total;
		int maxVal = Math.max(total, 6);
		java.util.Random rng = new java.util.Random();
		final int[] step = {0};

		animationTimer = new Timer(45, null);
		animationTimer.addActionListener(e -> {
			step[0]++;
			if (step[0] < 8) {
				totalLabel.setText(String.valueOf(rng.nextInt(maxVal) + 1));
				totalLabel.setForeground(mutedColor);
			} else {
				animationTimer.stop();
				totalLabel.setText(String.valueOf(finalTotal));
				totalLabel.setForeground(accentColor);
				showDieValues(vals, mod, mode, advResults);
			}
		});
		animationTimer.start();
	}

	private void showDieValues(int[] values, int mod,
	                           String mode, int[] advResults) {
		dieValuesPanel.removeAll();
		for (int v : values) {
			dieValuesPanel.add(dieFaceLabel(String.valueOf(v)));
		}
		if (mod != 0) {
			JLabel ml = new JLabel((mod > 0 ? "+" : "") + mod);
			ml.setFont(new Font("SansSerif", Font.BOLD, 16));
			ml.setForeground(mod > 0 ? successColor : dangerColor);
			dieValuesPanel.add(ml);
		}
		if ("Advantage".equals(mode) && advResults != null) {
			rollModeLabel.setText("Advantage \u2014 kept " + advResults[0]
				+ ", dropped " + advResults[1]);
			rollModeLabel.setForeground(successColor);
		} else if ("Disadvantage".equals(mode) && advResults != null) {
			rollModeLabel.setText("Disadvantage \u2014 kept " + advResults[0]
				+ ", dropped " + advResults[1]);
			rollModeLabel.setForeground(dangerColor);
		} else if ("Exploding".equals(mode)) {
			rollModeLabel.setText("Exploding dice!");
			rollModeLabel.setForeground(accentColor);
		} else {
			rollModeLabel.setText(" ");
		}
		dieValuesPanel.revalidate();
		dieValuesPanel.repaint();
	}

	// ─── Helpers ────────────────────────────────────────────────

	private void clearInputs() {
		notationField.setText("");
		diceField.setText("");
		facesField.setText("");
		modifierField.setText("0");
		advantageCheck.setSelected(false);
		disadvantageCheck.setSelected(false);
		explodingCheck.setSelected(false);
		totalLabel.setText("Roll the dice!");
		totalLabel.setForeground(accentColor);
		rollModeLabel.setText(" ");
		dieValuesPanel.removeAll();
		dieValuesPanel.revalidate();
		dieValuesPanel.repaint();
		errorLabel.setText(" ");
	}

	private void copyResult() {
		RollResult last = rollHistory.getLastResult();
		if (last == null) return;
		Toolkit.getDefaultToolkit().getSystemClipboard()
			.setContents(new StringSelection(last.toString()), null);
		errorLabel.setText("Copied to clipboard!");
		errorLabel.setForeground(successColor);
		Timer t = new Timer(2000, e -> {
			errorLabel.setText(" ");
			errorLabel.setForeground(dangerColor);
		});
		t.setRepeats(false);
		t.start();
	}

	private void updateStats() {
		statsLabel.setText(rollHistory.getStatsString());
	}

	// ═══════════════════════════════════════════════════════════
	//  WIDGET FACTORIES  (keep the panel code readable)
	// ═══════════════════════════════════════════════════════════

	private JPanel titledPanel(String title) {
		JPanel p = new JPanel();
		p.setBackground(panelColor);
		TitledBorder tb = BorderFactory.createTitledBorder(
			BorderFactory.createLineBorder(borderColor), title);
		tb.setTitleColor(accentColor);
		tb.setTitleFont(new Font("SansSerif", Font.BOLD, 13));
		p.setBorder(BorderFactory.createCompoundBorder(
			tb, BorderFactory.createEmptyBorder(4, 8, 8, 8)));
		return p;
	}

	private JLabel label(String text) {
		JLabel l = new JLabel(text);
		l.setForeground(fgColor);
		l.setFont(new Font("SansSerif", Font.PLAIN, 13));
		return l;
	}

	private JTextField textField(int cols) {
		JTextField f = new JTextField(cols);
		f.setBackground(fieldColor);
		f.setForeground(fgColor);
		f.setCaretColor(fgColor);
		f.setFont(new Font("SansSerif", Font.PLAIN, 14));
		f.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(borderColor),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)));
		return f;
	}

	private JCheckBox checkbox(String text, Color fg) {
		JCheckBox cb = new JCheckBox(text);
		cb.setOpaque(false);
		cb.setForeground(fg);
		cb.setFont(new Font("SansSerif", Font.PLAIN, 13));
		cb.setFocusPainted(false);
		return cb;
	}

	private JButton styledButton(String text) {
		return styledButton(text, null);
	}

	private JButton styledButton(String text, ActionListener al) {
		JButton b = new JButton(text);
		b.setFont(new Font("SansSerif", Font.PLAIN, 12));
		b.setBackground(fieldColor);
		b.setForeground(fgColor);
		b.setFocusPainted(false);
		b.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(borderColor),
			BorderFactory.createEmptyBorder(5, 12, 5, 12)));
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		final Color base = fieldColor;
		b.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) { b.setBackground(hoverColor); }
			public void mouseExited(MouseEvent e)  { b.setBackground(base); }
		});
		if (al != null) b.addActionListener(al);
		return b;
	}

	private JButton accentButton(String text, ActionListener al) {
		JButton b = new JButton(text);
		b.setFont(new Font("SansSerif", Font.BOLD, 14));
		b.setBackground(accentColor);
		b.setForeground(bgColor);
		b.setFocusPainted(false);
		b.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		b.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) { b.setBackground(accentHover); }
			public void mouseExited(MouseEvent e)  { b.setBackground(accentColor); }
		});
		if (al != null) b.addActionListener(al);
		return b;
	}

	private JButton presetButton(String text) {
		JButton b = new JButton(text);
		b.setFont(new Font("SansSerif", Font.BOLD, 12));
		b.setBackground(presetBg);
		b.setForeground(accentColor);
		b.setFocusPainted(false);
		b.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(accentColor.darker()),
			BorderFactory.createEmptyBorder(4, 10, 4, 10)));
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		b.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				b.setBackground(accentColor.darker());
				b.setForeground(fgColor);
			}
			public void mouseExited(MouseEvent e) {
				b.setBackground(presetBg);
				b.setForeground(accentColor);
			}
		});
		return b;
	}

	private JLabel dieFaceLabel(String text) {
		JLabel l = new JLabel(text);
		l.setFont(new Font("SansSerif", Font.BOLD, 16));
		l.setForeground(fgColor);
		l.setOpaque(true);
		l.setBackground(fieldColor);
		l.setHorizontalAlignment(SwingConstants.CENTER);
		l.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(accentColor, 1),
			BorderFactory.createEmptyBorder(4, 10, 4, 10)));
		return l;
	}

	private JPanel transparentFlow() {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
		p.setOpaque(false);
		return p;
	}

	/** Wrap a component in a centered FlowLayout with vertical padding. */
	private JPanel centered(JComponent comp, int padTop, int padBot) {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
		p.setOpaque(false);
		p.setBorder(BorderFactory.createEmptyBorder(padTop, 0, padBot, 0));
		p.add(comp);
		return p;
	}

	private KeyAdapter enterKey() {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) doRoll();
			}
		};
	}
}
