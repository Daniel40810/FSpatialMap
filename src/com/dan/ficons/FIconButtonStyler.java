package com.dan.ficons;

import java.awt.Color;
import java.awt.Dimension;

/**
 * Wiederverwendbares Aussehens-/Verhaltens-Preset für {@link FIconComponent}.
 * Setzt in einem Aufruf konsistente Toolbar-Defaults (Größe, Glow, Easing) und
 * hält die Komponente selbst schlank — gemäß der Model/Enum/Styler-Trennung.
 *
 * <p>JavaBean: public no-arg Konstruktor, konfigurierbare Felder, statische
 * Presets. Der Styler mutiert keine Models und startet keine Timer; er setzt
 * nur Properties (deren Setter ggf. {@code repaint()} auslösen).</p>
 *
 * @author com.dan
 */
public final class FIconButtonStyler {

    public Color glowColor       = new Color(0x00, 0xB5, 0xAD); // Türkis
    public Color activeGlowColor = new Color(0x8B, 0x00, 0x24); // Weinrot für "aktiv"
    public int   iconSize        = 30;   // px Symbol (+ Gap)
    public int   iconGap         = 7;
    public float maxGlowAlpha    = 120f;
    public boolean hoverGlow     = true;
    public boolean press         = true;

    public FIconButtonStyler() { }

    /** Wendet das Preset auf eine Icon-Komponente an. */
    public void apply(FIconComponent c) {
        c.setGlowColor(glowColor);
        c.setActiveGlowColor(activeGlowColor);
        c.setHoverGlowEnabled(hoverGlow);
        c.setPressEnabled(press);
        c.setMaxGlowAlpha(maxGlowAlpha);
        c.setIconGap(iconGap);
        c.setPreferredSize(new Dimension(iconSize + 2 * iconGap, iconSize + 2 * iconGap));
    }

    /** Bequemer Ein-Zeiler: Komponente mit Typ erzeugen und stylen. */
    public FIconComponent build(FIconType type) {
        FIconComponent c = new FIconComponent(type);
        apply(c);
        return c;
    }

    // ----------------------------------------------------- Presets

    /** Türkis-Toolbar-Standard. */
    public static FIconButtonStyler toolbar() {
        return new FIconButtonStyler();
    }

    /** Kompaktes Menü-Preset (kleiner, dezenterer Glow). */
    public static FIconButtonStyler menu() {
        FIconButtonStyler s = new FIconButtonStyler();
        s.iconSize = 22;
        s.iconGap = 6;
        s.maxGlowAlpha = 90f;
        return s;
    }

    /** Großes, betontes Akzent-Icon (Weinrot-Glow). */
    public static FIconButtonStyler accent() {
        FIconButtonStyler s = new FIconButtonStyler();
        s.iconSize = 40;
        s.iconGap = 9;
        s.glowColor = new Color(0x8B, 0x00, 0x24);
        s.activeGlowColor = new Color(0xB3, 0x00, 0x1B);
        s.maxGlowAlpha = 140f;
        return s;
    }
}
