package com.dan.fgeochart;

import java.awt.Color;

/**
 * Styler-Preset fuer FGeoChart: setzt Farb- und Anzeige-Properties in
 * einem Aufruf, ohne Model oder Timer anzufassen (Skill-Konvention 2.3).
 */
public final class FGeoChartStyler {

    public Color backgroundDark = new Color(0x10, 0x16, 0x1E);
    public Color glassRim       = new Color(140, 200, 255, 160);
    public Color hoverGlow      = new Color(0x00B5AD);
    public boolean showGrid = true;
    public boolean showStatusBar = true;
    public boolean showZoomControls = true;
    public float animationSpeed = 0.22f;

    public FGeoChartStyler() {
    }

    public void apply(FGeoChart chart) {
        chart.setBackgroundDark(backgroundDark);
        chart.setGlassRim(glassRim);
        chart.setHoverGlowColor(hoverGlow);
        chart.setShowGrid(showGrid);
        chart.setShowStatusBar(showStatusBar);
        chart.setShowZoomControls(showZoomControls);
        chart.setAnimationSpeed(animationSpeed);
    }

    /** Laboratory Dark (Standard) — Tuerkis-Akzent auf Nachtblau. */
    public static FGeoChartStyler laboratoryDark() {
        return new FGeoChartStyler();
    }

    /** Ozean-Preset — tiefes Blau, kuehler Rim. */
    public static FGeoChartStyler ocean() {
        FGeoChartStyler s = new FGeoChartStyler();
        s.backgroundDark = new Color(0x0A, 0x1A, 0x28);
        s.glassRim = new Color(120, 190, 255, 170);
        s.hoverGlow = new Color(0x35, 0xC4, 0xFF);
        return s;
    }

    /** Print-Preset — heller Hintergrund fuer Screenshots/Export. */
    public static FGeoChartStyler print() {
        FGeoChartStyler s = new FGeoChartStyler();
        s.backgroundDark = new Color(0xF4, 0xF6, 0xF9);
        s.glassRim = new Color(90, 120, 150, 160);
        s.hoverGlow = new Color(0x8B, 0x00, 0x24);
        return s;
    }
}
