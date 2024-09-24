package Test;
import java.util.Scanner;

import PD.Dice;
import PD.DiceBag;
/**
* DiceTest is a Class that tests the DiceBag
* 
* @package Test
* @author(PraiseManzi) 
*/
public class DiceTest 
{
	Scanner scanner=new Scanner(System.in);
	/**
     * Test method acl1Test for demonstrating DiceBag functionality.
     * Prompts the user for the number of dice and faces, rolls the dice, and displays the results.
     */
	public void acl1Test()
	{
		int numberOfFaces;
		int numberOfDice;
		System.out.print("Enter the number of dice:");
		numberOfDice=scanner.nextInt();
		System.out.print("Enter the number of faces:");
		numberOfFaces=scanner.nextInt();
		
		DiceBag diceBag1=new DiceBag();
		diceBag1.rollDice();
		System.out.println(diceBag1);
	}
	 /**
     * Test method acl2Test for demonstrating Dice functionality.
     * Prompts the user for the number of faces, rolls a single die, and displays the result.
     */
	public void acl2Test()
	{
		int value;
		int numberOfFaces;
		System.out.print("number of Faces:");
		numberOfFaces=scanner.nextInt();

		Dice dice=new Dice(numberOfFaces);
		value=dice.roll();
		System.out.println("The rolled value is: "+ value);
	}
	
	/**
     * Test method acl3Test for demonstrating Dice functionality.
     * Prompts the user for the number of faces, rolls the die 100 times, and displays the frequency of each face.
     */
	public void acl3Test()
	{
		int numberOfFaces;
		System.out.print("number of faces:");
		numberOfFaces=scanner.nextInt();
		Dice newDice=new Dice(numberOfFaces);
		
		int []diceFrequency=new int[newDice.getNumberOfFaces()];
		for( int i=0;i<100;i++)
		{
			diceFrequency[newDice.roll()-1]++;
		}
		for(int i=0;i<newDice.getNumberOfFaces();i++)
		{
			System.out.println("Face"+(i+1)+":"+diceFrequency[i]);
		}
	}
	

	

}
