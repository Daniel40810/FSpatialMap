package com.dan.fslider;

import java.awt.Color;

/**
 * Wiederverwendbares Aussehens-Preset fuer {@link FSlider}. Setzt Liquid-Farben,
 * Glas-Rim, Variante und Thumb-Groesse in einem {@link #apply(FSlider)}-Aufruf.
 * Mutiert kein Model und startet keine Timer.
 *
 * Java 8 kompatibel.
 */
public final class FSliderStyler {

    public Color liquidColorTop    = new Color(0x00, 0xB5, 0xAD);
    public Color liquidColorBottom = new Color(0x8B, 0x00, 0x24);
    public Color glassRim          = new Color(0x8C, 0xC8, 0xFF, 160);
    public Color glowColor         = new Color(0x00, 0xB5, 0xAD, 120);
    public int   arc               = 16;
    public int   thumbSize         = 22;
    public FSliderVariant variant  = FSliderVariant.STANDARD;

    public FSliderStyler() { }

    public void apply(FSlider s) {
        s.setLiquidColorTop(liquidColorTop);
        s.setLiquidColorBottom(liquidColorBottom);
        s.setGlassRim(glassRim);
        s.setGlowColor(glowColor);
        s.setArc(arc);
        s.setThumbSize(thumbSize);
        s.setVariant(variant);
    }

    // ---- Statische Presets ----------------------------------------------

    /** Laboratory Dark: Tuerkis -> Weinrot (Standard). */
    public static FSliderStyler laboratory() {
        return new FSliderStyler();
    }

    /** Neon-Cyan mit kraeftigem Glow. */
    public static FSliderStyler neon() {
        FSliderStyler s = new FSliderStyler();
        s.liquidColorTop    = new Color(0x00, 0xE5, 0xFF);
        s.liquidColorBottom = new Color(0x00, 0x3C, 0x8B);
        s.glowColor         = new Color(0x00, 0xE5, 0xFF, 150);
        s.glassRim          = new Color(0xB0, 0xF4, 0xFF, 180);
        return s;
    }

    /** Feurig: Orange -> Weinrot. */
    public static FSliderStyler fire() {
        FSliderStyler s = new FSliderStyler();
        s.liquidColorTop    = new Color(0xFF, 0x8C, 0x00);
        s.liquidColorBottom = new Color(0x8B, 0x00, 0x24);
        s.glowColor         = new Color(0xFF, 0x8C, 0x00, 130);
        return s;
    }

    /** Smaragd: Hellgruen -> Dunkelgruen. */
    public static FSliderStyler emerald() {
        FSliderStyler s = new FSliderStyler();
        s.liquidColorTop    = new Color(0x00, 0xCC, 0x66);
        s.liquidColorBottom = new Color(0x00, 0x44, 0x22);
        s.glowColor         = new Color(0x00, 0xCC, 0x66, 130);
        return s;
    }

    /** Schlank, ruhig, ohne Blasen &ndash; fuer Toolbars/Formulare. */
    public static FSliderStyler minimal() {
        FSliderStyler s = new FSliderStyler();
        s.variant   = FSliderVariant.MINIMAL;
        s.thumbSize = 18;
        s.arc       = 10;
        return s;
    }

    /** Mit Skala und Wertebereich-Anzeige. */
    public static FSliderStyler ticks() {
        FSliderStyler s = new FSliderStyler();
        s.variant = FSliderVariant.TICKS;
        return s;
    }
}
