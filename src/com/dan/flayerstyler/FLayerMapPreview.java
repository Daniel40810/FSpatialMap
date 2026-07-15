package com.dan.flayerstyler;

import com.dan.fgeochart.FGeoFeature;
import com.dan.fmaplegend.FLayerGeometryType;
import com.dan.fstyle.FColors;
import com.dan.fstyle.FFontRole;
import com.dan.fstyle.FTheme;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.Collections;
import java.util.List;

/**
 * Einfache, selbstgemalte Kartenvorschau (equirechteckige Projektion) fuer
 * {@link FLayerStyler}. Faerbt jedes Feature einzeln nach seiner
 * {@link FFeatureRow#getColor()} — {@link com.dan.fgeochart.FGeoChart} faerbt
 * nur pro Layer/Ramp und eignet sich daher nicht fuer diesen Zweck.
 * <p>
 * Bewusst reduziert (kein Pan/Zoom-Interaktion, keine Tiles) — Zweck ist die
 * schnelle visuelle Kontrolle der Faerbung, nicht Ersatz fuer eine volle Karte.
 */
public class FLayerMapPreview extends JComponent {

    private List<FFeatureRow> rows = Collections.emptyList();
    private double minLon, maxLon, minLat, maxLat;
    private boolean hasBounds;

    public FLayerMapPreview() {
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(240, 220));
    }

    public void setRows(List<FFeatureRow> rows) {
        this.rows = rows == null ? Collections.emptyList() : rows;
        computeBounds();
        repaint();
    }

    /** Setzt die Sicht so, dass alle Features hineinpassen (wird bei setRows automatisch aufgerufen). */
    public void zoomToFit() {
        computeBounds();
        repaint();
    }

    private void computeBounds() {
        hasBounds = false;
        for (FFeatureRow row : rows) {
            FGeoFeature f = row.getFeature();
            double fMinLon = f.getMinLon(), fMaxLon = f.getMaxLon();
            double fMinLat = f.getMinLat(), fMaxLat = f.getMaxLat();
            if (!hasBounds) {
                minLon = fMinLon; maxLon = fMaxLon; minLat = fMinLat; maxLat = fMaxLat;
                hasBounds = true;
            } else {
                minLon = Math.min(minLon, fMinLon);
                maxLon = Math.max(maxLon, fMaxLon);
                minLat = Math.min(minLat, fMinLat);
                maxLat = Math.max(maxLat, fMaxLat);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FTheme theme = FTheme.getInstance();
        int w = getWidth(), h = getHeight();

        g2.setColor(FColors.withAlpha(theme.getSurface(), 160));
        g2.fillRoundRect(0, 0, w, h, 14, 14);

        if (!hasBounds || rows.isEmpty()) {
            g2.setFont(theme.getFont(FFontRole.BODY));
            g2.setColor(theme.getTextMuted());
            g2.drawString("Kein Layer geladen", 14, h / 2);
            g2.dispose();
            return;
        }

        int pad = 16;
        double lonSpan = Math.max(1e-9, maxLon - minLon);
        double latSpan = Math.max(1e-9, maxLat - minLat);
        double scaleX = (w - 2 * pad) / lonSpan;
        double scaleY = (h - 2 * pad) / latSpan;
        double scale = Math.min(scaleX, scaleY);
        double offX = pad + ((w - 2 * pad) - lonSpan * scale) / 2.0;
        double offY = pad + ((h - 2 * pad) - latSpan * scale) / 2.0;

        for (FFeatureRow row : rows) {
            paintFeature(g2, row, offX, offY, scale, theme);
        }
        g2.dispose();
    }

    private double toX(double lon, double offX, double scale) {
        return offX + (lon - minLon) * scale;
    }

    private double toY(double lat, double offY, double scale) {
        // Y waechst nach unten, geografische Breite nach oben -> spiegeln
        return getHeight() - offY - (lat - minLat) * scale;
    }

    private void paintFeature(Graphics2D g2, FFeatureRow row, double offX, double offY, double scale, FTheme theme) {
        FGeoFeature f = row.getFeature();
        Color color = row.isColored() ? row.getColor().toColor() : FColors.withAlpha(theme.getTextMuted(), 120);
        FLayerGeometryType type = f.getGeometryType();

        if (type == FLayerGeometryType.POINT) {
            double x = toX(f.getLon(0), offX, scale);
            double y = toY(f.getLat(0), offY, scale);
            double r = row.isColored() ? 5 : 3.5;
            Ellipse2D dot = new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
            g2.setColor(color);
            g2.fill(dot);
            g2.setColor(theme.getGlassRim());
            g2.setStroke(new BasicStroke(1f));
            g2.draw(dot);
            return;
        }

        Path2D path = new Path2D.Double();
        for (int i = 0; i < f.getPointCount(); i++) {
            double x = toX(f.getLon(i), offX, scale);
            double y = toY(f.getLat(i), offY, scale);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        if (type == FLayerGeometryType.POLYGON) {
            path.closePath();
            g2.setColor(FColors.withAlpha(color, 140));
            g2.fill(path);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.4f));
            g2.draw(path);
        } else {
            // LINE / NETWORK / RASTER-Fallback: als Linie darstellen
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(path);
        }
    }
}
