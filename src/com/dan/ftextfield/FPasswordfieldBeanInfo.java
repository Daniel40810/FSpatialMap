package com.dan.ftextfield;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo fuer {@link FPasswordfield}. Zeichnet das Palette-Icon
 * programmatisch: Glaskoerper im Weinrot&rarr;Tuerkis-Verlauf mit hellblauem
 * Rand, Glanzstreifen, schwebendem Label-Strich und Echo-Punkten &ndash; im
 * gleichen Stil wie {@code FComboBoxBeanInfo}.
 */
public class FPasswordfieldBeanInfo extends SimpleBeanInfo {

    @Override
    public Image getIcon(int iconKind) {
        int size = (iconKind == ICON_COLOR_32x32 || iconKind == ICON_MONO_32x32) ? 32 : 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            float arc = size / 3f;
            float top = size * 0.30f;
            float h = size * 0.42f;

            // Glaskoerper mit Weinrot->Tuerkis Verlauf.
            g2.setPaint(new GradientPaint(
                    0, size, new Color(0x8B, 0x00, 0x24),
                    size, 0, new Color(0x00, 0xB5, 0xAD)));
            g2.fillRoundRect(1, Math.round(top), size - 2, Math.round(h), Math.round(arc), Math.round(arc));

            // Glasrand (hellblau).
            g2.setColor(new Color(140, 200, 255, 180));
            g2.setStroke(new BasicStroke(Math.max(1f, size / 16f)));
            g2.drawRoundRect(1, Math.round(top), size - 3, Math.round(h) - 1,
                    Math.round(arc), Math.round(arc));

            // Linker Glanzstreifen.
            g2.setColor(new Color(255, 255, 255, 120));
            g2.fillRect(3, Math.round(top) + 2, Math.max(1, size / 10), Math.round(h) - 4);

            // Schwebendes Label.
            g2.setColor(new Color(0xBF, 0xEE, 0xEB));
            g2.setStroke(new BasicStroke(Math.max(1f, size / 16f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(3, Math.round(top) - Math.max(3, size / 8), Math.max(7, size / 3), Math.round(top) - Math.max(3, size / 8));

            // Echo-Punkte (verdeckte Passwort-Eingabe).
            g2.setColor(new Color(0xE0, 0xFA, 0xF8));
            int dot = Math.max(2, size / 9);
            int gap = dot + Math.max(1, size / 12);
            int cy = Math.round(top + h / 2f);
            int start = 5;
            for (int i = 0; i < 3; i++) {
                g2.fillOval(start + i * gap, cy - dot / 2, dot, dot);
            }
        } finally {
            g2.dispose();
        }
        return img;
    }
}
