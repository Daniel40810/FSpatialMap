package com.dan.fstyle;

/**
 * Animations-Parameter des F-Stils (Easing-Geschwindigkeiten, Timer-Takt).
 *
 * <p>Spiegelt das im {@code f-style-swing}-Skill verbindliche Easing-Idiom wider
 * ({@code progress += (target - progress) * speed}, Settle-Schwelle, ein 16&nbsp;ms-Timer).
 * Komponenten ziehen ihre Default-Geschwindigkeiten aus dieser Config statt sie
 * hart zu codieren.</p>
 *
 * <p>Felder sind absichtlich {@code public} (konsistent mit den F-Komponenten);
 * für Presets gibt es Factory-Methoden, für sichere Kopien {@link #copy()}.</p>
 *
 * @author com.dan
 */
public class FAnimationConfig {

    /** Easing beim Einblenden (Hover/Focus/Selection-Anstieg). */
    public double hoverEaseIn   = 0.18;
    /** Easing beim Ausblenden. */
    public double hoverEaseOut  = 0.14;
    /** Ausbreitungs-Easing des Ripple-Radius. */
    public double rippleSpeed   = 0.14;
    /** Alpha-Abbau des Ripples pro Frame. */
    public double rippleFade    = 0.045;
    /** Überschwing-Anteil für Reveal-Animationen (Charts), 0.0&ndash;0.30. */
    public double overshoot     = 0.10;
    /** Settle-Schwelle: darunter rastet {@code progress} auf {@code target} ein. */
    public double settleThreshold = 0.004;
    /** Timer-Intervall in ms (Standard ~60&nbsp;fps). */
    public int    timerIntervalMs = 16;

    public FAnimationConfig() { }

    /** Standard-Profil. */
    public static FAnimationConfig defaults() {
        return new FAnimationConfig();
    }

    /** Schnelleres, knackigeres Profil. */
    public static FAnimationConfig fast() {
        FAnimationConfig c = new FAnimationConfig();
        c.hoverEaseIn  = 0.28;
        c.hoverEaseOut = 0.22;
        c.rippleSpeed  = 0.22;
        c.rippleFade   = 0.060;
        return c;
    }

    /** Langsameres, weicheres Profil. */
    public static FAnimationConfig slow() {
        FAnimationConfig c = new FAnimationConfig();
        c.hoverEaseIn  = 0.10;
        c.hoverEaseOut = 0.08;
        c.rippleSpeed  = 0.08;
        c.rippleFade   = 0.030;
        return c;
    }

    /** "Reduzierte Bewegung": (fast) sofortige Übergänge, kein Überschwingen. */
    public static FAnimationConfig reducedMotion() {
        FAnimationConfig c = new FAnimationConfig();
        c.hoverEaseIn  = 0.9;
        c.hoverEaseOut = 0.9;
        c.rippleSpeed  = 0.9;
        c.rippleFade   = 0.5;
        c.overshoot    = 0.0;
        return c;
    }

    /** Tiefe Kopie. */
    public FAnimationConfig copy() {
        FAnimationConfig c = new FAnimationConfig();
        c.hoverEaseIn     = hoverEaseIn;
        c.hoverEaseOut    = hoverEaseOut;
        c.rippleSpeed     = rippleSpeed;
        c.rippleFade      = rippleFade;
        c.overshoot       = overshoot;
        c.settleThreshold = settleThreshold;
        c.timerIntervalMs = timerIntervalMs;
        return c;
    }
}
