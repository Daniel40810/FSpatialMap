package com.dan.fframe;

import com.dan.ficons.FIconType;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo für {@link FFrame}. Palette-Icon zeigt ein Mini-Fenster mit
 * Türkis&rarr;Weinrot-Taskbar, Katzen-Logo und drei Steuer-Punkten —
 * vollständig programmatisch gemalt.
 *
 * @author com.dan
 */
public class FFrameBeanInfo extends SimpleBeanInfo {

    @Override
    public Image getIcon(int kind) {
        if (kind == ICON_COLOR_16x16 || kind == ICON_MONO_16x16) return paint(16);
        if (kind == ICON_COLOR_32x32 || kind == ICON_MONO_32x32) return paint(32);
        return null;
    }

    private Image paint(int dim) {
        BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            float pad = dim * 0.08f, w = dim - 2 * pad, h = dim - 2 * pad, arc = dim * 0.22f;
            RoundRectangle2D win = new RoundRectangle2D.Float(pad, pad, w, h, arc, arc);
            // Fensterkörper
            g2.setColor(new Color(0x0C, 0x11, 0x17));
            g2.fill(win);
            // Taskbar-Streifen oben
            float barH = h * 0.34f;
            java.awt.Shape clip = g2.getClip();
            g2.clip(win);
            g2.setPaint(new GradientPaint(pad, pad, new Color(0x01, 0x4B, 0x4E),
                    pad + w, pad, new Color(0x5C, 0x00, 0x2A)));
            g2.fill(new RoundRectangle2D.Float(pad, pad, w, barH + arc, arc, arc));
            g2.setClip(clip);
            // Katze klein links
            int cs = (int) (barH * 1.5f);
            int cx = (int) (pad + dim * 0.02f);
            int cy = (int) (pad - dim * 0.02f);
            g2.translate(cx, cy);
            FIconType.CAT.paintGlyph(g2, cs);
            g2.translate(-cx, -cy);
            // drei Steuer-Punkte rechts
            if (dim >= 24) {
                float r = dim * 0.035f, py = pad + barH * 0.5f;
                Color[] cols = {new Color(0x2A, 0xC8, 0xC0), new Color(0x2A, 0xC8, 0xC0), new Color(0xB3, 0x00, 0x1B)};
                for (int i = 0; i < 3; i++) {
                    float px = pad + w - dim * (0.20f - i * 0.07f);
                    g2.setColor(cols[i]);
                    g2.fillOval((int) (px - r), (int) (py - r), (int) (2 * r), (int) (2 * r));
                }
            }
            // Rahmen
            g2.setColor(new Color(140, 200, 255, 110));
            g2.draw(win);
        } finally {
            g2.dispose();
        }
        return img;
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            PropertyDescriptor logo = new PropertyDescriptor("logoType", FFrame.class);
            logo.setDisplayName("Logo-Symbol");
            logo.setShortDescription("Gemaltes Logo-Glyph in der Taskbar (Standard: Katze)");

            PropertyDescriptor title = new PropertyDescriptor("title", FFrame.class);
            title.setDisplayName("Titel");

            PropertyDescriptor lift = new PropertyDescriptor("iconHoverLift", FFrame.class);
            lift.setDisplayName("Icon-Hover-Lift");
            lift.setShortDescription("Wie weit die Icons bei Hover nach vorne treten");

            PropertyDescriptor glow = new PropertyDescriptor("iconGlowColor", FFrame.class);
            glow.setDisplayName("Icon-Glow");

            PropertyDescriptor close = new PropertyDescriptor("closeGlowColor", FFrame.class);
            close.setDisplayName("Schließen-Glow");

            PropertyDescriptor pane = new PropertyDescriptor("componentPaneColor", FFrame.class);
            pane.setDisplayName("Inhaltsfläche");

            return new PropertyDescriptor[]{ logo, title, lift, glow, close, pane };
        } catch (IntrospectionException e) {
            return null;
        }
    }
}
