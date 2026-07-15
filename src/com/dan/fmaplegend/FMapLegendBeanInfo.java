package com.dan.fmaplegend;

import com.dan.ficons.FIconPaint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * NetBeans-Palette-Integration fuer FMapLegend.
 * Icon programmatisch gemalt: Glas-Kachel mit drei Legendenzeilen
 * (Mini-Glyph + Farbbalken), ReagenzglasBar-Look.
 */
public class FMapLegendBeanInfo extends SimpleBeanInfo {

    @Override
    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor bd = new BeanDescriptor(FMapLegend.class);
        bd.setDisplayName("FMapLegend");
        bd.setShortDescription("Interaktive Spatial-Layer-Legende mit Geometrie-Glyphs, "
                + "Farbbalken (COLOR_ADMIN) und Glas-Toggles");
        return bd;
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            PropertyDescriptor title = pd("title", "Titel", null);
            PropertyDescriptor showTitle = pd("showTitle", "Titel anzeigen", null);
            PropertyDescriptor showDetailsPopup = pd("showDetailsPopup", "Detail-Popup",
                    "Feature-Anzahl und Bounds bei Hover");
            PropertyDescriptor paintOwnFrame = pd("paintOwnFrame", "Eigener Rahmen",
                    "Glasrahmen um die Legende (aus in FCard einbetten)");
            PropertyDescriptor rowHeight = pd("rowHeight", "Zeilenhoehe", null);
            PropertyDescriptor iconSize = pd("iconSize", "Glyph-Groesse", null);
            PropertyDescriptor colorBarWidth = pd("colorBarWidth", "Farbbalken-Breite", null);
            PropertyDescriptor animated = pd("animated", "Animiert", null);
            return new PropertyDescriptor[]{
                    title, showTitle, showDetailsPopup, paintOwnFrame,
                    rowHeight, iconSize, colorBarWidth, animated
            };
        } catch (IntrospectionException ex) {
            return null;
        }
    }

    private PropertyDescriptor pd(String name, String display, String desc)
            throws IntrospectionException {
        PropertyDescriptor p = new PropertyDescriptor(name, FMapLegend.class);
        p.setDisplayName(display);
        if (desc != null) {
            p.setShortDescription(desc);
        }
        return p;
    }

    @Override
    public Image getIcon(int kind) {
        switch (kind) {
            case ICON_COLOR_16x16:
            case ICON_MONO_16x16:
                return paintIcon(16);
            case ICON_COLOR_32x32:
            case ICON_MONO_32x32:
                return paintIcon(32);
            default:
                return null;
        }
    }

    private Image paintIcon(int dim) {
        BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            FIconPaint.hints(g2);
            float d = dim, pad = d * 0.08f;

            // Glas-Kachel
            RoundRectangle2D tile = new RoundRectangle2D.Float(
                    pad, pad, d - 2 * pad, d - 2 * pad, d * 0.24f, d * 0.24f);
            g2.setColor(FIconPaint.GLASS_BODY);
            g2.fill(tile);
            Shape oldClip = g2.getClip();
            g2.clip(tile);
            FIconPaint.shine(g2, pad, pad, d - 2 * pad, (d - 2 * pad) * 0.4f);

            // Drei Legendenzeilen: Mini-Glyph links + Farbbalken rechts
            float rowH = (d - 2 * pad) / 3.6f;
            float y0 = pad + rowH * 0.35f;
            Color[] barCols = {FIconPaint.TURQUOISE,
                    new Color(0xC8, 0xD4, 0x30), FIconPaint.WINE_RED};

            for (int i = 0; i < 3; i++) {
                float ry = y0 + i * rowH * 1.15f;
                float gx = pad + d * 0.10f;
                float gs = rowH * 0.7f;

                // Mini-Glyph: Zeile 0 Dreieck (Polygon), 1 Strich (Linie), 2 Punkt
                g2.setColor(FIconPaint.INK);
                if (i == 0) {
                    Path2D tri = new Path2D.Float();
                    tri.moveTo(gx + gs / 2, ry);
                    tri.lineTo(gx + gs, ry + gs);
                    tri.lineTo(gx, ry + gs);
                    tri.closePath();
                    g2.fill(tri);
                } else if (i == 1) {
                    g2.setStroke(new BasicStroke(Math.max(1f, d * 0.06f),
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine((int) gx, (int) (ry + gs), (int) (gx + gs), (int) ry);
                } else {
                    g2.fill(new Ellipse2D.Float(gx + gs * 0.2f, ry + gs * 0.2f,
                            gs * 0.7f, gs * 0.7f));
                }

                // Farbbalken
                float bx = gx + gs + d * 0.10f;
                float bw = (d - pad) - bx - d * 0.10f;
                float bh = Math.max(2f, gs * 0.55f);
                RoundRectangle2D bar = new RoundRectangle2D.Float(
                        bx, ry + (gs - bh) / 2, bw, bh, bh, bh);
                g2.setColor(barCols[i]);
                g2.fill(bar);
            }

            g2.setClip(oldClip);
            FIconPaint.rim(g2, tile, Math.max(1f, d * 0.045f));
        } finally {
            g2.dispose();
        }
        return img;
    }
}
