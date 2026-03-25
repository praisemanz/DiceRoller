package UI;

import java.awt.Dimension;
import javax.swing.JFrame;

/**
 * Application window for the Dice Roller.
 */
public class DiceRollerFrame extends JFrame {

	public DiceRollerFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Dice Roller");
		setMinimumSize(new Dimension(540, 720));
		setSize(540, 720);
		setLocationRelativeTo(null);
		getContentPane().add(new DiceRollerPanel());
	}
}
