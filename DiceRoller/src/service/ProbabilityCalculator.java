package service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Computes exact probability distributions for NdF+M using dynamic programming.
 * No simulation - results are mathematically precise.
 */
public class ProbabilityCalculator {

    public static final int MAX_DICE  = 20;
    public static final int MAX_FACES = 200;

    /**
     * Returns an ordered map of {outcome -> probability (0.0..1.0)}.
     * Throws IllegalArgumentException if the computation would be too expensive.
     */
    public static Map<Integer, Double> computeDistribution(int numDice, int numFaces, int modifier) {
        if (numDice > MAX_DICE || numFaces > MAX_FACES) {
            throw new IllegalArgumentException(
                "Too many combinations (max " + MAX_DICE + "d" + MAX_FACES + ")");
        }
        if (numDice <= 0 || numFaces <= 0) {
            throw new IllegalArgumentException("Dice and faces must be positive");
        }

        // dp[s] = number of ways to roll sum s with all dice so far
        int maxSum = numDice * numFaces;
        long[] dp = new long[maxSum + 1];
        dp[0] = 1;

        for (int die = 0; die < numDice; die++) {
            long[] next = new long[maxSum + 1];
            for (int s = 0; s <= maxSum; s++) {
                if (dp[s] == 0) continue;
                for (int face = 1; face <= numFaces; face++) {
                    int ns = s + face;
                    if (ns <= maxSum) {
                        next[ns] += dp[s];
                    }
                }
            }
            dp = next;
        }

        // Count total outcomes = numFaces^numDice  (use double to avoid overflow)
        double total = Math.pow(numFaces, numDice);

        Map<Integer, Double> dist = new LinkedHashMap<>();
        for (int s = numDice; s <= maxSum; s++) {
            if (dp[s] > 0) {
                dist.put(s + modifier, dp[s] / total);
            }
        }
        return dist;
    }

    /** Expected value = numDice * (numFaces + 1) / 2 + modifier */
    public static double expectedValue(int numDice, int numFaces, int modifier) {
        return numDice * (numFaces + 1.0) / 2.0 + modifier;
    }

    /** Standard deviation = sqrt(numDice * (numFaces^2 - 1) / 12) */
    public static double standardDeviation(int numDice, int numFaces) {
        return Math.sqrt(numDice * (numFaces * (double) numFaces - 1.0) / 12.0);
    }

    /**
     * Returns probability of rolling >= threshold.
     */
    public static double probabilityAtLeast(Map<Integer, Double> dist, int threshold) {
        double prob = 0.0;
        for (Map.Entry<Integer, Double> e : dist.entrySet()) {
            if (e.getKey() >= threshold) {
                prob += e.getValue();
            }
        }
        return prob;
    }

    /**
     * Returns probability of rolling <= threshold.
     */
    public static double probabilityAtMost(Map<Integer, Double> dist, int threshold) {
        double prob = 0.0;
        for (Map.Entry<Integer, Double> e : dist.entrySet()) {
            if (e.getKey() <= threshold) {
                prob += e.getValue();
            }
        }
        return prob;
    }
}
