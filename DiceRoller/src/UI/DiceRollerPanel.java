package UI;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import PD.DiceBag;
import PD.DiceRangeException;
import PD.FaceRangeException;

import java.awt.event.ActionEvent;
/**
* DiceRollerPanel is a Class that makes the panel
* 
* @package UI
* @author(PraiseManzi) 
*/
	public class DiceRollerPanel extends JPanel 
	{

		private JButton push;
		private JLabel label;
		private JLabel label1;
		private JLabel label2;
		private JLabel total;
		private JLabel errorMessageLabel;
		private JTextField faceText;
		private JTextField diceText;
		private JTextField text2;
		private int count;
		DiceBag diceBag;
		
		 /**
	     * Constructor for initialising the DiceRollerPanel.
	     */
		public DiceRollerPanel() {

			
			
			label1 = new JLabel("Number of Faces: ");
			this.add(label1);
			
			faceText= new JTextField(5);
			this.add(faceText);
			
			label2 = new JLabel("Number of Dice: ");
			this.add(label2);
			diceText= new JTextField(5);
			this.add(diceText);
			
			push = new JButton("Roll");
			push.addActionListener(new CountButtonListener());
			this.add(push);
			
			total = new JLabel("Total:");
			this.add(total);
			text2= new JTextField(5);
			
			errorMessageLabel = new JLabel("");
			this.add(errorMessageLabel);
			
			
			
			

			setBackground(Color.magenta.brighter());
			setPreferredSize(new Dimension(300, 100));

		}
		// Declare as an inner class so we can access the private class variables
		private class CountButtonListener implements ActionListener 
		{

			@Override
			public void actionPerformed(ActionEvent event) 
			{
				
					int newFace;
					int newDice;
					newFace=Integer.parseInt(faceText.getText());
					newDice= Integer.parseInt(diceText.getText());
					errorMessageLabel.setText("");
					try 
					{
					diceBag=new DiceBag(newDice,newFace);
					}
					catch(FaceRangeException exception)
					{
						errorMessageLabel.setText(exception.getMessage());
						total.setText("Total:0");
						
						return;
					}
					catch(DiceRangeException exception)
					{
						
						errorMessageLabel.setText(exception.getMessage());
						total.setText("Total:0");
						
						return;
					}
				
					diceBag.rollDice();
					total.setText(diceBag.toString());
			}
		}


}
