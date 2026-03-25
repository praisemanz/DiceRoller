package service;

import PD.RollHistory;
import PD.RollResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Saves and loads roll history to/from ~/.diceroller/history_YYYYMMDD_HHmmss.dat
 * Each session gets its own timestamped file.
 * Also supports CSV export.
 */
public class HistoryPersistence {

    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), ".diceroller");
    private static final DateTimeFormatter FILE_FMT =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public HistoryPersistence() {
        ensureDir();
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    public void saveHistory(RollHistory history, String sessionId) {
        if (history.getRollCount() == 0) return;
        Path file = DATA_DIR.resolve("history_" + sessionId + ".dat");
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (RollResult r : history.getHistory()) {
                writer.write(r.toJsonLine());
                writer.newLine();
            }
        } catch (IOException ignored) {}
    }

    /** Saves the current session with a timestamp-based filename. */
    public void saveSession(RollHistory history) {
        String id = LocalDateTime.now().format(FILE_FMT);
        saveHistory(history, id);
    }

    // ── Load ─────────────────────────────────────────────────────────────────

    /** Loads the most recently modified history file, or returns an empty history. */
    public RollHistory loadLatestHistory() {
        List<Path> sessions = listSessions();
        if (sessions.isEmpty()) return new RollHistory();
        return loadFromFile(sessions.get(0));
    }

    public RollHistory loadFromFile(Path file) {
        RollHistory h = new RollHistory();
        if (!Files.exists(file)) return h;
        List<RollResult> results = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("{")) {
                    RollResult r = RollResult.fromJsonLine(line);
                    if (r != null) results.add(r);
                }
            }
        } catch (IOException ignored) {}
        h.setHistory(results);
        return h;
    }

    /** Lists all session files sorted newest first. */
    public List<Path> listSessions() {
        List<Path> paths = new ArrayList<>();
        if (!Files.exists(DATA_DIR)) return paths;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DATA_DIR, "history_*.dat")) {
            for (Path p : stream) {
                paths.add(p);
            }
        } catch (IOException ignored) {}
        Collections.sort(paths, (a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()));
        return paths;
    }

    // ── CSV export ───────────────────────────────────────────────────────────

    public void exportCsv(RollHistory history, Path outputFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write("Index,Notation,Total,DiceValues,Modifier,Mode,Timestamp,Hash");
            writer.newLine();
            List<RollResult> results = history.getHistory();
            for (int i = 0; i < results.size(); i++) {
                RollResult r = results.get(i);
                writer.write(String.join(",",
                    String.valueOf(i + 1),
                    csvEscape(r.getNotation()),
                    String.valueOf(r.getTotal()),
                    csvEscape(r.getDieValuesString()),
                    String.valueOf(r.getModifier()),
                    csvEscape(r.getRollMode()),
                    csvEscape(r.getTimeString()),
                    csvEscape(r.getVerificationHash())
                ));
                writer.newLine();
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void ensureDir() {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException ignored) {}
    }
}
