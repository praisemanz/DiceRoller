package PD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A named group of dice formulas rolled together in one action.
 * Used by MultiRollPanel for "combat round" style multi-dice rolls.
 */
public class RollGroup {

    /** One line in a multi-roll group. */
    public static class GroupLine {
        public final String label;
        public final String notation;
        public RollResult result;

        public GroupLine(String label, String notation) {
            this.label    = label;
            this.notation = notation;
        }
    }

    private final String groupName;
    private final List<GroupLine> lines;

    public RollGroup(String groupName, List<GroupLine> lines) {
        this.groupName = groupName;
        this.lines     = new ArrayList<>(lines);
    }

    /**
     * Parses multi-line input where each line is either:
     *   "Label: 3d6+2"  or just  "3d6+2"
     * Blank lines are ignored.
     */
    public static RollGroup fromText(String groupName, String text) {
        List<GroupLine> lines = new ArrayList<>();
        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String label;
            String notation;
            int colon = line.indexOf(':');
            if (colon > 0 && colon < line.length() - 1) {
                label    = line.substring(0, colon).trim();
                notation = line.substring(colon + 1).trim();
            } else {
                label    = line;
                notation = line;
            }
            lines.add(new GroupLine(label, notation));
        }
        return new RollGroup(groupName, lines);
    }

    /**
     * Rolls every formula in the group and stores RollResult in each line.
     */
    public void rollAll() throws DiceRangeException, FaceRangeException {
        for (GroupLine gl : lines) {
            DiceNotation dn = new DiceNotation(gl.notation);
            DiceBag bag     = dn.createDiceBag();
            bag.rollDice();
            gl.result = new RollResult(
                bag.getNotation(),
                bag.getTotal(),
                bag.getDieValues(),
                bag.getModifier(),
                "Normal"
            );
        }
    }

    public int getGrandTotal() {
        int sum = 0;
        for (GroupLine gl : lines) {
            if (gl.result != null) sum += gl.result.getTotal();
        }
        return sum;
    }

    public String getGroupName()        { return groupName; }
    public List<GroupLine> getLines()   { return Collections.unmodifiableList(lines); }
    public int getLineCount()           { return lines.size(); }
}
