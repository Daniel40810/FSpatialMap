package com.dan.fgeochart;

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
 * (Tuerkis→Wein-Gradient, Glas-Rim, Mini-Karte mit Route und Pin).
 */
public class FGeoChartBeanInfo extends SimpleBeanInfo {

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
            RoundRectangle2D body = new RoundRectangle2D.Float(
                    1.5f * s, 1.5f * s, 29f * s, 29f * s, 9f * s, 9f * s);

            // Hintergrund-Gradient: Tuerkis -> Wein
            g.setPaint(new GradientPaint(0, 0, new Color(0x00, 0xB5, 0xAD),
                    0, size, new Color(0x8B, 0x00, 0x24)));
            g.fill(body);

            // Grid-Linien (Karten-Anmutung)
            g.setColor(new Color(255, 255, 255, 45));
            g.setStroke(new BasicStroke(Math.max(1f, 1f * s)));
            g.drawLine((int) (10 * s), (int) (3 * s), (int) (10 * s), (int) (29 * s));
            g.drawLine((int) (21 * s), (int) (3 * s), (int) (21 * s), (int) (29 * s));
            g.drawLine((int) (3 * s), (int) (12 * s), (int) (29 * s), (int) (12 * s));
            g.drawLine((int) (3 * s), (int) (21 * s), (int) (29 * s), (int) (21 * s));

            // Route (weisse geschwungene Linie)
            Path2D route = new Path2D.Float();
            route.moveTo(5 * s, 25 * s);
            route.curveTo(12 * s, 15 * s, 16 * s, 24 * s, 26 * s, 9 * s);
            g.setColor(new Color(255, 255, 255, 235));
            g.setStroke(new BasicStroke(Math.max(1.2f, 2.2f * s),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(route);

            // Pin (Punkt-Feature)
            float pr = 3.4f * s;
            Ellipse2D pin = new Ellipse2D.Float(24 * s - pr, 7 * s - pr, pr * 2, pr * 2);
            g.setColor(new Color(255, 255, 255));
            g.fill(pin);
            g.setColor(new Color(0x8B, 0x00, 0x24));
            g.fill(new Ellipse2D.Float(24 * s - pr * 0.45f, 7 * s - pr * 0.45f,
                    pr * 0.9f, pr * 0.9f));

            // Shine oben
            g.setColor(new Color(255, 255, 255, 55));
            g.fill(new RoundRectangle2D.Float(3f * s, 3f * s, 26f * s, 9f * s, 7f * s, 7f * s));

            // Glas-Rim
            g.setColor(new Color(180, 225, 255, 200));
            g.setStroke(new BasicStroke(Math.max(1f, 1.6f * s)));
            g.draw(body);
        } finally {
            g.dispose();
        }
        return img;
    }
}
