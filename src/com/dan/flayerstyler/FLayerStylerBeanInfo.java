package com.dan.flayerstyler;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo fuer die NetBeans-Palette. Malt ein kompaktes Symbol: Glasrahmen
 * mit zwei bunten Punkten (eingefaerbte Features) und einem gestrichelten
 * (ungefaerbten) — Anspielung auf die Kernfunktion des FLayerStyler.
 */
public class FLayerStylerBeanInfo extends SimpleBeanInfo {

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

        float pad = dim * 0.14f;
        g2.setStroke(new java.awt.BasicStroke(Math.max(1f, dim * 0.05f)));
        g2.setColor(new Color(140, 200, 255, 150));
        g2.drawRoundRect(Math.round(pad), Math.round(pad), Math.round(dim - 2 * pad), Math.round(dim - 2 * pad),
                Math.round(dim * 0.2f), Math.round(dim * 0.2f));

        float r = dim * 0.11f;
        drawDot(g2, dim * 0.32f, dim * 0.38f, r, TURQUOISE, true);
        drawDot(g2, dim * 0.62f, dim * 0.62f, r, WINE_RED, true);
        drawDot(g2, dim * 0.68f, dim * 0.30f, r * 0.85f, new Color(140, 200, 255, 150), false);

        g2.dispose();
        return img;
    }

    private void drawDot(Graphics2D g2, float cx, float cy, float r, Color c, boolean filled) {
        Ellipse2D dot = new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2);
        if (filled) {
            g2.setColor(c);
            g2.fill(dot);
        } else {
            g2.setStroke(new java.awt.BasicStroke(1f, java.awt.BasicStroke.CAP_BUTT,
                    java.awt.BasicStroke.JOIN_MITER, 1f, new float[]{2f, 1.5f}, 0f));
            g2.setColor(c);
            g2.draw(dot);
        }
    }
}
