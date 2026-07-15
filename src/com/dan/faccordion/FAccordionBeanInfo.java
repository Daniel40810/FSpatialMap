package com.dan.faccordion;

import com.dan.ficons.FIconPaint;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/** NetBeans-Palette-Metadaten fuer {@link FAccordion}: gemaltes Icon, Kern-Properties. */
public class FAccordionBeanInfo extends SimpleBeanInfo {

    @Override
    public Image getIcon(int kind) {
        int dim = (kind == BeanInfo.ICON_COLOR_16x16 || kind == BeanInfo.ICON_MONO_16x16) ? 16 : 32;
        return paintIcon(dim);
    }

    private Image paintIcon(int dim) {
        BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float pad = dim * 0.10f;
            float w = dim - 2 * pad;
            float rowH = dim * 0.28f;
            float arc = dim * 0.14f;

            // Obere Section: offen (volle Liquid-Fuellung)
            RoundRectangle2D openBar = new RoundRectangle2D.Float(pad, pad, w, rowH, arc, arc);
            g2.setPaint(FIconPaint.liquid(pad, pad, w, rowH));
            g2.fill(openBar);
            FIconPaint.rim(g2, openBar, Math.max(1f, dim * 0.045f));

            // Chevron nach oben (aufgeklappt)
            GeneralPath chevOpen = new GeneralPath();
            float ccx = pad + w - rowH * 0.5f;
            float ccy = pad + rowH * 0.5f;
            float cs = rowH * 0.28f;
            chevOpen.moveTo(ccx - cs, ccy + cs * 0.4f);
            chevOpen.lineTo(ccx, ccy - cs * 0.5f);
            chevOpen.lineTo(ccx + cs, ccy + cs * 0.4f);
            g2.setStroke(new java.awt.BasicStroke(Math.max(1f, dim * 0.06f),
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g2.setColor(FIconPaint.INK);
            g2.draw(chevOpen);

            // Untere Section: zu (nur Glasrand, keine Fuellung)
            float y2 = pad + rowH + dim * 0.10f;
            RoundRectangle2D closedBar = new RoundRectangle2D.Float(pad, y2, w, rowH, arc, arc);
            g2.setPaint(FIconPaint.alpha(FIconPaint.GLASS_BODY, 60));
            g2.fill(closedBar);
            FIconPaint.rim(g2, closedBar, Math.max(1f, dim * 0.045f));

            // Chevron nach unten (zugeklappt)
            GeneralPath chevClosed = new GeneralPath();
            float ccy2 = y2 + rowH * 0.5f;
            chevClosed.moveTo(ccx - cs, ccy2 - cs * 0.4f);
            chevClosed.lineTo(ccx, ccy2 + cs * 0.5f);
            chevClosed.lineTo(ccx + cs, ccy2 - cs * 0.4f);
            g2.setColor(FIconPaint.alpha(FIconPaint.INK, 190));
            g2.draw(chevClosed);
        } finally {
            g2.dispose();
        }
        return img;
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            PropertyDescriptor multipleOpen = new PropertyDescriptor("multipleOpen", FAccordion.class);
            multipleOpen.setDisplayName("Mehrere gleichzeitig offen");
            multipleOpen.setShortDescription("Erlaubt mehrere gleichzeitig geoeffnete Sections.");

            PropertyDescriptor collapsible = new PropertyDescriptor("collapsible", FAccordion.class);
            collapsible.setDisplayName("Vollstaendig zuklappbar");
            collapsible.setShortDescription("Wenn deaktiviert, bleibt immer mindestens eine Section offen.");

            PropertyDescriptor animationSpeed = new PropertyDescriptor("animationSpeed", FAccordion.class);
            animationSpeed.setDisplayName("Animationsgeschwindigkeit");

            PropertyDescriptor chevronPosition = new PropertyDescriptor("chevronPosition", FAccordion.class);
            chevronPosition.setDisplayName("Chevron-Position");

            PropertyDescriptor liquidTop = new PropertyDescriptor("liquidColorTop", FAccordion.class);
            liquidTop.setDisplayName("Liquid-Farbe oben");

            PropertyDescriptor liquidBottom = new PropertyDescriptor("liquidColorBottom", FAccordion.class);
            liquidBottom.setDisplayName("Liquid-Farbe unten");

            return new PropertyDescriptor[]{
                    multipleOpen, collapsible, animationSpeed, chevronPosition, liquidTop, liquidBottom
            };
        } catch (IntrospectionException e) {
            return null;
        }
    }
}
