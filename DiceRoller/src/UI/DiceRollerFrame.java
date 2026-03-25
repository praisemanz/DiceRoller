package UI;

import PD.RollHistory;
import UI.panel.*;
import UI.theme.AppTheme;
import service.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/**
 * Main application window.
 *
 * Layout:
 *   ┌─────────────────────────────────────────────┐
 *   │  HEADER  (title · sound · theme toggles)    │
 *   │  TAB BAR (custom pill-shaped buttons)       │
 *   ├─────────────────────────────────────────────┤
 *   │  CONTENT  (CardLayout — one panel per tab)  │
 *   ├─────────────────────────────────────────────┤
 *   │  STATUS BAR  (roll stats · version)         │
 *   └─────────────────────────────────────────────┘
 */
public class DiceRollerFrame extends JFrame {

    private static final String VERSION = "v2.0";

    private final RollHistory        rollHistory;
    private final MacroService       macroService;
    private final HistoryPersistence historyPersistence;
    private final SoundService       soundService;

    private static final String[] TAB_IDS = {"roll","macros","multi","history","prob","fate"};
    private static final String[] TAB_LABELS = {
        "\uD83C\uDFB2  Roll",
        "\u2B50  Macros",
        "\uD83C\uDFB2\uD83C\uDFB2  Multi",
        "\uD83D\uDCDC  History",
        "\uD83D\uDCCA  Probability",
        "\u2721  Fate"
    };

    private HistoryPanel historyPanel;
    private JLabel       statusBar;

    // ── Constructor ───────────────────────────────────────────────────────────

