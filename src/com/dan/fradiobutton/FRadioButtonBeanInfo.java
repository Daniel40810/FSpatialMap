package com.dan.fradiobutton;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo für {@link FRadioButton} — liefert Palette-Icons für NetBeans.
 * Icons werden vollständig programmatisch gezeichnet (keine externen Bild-Ressourcen).
 */
public class FRadioButtonBeanInfo extends SimpleBeanInfo {

    @Override
    public Image getIcon(int iconKind) {
        int size = (iconKind == ICON_COLOR_32x32 || iconKind == ICON_MONO_32x32) ? 32 : 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int pad   = Math.max(1, size / 8);
            int cSize = size - 2 * pad;
            int cx    = pad + cSize / 2;
            int cy    = size / 2;

            // ── Schatten ──
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
            g2.setColor(Color.BLACK);
            g2.fillOval(pad + 1, pad + 1, cSize, cSize);
            g2.setComposite(AlphaComposite.SrcOver);

            // ── Glas-Körper ──
            g2.setColor(new Color(180, 220, 255, 35));
            g2.fillOval(pad, pad, cSize, cSize);

            // ── Gradient-Kern (Selektion simuliert) ──
            int innerR = Math.max(2, cSize / 4);
            GradientPaint gp = new GradientPaint(
                cx, cy + innerR, new Color(0x8B, 0x00, 0x24),
                cx, cy - innerR, new Color(0x00, 0xB5, 0xAD)
            );
            g2.setPaint(gp);
            Shape clip = new java.awt.geom.Ellipse2D.Float(pad + 1f, pad + 1f, cSize - 2f, cSize - 2f);
            g2.setClip(clip);
            g2.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);
            g2.setClip(null);

            // ── Glanzpunkt ──
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f));
            g2.setColor(Color.WHITE);
            int hp = Math.max(1, innerR / 2);
            g2.fillOval(cx - hp - 1, cy - hp - 1, hp, hp);
            g2.setComposite(AlphaComposite.SrcOver);

            // ── Glasrand ──
            g2.setColor(new Color(140, 200, 255, 180));
            g2.setStroke(new BasicStroke(Math.max(1f, size / 12f)));
            g2.drawOval(pad, pad, cSize - 1, cSize - 1);

            // ── Highlight-Bogen oben-links ──
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.60f));
            g2.setColor(new Color(255, 255, 255, 120));
            g2.setStroke(new BasicStroke(Math.max(1f, size / 14f)));
            g2.drawArc(pad + 2, pad + 2, cSize / 3, cSize / 3, 120, 100);
            g2.setComposite(AlphaComposite.SrcOver);

        } finally {
            g2.dispose();
        }
        return img;
    }
}
