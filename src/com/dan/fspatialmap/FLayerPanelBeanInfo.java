package com.dan.fspatialmap;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.SimpleBeanInfo;

/**
 * NetBeans-Palette-Icon fuer {@link FLayerPanel}: gestapelte Layer-Karten im
 * Türkis-Weinrot-Glas-Look, deutet Sichtbarkeits-/Transparenz-Steuerung an.
 */
public class FLayerPanelBeanInfo extends SimpleBeanInfo {

    @Override
    public java.awt.Image getIcon(int kind) {
        int dim = (kind == BeanInfo.ICON_COLOR_16x16 || kind == BeanInfo.ICON_MONO_16x16) ? 16 : 32;
        BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            float pad = dim * 0.10f;
            float w = dim - 2 * pad;
            float h = dim * 0.22f;
            Color top = new Color(0x00, 0xB5, 0xAD);
            Color bottom = new Color(0x8B, 0x00, 0x24);
            for (int i = 0; i < 3; i++) {
                float y = pad + i * (h + dim * 0.06f);
                float alpha = 1.0f - i * 0.22f;
                RoundRectangle2D card = new RoundRectangle2D.Float(pad, y, w, h, dim * 0.10f, dim * 0.10f);
                g2.setPaint(new GradientPaint(pad, y, withAlpha(top, alpha), pad, y + h, withAlpha(bottom, alpha)));
                g2.fill(card);
                g2.setColor(new Color(140, 200, 255, Math.round(170 * alpha)));
                g2.setStroke(new java.awt.BasicStroke(Math.max(0.6f, dim * 0.03f)));
                g2.draw(card);
            }
        } finally {
            g2.dispose();
        }
        return img;
    }

    private static Color withAlpha(Color c, float a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(255 * Math.max(0f, Math.min(1f, a))));
    }
}
