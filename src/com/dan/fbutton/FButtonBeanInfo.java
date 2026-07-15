package com.dan.fbutton;

import java.awt.BasicStroke;
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
 * BeanInfo für {@link FButton}. Gemalte 16×16- und 32×32-Icons (kein Bild-Asset).
 *
 * @author com.dan
 */
public class FButtonBeanInfo extends SimpleBeanInfo {

    private static final Class<FButton> BEAN = FButton.class;

    @Override
    public Image getIcon(int kind) {
        switch (kind) {
            case BeanInfo.ICON_COLOR_16x16:
            case BeanInfo.ICON_MONO_16x16:  return paint(16);
            case BeanInfo.ICON_COLOR_32x32:
            case BeanInfo.ICON_MONO_32x32:  return paint(32);
            default: return null;
        }
    }

    private Image paint(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            float pad  = s * 0.10f;
            float arc  = s * 0.28f;
            float w    = s - 2 * pad;
            float h    = s * 0.54f;
            float y    = s * 0.20f;

            // Schaltflächen-Körper: Türkis→Weinrot
            Color top = new Color(0x00, 0xB5, 0xAD);
            Color bot = new Color(0x8B, 0x00, 0x24);
            g.setPaint(new GradientPaint(0, y, top, 0, y + h, bot));
            g.fill(new RoundRectangle2D.Float(pad, y, w, h, arc, arc));

            // Glasfüllung
            g.setColor(new Color(180, 220, 255, 40));
            g.fill(new RoundRectangle2D.Float(pad, y, w, h, arc, arc));

            // Glanzstreifen
            int sw = (int) (w * 0.25f);
            g.setPaint(new GradientPaint(pad, 0,
                    new Color(255, 255, 255, 100), pad + sw, 0, new Color(255, 255, 255, 0)));
            g.fillRect((int) pad, (int) y, sw, (int) h);

            // Rim
            g.setColor(new Color(140, 200, 255, 160));
            g.setStroke(new BasicStroke(Math.max(1f, s / 18f)));
            g.draw(new RoundRectangle2D.Float(pad, y, w, h, arc, arc));

            // Akzent-Balken unten
            int bh = Math.max(1, s / 10);
            g.setPaint(new GradientPaint(pad, 0, bot, pad + w, 0, top));
            g.fillRoundRect((int) (pad + w * 0.10f), (int) (y + h - bh - 1),
                    (int) (w * 0.80f), bh, bh, bh);

        } finally {
            g.dispose();
        }
        return img;
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            return new PropertyDescriptor[] {
                    pd("variant",          "Stil-Variante (PRIMARY / GHOST / DANGER / SUCCESS)"),
                    pd("fIcon",            "Optionales Icon links vom Text (FIconType.toImage)"),
                    pd("liquidColorTop",   "Akzentfarbe oben (Türkis)"),
                    pd("liquidColorBottom","Akzentfarbe unten (Weinrot)"),
                    pd("arc",             "Eckenradius"),
                    pd("padX",            "Horizontaler Innenabstand"),
                    pd("padY",            "Vertikaler Innenabstand"),
                    pd("accentHeight",    "Höhe des Unterstrich-Balkens"),
                    pd("restAlpha",       "Glasfläche im Ruhezustand (0..255)"),
                    pd("hoverAlpha",      "Glasfläche beim Hover (0..255)"),
                    pd("hoverEnabled",    "Hover-Animation aktiv"),
                    pd("rippleEnabled",   "Ripple beim Klick aktiv"),
                    pd("accentVisible",   "Unterstrich-Balken sichtbar"),
                    pd("animated",        "Shimmer im Balken aktiv"),
            };
        } catch (IntrospectionException ex) {
            return super.getPropertyDescriptors();
        }
    }

    private static PropertyDescriptor pd(String name, String desc)
            throws IntrospectionException {
        PropertyDescriptor d = new PropertyDescriptor(name, BEAN);
        d.setShortDescription(desc);
        return d;
    }
}
