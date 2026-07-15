package com.dan.fmaplegend;

import com.dan.ficons.FIcon;
import com.dan.ficons.FIconPaint;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Geometrie-Typ eines Spatial-Layers — jedes Enum-Element malt sein eigenes
 * F-Style-Glyph (implements FIcon) und ist damit direkt in FIconComponent
 * nutzbar: {@code iconComponent.setIcon(FLayerGeometryType.POLYGON)}.
 *
 * Mal-Konventionen nach ficon-custom: hints -> Form -> liquid -> shine(clip)
 * -> rim -> Innensymbol in INK. Alles dim-relativ, kein Glow im Glyph.
 */
public enum FLayerGeometryType implements FIcon {

    POLYGON("Polygon") {
        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            FIconPaint.hints(g2);
            float d = dim, pad = d * 0.14f;
            // Unregelmaessiges Fuenfeck (typische Flaechengeometrie)
            Path2D p = new Path2D.Float();
            p.moveTo(d * 0.50f, pad);
            p.lineTo(d - pad, d * 0.42f);
            p.lineTo(d * 0.78f, d - pad);
            p.lineTo(d * 0.26f, d - pad * 1.1f);
            p.lineTo(pad, d * 0.46f);
            p.closePath();
            g2.setPaint(FIconPaint.liquid(pad, pad, d - 2 * pad, d - 2 * pad));
            g2.fill(p);
            Shape oldClip = g2.getClip();
            g2.clip(p);
            FIconPaint.shine(g2, pad, pad, d - 2 * pad, (d - 2 * pad) * 0.5f);
            g2.setClip(oldClip);
            FIconPaint.rim(g2, p, Math.max(1f, d * 0.045f));
            // Vertex-Punkte in INK
            float r = Math.max(1f, d * 0.055f);
            g2.setColor(FIconPaint.INK);
            g2.fill(new Ellipse2D.Float(d * 0.50f - r, pad - r, 2 * r, 2 * r));
            g2.fill(new Ellipse2D.Float(d - pad - r, d * 0.42f - r, 2 * r, 2 * r));
            g2.fill(new Ellipse2D.Float(d * 0.78f - r, d - pad - r, 2 * r, 2 * r));
            g2.fill(new Ellipse2D.Float(d * 0.26f - r, d - pad * 1.1f - r, 2 * r, 2 * r));
            g2.fill(new Ellipse2D.Float(pad - r, d * 0.46f - r, 2 * r, 2 * r));
        }
    },

    LINE("Linie") {
        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            FIconPaint.hints(g2);
            float d = dim, pad = d * 0.16f;
            // Polyline (Strassen-/Fluss-Geometrie)
            Path2D p = new Path2D.Float();
            p.moveTo(pad, d - pad);
            p.lineTo(d * 0.38f, d * 0.44f);
            p.lineTo(d * 0.62f, d * 0.62f);
            p.lineTo(d - pad, pad);
            BasicStroke stroke = new BasicStroke(Math.max(1.5f, d * 0.12f),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            Shape fat = stroke.createStrokedShape(p);
            g2.setPaint(FIconPaint.liquidDiagonal(pad, d - pad, d - pad, pad));
            g2.fill(fat);
            Shape oldClip = g2.getClip();
            g2.clip(fat);
            FIconPaint.shine(g2, pad, pad, d - 2 * pad, (d - 2 * pad) * 0.5f);
            g2.setClip(oldClip);
            FIconPaint.rim(g2, fat, Math.max(1f, d * 0.035f));
            // Endpunkte in INK
            float r = Math.max(1f, d * 0.07f);
            g2.setColor(FIconPaint.INK);
            g2.fill(new Ellipse2D.Float(pad - r, d - pad - r, 2 * r, 2 * r));
            g2.fill(new Ellipse2D.Float(d - pad - r, pad - r, 2 * r, 2 * r));
        }
    },

    POINT("Punkt") {
        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            FIconPaint.hints(g2);
            float d = dim;
            // Grosser Punkt-Marker + zwei Satelliten (Punktwolke)
            float cr = d * 0.26f;
            float cx = d * 0.42f, cy = d * 0.46f;
            Ellipse2D main = new Ellipse2D.Float(cx - cr, cy - cr, 2 * cr, 2 * cr);
            g2.setPaint(FIconPaint.liquid(cx - cr, cy - cr, 2 * cr, 2 * cr));
            g2.fill(main);
            Shape oldClip = g2.getClip();
            g2.clip(main);
            FIconPaint.shine(g2, cx - cr, cy - cr, 2 * cr, cr);
            g2.setClip(oldClip);
            FIconPaint.rim(g2, main, Math.max(1f, d * 0.045f));
            g2.setColor(FIconPaint.DARK_HOLE);
            float ir = cr * 0.32f;
            g2.fill(new Ellipse2D.Float(cx - ir, cy - ir, 2 * ir, 2 * ir));
            // Satelliten
            g2.setColor(FIconPaint.INK);
            float sr = d * 0.07f;
            g2.fill(new Ellipse2D.Float(d * 0.76f - sr, d * 0.22f - sr, 2 * sr, 2 * sr));
            g2.fill(new Ellipse2D.Float(d * 0.72f - sr, d * 0.76f - sr, 2 * sr, 2 * sr));
        }
    },

    RASTER("Raster") {
        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            FIconPaint.hints(g2);
            float d = dim, pad = d * 0.14f;
            float size = d - 2 * pad;
            RoundRectangle2D frame = new RoundRectangle2D.Float(
                    pad, pad, size, size, d * 0.14f, d * 0.14f);
            g2.setPaint(FIconPaint.liquid(pad, pad, size, size));
            g2.fill(frame);
            Shape oldClip = g2.getClip();
            g2.clip(frame);
            FIconPaint.shine(g2, pad, pad, size, size * 0.5f);
            // Gitterlinien (GeoRaster-Zellen)
            g2.setColor(FIconPaint.alpha(FIconPaint.DARK_HOLE, 170));
            float lw = Math.max(1f, d * 0.05f);
            g2.setStroke(new BasicStroke(lw));
            float third = size / 3f;
            for (int i = 1; i <= 2; i++) {
                g2.draw(new Rectangle2D.Float(pad + i * third - lw / 2, pad, 0.1f, size));
                g2.draw(new Rectangle2D.Float(pad, pad + i * third - lw / 2, size, 0.1f));
            }
            g2.setClip(oldClip);
            FIconPaint.rim(g2, frame, Math.max(1f, d * 0.045f));
            // Eine "aktive" Zelle in INK
            g2.setColor(FIconPaint.alpha(FIconPaint.INK, 200));
            g2.fill(new Rectangle2D.Float(pad + third + lw, pad + lw, third - 2 * lw, third - 2 * lw));
        }
    },

    NETWORK("Netzwerk") {
        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            FIconPaint.hints(g2);
            float d = dim, pad = d * 0.16f;
            float n1x = d * 0.50f, n1y = pad;
            float n2x = pad, n2y = d - pad;
            float n3x = d - pad, n3y = d - pad * 1.2f;
            // Kanten zuerst (liquid-Strich)
            BasicStroke stroke = new BasicStroke(Math.max(1.2f, d * 0.08f),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            Path2D edges = new Path2D.Float();
            edges.moveTo(n1x, n1y);
            edges.lineTo(n2x, n2y);
            edges.lineTo(n3x, n3y);
            edges.lineTo(n1x, n1y);
            Shape fat = stroke.createStrokedShape(edges);
            g2.setPaint(FIconPaint.liquidDiagonal(pad, pad, d - pad, d - pad));
            g2.fill(fat);
            // Knoten als Glaskugeln
            float nr = d * 0.14f;
            float[][] nodes = {{n1x, n1y}, {n2x, n2y}, {n3x, n3y}};
            for (int i = 0; i < nodes.length; i++) {
                Ellipse2D node = new Ellipse2D.Float(
                        nodes[i][0] - nr, nodes[i][1] - nr, 2 * nr, 2 * nr);
                g2.setPaint(FIconPaint.liquid(nodes[i][0] - nr, nodes[i][1] - nr, 2 * nr, 2 * nr));
                g2.fill(node);
                Shape oldClip = g2.getClip();
                g2.clip(node);
                FIconPaint.shine(g2, nodes[i][0] - nr, nodes[i][1] - nr, 2 * nr, nr);
                g2.setClip(oldClip);
                FIconPaint.rim(g2, node, Math.max(1f, d * 0.035f));
            }
            // Zentrumsknoten-Punkt in INK
            g2.setColor(FIconPaint.INK);
            float ir = nr * 0.35f;
            g2.fill(new Ellipse2D.Float(n1x - ir, n1y - ir, 2 * ir, 2 * ir));
        }
    };

    private final String label;

    FLayerGeometryType(String label) {
        this.label = label;
    }

    /** Deutscher Anzeigename (Tooltips, Legende). */
    public String getLabel() {
        return label;
    }

    /** Case-insensitive Suche per name(); null wenn unbekannt. */
    public static FLayerGeometryType find(String name) {
        if (name == null) {
            return null;
        }
        for (FLayerGeometryType t : values()) {
            if (t.name().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return label;
    }
}
