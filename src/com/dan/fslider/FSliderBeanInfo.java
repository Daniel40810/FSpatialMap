package com.dan.fslider;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo fuer {@link FSlider}: programmatisch gemaltes Palette-Icon (16/32),
 * keine externen Bild-Assets. Zeigt einen Glas-Track mit Liquid-Fuellung und Thumb.
 *
 * Java 8 kompatibel.
 */
public class FSliderBeanInfo extends SimpleBeanInfo {

    @Override
    public java.awt.Image getIcon(int iconKind) {
        switch (iconKind) {
            case BeanInfo.ICON_COLOR_16x16:
            case BeanInfo.ICON_MONO_16x16:
                return paintIcon(16);
            case BeanInfo.ICON_COLOR_32x32:
            case BeanInfo.ICON_MONO_32x32:
                return paintIcon(32);
            default:
                return null;
        }
    }

    private java.awt.Image paintIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float scale = size / 32f;
        int trackH = Math.round(9 * scale);
        int trackY = Math.round(12 * scale);
        int trackX = Math.round(3 * scale);
        int trackW = size - 2 * trackX;
        int arc = Math.round(6 * scale);

        // Dunkle Glasrinne
        RoundRectangle2D track = new RoundRectangle2D.Float(
                trackX, trackY, trackW, trackH, arc, arc);
        g.setColor(new Color(0x12, 0x16, 0x20));
        g.fill(track);

        // Liquid-Fuellung (Tuerkis -> Weinrot), ca. 55%
        int fillW = Math.round(trackW * 0.55f);
        java.awt.Shape old = g.getClip();
        g.clip(track);
        g.setPaint(new GradientPaint(
                trackX, trackY, new Color(0x00, 0xB5, 0xAD),
                trackX, trackY + trackH, new Color(0x8B, 0x00, 0x24)));
        g.fillRect(trackX, trackY, fillW, trackH);
        g.setClip(old);

        // Rim
        g.setStroke(new BasicStroke(Math.max(1f, 1.4f * scale)));
        g.setColor(new Color(0x8C, 0xC8, 0xFF, 180));
        g.draw(track);

        // Glas-Thumb an der Fuellgrenze
        int ts = Math.round(15 * scale);
        int tcx = trackX + fillW;
        int tcy = trackY + trackH / 2;
        Ellipse2D thumb = new Ellipse2D.Float(tcx - ts / 2f, tcy - ts / 2f, ts, ts);
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Float(tcx - ts / 2f, tcy - ts / 2f + 1.5f * scale, ts, ts));
        g.setPaint(new GradientPaint(
                tcx, tcy - ts / 2f, new Color(255, 255, 255, 230),
                tcx, tcy + ts / 2f, new Color(0x00, 0x8B, 0x86)));
        g.fill(thumb);
        g.setStroke(new BasicStroke(Math.max(1f, 1.2f * scale)));
        g.setColor(new Color(0x8C, 0xC8, 0xFF, 200));
        g.draw(thumb);
        // Reflex
        g.setColor(new Color(255, 255, 255, 170));
        g.fill(new Ellipse2D.Float(tcx - ts * 0.28f, tcy - ts * 0.34f, ts * 0.3f, ts * 0.26f));

        g.dispose();
        return img;
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            PropertyDescriptor variant = new PropertyDescriptor("variant", FSlider.class);
            variant.setDisplayName("Variante");
            variant.setShortDescription("STANDARD, MINIMAL oder TICKS (mit Skala).");

            PropertyDescriptor liqTop = new PropertyDescriptor("liquidColorTop", FSlider.class);
            liqTop.setDisplayName("Liquid oben");

            PropertyDescriptor liqBot = new PropertyDescriptor("liquidColorBottom", FSlider.class);
            liqBot.setDisplayName("Liquid unten");

            PropertyDescriptor animated = new PropertyDescriptor("animated", FSlider.class);
            animated.setDisplayName("Animiert");
            animated.setShortDescription("Lebendige Liquid-Welle und Blasen an/aus.");

            PropertyDescriptor thumb = new PropertyDescriptor("thumbSize", FSlider.class);
            thumb.setDisplayName("Thumb-Groesse");

            return new PropertyDescriptor[]{ variant, liqTop, liqBot, animated, thumb };
        } catch (IntrospectionException e) {
            return super.getPropertyDescriptors();
        }
    }
}
