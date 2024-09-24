package UI;
import javax.swing.JFrame;

public class DiceRollerFrame extends JFrame{
	
	public DiceRollerFrame()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Dice Roller");
		getContentPane().add(new DiceRollerPanel());
	}
}

