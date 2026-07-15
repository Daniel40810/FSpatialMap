package com.dan.ftooltip;

import java.awt.Color;

/**
 * Styler-Preset fuer FTooltip: setzt Farb-/Timing-Properties in einem
 * Aufruf, ohne Model oder Timer direkt anzufassen (Skill-Konvention 2.3).
 * Wirkt sowohl auf eine {@link FTooltip}-Instanz als auch optional global
 * auf den {@link FTooltipManager}.
 */
public final class FTooltipStyler {

    public Color glassBody      = new Color(20, 26, 36, 235);
    public Color glassRim       = new Color(140, 200, 255, 170);
    public float animationSpeed = 0.28f;
    public int showDelayMs = 450;
    public int hideDelayMs = 150;

    public FTooltipStyler() {
    }

    public void apply(FTooltip tip) {
        tip.setGlassBody(glassBody);
        tip.setGlassRim(glassRim);
        tip.setAnimationSpeed(animationSpeed);
    }

    /** Wendet nur die Timing-Werte global auf den Manager an. */
    public void applyTiming(FTooltipManager manager) {
        manager.setShowDelayMs(showDelayMs);
        manager.setHideDelayMs(hideDelayMs);
    }

    /** Laboratory Dark (Standard). */
    public static FTooltipStyler laboratoryDark() {
        return new FTooltipStyler();
    }

    /** Schneller, dichterer Stil fuer Entwickler-Tools (kurze Delays). */
    public static FTooltipStyler snappy() {
        FTooltipStyler s = new FTooltipStyler();
        s.showDelayMs = 150;
        s.hideDelayMs = 60;
        s.animationSpeed = 0.4f;
        return s;
    }

    /** Ruhiger Stil fuer Endnutzer-Dialoge (laengere Delays, sanftere Animation). */
    public static FTooltipStyler gentle() {
        FTooltipStyler s = new FTooltipStyler();
        s.showDelayMs = 700;
        s.hideDelayMs = 300;
        s.animationSpeed = 0.18f;
        return s;
    }
}