    public DiceRollerFrame(RollHistory rollHistory,
                           MacroService macroService,
                           HistoryPersistence historyPersistence,
                           SoundService soundService) {
        this.rollHistory        = rollHistory;
        this.macroService       = macroService;
        this.historyPersistence = historyPersistence;
        this.soundService       = soundService;

        setTitle("Dice Roller  \u2014  " + VERSION);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(900, 720));
        setSize(1000, 800);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                historyPersistence.saveSession(rollHistory);
                dispose();
                System.exit(0);
            }
        });

        customizeUIManager();
        buildContent();
        setVisible(true);
    }

    // ── Build / rebuild ───────────────────────────────────────────────────────

    private void buildContent() {
        getContentPane().removeAll();

        // Gradient root panel
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(AppTheme.bgGradient(getWidth(), getHeight()));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setOpaque(false);

        // CardLayout content area
        JPanel contentArea = new JPanel(new CardLayout());
        contentArea.setOpaque(false);

        // Deferred tab-switch callback (resolved once TabBar is created)
        final Runnable[] switchToRoll = { () -> {} };

        // Build panels
        RollPanel        rollPanel       = new RollPanel(rollHistory, macroService, soundService, this::toggleTheme);
        historyPanel                     = new HistoryPanel(rollHistory, historyPersistence);
        MacroPanel       macroPanel      = new MacroPanel(macroService, rollPanel,
                                                          () -> switchToRoll[0].run(), soundService);
        MultiRollPanel   multiRollPanel  = new MultiRollPanel(rollHistory, soundService);
        ProbabilityPanel probPanel       = new ProbabilityPanel();
        FateDicePanel    fatePanel       = new FateDicePanel(rollHistory, soundService);

        JPanel[] panels = { rollPanel, macroPanel, multiRollPanel,
                            historyPanel, probPanel, fatePanel };

        for (int i = 0; i < TAB_IDS.length; i++) {
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);
            wrapper.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
            wrapper.add(panels[i], BorderLayout.CENTER);
            contentArea.add(wrapper, TAB_IDS[i]);
        }

        // Tab bar (requires contentArea to be populated)
        TabBar tabBar = new TabBar(TAB_LABELS, TAB_IDS, contentArea,
                                   3, historyPanel::refresh);

        // Now wire the deferred callback
        switchToRoll[0] = () -> tabBar.switchToTab(0);

        // Assemble
        root.add(buildHeader(tabBar), BorderLayout.NORTH);
        root.add(contentArea,         BorderLayout.CENTER);
        root.add(buildStatusBar(),    BorderLayout.SOUTH);

        getContentPane().setBackground(AppTheme.bg());
        getContentPane().add(root, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeader(TabBar tabBar) {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(AppTheme.tabbar());
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(AppTheme.withAlpha(AppTheme.border(), 150));
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        header.setOpaque(false);

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setBorder(BorderFactory.createEmptyBorder(14, 20, 6, 20));

        JLabel title = new JLabel("\uD83C\uDFB2  Dice Roller");
        title.setFont(AppTheme.fontBold(20));
        title.setForeground(AppTheme.accent());
        titleRow.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.setOpaque(false);

        JToggleButton soundBtn = headerToggle(
            soundService.isEnabled() ? "\uD83D\uDD0A  Sound" : "\uD83D\uDD07  Muted",
            soundService.isEnabled());
        soundBtn.addActionListener(e -> {
            soundService.setEnabled(soundBtn.isSelected());
            soundBtn.setText(soundService.isEnabled() ? "\uD83D\uDD0A  Sound" : "\uD83D\uDD07  Muted");
            soundBtn.repaint();
        });

        JButton themeBtn = headerButton(AppTheme.isDark() ? "\u2600  Light" : "\u263D  Dark");
        themeBtn.addActionListener(e -> toggleTheme());

        controls.add(soundBtn);
        controls.add(themeBtn);
        titleRow.add(controls, BorderLayout.EAST);

        header.add(titleRow, BorderLayout.NORTH);
        header.add(tabBar,   BorderLayout.CENTER);
        return header;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(AppTheme.tabbar());
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(AppTheme.withAlpha(AppTheme.border(), 100));
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));

        statusBar = new JLabel(rollHistory.getStatsString());
        statusBar.setFont(AppTheme.font(11));
        statusBar.setForeground(AppTheme.muted());
        bar.add(statusBar, BorderLayout.WEST);

        JLabel versionLabel = new JLabel("Dice Roller " + VERSION + "  \u2014  SHA-256 verified rolls");
        versionLabel.setFont(AppTheme.font(11));
        versionLabel.setForeground(AppTheme.withAlpha(AppTheme.muted(), 140));
        bar.add(versionLabel, BorderLayout.EAST);
        return bar;
    }

    // ── Theme toggle ──────────────────────────────────────────────────────────

    private void toggleTheme() {
        AppTheme.toggle();
        AppTheme.refreshFonts();
        customizeUIManager();
        buildContent();
    }

    // ── UIManager ─────────────────────────────────────────────────────────────

    private void customizeUIManager() {
        UIManager.put("ToolTip.background",            AppTheme.field());
        UIManager.put("ToolTip.foreground",            AppTheme.fg());
        UIManager.put("ToolTip.border",                BorderFactory.createLineBorder(AppTheme.border()));
        UIManager.put("OptionPane.background",         AppTheme.panel());
        UIManager.put("OptionPane.messageForeground",  AppTheme.fg());
        UIManager.put("Panel.background",              AppTheme.bg());
        UIManager.put("SplitPane.background",          AppTheme.bg());
        UIManager.put("Table.background",              AppTheme.field());
        UIManager.put("Table.foreground",              AppTheme.fg());
        UIManager.put("Table.selectionBackground",     AppTheme.hover());
        UIManager.put("Table.selectionForeground",     AppTheme.fg());
        UIManager.put("Table.gridColor",               AppTheme.border());
        UIManager.put("FileChooser.background",        AppTheme.panel());
        UIManager.put("List.background",               AppTheme.field());
        UIManager.put("List.foreground",               AppTheme.fg());
        UIManager.put("List.selectionBackground",      AppTheme.hover());
        UIManager.put("List.selectionForeground",      AppTheme.fg());
        UIManager.put("TextArea.background",           AppTheme.field());
        UIManager.put("TextArea.foreground",           AppTheme.fg());
        UIManager.put("TextField.background",          AppTheme.field());
        UIManager.put("TextField.foreground",          AppTheme.fg());
        UIManager.put("TextField.caretForeground",     AppTheme.fg());
        UIManager.put("Label.foreground",              AppTheme.fg());
        UIManager.put("CheckBox.background",           AppTheme.bg());
        UIManager.put("CheckBox.foreground",           AppTheme.fg());
        UIManager.put("Spinner.background",            AppTheme.field());
        UIManager.put("ComboBox.background",           AppTheme.field());
        UIManager.put("ComboBox.foreground",           AppTheme.fg());
        UIManager.put("ComboBox.selectionBackground",  AppTheme.hover());
        UIManager.put("ComboBox.selectionForeground",  AppTheme.fg());
    }

    // ── Header widget helpers ─────────────────────────────────────────────────

    private JButton headerButton(String text) {
        JButton b = new JButton(text) {
            boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hov) {
                    g2.setColor(AppTheme.withAlpha(AppTheme.hover(), 100));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                }
                g2.setFont(AppTheme.font(12));
                g2.setColor(AppTheme.muted());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JToggleButton headerToggle(String text, boolean selected) {
        JToggleButton b = new JToggleButton(text, selected) {
            boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = isSelected()
                    ? AppTheme.withAlpha(AppTheme.accent(), 35)
                    : (hov ? AppTheme.withAlpha(AppTheme.hover(), 100) : new Color(0,0,0,0));
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setFont(AppTheme.font(12));
                g2.setColor(isSelected() ? AppTheme.accent() : AppTheme.muted());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Inner class: Custom pill-shaped tab bar
    // ═══════════════════════════════════════════════════════════════════════

    static class TabBar extends JPanel {

        private final JPanel    contentArea;
        private final int       refreshIndex;
        private final Runnable  refreshCallback;
        private final TabPill[] pills;

        TabBar(String[] labels, String[] ids, JPanel contentArea,
               int refreshIndex, Runnable refreshCallback) {
            this.contentArea     = contentArea;
            this.refreshIndex    = refreshIndex;
            this.refreshCallback = refreshCallback;

            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.LEFT, 4, 8));
            setBorder(BorderFactory.createEmptyBorder(0, 14, 4, 14));

            pills = new TabPill[labels.length];
            ButtonGroup group = new ButtonGroup();
            for (int i = 0; i < labels.length; i++) {
                final int idx = i;
                pills[i] = new TabPill(labels[i]);
                pills[i].addActionListener(e -> switchToTab(idx));
                group.add(pills[i]);
                add(pills[i]);
            }
            pills[0].setSelected(true);
        }

        /** Switch to tab by index — callable from anywhere in the frame. */
        void switchToTab(int idx) {
            if (idx < 0 || idx >= pills.length) return;
            CardLayout cl = (CardLayout) contentArea.getLayout();
            cl.show(contentArea, DiceRollerFrame.TAB_IDS[idx]);
            for (int i = 0; i < pills.length; i++) {
                pills[i].setSelected(i == idx);
                pills[i].repaint();
            }
            if (idx == refreshIndex && refreshCallback != null) refreshCallback.run();
        }

        // ── Tab pill button ───────────────────────────────────────────────────

        static class TabPill extends JToggleButton {
            private boolean hov = false;

            TabPill(String text) {
                super(text);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setOpaque(false);
                setBorder(BorderFactory.createEmptyBorder(7, 18, 7, 18));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth(), h = getHeight();

                if (isSelected()) {
                    // Pill fill: gradient from lighter top to accent
                    GradientPaint gp = new GradientPaint(
                        0, 0, AppTheme.lighten(AppTheme.accent(), 18),
                        0, h, AppTheme.accent());
                    g2.setPaint(gp);
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));

                    // Inner glow line at top
                    g2.setColor(AppTheme.withAlpha(Color.WHITE, 30));
                    g2.fill(new RoundRectangle2D.Float(2, 1, w - 4, h / 3, h / 2, h / 2));

                    g2.setFont(AppTheme.fontBold(12));
                    g2.setColor(AppTheme.bg());
                } else if (hov) {
                    g2.setColor(AppTheme.withAlpha(AppTheme.hover(), 130));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
                    g2.setFont(AppTheme.font(12));
                    g2.setColor(AppTheme.fg());
                } else {
                    g2.setFont(AppTheme.font(12));
                    g2.setColor(AppTheme.muted());
                }

                FontMetrics fm = g2.getFontMetrics();
                String txt = getText();
                g2.drawString(txt, (w - fm.stringWidth(txt)) / 2,
                    (h - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }

            @Override public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width  = Math.max(d.width, 90);
                d.height = 36;
                return d;
            }
        }
    }
}
