package com.dan.fradiobutton;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo für {@link FButtonGroup} — liefert Palette-Icons für NetBeans.
 * Zeigt drei kleine Kreise (Radio-Buttons in einer Gruppe), einer selektiert.
 */
public class FButtonGroupBeanInfo extends SimpleBeanInfo {

    @Override
    public Image getIcon(int iconKind) {
        int size = (iconKind == ICON_COLOR_32x32 || iconKind == ICON_MONO_32x32) ? 32 : 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cSize = Math.max(4, size / 5);       // Kreis-Durchmesser
            int gap   = Math.max(1, size / 8);        // Abstand zwischen Kreisen
            int totalH = 3 * cSize + 2 * gap;
            int startY = (size - totalH) / 2;
            int cx     = size / 2;

            for (int i = 0; i < 3; i++) {
                int cy = startY + i * (cSize + gap) + cSize / 2;
                int ox = cx - cSize / 2;
                int oy = cy - cSize / 2;
                boolean selected = (i == 0);

                // Schatten
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
                g2.setColor(Color.BLACK);
                g2.fillOval(ox + 1, oy + 1, cSize, cSize);
                g2.setComposite(AlphaComposite.SrcOver);

                // Glas-Körper
                g2.setColor(new Color(180, 220, 255, 40));
                g2.fillOval(ox, oy, cSize, cSize);

                // Gradient-Kern wenn selektiert
                if (selected) {
                    int innerR = Math.max(1, cSize / 4);
                    GradientPaint gp = new GradientPaint(
                        cx, cy + innerR, new Color(0x8B, 0x00, 0x24),
                        cx, cy - innerR, new Color(0x00, 0xB5, 0xAD)
                    );
                    g2.setPaint(gp);
                    Shape clip = new java.awt.geom.Ellipse2D.Float(ox + 1f, oy + 1f, cSize - 2f, cSize - 2f);
                    g2.setClip(clip);
                    g2.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);
                    g2.setClip(null);

                    // Glanzpunkt
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
                    g2.setColor(Color.WHITE);
                    int hp = Math.max(1, innerR / 2);
                    g2.fillOval(cx - hp - 1, cy - hp - 1, hp, hp);
                    g2.setComposite(AlphaComposite.SrcOver);
                }

                // Glasrand
                g2.setColor(new Color(140, 200, 255, 180));
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(ox, oy, cSize - 1, cSize - 1);
            }

        } finally {
            g2.dispose();
        }
        return img;
    }
}
