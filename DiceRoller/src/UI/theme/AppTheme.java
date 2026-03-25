package UI.theme;

import java.awt.*;
import javax.swing.UIManager;

/**
 * Centralized theme: colors, fonts, and utilities.
 * All UI code reads from here — never hardcode colors/fonts elsewhere.
 */
public class AppTheme {

    // ── Dark palette (Dracula-inspired, refined) ──────────────────────────────
    public static final Color DARK_BG      = new Color(18, 19, 28);   // deepest background
    public static final Color DARK_PANEL   = new Color(28, 29, 43);   // card / panel surface
    public static final Color DARK_FIELD   = new Color(42, 44, 62);   // input fields
    public static final Color DARK_FG      = new Color(248, 248, 242);
    public static final Color DARK_ACCENT  = new Color(139, 233, 253); // cyan
    public static final Color DARK_ACCENT2 = new Color(189, 147, 249); // purple
    public static final Color DARK_SUCCESS = new Color(80, 250, 123);
    public static final Color DARK_DANGER  = new Color(255, 85, 85);
    public static final Color DARK_WARNING = new Color(255, 184, 108);
    public static final Color DARK_MUTED   = new Color(98, 114, 164);
    public static final Color DARK_BORDER  = new Color(60, 63, 84);
    public static final Color DARK_HOVER   = new Color(50, 53, 74);
    public static final Color DARK_PRESET  = new Color(34, 36, 52);
    public static final Color DARK_TABBAR  = new Color(14, 15, 22);   // tab bar strip

    // ── Light palette ────────────────────────────────────────────────────────
    public static final Color LIGHT_BG      = new Color(244, 246, 252);
    public static final Color LIGHT_PANEL   = new Color(255, 255, 255);
    public static final Color LIGHT_FIELD   = new Color(228, 232, 244);
    public static final Color LIGHT_FG      = new Color(18, 19, 28);
    public static final Color LIGHT_ACCENT  = new Color(0, 148, 178);
    public static final Color LIGHT_ACCENT2 = new Color(118, 76, 198);
    public static final Color LIGHT_SUCCESS = new Color(32, 150, 68);
    public static final Color LIGHT_DANGER  = new Color(208, 44, 44);
    public static final Color LIGHT_WARNING = new Color(192, 112, 28);
    public static final Color LIGHT_MUTED   = new Color(128, 138, 162);
    public static final Color LIGHT_BORDER  = new Color(196, 202, 220);
    public static final Color LIGHT_HOVER   = new Color(214, 220, 236);
    public static final Color LIGHT_PRESET  = new Color(220, 230, 250);
    public static final Color LIGHT_TABBAR  = new Color(230, 233, 244);

    // ── Typography (uses system font: SF Pro on macOS, Segoe UI on Windows) ──
    private static Font _base;

    private static Font base() {
        if (_base != null) return _base;
        Font sys = UIManager.getFont("Label.font");
        _base = (sys != null) ? sys : new Font("SansSerif", Font.PLAIN, 13);
        return _base;
    }

    /** Returns the system UI font at the given style/size. */
    public static Font font(float size) {
        return base().deriveFont(Font.PLAIN, size);
    }

    public static Font fontBold(float size) {
        return base().deriveFont(Font.BOLD, size);
    }

    public static Font fontItalic(float size) {
        return base().deriveFont(Font.ITALIC, size);
    }

    public static Font fontMono(float size) {
        return new Font("Monospaced", Font.PLAIN, (int) size);
    }

    // Convenience constants (still used in existing code)
    public static Font FONT_TITLE;
    public static Font FONT_RESULT;
    public static Font FONT_LARGE;
    public static Font FONT_BODY;
    public static Font FONT_BOLD;
    public static Font FONT_MONO;
    public static Font FONT_SMALL;

    static {
        // These will be overridden by refreshFonts() after UIManager is initialized,
        // but provide safe defaults in case they are accessed early.
        FONT_TITLE  = new Font("SansSerif", Font.BOLD,  22);
        FONT_RESULT = new Font("SansSerif", Font.BOLD,  52);
        FONT_LARGE  = new Font("SansSerif", Font.BOLD,  18);
        FONT_BODY   = new Font("SansSerif", Font.PLAIN, 13);
        FONT_BOLD   = new Font("SansSerif", Font.BOLD,  13);
        FONT_MONO   = new Font("Monospaced", Font.PLAIN,12);
        FONT_SMALL  = new Font("SansSerif", Font.PLAIN, 11);
    }

    /** Call once after UIManager L&F is set to pull the real system fonts. */
    public static void refreshFonts() {
        _base      = null; // reset cache
        FONT_TITLE  = fontBold(22);
        FONT_RESULT = fontBold(52);
        FONT_LARGE  = fontBold(18);
        FONT_BODY   = font(13);
        FONT_BOLD   = fontBold(13);
        FONT_MONO   = fontMono(12);
        FONT_SMALL  = font(11);
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private static boolean dark = true;

    public static boolean isDark()        { return dark; }
    public static void setDark(boolean d) { dark = d; }
    public static void toggle()           { dark = !dark; }

    // ── Dynamic color accessors ───────────────────────────────────────────────
    public static Color bg()       { return dark ? DARK_BG      : LIGHT_BG;      }
    public static Color panel()    { return dark ? DARK_PANEL   : LIGHT_PANEL;   }
    public static Color field()    { return dark ? DARK_FIELD   : LIGHT_FIELD;   }
    public static Color fg()       { return dark ? DARK_FG      : LIGHT_FG;      }
    public static Color accent()   { return dark ? DARK_ACCENT  : LIGHT_ACCENT;  }
    public static Color accent2()  { return dark ? DARK_ACCENT2 : LIGHT_ACCENT2; }
    public static Color success()  { return dark ? DARK_SUCCESS : LIGHT_SUCCESS; }
    public static Color danger()   { return dark ? DARK_DANGER  : LIGHT_DANGER;  }
    public static Color warning()  { return dark ? DARK_WARNING : LIGHT_WARNING; }
    public static Color muted()    { return dark ? DARK_MUTED   : LIGHT_MUTED;   }
    public static Color border()   { return dark ? DARK_BORDER  : LIGHT_BORDER;  }
    public static Color hover()    { return dark ? DARK_HOVER   : LIGHT_HOVER;   }
    public static Color presetBg() { return dark ? DARK_PRESET  : LIGHT_PRESET;  }
    public static Color tabbar()   { return dark ? DARK_TABBAR  : LIGHT_TABBAR;  }

    public static Color accentHover() {
        Color a = accent();
        return dark
            ? new Color(Math.min(255, a.getRed() + 22), Math.min(255, a.getGreen() + 22), Math.min(255, a.getBlue() + 22))
            : new Color(Math.max(0, a.getRed() - 22),   Math.max(0, a.getGreen() - 22),   Math.max(0, a.getBlue() - 22));
    }

    // ── Color utilities ───────────────────────────────────────────────────────

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public static Color blend(Color a, Color b, float t) {
        int r = (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bv= (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t);
        return new Color(clamp(r), clamp(g), clamp(bv));
    }

    public static Color lighten(Color c, int amount) {
        return new Color(clamp(c.getRed() + amount), clamp(c.getGreen() + amount), clamp(c.getBlue() + amount));
    }

    public static Color darken(Color c, int amount) {
        return lighten(c, -amount);
    }

    /** Background gradient: top-left slightly lighter than the base bg. */
    public static GradientPaint bgGradient(int width, int height) {
        Color top = lighten(bg(), dark ? 6 : 4);
        return new GradientPaint(0, 0, top, width, height, bg());
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
