package PD;

/**
 * A named, saved dice roll configuration.
 * Persisted to ~/.diceroller/macros.dat by MacroService.
 */
public class RollMacro {

    private String name;
    private String notation;
    private String rollMode;    // "Normal" | "Advantage" | "Disadvantage" | "Exploding"
    private String description;
    private long   createdAt;   // epoch millis

    public RollMacro(String name, String notation, String rollMode, String description) {
        this.name        = name;
        this.notation    = notation;
        this.rollMode    = rollMode;
        this.description = description;
        this.createdAt   = System.currentTimeMillis();
    }

    // ── Getters / setters ────────────────────────────────────────────────────
    public String getName()           { return name; }
    public void   setName(String n)   { name = n; }

    public String getNotation()           { return notation; }
    public void   setNotation(String n)   { notation = n; }

    public String getRollMode()           { return rollMode; }
    public void   setRollMode(String m)   { rollMode = m; }

    public String getDescription()        { return description; }
    public void   setDescription(String d){ description = d; }

    public long   getCreatedAt()          { return createdAt; }
    public void   setCreatedAt(long t)    { createdAt = t; }

    // ── Serialization helpers ────────────────────────────────────────────────

    /** Encodes as a single-line JSON object (safe to write/read line by line). */
    public String toJsonLine() {
        return "{"
            + "\"name\":"        + jsonStr(name)        + ","
            + "\"notation\":"    + jsonStr(notation)    + ","
            + "\"rollMode\":"    + jsonStr(rollMode)    + ","
            + "\"description\":" + jsonStr(description) + ","
            + "\"createdAt\":"   + createdAt
            + "}";
    }

    /** Parses a JSON line produced by toJsonLine(). Returns null on failure. */
    public static RollMacro fromJsonLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        try {
            String name  = extractString(line, "name");
            String nota  = extractString(line, "notation");
            String mode  = extractString(line, "rollMode");
            String desc  = extractString(line, "description");
            long   ts    = extractLong(line, "createdAt");
            RollMacro m  = new RollMacro(name, nota, mode, desc);
            m.createdAt  = ts;
            return m;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return name + "  [" + notation + "]  " + rollMode;
    }

    // ── JSON utilities ───────────────────────────────────────────────────────

    private static String jsonStr(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String extractString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start < 0) return "";
        start += pattern.length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                sb.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static long extractLong(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start < 0) return 0;
        start += pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        String numStr = json.substring(start, end).trim();
        return numStr.isEmpty() ? 0 : Long.parseLong(numStr);
    }
}
