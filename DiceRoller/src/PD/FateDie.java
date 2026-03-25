package PD;

import java.util.Random;

/**
 * A Fate/Fudge die with three faces: PLUS (+1), MINUS (-1), BLANK (0).
 * Standard Fate RPG uses 4 of these per roll.
 */
public class FateDie {

    public enum Face {
        PLUS("+1", "[+]"),
        MINUS("-1", "[-]"),
        BLANK(" 0", "[ ]");

        private final String numericLabel;
        private final String symbol;

        Face(String numericLabel, String symbol) {
            this.numericLabel = numericLabel;
            this.symbol = symbol;
        }

        public int numericValue() {
            if (this == PLUS)  return 1;
            if (this == MINUS) return -1;
            return 0;
        }

        public String getSymbol()       { return symbol; }
        public String getNumericLabel() { return numericLabel; }
    }

    private static final Random RNG = new Random();
    private Face currentFace = Face.BLANK;

    public Face roll() {
        Face[] faces = Face.values();
        currentFace = faces[RNG.nextInt(faces.length)];
        return currentFace;
    }

    public Face getCurrentFace()     { return currentFace; }
    public int  getCurrentValue()    { return currentFace.numericValue(); }
    public String getCurrentSymbol() { return currentFace.getSymbol(); }
}
