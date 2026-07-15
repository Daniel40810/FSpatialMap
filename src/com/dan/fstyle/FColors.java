package com.dan.fstyle;

import java.awt.Color;

/**
 * Statische Farb-Hilfsfunktionen für den F-Stil.
 *
 * <p>Bündelt die kleinen Farb-Operationen, die bisher in jeder F-Komponente
 * dupliziert wurden ({@code withAlpha}, {@code blend} &hellip;). Reine
 * Utility-Klasse &mdash; nicht instanziierbar, keine Abhängigkeiten außer
 * {@code java.awt.Color}.</p>
 *
 * @author com.dan
 */
public final class FColors {

    private FColors() { }

    /** Gibt {@code c} mit dem absoluten Alpha {@code a} (0..255) zurück. */
    public static Color withAlpha(Color c, int a) {
        a = clampInt(a, 0, 255);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    /** Gibt {@code c} mit dem relativen Alpha {@code a01} (0.0..1.0) zurück. */
    public static Color withAlpha(Color c, float a01) {
        return withAlpha(c, Math.round(clampF(a01, 0f, 1f) * 255f));
    }

    /** Skaliert das vorhandene Alpha von {@code c} mit {@code factor} (0..1). */
    public static Color scaleAlpha(Color c, float factor) {
        return withAlpha(c, Math.round(c.getAlpha() * clampF(factor, 0f, 1f)));
    }

    /**
     * Lineare Mischung zwischen {@code a} (t=0) und {@code b} (t=1),
     * inklusive Alpha-Kanal.
     */
    public static Color blend(Color a, Color b, float t) {
        t = clampF(t, 0f, 1f);
        return new Color(
                lerp(a.getRed(),   b.getRed(),   t),
                lerp(a.getGreen(), b.getGreen(), t),
                lerp(a.getBlue(),  b.getBlue(),  t),
                lerp(a.getAlpha(), b.getAlpha(), t));
    }

    /** Hellt {@code c} um {@code amt} (0..1) Richtung Weiß auf. Alpha bleibt. */
    public static Color lighten(Color c, float amt) {
        Color out = blend(c, Color.WHITE, amt);
        return withAlpha(out, c.getAlpha());
    }

    /** Dunkelt {@code c} um {@code amt} (0..1) Richtung Schwarz ab. Alpha bleibt. */
    public static Color darken(Color c, float amt) {
        Color out = blend(c, Color.BLACK, amt);
        return withAlpha(out, c.getAlpha());
    }

    /**
     * Parst {@code "#RRGGBB"} oder {@code "#AARRGGBB"} (das {@code #} ist
     * optional) in eine {@link Color}.
     *
     * @throws IllegalArgumentException bei ungültigem Format
     */
    public static Color hex(String hex) {
        if (hex == null) throw new IllegalArgumentException("hex == null");
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        long v;
        try {
            v = Long.parseLong(s, 16);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Ungültige Hex-Farbe: " + hex);
        }
        if (s.length() == 6) {
            return new Color((int) ((v >> 16) & 0xFF),
                             (int) ((v >> 8) & 0xFF),
                             (int) (v & 0xFF));
        } else if (s.length() == 8) {
            return new Color((int) ((v >> 16) & 0xFF),
                             (int) ((v >> 8) & 0xFF),
                             (int) (v & 0xFF),
                             (int) ((v >> 24) & 0xFF));
        }
        throw new IllegalArgumentException("Hex muss 6 oder 8 Stellen haben: " + hex);
    }

    /** Wandelt {@code c} in {@code "#AARRGGBB"}. */
    public static String toHex(Color c) {
        return String.format("#%02X%02X%02X%02X",
                c.getAlpha(), c.getRed(), c.getGreen(), c.getBlue());
    }

    // ---------------------------------------------------------------- intern

    private static int lerp(int a, int b, float t) {
        return clampInt(Math.round(a + (b - a) * t), 0, 255);
    }

    private static int clampInt(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static float clampF(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
