package com.dan.fstyle;

import java.awt.Color;

/**
 * Vordefinierte Farb-/Schrift-Sets für {@link FTheme}.
 *
 * <p>Jedes Preset ist ein reiner Daten-Container mit der vollständigen Palette.
 * {@link FTheme#applyPreset(FThemePreset)} liest die Werte und übernimmt sie in
 * einem Rutsch (eine einzige Listener-Benachrichtigung).</p>
 *
 * @author com.dan
 */
public enum FThemePreset {

    /** Standard: ReagenzglasBar / Laboratory Dark (Türkis &amp; Weinrot). */
    LABORATORY_DARK(
            "Labor Dark (Standard)",
            /* background     */ new Color(0x12, 0x14, 0x1E),
            /* surface        */ new Color(0x1B, 0x25, 0x30),
            /* primary        */ new Color(0x00, 0xB5, 0xAD),
            /* accent         */ new Color(0x8B, 0x00, 0x24),
            /* text           */ new Color(0xCF, 0xE9, 0xF2),
            /* textMuted      */ new Color(0x8F, 0xA9, 0xB6),
            /* selectionBg    */ new Color(0x00, 0xB5, 0xAD),
            /* selectionFg    */ new Color(0x07, 0x1A, 0x1C),
            /* glassRim       */ new Color(140, 200, 255, 160),
            /* glassBody      */ new Color(180, 220, 255, 35),
            /* glassHighlight */ new Color(255, 255, 255, 110),
            /* shadow         */ new Color(0, 0, 0, 70),
            /* ripple         */ new Color(200, 240, 255, 200),
            /* glow           */ new Color(0x00, 0xB5, 0xAD, 120),
            /* fontFamily     */ "Segoe UI"),

    /** Mitternachtsblau mit Orange-Akzent. */
    MIDNIGHT_BLUE(
            "Mitternachtsblau",
            new Color(0x0C, 0x14, 0x24),
            new Color(0x14, 0x20, 0x3A),
            new Color(0x1E, 0x7A, 0xFF),
            new Color(0xFF, 0x6B, 0x00),
            new Color(0xE0, 0xF0, 0xFF),
            new Color(0x93, 0xAF, 0xC9),
            new Color(0x1E, 0x7A, 0xFF),
            new Color(0x04, 0x0C, 0x1A),
            new Color(150, 190, 255, 160),
            new Color(170, 210, 255, 35),
            new Color(255, 255, 255, 110),
            new Color(0, 0, 0, 100),
            new Color(200, 225, 255, 200),
            new Color(0x1E, 0x7A, 0xFF, 120),
            "Segoe UI"),

    /** Neon-Cyberpunk (Magenta &amp; Cyan). */
    NEON_CYBERPUNK(
            "Neon-Cyberpunk",
            new Color(0x0A, 0x07, 0x10),
            new Color(0x17, 0x0A, 0x22),
            new Color(0xFF, 0x2B, 0xD6),
            new Color(0x16, 0xF2, 0xE6),
            new Color(0xF5, 0xE9, 0xFF),
            new Color(0xB8, 0x9A, 0xD6),
            new Color(0xFF, 0x2B, 0xD6),
            new Color(0x10, 0x06, 0x18),
            new Color(255, 120, 230, 150),
            new Color(180, 120, 255, 40),
            new Color(255, 255, 255, 120),
            new Color(0, 0, 0, 130),
            new Color(255, 180, 250, 210),
            new Color(0xFF, 0x2B, 0xD6, 130),
            "Segoe UI");

    // ---- Palette (package-private, von FTheme gelesen) ----
    final String displayName;
    final Color background;
    final Color surface;
    final Color primary;
    final Color accent;
    final Color text;
    final Color textMuted;
    final Color selectionBackground;
    final Color selectionForeground;
    final Color glassRim;
    final Color glassBody;
    final Color glassHighlight;
    final Color shadow;
    final Color ripple;
    final Color glow;
    final String fontFamily;

    FThemePreset(String displayName,
                 Color background, Color surface, Color primary, Color accent,
                 Color text, Color textMuted,
                 Color selectionBackground, Color selectionForeground,
                 Color glassRim, Color glassBody, Color glassHighlight,
                 Color shadow, Color ripple, Color glow,
                 String fontFamily) {
        this.displayName = displayName;
        this.background = background;
        this.surface = surface;
        this.primary = primary;
        this.accent = accent;
        this.text = text;
        this.textMuted = textMuted;
        this.selectionBackground = selectionBackground;
        this.selectionForeground = selectionForeground;
        this.glassRim = glassRim;
        this.glassBody = glassBody;
        this.glassHighlight = glassHighlight;
        this.shadow = shadow;
        this.ripple = ripple;
        this.glow = glow;
        this.fontFamily = fontFamily;
    }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}
