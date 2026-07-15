package com.dan.fcolorpalette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo fuer die NetBeans-Palette. Malt ein kompaktes Symbol: drei
 * Farbkacheln im Türkis→Weinrot-Verlauf hinter einem Glasrahmen — Anspielung
 * auf die Paletten-/Swatch-Funktion der Komponente.
 */
public class FColorPaletteEditorBeanInfo extends SimpleBeanInfo {

    private static final Color TURQUOISE = new Color(0x00, 0xB5, 0xAD);
    private static final Color WINE_RED = new Color(0x8B, 0x00, 0x24);

    @Override
    public java.awt.Image getIcon(int kind) {
        int dim = (kind == BeanInfo.ICON_COLOR_32x32 || kind == BeanInfo.ICON_MONO_32x32) ? 32 : 16;
        return paint(dim);
    }

    private java.awt.Image paint(int dim) {
        BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float pad = dim * 0.1f;
        float tileW = (dim - 2 * pad) / 3f;
        float tileH = dim - 2 * pad;
        for (int i = 0; i < 3; i++) {
            float t = i / 2f;
            Color c = blend(TURQUOISE, WINE_RED, t);
            g2.setColor(c);
            g2.fillRoundRect(Math.round(pad + i * tileW), Math.round(pad),
                    Math.round(tileW - 1), Math.round(tileH), Math.round(dim * 0.12f), Math.round(dim * 0.12f));
        }
        g2.setStroke(new java.awt.BasicStroke(Math.max(1f, dim * 0.045f)));
        g2.setColor(new Color(140, 200, 255, 150));
        g2.drawRoundRect(Math.round(pad), Math.round(pad), Math.round(dim - 2 * pad), Math.round(dim - 2 * pad),
                Math.round(dim * 0.18f), Math.round(dim * 0.18f));
        g2.dispose();
        return img;
    }

    private static Color blend(Color a, Color b, float t) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }
}
