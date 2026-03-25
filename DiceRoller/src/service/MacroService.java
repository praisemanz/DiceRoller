package service;

import PD.RollMacro;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CRUD + JSON file persistence for named roll macros.
 * Saved to ~/.diceroller/macros.dat (one JSON object per line).
 */
public class MacroService {

    private static final Path DATA_DIR   = Paths.get(System.getProperty("user.home"), ".diceroller");
    private static final Path MACRO_FILE = DATA_DIR.resolve("macros.dat");

    private final List<RollMacro> macros = new ArrayList<>();

    public MacroService() {
        ensureDir();
        load();
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    public List<RollMacro> getMacros() {
        return Collections.unmodifiableList(macros);
    }

    public void addMacro(RollMacro m) {
        macros.add(m);
        save();
    }

    public void removeMacro(int index) {
        if (index >= 0 && index < macros.size()) {
            macros.remove(index);
            save();
        }
    }

    public void updateMacro(int index, RollMacro m) {
        if (index >= 0 && index < macros.size()) {
            macros.set(index, m);
            save();
        }
    }

    public int getCount() {
        return macros.size();
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private void load() {
        if (!Files.exists(MACRO_FILE)) return;
        try (BufferedReader reader = Files.newBufferedReader(MACRO_FILE, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("{")) {
                    RollMacro m = RollMacro.fromJsonLine(line);
                    if (m != null) macros.add(m);
                }
            }
        } catch (IOException ignored) {}
    }

    private void save() {
        try {
            ensureDir();
            try (BufferedWriter writer = Files.newBufferedWriter(MACRO_FILE, StandardCharsets.UTF_8)) {
                for (RollMacro m : macros) {
                    writer.write(m.toJsonLine());
                    writer.newLine();
                }
            }
        } catch (IOException ignored) {}
    }

    private void ensureDir() {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException ignored) {}
    }
}
