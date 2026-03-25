package service;

import javax.sound.sampled.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Generates and plays synthesized sound effects for dice events.
 * No external audio files required — tones are generated in memory.
 */
public class SoundService {

    public enum Sound { ROLL, CRITICAL, FUMBLE, MACRO, SAVE }

    private static final int SAMPLE_RATE = 44100;
    private static final AudioFormat FORMAT =
        new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private final Map<Sound, byte[]> clips = new EnumMap<>(Sound.class);
    private boolean enabled = true;

    public SoundService() {
        try {
            clips.put(Sound.ROLL,     synthesizeNoise(120, 0.15));
            clips.put(Sound.CRITICAL, synthesizeArpeggio(new int[]{330, 440, 554, 660}, 60));
            clips.put(Sound.FUMBLE,   synthesizeTone(200, 280, true));
            clips.put(Sound.MACRO,    synthesizeTone(520, 80, false));
            clips.put(Sound.SAVE,     synthesizeArpeggio(new int[]{440, 550}, 80));
        } catch (Exception ignored) {
            // Sound is optional; silently skip
        }
    }

    public void play(Sound sound) {
        if (!enabled) return;
        byte[] data = clips.get(sound);
        if (data == null) return;
        new Thread(() -> {
            try {
                DataLine.Info info = new DataLine.Info(Clip.class, FORMAT);
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.open(FORMAT, data, 0, data.length);
                clip.start();
                // Wait for it to finish then release resources
                Thread.sleep(clip.getMicrosecondLength() / 1000 + 50);
                clip.close();
            } catch (Exception ignored) {}
        }, "dice-sound").start();
    }

    public boolean isEnabled()            { return enabled; }
    public void setEnabled(boolean b)     { enabled = b; }

    // ── Synthesis helpers ────────────────────────────────────────────────────

    /** White noise burst with fade-in/out envelope. */
    private byte[] synthesizeNoise(int durationMs, double attackFraction) {
        int samples = SAMPLE_RATE * durationMs / 1000;
        byte[] buf = new byte[samples * 2];
        Random rng = new Random();
        for (int i = 0; i < samples; i++) {
            double t = (double) i / samples;
            double env = t < attackFraction
                ? t / attackFraction
                : 1.0 - (t - attackFraction) / (1.0 - attackFraction + 1e-9);
            short sample = (short) ((rng.nextInt(65536) - 32768) * 0.35 * env);
            buf[i * 2]     = (byte) (sample & 0xFF);
            buf[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return buf;
    }

    /** Single sine-wave tone with bell envelope. descend=true adds pitch slide down. */
    private byte[] synthesizeTone(int frequency, int durationMs, boolean descend) {
        int samples = SAMPLE_RATE * durationMs / 1000;
        byte[] buf = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double t = (double) i / samples;
            double env = Math.sin(Math.PI * t);
            double freq = descend ? frequency * (1.0 - t * 0.35) : frequency;
            double angle = 2.0 * Math.PI * freq * i / SAMPLE_RATE;
            short sample = (short) (Math.sin(angle) * env * 0.75 * Short.MAX_VALUE);
            buf[i * 2]     = (byte) (sample & 0xFF);
            buf[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return buf;
    }

    /** Quick staccato arpeggio across given frequencies. */
    private byte[] synthesizeArpeggio(int[] freqs, int noteDurationMs) {
        int samplesPerNote = SAMPLE_RATE * noteDurationMs / 1000;
        int total = samplesPerNote * freqs.length;
        byte[] buf = new byte[total * 2];
        int offset = 0;
        for (int freq : freqs) {
            for (int i = 0; i < samplesPerNote; i++) {
                double t = (double) i / samplesPerNote;
                double env = Math.sin(Math.PI * t);
                double angle = 2.0 * Math.PI * freq * i / SAMPLE_RATE;
                short sample = (short) (Math.sin(angle) * env * 0.7 * Short.MAX_VALUE);
                buf[offset * 2]     = (byte) (sample & 0xFF);
                buf[offset * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
                offset++;
            }
        }
        return buf;
    }
}
