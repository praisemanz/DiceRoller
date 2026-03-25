package PD;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses RPG-style dice notation strings like "3d6", "d20", "2d8+3", "4d6-1".
 */
public class DiceNotation {

	private static final Pattern NOTATION_PATTERN = Pattern.compile(
		"^(\\d*)d(\\d+)([+-]\\d+)?$", Pattern.CASE_INSENSITIVE);

	private int numberOfDice;
	private int numberOfFaces;
	private int modifier;

	public DiceNotation(String notation) throws IllegalArgumentException {
		parse(notation.trim().toLowerCase());
	}

	private void parse(String notation) {
		Matcher m = NOTATION_PATTERN.matcher(notation);
		if (!m.matches()) {
			throw new IllegalArgumentException(
				"Invalid notation: \"" + notation + "\". Use format like 3d6, d20, 2d8+3");
		}
		String diceStr = m.group(1);
		numberOfDice = (diceStr == null || diceStr.isEmpty()) ? 1 : Integer.parseInt(diceStr);
		numberOfFaces = Integer.parseInt(m.group(2));
		String modStr = m.group(3);
		modifier = (modStr == null) ? 0 : Integer.parseInt(modStr);
	}

	public int getNumberOfDice() {
		return numberOfDice;
	}

	public int getNumberOfFaces() {
		return numberOfFaces;
	}

	public int getModifier() {
		return modifier;
	}

	public DiceBag createDiceBag() throws DiceRangeException, FaceRangeException {
		return new DiceBag(numberOfDice, numberOfFaces, modifier);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(numberOfDice).append("d").append(numberOfFaces);
		if (modifier > 0) sb.append("+").append(modifier);
		else if (modifier < 0) sb.append(modifier);
		return sb.toString();
	}
}
