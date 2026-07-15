package com.dan.fcheckbox;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo für {@link FCheckBox} mit programmatisch gemaltem Palette-Icon
 * (16×16 und 32×32). Keine externen Bilddateien.
 */
public class FCheckBoxBeanInfo extends SimpleBeanInfo {

    @Override
    public Image getIcon(int iconKind) {
        int size = (iconKind == ICON_COLOR_32x32 || iconKind == ICON_MONO_32x32) ? 32 : 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);

            float pad = size * 0.16f;
            float bw = size - 2 * pad;
            float arc = size * 0.28f;

            // Glas-Box mit Türkis→Weinrot-Verlauf
            g2.setPaint(new GradientPaint(0, pad, new Color(0x00, 0xB5, 0xAD),
                    0, pad + bw, new Color(0x8B, 0x00, 0x24)));
            g2.fill(new RoundRectangle2D.Float(pad, pad, bw, bw, arc, arc));

            // Rim
            g2.setColor(new Color(140, 200, 255, 190));
            g2.setStroke(new BasicStroke(Math.max(1f, size / 14f)));
            g2.draw(new RoundRectangle2D.Float(pad + 0.5f, pad + 0.5f,
                    bw - 1f, bw - 1f, arc, arc));

            // Glanz oben links
            g2.setColor(new Color(255, 255, 255, 120));
            g2.drawLine((int) (pad + bw * 0.2f), (int) (pad + 2f),
                    (int) (pad + bw * 0.8f), (int) (pad + 2f));

            // Haken
            Path2D check = new Path2D.Float();
            check.moveTo(pad + bw * 0.26f, pad + bw * 0.54f);
            check.lineTo(pad + bw * 0.44f, pad + bw * 0.72f);
            check.lineTo(pad + bw * 0.78f, pad + bw * 0.30f);
            g2.setColor(new Color(255, 255, 255, 240));
            g2.setStroke(new BasicStroke(Math.max(1.4f, size / 9f),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(check);
        } finally {
            g2.dispose();
        }
        return img;
    }
}
