package service;

import PD.RollResult;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Generates a SHA-256 verification hash for a roll result.
 * Allows players to prove a roll was not faked (deterministic from inputs).
 */
public class HashService {

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "error";
        }
    }

    /** Returns the full 64-char hex hash for a roll result. */
    public static String hashResult(RollResult r) {
        String raw = r.getNotation()
            + "|" + r.getTotal()
            + "|" + Arrays.toString(r.getDieValues())
            + "|" + r.getModifier()
            + "|" + r.getRollMode()
            + "|" + r.getTimestamp().toString();
        return sha256(raw);
    }

    /** Returns an 8-character prefix suitable for display. */
    public static String shortHash(RollResult r) {
        return hashResult(r).substring(0, 8);
    }
}
