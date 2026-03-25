package PD;

/**
 * DiceBag represents a collection of dice with the same number of faces,
 * supporting modifiers, advantage/disadvantage, and exploding dice.
 *
 * @author PraiseManzi
 */
public class DiceBag {

	private int numberOfFaces;
	private int numberOfDice;
	private int modifier;
	private int total;
	private Dice[] dice;

	public DiceBag() {
		this.numberOfFaces = 0;
		this.numberOfDice = 0;
		this.modifier = 0;
		this.total = 0;
	}

	public DiceBag(int numberOfDice, int numberOfFaces) throws DiceRangeException, FaceRangeException {
		if (numberOfDice <= 0) {
			throw new DiceRangeException("Number of dice must be greater than zero");
		}
		if (numberOfFaces <= 0) {
			throw new FaceRangeException("Number of faces must be greater than zero");
		}
		this.numberOfDice = numberOfDice;
		this.numberOfFaces = numberOfFaces;
		this.modifier = 0;
		dice = new Dice[numberOfDice];
		for (int i = 0; i < numberOfDice; i++) {
			dice[i] = new Dice(numberOfFaces);
		}
	}

	public DiceBag(int numberOfDice, int numberOfFaces, int modifier) throws DiceRangeException, FaceRangeException {
		this(numberOfDice, numberOfFaces);
		this.modifier = modifier;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getNumberOfDice() {
		return numberOfDice;
	}

	public void setNumberOfDice(int numberOfDice) {
		this.numberOfDice = numberOfDice;
	}

	public int getNumberOfFaces() {
		return numberOfFaces;
	}

	public void setNumberOfFaces(int numberOfFaces) {
		this.numberOfFaces = numberOfFaces;
	}

	public int getModifier() {
		return modifier;
	}

	public void setModifier(int modifier) {
		this.modifier = modifier;
	}

	public int[] getDieValues() {
		if (dice == null) return new int[0];
		int[] values = new int[numberOfDice];
		for (int i = 0; i < numberOfDice; i++) {
			values[i] = dice[i].getCurrentFace();
		}
		return values;
	}

	public int rollDice() {
		int sum = 0;
		for (int i = 0; i < numberOfDice; i++) {
			sum += dice[i].roll();
		}
		this.total = sum + modifier;
		return total;
	}

	public int rollDiceExploding() {
		int sum = 0;
		for (int i = 0; i < numberOfDice; i++) {
			sum += dice[i].rollExploding(10);
		}
		this.total = sum + modifier;
		return total;
	}

	/**
	 * Roll twice, keep the higher total.
	 * @return [chosen_total, discarded_total]
	 */
	public int[] rollWithAdvantage() {
		int roll1 = rollDice();
		int[] values1 = getDieValues().clone();

		int roll2 = rollDice();

		if (roll1 >= roll2) {
			for (int i = 0; i < numberOfDice; i++) {
				dice[i].setCurrentFace(values1[i]);
			}
			total = roll1;
			return new int[]{roll1, roll2};
		} else {
			total = roll2;
			return new int[]{roll2, roll1};
		}
	}

	/**
	 * Roll twice, keep the lower total.
	 * @return [chosen_total, discarded_total]
	 */
	public int[] rollWithDisadvantage() {
		int roll1 = rollDice();
		int[] values1 = getDieValues().clone();

		int roll2 = rollDice();

		if (roll1 <= roll2) {
			for (int i = 0; i < numberOfDice; i++) {
				dice[i].setCurrentFace(values1[i]);
			}
			total = roll1;
			return new int[]{roll1, roll2};
		} else {
			total = roll2;
			return new int[]{roll2, roll1};
		}
	}

	public String getNotation() {
		StringBuilder sb = new StringBuilder();
		sb.append(numberOfDice).append("d").append(numberOfFaces);
		if (modifier > 0) sb.append("+").append(modifier);
		else if (modifier < 0) sb.append(modifier);
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Total: ").append(total);
		if (dice != null) {
			sb.append(" [");
			for (int i = 0; i < numberOfDice; i++) {
				if (i > 0) sb.append(", ");
				sb.append(dice[i].getCurrentFace());
			}
			sb.append("]");
		}
		if (modifier != 0) {
			sb.append(modifier > 0 ? " +" : " ").append(modifier);
		}
		return sb.toString();
	}
}
