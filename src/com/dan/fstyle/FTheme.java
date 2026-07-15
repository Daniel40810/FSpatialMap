package com.dan.fstyle;

import java.awt.Color;
import java.awt.Font;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Zentrale Stil-Registry des F-Stils (ReagenzglasBar / Laboratory Dark).
 *
 * <p><b>Eine Stelle</b> für Farben, Typografie, Animations- und Geometrie-Defaults
 * der gesamten {@code com.dan}-Komponentenbibliothek &mdash; statt überall
 * hartcodierter Werte. Komponenten/Styler lesen ihre Defaults hier; ein
 * {@link ThemeListener} erlaubt Live-Umschalten zur Laufzeit.</p>
 *
 * <pre>{@code
 * FTheme t = FTheme.getInstance();
 * t.applyPreset(FThemePreset.MIDNIGHT_BLUE);   // ganze Palette + Schrift
 * t.setPrimary(new Color(0x00, 0xC8, 0xB0));    // einzelne Farbe überschreiben
 * Font f = t.getFont(FFontRole.TITLE);          // Schrift per Rolle
 * }</pre>
 *
 * <p>Thread-Sicherheit: {@link #getInstance()} ist synchronisiert; Listener
 * liegen in einer {@link CopyOnWriteArrayList}, sodass Komponenten sich während
 * einer Benachrichtigung gefahrlos ab-/anmelden können. Farben/Configs sind für
 * den EDT gedacht.</p>
 *
 * @author com.dan
 */
public final class FTheme {

    private static FTheme instance;

    // ---- aktuelles Preset (informativ) ----
    private FThemePreset currentPreset = FThemePreset.LABORATORY_DARK;
    private boolean darkMode = true;

    // ---- Farbpalette ----
    private Color background;
    private Color surface;
    private Color primary;
    private Color accent;
    private Color text;
    private Color textMuted;
    private Color selectionBackground;
    private Color selectionForeground;
    private Color glassRim;
    private Color glassBody;
    private Color glassHighlight;
    private Color shadow;
    private Color ripple;
    private Color glow;

    /** Max-Deckkraft der Glas-Füllfläche (0..255), z.&nbsp;B. FLabel.bgAlpha. */
    private float glassFillAlpha = 70f;

    // ---- delegierte Configs ----
    private FTypography     typography     = new FTypography();
    private FAnimationConfig animation     = new FAnimationConfig();
    private FGeometryConfig  geometry      = new FGeometryConfig();

    // ---- Listener ----
    private final CopyOnWriteArrayList<ThemeListener> listeners =
            new CopyOnWriteArrayList<ThemeListener>();

    private FTheme() {
        loadPaletteFrom(FThemePreset.LABORATORY_DARK);
    }

    /** Globale Instanz. */
    public static synchronized FTheme getInstance() {
        if (instance == null) instance = new FTheme();
        return instance;
    }

    // ============================================================== Presets

    /** Übernimmt die komplette Palette + Schriftfamilie eines Presets (eine Notify). */
    public void applyPreset(FThemePreset preset) {
        if (preset == null) return;
        loadPaletteFrom(preset);
        notifyListeners();
    }

    public FThemePreset getCurrentPreset() { return currentPreset; }

    /** Setzt Palette, Typografie, Animation und Geometrie auf die Standardwerte zurück. */
    public void reset() {
        loadPaletteFrom(FThemePreset.LABORATORY_DARK);
        typography = new FTypography();
        animation  = new FAnimationConfig();
        geometry   = new FGeometryConfig();
        glassFillAlpha = 70f;
        darkMode = true;
        notifyListeners();
    }

    private void loadPaletteFrom(FThemePreset p) {
        currentPreset       = p;
        background          = p.background;
        surface             = p.surface;
        primary             = p.primary;
        accent              = p.accent;
        text                = p.text;
        textMuted           = p.textMuted;
        selectionBackground = p.selectionBackground;
        selectionForeground = p.selectionForeground;
        glassRim            = p.glassRim;
        glassBody           = p.glassBody;
        glassHighlight      = p.glassHighlight;
        shadow              = p.shadow;
        ripple              = p.ripple;
        glow                = p.glow;
        if (p.fontFamily != null) typography.setFontFamily(p.fontFamily);
    }

    // ============================================================== Farben

    public Color getBackground() { return background; }
    public void setBackground(Color c) { this.background = c; notifyListeners(); }

    public Color getSurface() { return surface; }
    public void setSurface(Color c) { this.surface = c; notifyListeners(); }

    public Color getPrimary() { return primary; }
    public void setPrimary(Color c) { this.primary = c; notifyListeners(); }

    public Color getAccent() { return accent; }
    public void setAccent(Color c) { this.accent = c; notifyListeners(); }

    public Color getText() { return text; }
    public void setText(Color c) { this.text = c; notifyListeners(); }

    public Color getTextMuted() { return textMuted; }
    public void setTextMuted(Color c) { this.textMuted = c; notifyListeners(); }

    public Color getSelectionBackground() { return selectionBackground; }
    public void setSelectionBackground(Color c) { this.selectionBackground = c; notifyListeners(); }

    public Color getSelectionForeground() { return selectionForeground; }
    public void setSelectionForeground(Color c) { this.selectionForeground = c; notifyListeners(); }

    public Color getGlassRim() { return glassRim; }
    public void setGlassRim(Color c) { this.glassRim = c; notifyListeners(); }

    public Color getGlassBody() { return glassBody; }
    public void setGlassBody(Color c) { this.glassBody = c; notifyListeners(); }

    public Color getGlassHighlight() { return glassHighlight; }
    public void setGlassHighlight(Color c) { this.glassHighlight = c; notifyListeners(); }

    public Color getShadow() { return shadow; }
    public void setShadow(Color c) { this.shadow = c; notifyListeners(); }

    public Color getRipple() { return ripple; }
    public void setRipple(Color c) { this.ripple = c; notifyListeners(); }

    public Color getGlow() { return glow; }
    public void setGlow(Color c) { this.glow = c; notifyListeners(); }

    public float getGlassFillAlpha() { return glassFillAlpha; }
    public void setGlassFillAlpha(float a) {
        this.glassFillAlpha = a < 0f ? 0f : (a > 255f ? 255f : a);
        notifyListeners();
    }

    public boolean isDarkMode() { return darkMode; }
    public void setDarkMode(boolean b) { this.darkMode = b; notifyListeners(); }

    // ============================================================== Typografie

    public FTypography getTypography() { return typography; }
    public void setTypography(FTypography t) {
        if (t != null) { this.typography = t; notifyListeners(); }
    }

    /** Kurzweg: Schrift für eine Rolle. */
    public Font getFont(FFontRole role) { return typography.getFont(role); }

    /** Kurzweg: bevorzugte Schriftfamilie setzen (mit Fallback-Auflösung). */
    public void setFontFamily(String family) {
        typography.setFontFamily(family);
        notifyListeners();
    }

    // ============================================================== Animation/Geometrie

    public FAnimationConfig getAnimationConfig() { return animation; }
    public void setAnimationConfig(FAnimationConfig c) {
        if (c != null) { this.animation = c; notifyListeners(); }
    }

    public FGeometryConfig getGeometryConfig() { return geometry; }
    public void setGeometryConfig(FGeometryConfig c) {
        if (c != null) { this.geometry = c; notifyListeners(); }
    }

    // ============================================================== Listener

    public void addThemeListener(ThemeListener l) {
        if (l != null) listeners.addIfAbsent(l);
    }

    public void removeThemeListener(ThemeListener l) {
        listeners.remove(l);
    }

    /**
     * Manuelles Auslösen einer Benachrichtigung &mdash; nötig, nachdem ein über
     * {@link #getAnimationConfig()}/{@link #getGeometryConfig()}/{@link #getTypography()}
     * geholtes Objekt direkt (in-place) verändert wurde.
     */
    public void refresh() { notifyListeners(); }

    private void notifyListeners() {
        for (ThemeListener l : listeners) {
            l.themeChanged(this);
        }
    }

    /** Callback bei jeder Theme-Änderung. */
    public interface ThemeListener {
        void themeChanged(FTheme theme);
    }
}
