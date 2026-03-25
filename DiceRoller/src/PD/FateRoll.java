package PD;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a standard Fate/Fudge roll: 4dF + modifier.
 * Includes ladder adjective mapping per the Fate Core system.
 */
public class FateRoll {

    private static final int NUM_DICE = 4;

    private final List<FateDie> dice = new ArrayList<>();
    private int modifier;
    private int total;

    public FateRoll(int modifier) {
        this.modifier = modifier;
        for (int i = 0; i < NUM_DICE; i++) {
            dice.add(new FateDie());
        }
    }

    /** Rolls all dice and returns the numeric total including modifier. */
    public int roll() {
        int sum = 0;
        for (FateDie d : dice) {
            d.roll();
            sum += d.getCurrentValue();
        }
        total = sum + modifier;
        return total;
    }

    public int getTotal()           { return total; }
    public int getModifier()        { return modifier; }
    public void setModifier(int m)  { modifier = m; }
    public List<FateDie> getDice()  { return dice; }

    /**
     * Returns the Fate Ladder adjective for the current total.
     * Range: Terrible (-4) through Legendary (+4), beyond mapped to extremes.
     */
    public String getLadderResult() {
        if (total >= 4)  return "Legendary";
        if (total == 3)  return "Fantastic";
        if (total == 2)  return "Superb";
        if (total == 1)  return "Great";
        if (total == 0)  return "Good";
        if (total == -1) return "Fair";
        if (total == -2) return "Mediocre";
        if (total == -3) return "Poor";
        return "Terrible";
    }

    /** Returns all die symbols as a space-separated string, e.g. "[+] [ ] [-] [+]" */
    public String getDiceSymbols() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dice.size(); i++) {
            if (i > 0) sb.append("  ");
            sb.append(dice.get(i).getCurrentSymbol());
        }
        return sb.toString();
    }

    /** Returns numeric values as int array for history integration. */
    public int[] getDieValues() {
        int[] vals = new int[dice.size()];
        for (int i = 0; i < dice.size(); i++) {
            vals[i] = dice.get(i).getCurrentValue();
        }
        return vals;
    }
}
