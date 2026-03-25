package PD;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures the full state of a single roll: values, total, modifier,
 * notation used, roll mode, and timestamp.
 */
public class RollResult {

	private static final DateTimeFormatter TIME_FMT =
		DateTimeFormatter.ofPattern("HH:mm:ss");

	private String notation;
	private int total;
	private int[] dieValues;
	private int modifier;
	private LocalDateTime timestamp;
	private String rollMode;
	private int altTotal;

	public RollResult(String notation, int total, int[] dieValues,
	                  int modifier, String rollMode) {
		this.notation = notation;
		this.total = total;
		this.dieValues = dieValues;
		this.modifier = modifier;
		this.rollMode = rollMode;
		this.timestamp = LocalDateTime.now();
		this.altTotal = -1;
	}

	public String getNotation()       { return notation; }
	public int getTotal()             { return total; }
	public int[] getDieValues()       { return dieValues; }
	public int getModifier()          { return modifier; }
	public LocalDateTime getTimestamp(){ return timestamp; }
	public String getRollMode()       { return rollMode; }
	public int getAltTotal()          { return altTotal; }

	public void setAltTotal(int altTotal) {
		this.altTotal = altTotal;
	}

	public String getTimeString() {
		return timestamp.format(TIME_FMT);
	}

	public String getDieValuesString() {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < dieValues.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(dieValues[i]);
		}
		sb.append("]");
		if (modifier != 0) {
			sb.append(modifier > 0 ? "+" : "").append(modifier);
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(notation);
		sb.append(" \u2192 ").append(total);
		sb.append("  ").append(getDieValuesString());
		if (!"Normal".equals(rollMode)) {
			sb.append(" (").append(rollMode);
			if (altTotal >= 0) {
				sb.append(", other: ").append(altTotal);
			}
			sb.append(")");
		}
		sb.append("  ").append(getTimeString());
		return sb.toString();
	}
}
