package com.dan.ficons;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo für {@link FIconComponent}. Palette-Icons (16&times;16 und 32&times;32)
 * werden vollständig programmatisch gemalt — kein externes Bild-Asset.
 *
 * @author com.dan
 */
public class FIconComponentBeanInfo extends SimpleBeanInfo {

    @Override
    public Image getIcon(int kind) {
        if (kind == ICON_COLOR_16x16 || kind == ICON_MONO_16x16) return paintPaletteIcon(16);
        if (kind == ICON_COLOR_32x32 || kind == ICON_MONO_32x32) return paintPaletteIcon(32);
        return null;
    }

    /** Gerundeter Glas-Hintergrund + repräsentatives Geo-Glyph als Palette-Marke. */
    private Image paintPaletteIcon(int dim) {
        BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            float pad = dim * 0.06f, sz = dim - 2 * pad, arc = dim * 0.28f;
            RoundRectangle2D bg = new RoundRectangle2D.Float(pad, pad, sz, sz, arc, arc);
            g2.setColor(new Color(0x14, 0x1A, 0x20));
            g2.fill(bg);
            g2.setColor(new Color(140, 200, 255, 110));
            g2.draw(bg);
            // Glyph leicht eingerückt
            int inner = (int) (dim * 0.72f);
            int off = (dim - inner) / 2;
            g2.translate(off, off);
            FIconType.GEO.paintGlyph(g2, inner);
        } finally {
            g2.dispose();
        }
        return img;
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            PropertyDescriptor type   = new PropertyDescriptor("type", FIconComponent.class);
            type.setDisplayName("Symbol");
            type.setShortDescription("Standard-Symbol aus der F-Style-Bibliothek");

            PropertyDescriptor active = new PropertyDescriptor("active", FIconComponent.class);
            active.setDisplayName("Aktiv (Puls)");
            active.setShortDescription("Dauerhafter pulsierender Glow für aktive Icons");

            PropertyDescriptor glow   = new PropertyDescriptor("glowColor", FIconComponent.class);
            glow.setDisplayName("Glow-Farbe");

            PropertyDescriptor hov    = new PropertyDescriptor("hoverGlowEnabled", FIconComponent.class);
            hov.setDisplayName("Hover-Glow");

            PropertyDescriptor anim   = new PropertyDescriptor("animated", FIconComponent.class);
            anim.setDisplayName("Animiert");

            return new PropertyDescriptor[]{ type, active, glow, hov, anim };
        } catch (IntrospectionException e) {
            return null;   // Introspection übernimmt den Rest
        }
    }

    @Override
    public BeanInfo[] getAdditionalBeanInfo() {
        return null;
    }
}
