package PD;

import java.util.Random;

/**
 * Dice represents a single die with a configurable number of faces.
 * Supports standard rolling and exploding dice (re-roll on max face).
 *
 * @author PraiseManzi
 */
public class Dice {

	private int numberOfFaces;
	private int currentFace;
	private Random randomGen;

	public Dice(int numberOfFaces) {
		this.numberOfFaces = numberOfFaces;
		randomGen = new Random();
	}

	public int getNumberOfFaces() {
		return numberOfFaces;
	}

	public void setNumberOfFaces(int numberOfFaces) throws FaceRangeException {
		if (numberOfFaces <= 0) {
			throw new FaceRangeException("Number of faces must be greater than zero");
		}
		this.numberOfFaces = numberOfFaces;
	}

	public int getCurrentFace() {
		return currentFace;
	}

	public void setCurrentFace(int currentFace) {
		this.currentFace = currentFace;
	}

	public Random getRandom() {
		return randomGen;
	}

	public void setRandom(Random randomGen) {
		this.randomGen = randomGen;
	}

	public int roll() {
		currentFace = randomGen.nextInt(numberOfFaces) + 1;
		return currentFace;
	}

	/**
	 * Exploding dice: if the max face is rolled, roll again and add.
	 * Capped at maxRerolls to prevent runaway loops.
	 */
	public int rollExploding(int maxRerolls) {
		int total = 0;
		int rolls = 0;
		int result;
		do {
			result = roll();
			total += result;
			rolls++;
		} while (result == numberOfFaces && rolls <= maxRerolls);
		currentFace = total;
		return total;
	}

	@Override
	public String toString() {
		return Integer.toString(currentFace);
	}
}
