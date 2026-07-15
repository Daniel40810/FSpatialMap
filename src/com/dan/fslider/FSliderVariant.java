package com.dan.fslider;

/**
 * Erscheinungsbild-Varianten fuer den {@link FSlider}.
 *
 * <ul>
 *   <li>{@code STANDARD} &ndash; dicker Glas-Track mit lebendiger Liquid-Welle,
 *       Schaumkrone und aufsteigenden Blasen.</li>
 *   <li>{@code MINIMAL} &ndash; schlanker Track, ruhige Welle, keine Blasen &ndash;
 *       fuer dichte Formulare und Toolbars.</li>
 *   <li>{@code TICKS} &ndash; wie STANDARD, zusaetzlich mit gemalten F-Style-Skalenstrichen
 *       und Min/Wert/Max-Beschriftung fuer die Wertebereich-Anzeige.</li>
 * </ul>
 *
 * Java 8 kompatibel (kein var, keine Switch-Expression).
 */
public enum FSliderVariant {

    STANDARD(14, true,  false),
    MINIMAL ( 8, false, false),
    TICKS   (14, true,  true);

    /** Dicke des Liquid-Tracks in Pixeln (Querachse). */
    public final int trackThickness;
    /** Ob Schaumkrone und aufsteigende Blasen gezeichnet werden. */
    public final boolean showBubbles;
    /** Ob Skalenstriche und Wertebereich-Labels gezeichnet werden. */
    public final boolean showTicks;

    FSliderVariant(int trackThickness, boolean showBubbles, boolean showTicks) {
        this.trackThickness = trackThickness;
        this.showBubbles = showBubbles;
        this.showTicks = showTicks;
    }
}
