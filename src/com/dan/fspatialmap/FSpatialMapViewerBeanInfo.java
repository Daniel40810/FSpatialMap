package com.dan.fspatialmap;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.SimpleBeanInfo;

/**
 * NetBeans-Palette-Icon fuer {@link FSpatialMapViewer}: Kartenkachel mit
 * Gitter und Standort-Pin im Türkis-Weinrot-Glas-Look.
 */
public class FSpatialMapViewerBeanInfo extends SimpleBeanInfo {

    @Override
    public java.awt.Image getIcon(int kind) {
        int dim = (kind == BeanInfo.ICON_COLOR_16x16 || kind == BeanInfo.ICON_MONO_16x16) ? 16 : 32;
        BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            float pad = dim * 0.08f;
            float sz = dim - 2 * pad;
            RoundRectangle2D tile = new RoundRectangle2D.Float(pad, pad, sz, sz, dim * 0.18f, dim * 0.18f);

            g2.setPaint(new GradientPaint(pad, pad, new Color(0x00, 0xB5, 0xAD),
                    pad, pad + sz, new Color(0x8B, 0x00, 0x24)));
            g2.fill(tile);

            g2.setColor(new Color(255, 255, 255, 60));
            g2.setStroke(new java.awt.BasicStroke(Math.max(0.6f, dim * 0.02f)));
            for (int i = 1; i < 3; i++) {
                float gx = pad + sz * i / 3f;
                float gy = pad + sz * i / 3f;
                g2.draw(new Line2D.Float(gx, pad, gx, pad + sz));
                g2.draw(new Line2D.Float(pad, gy, pad + sz, gy));
            }

            float pinR = sz * 0.16f;
            float pinCx = pad + sz * 0.58f;
            float pinCy = pad + sz * 0.42f;
            g2.setColor(new Color(0x12, 0x14, 0x1E));
            g2.fill(new Ellipse2D.Float(pinCx - pinR, pinCy - pinR, pinR * 2, pinR * 2));
            g2.setColor(Color.WHITE);
            float innerR = pinR * 0.45f;
            g2.fill(new Ellipse2D.Float(pinCx - innerR, pinCy - innerR, innerR * 2, innerR * 2));

            g2.setColor(new Color(140, 200, 255, 170));
            g2.setStroke(new java.awt.BasicStroke(Math.max(0.8f, dim * 0.045f)));
            g2.draw(tile);
        } finally {
            g2.dispose();
        }
        return img;
    }
}
