package UI;
import javax.swing.JFrame;
/**
* DiceRollerUI is a Class that makes the UI 
* 
* @package UI
* @author(PraiseManzi) 
*/
public class DiceRollerUI 
{
	public static void main(String[] args) {

		JFrame frame = new DiceRollerFrame();
		frame.pack(); // set the size based on content
		frame.setVisible(true); // so we can see the frame
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(420,420);
		
	}
}
