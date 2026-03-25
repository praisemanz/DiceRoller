package PD;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks every roll in the current session and provides
 * aggregate statistics (count, min, max, average).
 */
public class RollHistory {

    private List<RollResult> history;

    public RollHistory() {
        history = new ArrayList<>();
    }

    public void addResult(RollResult result) {
        history.add(result);
    }

    public List<RollResult> getHistory() {
        return new ArrayList<>(history);
    }

    /** Replaces the internal list — used when loading from disk. */
    public void setHistory(List<RollResult> loaded) {
        history = new ArrayList<>(loaded);
    }

    public RollResult getLastResult() {
        if (history.isEmpty()) return null;
        return history.get(history.size() - 1);
    }

    public int getRollCount() {
        return history.size();
    }

    public int getMin() {
        return history.stream().mapToInt(RollResult::getTotal).min().orElse(0);
    }

    public int getMax() {
        return history.stream().mapToInt(RollResult::getTotal).max().orElse(0);
    }

    public double getAverage() {
        return history.stream().mapToInt(RollResult::getTotal).average().orElse(0.0);
    }

    public void clear() {
        history.clear();
    }

    public String getStatsString() {
        if (history.isEmpty()) return "No rolls yet";
        return String.format("Rolls: %d  |  Min: %d  |  Max: %d  |  Avg: %.1f",
            getRollCount(), getMin(), getMax(), getAverage());
    }
}
