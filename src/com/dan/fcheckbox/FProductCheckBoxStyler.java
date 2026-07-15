package com.dan.fcheckbox;

import java.awt.Color;

/**
 * Wiederverwendbares Aussehens-Preset für {@link FCheckBox} (Styler-Muster).
 *
 * <p>Ein Styler kapselt ein benanntes Erscheinungsbild und setzt es per
 * {@link #apply(FCheckBox)} auf eine Instanz. Styler mutieren keine Models
 * und starten keine Timer – sie setzen nur Properties; das {@code repaint()}
 * erledigen die Setter der Komponente.</p>
 *
 * <pre>{@code
 * FCheckBox cb = new FCheckBox("Aktiv");
 * FProductCheckBoxStyler.switchPrimary().apply(cb);
 * }</pre>
 */
public final class FProductCheckBoxStyler {

    public Color accentTop     = new Color(0x00, 0xB5, 0xAD);
    public Color accentBottom  = new Color(0x8B, 0x00, 0x24);
    public Color glassBody     = new Color(180, 220, 255, 38);
    public Color glassRim      = new Color(140, 200, 255, 160);
    public Color checkColor    = new Color(255, 255, 255, 235);
    public Color textColor     = new Color(228, 238, 246);
    public Color focusGlow     = new Color(0x00, 0xB5, 0xAD);
    public FCheckBoxVariant variant = FCheckBoxVariant.BOX;
    public int     boxSize     = 20;
    public int     arc         = 8;
    public int     iconTextGap = 10;
    public boolean textShadow  = true;

    public FProductCheckBoxStyler() {
    }

    /** Wendet alle Preset-Werte auf die übergebene Komponente an. */
    public void apply(FCheckBox cb) {
        if (cb == null) {
            return;
        }
        cb.setVariant(variant);
        cb.setLiquidColorTop(accentTop);
        cb.setLiquidColorBottom(accentBottom);
        cb.setGlassBody(glassBody);
        cb.setGlassRim(glassRim);
        cb.setCheckColor(checkColor);
        cb.setTextColor(textColor);
        cb.setFocusGlowColor(focusGlow);
        cb.setBoxSize(boxSize);
        cb.setArc(arc);
        cb.setIconTextGap(iconTextGap);
        cb.setTextShadowVisible(textShadow);
    }

    // ---- Benannte Presets -------------------------------------------------

    /** Standard-Glas-Box in Türkis/Weinrot. */
    public static FProductCheckBoxStyler primary() {
        return new FProductCheckBoxStyler();
    }

    /** Gefahr-/Rot-Variante (z. B. destruktive Optionen). */
    public static FProductCheckBoxStyler danger() {
        FProductCheckBoxStyler s = new FProductCheckBoxStyler();
        s.accentTop    = new Color(0xB3, 0x00, 0x1B);
        s.accentBottom = new Color(0x4D, 0x00, 0x10);
        s.focusGlow    = new Color(0xB3, 0x00, 0x1B);
        return s;
    }

    /** Kippschalter-Variante in Standardfarben. */
    public static FProductCheckBoxStyler switchPrimary() {
        FProductCheckBoxStyler s = new FProductCheckBoxStyler();
        s.variant = FCheckBoxVariant.SWITCH;
        s.arc = 12;
        return s;
    }
}
