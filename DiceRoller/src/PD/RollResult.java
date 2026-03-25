package PD;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures the full state of a single roll, including a SHA-256 verification hash.
 */
public class RollResult {

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private String        notation;
    private int           total;
    private int[]         dieValues;
    private int           modifier;
    private LocalDateTime timestamp;
    private String        rollMode;
    private int           altTotal;
    private String        verificationHash;  // SHA-256 short hash (set after construction)

    public RollResult(String notation, int total, int[] dieValues,
                      int modifier, String rollMode) {
        this.notation  = notation;
        this.total     = total;
        this.dieValues = dieValues;
        this.modifier  = modifier;
        this.rollMode  = rollMode;
        this.timestamp = LocalDateTime.now();
        this.altTotal  = -1;
        this.verificationHash = "";
    }

    // ── Getters ─────────────────────────────────────────────────────────────
    public String        getNotation()        { return notation; }
    public int           getTotal()           { return total; }
    public int[]         getDieValues()       { return dieValues; }
    public int           getModifier()        { return modifier; }
    public LocalDateTime getTimestamp()       { return timestamp; }
    public String        getRollMode()        { return rollMode; }
    public int           getAltTotal()        { return altTotal; }
    public String        getVerificationHash(){ return verificationHash; }

    public void setAltTotal(int altTotal)                 { this.altTotal = altTotal; }
    public void setVerificationHash(String hash)          { this.verificationHash = hash; }

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
            if (altTotal >= 0) sb.append(", other: ").append(altTotal);
            sb.append(")");
        }
        sb.append("  ").append(getTimeString());
        if (verificationHash != null && !verificationHash.isEmpty()) {
            sb.append("  #").append(verificationHash, 0, Math.min(8, verificationHash.length()));
        }
        return sb.toString();
    }

    // ── JSON serialization ───────────────────────────────────────────────────

    /** Serializes to a single-line JSON object for file persistence. */
    public String toJsonLine() {
        StringBuilder dv = new StringBuilder("[");
        for (int i = 0; i < dieValues.length; i++) {
            if (i > 0) dv.append(",");
            dv.append(dieValues[i]);
        }
        dv.append("]");

        return "{"
            + "\"notation\":"  + jsonStr(notation) + ","
            + "\"total\":"     + total              + ","
            + "\"dieValues\":" + dv                 + ","
            + "\"modifier\":"  + modifier           + ","
            + "\"rollMode\":"  + jsonStr(rollMode)  + ","
            + "\"timestamp\":" + jsonStr(timestamp.format(DT_FMT)) + ","
            + "\"altTotal\":"  + altTotal           + ","
            + "\"hash\":"      + jsonStr(verificationHash)
            + "}";
    }

    /** Parses a JSON line produced by toJsonLine(). Returns null on failure. */
    public static RollResult fromJsonLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        try {
            String nota    = extractString(line, "notation");
            int    total   = (int) extractLong(line, "total");
            int[]  dv      = extractIntArray(line, "dieValues");
            int    mod     = (int) extractLong(line, "modifier");
            String mode    = extractString(line, "rollMode");
            String tsStr   = extractString(line, "timestamp");
            int    altTot  = (int) extractLong(line, "altTotal");
            String hash    = extractString(line, "hash");

            RollResult r   = new RollResult(nota, total, dv, mod, mode);
            r.altTotal     = altTot;
            r.verificationHash = hash;
            if (tsStr != null && !tsStr.isEmpty()) {
                try { r.timestamp = LocalDateTime.parse(tsStr, DT_FMT); } catch (Exception ignored) {}
            }
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    // ── Private JSON helpers ─────────────────────────────────────────────────

    private static String jsonStr(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    static String extractString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start < 0) return "";
        start += pattern.length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) { sb.append(c); escaped = false; }
            else if (c == '\\') escaped = true;
            else if (c == '"') break;
            else sb.append(c);
        }
        return sb.toString();
    }

    static long extractLong(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start < 0) return 0;
        start += pattern.length();
        if (start < json.length() && json.charAt(start) == '"') start++; // skip optional quote
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        String s = json.substring(start, end).trim();
        return s.isEmpty() ? 0 : Long.parseLong(s);
    }

    private static int[] extractIntArray(String json, String key) {
        String pattern = "\"" + key + "\":[";
        int start = json.indexOf(pattern);
        if (start < 0) return new int[0];
        start += pattern.length();
        int end = json.indexOf(']', start);
        if (end < 0) return new int[0];
        String inner = json.substring(start, end).trim();
        if (inner.isEmpty()) return new int[0];
        String[] parts = inner.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            arr[i] = Integer.parseInt(parts[i].trim());
        }
        return arr;
    }
}
