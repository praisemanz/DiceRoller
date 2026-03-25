package UI;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point. Launches the Dice Roller on the Swing event thread.
 */
public class DiceRollerUI {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(
					UIManager.getCrossPlatformLookAndFeelClassName());
			} catch (Exception ignored) {}

			DiceRollerFrame frame = new DiceRollerFrame();
			frame.setVisible(true);
		});
	}
}
