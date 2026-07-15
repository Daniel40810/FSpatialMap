package com.dan.fspatialmap;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.SimpleBeanInfo;

/**
 * NetBeans-Palette-Icon fuer {@link FSpatialMapToolbar}: drei kleine "Knopf"-Kreise
 * auf einer Glas-Leiste, deutet die Werkzeugauswahl an.
 */
public class FSpatialMapToolbarBeanInfo extends SimpleBeanInfo {

    @Override
    public java.awt.Image getIcon(int kind) {
        int dim = (kind == BeanInfo.ICON_COLOR_16x16 || kind == BeanInfo.ICON_MONO_16x16) ? 16 : 32;
        BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            float pad = dim * 0.08f;
            float w = dim - 2 * pad;
            float h = dim * 0.42f;
            float y = (dim - h) / 2f;
            RoundRectangle2D bar = new RoundRectangle2D.Float(pad, y, w, h, h * 0.5f, h * 0.5f);
            g2.setPaint(new GradientPaint(pad, y, new Color(0x00, 0xB5, 0xAD), pad, y + h, new Color(0x8B, 0x00, 0x24)));
            g2.fill(bar);
            g2.setColor(new Color(140, 200, 255, 170));
            g2.setStroke(new java.awt.BasicStroke(Math.max(0.6f, dim * 0.03f)));
            g2.draw(bar);

            float r = h * 0.32f;
            float cy = dim / 2f;
            for (int i = 0; i < 3; i++) {
                float cx = pad + w * (0.22f + i * 0.28f);
                g2.setColor(i == 1 ? Color.WHITE : new Color(255, 255, 255, 130));
                g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
            }
        } finally {
            g2.dispose();
        }
        return img;
    }
}
