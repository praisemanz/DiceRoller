package UI;

import PD.RollHistory;
import UI.theme.AppTheme;
import service.*;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point. Constructs all services, loads persisted state,
 * and launches the Dice Roller on the Swing event thread.
 */
public class DiceRollerUI {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Use the cross-platform L&F so our custom colors render consistently
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}

            // Construct services (order matters — history persistence needs its dir)
            HistoryPersistence histPersistence = new HistoryPersistence();
            MacroService       macroService    = new MacroService();
            SoundService       soundService    = new SoundService();

            // Load the most recent session's history (empty if no prior sessions)
            RollHistory rollHistory = histPersistence.loadLatestHistory();

            // Set initial theme (dark by default) and pull system fonts (SF Pro, Segoe UI)
            AppTheme.setDark(true);
            AppTheme.refreshFonts();

            // Build and show the frame (setVisible called inside constructor)
            new DiceRollerFrame(rollHistory, macroService, histPersistence, soundService);
        });
    }
}
