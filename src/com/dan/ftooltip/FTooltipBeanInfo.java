package com.dan.ftooltip;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo fuer die NetBeans-Palette: programmatisch gemaltes Icon
 * (Sprechblase mit Pfeil, Tuerkis→Wein-Gradient, Glas-Rim).
 */
public class FTooltipBeanInfo extends SimpleBeanInfo {

    @Override
    public Image getIcon(int iconKind) {
        switch (iconKind) {
            case ICON_COLOR_16x16:
            case ICON_MONO_16x16:
                return paintIcon(16);
            case ICON_COLOR_32x32:
            case ICON_MONO_32x32:
                return paintIcon(32);
            default:
                return null;
        }
    }

    private Image paintIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float s = size / 32f;

            RoundRectangle2D bubble = new RoundRectangle2D.Float(
                    2f * s, 2f * s, 28f * s, 20f * s, 9f * s, 9f * s);

            g.setPaint(new GradientPaint(0, 0, new Color(0x00, 0xB5, 0xAD),
                    0, size, new Color(0x8B, 0x00, 0x24)));
            g.fill(bubble);

            // Pfeil unten links
            Path2D arrow = new Path2D.Float();
            arrow.moveTo(9f * s, 21.5f * s);
            arrow.lineTo(9f * s, 28f * s);
            arrow.lineTo(15f * s, 21.5f * s);
            arrow.closePath();
            g.setPaint(new GradientPaint(0, 18 * s, new Color(0x00, 0x8A, 0x84),
                    0, 28 * s, new Color(0x6E, 0x00, 0x1D)));
            g.fill(arrow);

            // Drei Text-Punkte (Inhalt-Andeutung)
            g.setColor(new Color(255, 255, 255, 230));
            float dotR = 1.6f * s;
            float dy = 12f * s;
            g.fill(new Ellipse2D.Float(9f * s - dotR, dy - dotR, dotR * 2, dotR * 2));
            g.fill(new Ellipse2D.Float(16f * s - dotR, dy - dotR, dotR * 2, dotR * 2));
            g.fill(new Ellipse2D.Float(23f * s - dotR, dy - dotR, dotR * 2, dotR * 2));

            // Shine oben
            g.setColor(new Color(255, 255, 255, 55));
            g.fill(new RoundRectangle2D.Float(3f * s, 3f * s, 26f * s, 7f * s, 6f * s, 6f * s));

            // Glas-Rim
            g.setColor(new Color(180, 225, 255, 200));
            g.setStroke(new BasicStroke(Math.max(1f, 1.5f * s)));
            g.draw(bubble);
            g.draw(arrow);
        } finally {
            g.dispose();
        }
        return img;
    }
}
