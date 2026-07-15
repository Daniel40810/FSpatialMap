package com.dan.fscrollpane;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.SimpleBeanInfo;

/**
 * Palette-Icon fuer {@link FScrollPane}: abgerundeter Glaskoerper mit
 * Shine-Streifen und schlanker Tuerkis-Scrollbar rechts.
 */
public class FScrollPaneBeanInfo extends SimpleBeanInfo {

    @Override
    public java.awt.Image getIcon(int iconKind) {
        switch (iconKind) {
            case BeanInfo.ICON_COLOR_16x16:
            case BeanInfo.ICON_MONO_16x16:
                return paint(16);
            case BeanInfo.ICON_COLOR_32x32:
            case BeanInfo.ICON_MONO_32x32:
                return paint(32);
            default:
                return null;
        }
    }

    private BufferedImage paint(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float scale = s / 32f;
        int pad = Math.round(2 * scale);
        int w   = s - 2 * pad;
        int h   = s - 2 * pad;
        int arc = Math.round(7 * scale);

        // Koerper
        g.setColor(new Color(0x141A24));
        g.fillRoundRect(pad, pad, w, h, arc, arc);

        // Shine oben
        java.awt.Shape oldClip = g.getClip();
        g.clip(new java.awt.geom.RoundRectangle2D.Float(pad, pad, w, h, arc, arc));
        g.setPaint(new GradientPaint(0, pad, new Color(255, 255, 255, 40),
                0, pad + h / 4f, new Color(255, 255, 255, 0)));
        g.fillRect(pad, pad, w, h / 3);
        g.setClip(oldClip);

        // Scrollbar-Thumb rechts
        int sbW = Math.max(2, Math.round(3 * scale));
        int sbX = pad + w - sbW - Math.round(2 * scale);
        int sbY = pad + Math.round(4 * scale);
        int sbH = h - Math.round(8 * scale);
        g.setColor(new Color(0x00B5AD));
        g.fillRoundRect(sbX, sbY, sbW, sbH, sbW, sbW);

        // Rim
        g.setColor(new Color(140, 200, 255, 150));
        g.drawRoundRect(pad, pad, w - 1, h - 1, arc, arc);

        g.dispose();
        return img;
    }
}
