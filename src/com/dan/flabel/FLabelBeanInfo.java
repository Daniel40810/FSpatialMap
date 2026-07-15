package com.dan.flabel;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo für {@link FLabel}. Liefert programmatisch gezeichnete Palette-Icons
 * (16x16 und 32x32) im ReagenzglasBar-Stil: Glasfläche mit Türkis&rarr;Weinrot-
 * Gradient, hellblauer Rand, Glanzstreifen und ein "A"-Glyph als Label-Symbol.
 * Keine externen Bilddateien nötig.
 */
public class FLabelBeanInfo extends SimpleBeanInfo {

    @Override
    public Image getIcon(int iconKind) {
        int size = (iconKind == ICON_COLOR_32x32 || iconKind == ICON_MONO_32x32) ? 32 : 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            float pad = Math.max(1.5f, size / 8f);
            float w = size - 2 * pad;
            float h = size - 2 * pad;
            int arc = size / 3;

            // Glasfläche: Türkis (oben) → Weinrot (unten)
            g2.setPaint(new GradientPaint(
                    0, pad, new Color(0x00, 0xB5, 0xAD),
                    0, pad + h, new Color(0x8B, 0x00, 0x24)));
            g2.fillRoundRect((int) pad, (int) pad, (int) w, (int) h, arc, arc);

            // Glanzstreifen links
            g2.setColor(new Color(255, 255, 255, 90));
            g2.fillRoundRect((int) pad, (int) pad,
                    Math.max(2, (int) (w * 0.28f)), (int) h, arc, arc);

            // Glasrand
            g2.setColor(new Color(140, 200, 255, 180));
            g2.setStroke(new java.awt.BasicStroke(Math.max(1f, size / 16f)));
            g2.drawRoundRect((int) pad, (int) pad, (int) w - 1, (int) h - 1, arc, arc);

            // "A" als Label-Symbol
            Font font = new Font("Segoe UI", Font.BOLD, Math.max(8, (int) (size * 0.52f)));
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            String s = "A";
            int tx = (size - fm.stringWidth(s)) / 2;
            int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
            // Textschatten
            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawString(s, tx + 1, ty + 1);
            g2.setColor(new Color(0xEA, 0xFB, 0xFF));
            g2.drawString(s, tx, ty);

            // Akzent-Balken unten (Markenzeichen des FLabel-Hover)
            g2.setPaint(new GradientPaint(
                    pad, 0, new Color(0x8B, 0x00, 0x24),
                    pad + w, 0, new Color(0x00, 0xB5, 0xAD)));
            int barH = Math.max(1, size / 12);
            g2.fillRoundRect((int) (pad + 2), (int) (pad + h - barH - 1),
                    (int) (w - 4), barH, barH, barH);
        } finally {
            g2.dispose();
        }
        return img;
    }
}
