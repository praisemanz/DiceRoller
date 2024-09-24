package PD;
/**
* DiceBag is a Class that represents a collection of Die 
* that have the same number of faces.
* 
* @package PD
* @author(PraiseManzi) 
*/

public class DiceBag
{
	// instance variables
	private int numberOfFaces;
	private int numberOfDice;
	private int total;
	private Dice dice[];

//default constructor
	public DiceBag() 
	{
		this.setNumberOfFaces(0);
		this.setNumberOfDice(0);
		this.setTotal(0);
	}
	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getNumberOfDice()
	{
		return numberOfDice;
	}

	public void setNumberOfDice(int numberOfDice) {
		this.numberOfDice = numberOfDice;
	}

	public int getNumberOfFaces() {
		return numberOfFaces;
	}

	public void setNumberOfFaces(int numberOfFaces)
	{
		this.numberOfFaces = numberOfFaces;
	}
	 // Getters and setters for instance variables
    // ...

    /**
     * Constructor for creating a DiceBag with a specified number of dice and faces on each die.
     *
     * @param numberOfDice   The number of dice to include in the bag.
     * @param numberOfFaces  The number of faces on each die.
     * @throws numberOfDiceRangeExcpetion If the number of dice is less than or equal to zero.
     * @throws numberOfFaceRangeException If the number of faces on each die is less than or equal to zero.
     */
	public DiceBag(int numberOfDice,int numberOfFaces)throws DiceRangeException, FaceRangeException
	{	
		if(numberOfDice<=0)
		{
			DiceRangeException exception= new DiceRangeException(" Number of dice must be greater than zero ");
			throw exception;
		}
		else
		{
			this.numberOfDice= numberOfDice;
			
			dice =new Dice[numberOfDice];
			for(int i=0;i<numberOfDice;i++)
			{
				dice[i]= new Dice(numberOfFaces);
			}
		}
		if(numberOfFaces<=0)
		{
			FaceRangeException exception= new FaceRangeException(" Number of faces must be greater than zero ");
			throw exception;
		}
		else
		{
			this.numberOfFaces= numberOfFaces;
			
			dice =new Dice[numberOfFaces];
			for(int i=0;i<numberOfFaces;i++)
			{
				dice[i]= new Dice(numberOfFaces);
			}
		}
	}
	 /**
     * Rolls all the dice in the bag and calculates the total sum of their face values.
     *
     * @return The total sum of face values rolled.
     */
	public int rollDice()
	{
		int total = 0;
		for( int i=0;i<numberOfDice;i++)
		{
			total+= dice[i].roll();
		}
		this.total=total;
		return total;
	}
	/**
     * Returns a string representation of the total sum and face values of the rolled dice in the bag.
     *
     * @return A string containing the total sum and individual face values of the dice.
     */
	public String toString()
	{
		String value;
		value=" the total is: "+ Integer.toString(total);
		for(int i=0;i<numberOfDice;i++)
		{
			value=value+" The face: "+ dice[i].toString()+" ";
		}
		return value;
	}
	
}
	