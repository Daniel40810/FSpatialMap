package com.dan.ficons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import static com.dan.ficons.FIconPaint.DARK_HOLE;
import static com.dan.ficons.FIconPaint.GLASS_BODY;
import static com.dan.ficons.FIconPaint.INK;
import static com.dan.ficons.FIconPaint.TURQUOISE;
import static com.dan.ficons.FIconPaint.WINE_RED;
import static com.dan.ficons.FIconPaint.alpha;
import static com.dan.ficons.FIconPaint.liquid;
import static com.dan.ficons.FIconPaint.liquidDiagonal;
import static com.dan.ficons.FIconPaint.rim;
import static com.dan.ficons.FIconPaint.shine;

/**
 * Die Standard-Bibliothek gemalter F-Style-Glyphs. Jede Konstante malt ihr
 * Symbol vollständig programmatisch (kein PNG-Asset) im ReagenzglasBar-Look:
 * Türkis&rarr;Weinrot-Verlauf, Glasrand, heller Glanzstreifen.
 *
 * <p>Als Enum direkt JavaBean-/Palette-tauglich: {@code FIconComponent.setType(...)}
 * erscheint in NetBeans automatisch als Dropdown.</p>
 *
 * @author com.dan
 */
public enum FIconType implements FIcon {

    ADD("Hinzufügen") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.12f, sz = d - 2 * pad, cx = d / 2f, cy = d / 2f;
            Ellipse2D circ = new Ellipse2D.Float(pad, pad, sz, sz);
            g2.setPaint(liquid(pad, pad, sz, sz));
            g2.fill(circ);
            clipShine(g2, circ, pad, pad, sz, sz);
            rim(g2, circ, Math.max(1f, d * 0.045f));
            g2.setStroke(round(d * 0.11f));
            g2.setColor(INK);
            float arm = sz * 0.27f;
            g2.draw(new Line2D.Float(cx - arm, cy, cx + arm, cy));
            g2.draw(new Line2D.Float(cx, cy - arm, cx, cy + arm));
        }
    },

    DELETE("Löschen") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.18f, w = d - 2 * pad;
            float lidH = Math.max(2f, d * 0.10f);
            float top = pad + d * 0.06f;
            float bodyY = top + lidH + d * 0.03f;
            float bodyH = d - pad - bodyY;
            // Henkel
            g2.setColor(WINE_RED);
            g2.fill(new RoundRectangle2D.Float(d / 2f - w * 0.16f, pad, w * 0.32f, d * 0.07f, 3, 3));
            // Deckel
            RoundRectangle2D lid = new RoundRectangle2D.Float(pad - d * 0.02f, top, w + d * 0.04f, lidH, lidH, lidH);
            g2.fill(lid);
            // Korpus
            RoundRectangle2D body = new RoundRectangle2D.Float(pad + d * 0.03f, bodyY, w - d * 0.06f, bodyH, d * 0.12f, d * 0.12f);
            g2.setPaint(liquid(pad, bodyY, w, bodyH));
            g2.fill(body);
            clipShine(g2, body, pad, bodyY, w, bodyH);
            rim(g2, body, Math.max(1f, d * 0.04f));
            // Rillen
            g2.setColor(alpha(INK, 150));
            g2.setStroke(round(Math.max(1f, d * 0.035f)));
            for (int i = 1; i <= 2; i++) {
                float x = pad + d * 0.03f + (w - d * 0.06f) * (i / 3f);
                g2.draw(new Line2D.Float(x, bodyY + bodyH * 0.18f, x, bodyY + bodyH * 0.82f));
            }
        }
    },

    SAVE("Speichern") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.15f, sz = d - 2 * pad, cut = sz * 0.24f;
            Path2D body = new Path2D.Float();
            body.moveTo(pad, pad);
            body.lineTo(pad + sz - cut, pad);
            body.lineTo(pad + sz, pad + cut);
            body.lineTo(pad + sz, pad + sz);
            body.lineTo(pad, pad + sz);
            body.closePath();
            g2.setPaint(liquid(pad, pad, sz, sz));
            g2.fill(body);
            clipShine(g2, body, pad, pad, sz, sz);
            rim(g2, body, Math.max(1f, d * 0.04f));
            // Shutter (oben)
            g2.setColor(DARK_HOLE);
            g2.fill(new Rectangle2D.Float(pad + sz * 0.22f, pad, sz * 0.46f, sz * 0.30f));
            g2.setColor(alpha(INK, 210));
            g2.fill(new Rectangle2D.Float(pad + sz * 0.55f, pad + sz * 0.04f, sz * 0.08f, sz * 0.20f));
            // Label (unten)
            g2.setColor(alpha(INK, 230));
            g2.fill(new RoundRectangle2D.Float(pad + sz * 0.18f, pad + sz * 0.46f, sz * 0.64f, sz * 0.40f, sz * 0.08f, sz * 0.08f));
            g2.setColor(alpha(WINE_RED, 200));
            g2.setStroke(round(Math.max(1f, d * 0.03f)));
            g2.draw(new Line2D.Float(pad + sz * 0.28f, pad + sz * 0.60f, pad + sz * 0.72f, pad + sz * 0.60f));
            g2.draw(new Line2D.Float(pad + sz * 0.28f, pad + sz * 0.72f, pad + sz * 0.62f, pad + sz * 0.72f));
        }
    },

    EDIT("Bearbeiten") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            AffineTransform old = g2.getTransform();
            g2.rotate(Math.toRadians(45), d / 2f, d / 2f);
            float pw = d * 0.22f, px = d / 2f - pw / 2f;
            float top = d * 0.15f, bot = d * 0.85f;
            // Radiergummi-Kappe
            g2.setColor(WINE_RED);
            g2.fill(new RoundRectangle2D.Float(px, top, pw, d * 0.12f, pw * 0.4f, pw * 0.4f));
            // Schaft
            float shaftTop = top + d * 0.11f, shaftBot = bot - d * 0.18f;
            Rectangle2D shaft = new Rectangle2D.Float(px, shaftTop, pw, shaftBot - shaftTop);
            g2.setPaint(liquid(px, shaftTop, pw, shaftBot - shaftTop));
            g2.fill(shaft);
            clipShine(g2, shaft, px, shaftTop, pw, shaftBot - shaftTop);
            // Holzspitze
            Path2D tip = new Path2D.Float();
            tip.moveTo(px, shaftBot);
            tip.lineTo(px + pw, shaftBot);
            tip.lineTo(d / 2f, bot);
            tip.closePath();
            g2.setColor(alpha(INK, 235));
            g2.fill(tip);
            // Mine
            Path2D lead = new Path2D.Float();
            lead.moveTo(d / 2f - pw * 0.22f, bot - d * 0.07f);
            lead.lineTo(d / 2f + pw * 0.22f, bot - d * 0.07f);
            lead.lineTo(d / 2f, bot);
            lead.closePath();
            g2.setColor(new Color(0x20, 0x20, 0x20));
            g2.fill(lead);
            g2.setTransform(old);
        }
    },

    SEARCH("Suchen") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, r = d * 0.25f, cx = d * 0.42f, cy = d * 0.42f;
            Ellipse2D inner = new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r);
            g2.setColor(GLASS_BODY);
            g2.fill(inner);
            clipShine(g2, inner, cx - r, cy - r, 2 * r, 2 * r);
            // Griff
            g2.setColor(WINE_RED);
            g2.setStroke(new BasicStroke(d * 0.13f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float hx = cx + (float) Math.cos(Math.PI / 4) * r;
            float hy = cy + (float) Math.sin(Math.PI / 4) * r;
            g2.draw(new Line2D.Float(hx, hy, d * 0.84f, d * 0.84f));
            // Ring
            g2.setStroke(new BasicStroke(d * 0.11f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setPaint(liquidDiagonal(cx - r, cy - r, cx + r, cy + r));
            g2.draw(inner);
        }
    },

    REFRESH("Aktualisieren") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.16f, sz = d - 2 * pad, cx = d / 2f, cy = d / 2f, r = sz / 2f;
            float start = 65, extent = 255;
            Arc2D arc = new Arc2D.Float(pad, pad, sz, sz, start, extent, Arc2D.OPEN);
            g2.setStroke(new BasicStroke(d * 0.12f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g2.setPaint(liquidDiagonal(pad, pad, pad + sz, pad + sz));
            g2.draw(arc);
            // Pfeilspitze am Bogenende
            double end = Math.toRadians(start + extent);
            float ex = cx + (float) Math.cos(end) * r;
            float ey = cy - (float) Math.sin(end) * r;
            double tan = end + Math.PI / 2;          // Tangente (CCW)
            float tx = (float) Math.cos(tan), ty = -(float) Math.sin(tan);
            float nx = (float) Math.cos(end), ny = -(float) Math.sin(end);
            float a = d * 0.16f;
            Path2D head = new Path2D.Float();
            head.moveTo(ex + tx * a, ey + ty * a);
            head.lineTo(ex - nx * a * 0.9f, ey - ny * a * 0.9f);
            head.lineTo(ex + nx * a * 0.9f, ey + ny * a * 0.9f);
            head.closePath();
            g2.setColor(TURQUOISE);
            g2.fill(head);
        }
    },

    SETTINGS("Einstellungen") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, cx = d / 2f, cy = d / 2f;
            float rOut = d * 0.42f, toothW = d * 0.16f, toothH = d * 0.16f;
            // Zähne (8) als gerundete Rechtecke radial
            g2.setPaint(liquid(d * 0.1f, d * 0.1f, d * 0.8f, d * 0.8f));
            RoundRectangle2D tooth = new RoundRectangle2D.Float(
                    cx - toothW / 2f, cy - rOut, toothW, toothH, toothW * 0.4f, toothW * 0.4f);
            for (int k = 0; k < 8; k++) {
                AffineTransform at = AffineTransform.getRotateInstance(
                        Math.toRadians(k * 45.0), cx, cy);
                g2.fill(at.createTransformedShape(tooth));
            }
            // Körper
            float br = d * 0.30f;
            Ellipse2D body = new Ellipse2D.Float(cx - br, cy - br, 2 * br, 2 * br);
            g2.fill(body);
            clipShine(g2, body, cx - br, cy - br, 2 * br, 2 * br);
            rim(g2, body, Math.max(1f, d * 0.04f));
            // Nabe
            float hr = d * 0.13f;
            g2.setColor(DARK_HOLE);
            g2.fill(new Ellipse2D.Float(cx - hr, cy - hr, 2 * hr, 2 * hr));
            g2.setColor(TURQUOISE);
            float dr = d * 0.05f;
            g2.fill(new Ellipse2D.Float(cx - dr, cy - dr, 2 * dr, 2 * dr));
        }
    },

    DATABASE("Datenbank") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.18f, w = d - 2 * pad;
            float ellH = Math.max(3f, d * 0.18f);
            float top = pad, bot = d - pad;
            float midTop = top + ellH / 2f, midBot = bot - ellH / 2f;
            // Korpus
            Rectangle2D rect = new Rectangle2D.Float(pad, midTop, w, midBot - midTop);
            g2.setPaint(liquid(pad, top, w, bot - top));
            g2.fill(rect);
            // unterer Abschluss
            g2.fill(new Ellipse2D.Float(pad, bot - ellH, w, ellH));
            // Glanz auf Korpus
            Area bodyArea = new Area(rect);
            bodyArea.add(new Area(new Ellipse2D.Float(pad, bot - ellH, w, ellH)));
            bodyArea.add(new Area(new Ellipse2D.Float(pad, top, w, ellH)));
            clipShine(g2, bodyArea, pad, top, w, bot - top);
            // Trennlinien (Disk-Stapel)
            g2.setColor(alpha(INK, 150));
            g2.setStroke(round(Math.max(1f, d * 0.035f)));
            float h = midBot - midTop;
            for (float f : new float[]{0.34f, 0.67f}) {
                float yy = midTop + h * f - ellH / 2f;
                g2.draw(new Arc2D.Float(pad, yy, w, ellH, 180, 180, Arc2D.OPEN));
            }
            // Deckel (heller Türkis)
            Ellipse2D lid = new Ellipse2D.Float(pad, top, w, ellH);
            g2.setColor(new Color(0x2E, 0xD9, 0xD0));
            g2.fill(lid);
            rim(g2, lid, Math.max(1f, d * 0.04f));
        }
    },

    GEO("Geo / Karte") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.13f, sz = d - 2 * pad, cx = d / 2f, cy = d / 2f, r = sz / 2f;
            Ellipse2D globe = new Ellipse2D.Float(pad, pad, sz, sz);
            g2.setPaint(liquid(pad, pad, sz, sz));
            g2.fill(globe);
            clipShine(g2, globe, pad, pad, sz, sz);
            // Gitter (auf Kugel geclippt)
            Shape oc = g2.getClip();
            g2.clip(globe);
            g2.setColor(alpha(INK, 175));
            g2.setStroke(round(Math.max(1f, d * 0.028f)));
            g2.draw(new Line2D.Float(pad, cy, pad + sz, cy));                       // Äquator
            g2.draw(new Line2D.Float(pad, cy - r * 0.52f, pad + sz, cy - r * 0.52f)); // Breitengrad N
            g2.draw(new Line2D.Float(pad, cy + r * 0.52f, pad + sz, cy + r * 0.52f)); // Breitengrad S
            g2.draw(new Line2D.Float(cx, pad, cx, pad + sz));                        // Meridian Mitte
            g2.draw(new Ellipse2D.Float(cx - sz * 0.27f, pad, sz * 0.54f, sz));      // Seiten-Meridiane
            g2.setClip(oc);
            rim(g2, globe, Math.max(1f, d * 0.045f));
        }
    },

    LOCATION("Standort") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, cx = d / 2f, w = d * 0.54f, x = cx - w / 2f, topY = d * 0.12f;
            float tipY = d * 0.90f, r = w / 2f, cyc = topY + r;
            // Tropfenform
            Path2D pin = new Path2D.Float();
            pin.moveTo(cx, tipY);
            pin.quadTo(x, cyc + r * 0.55f, x, cyc);
            Arc2D top = new Arc2D.Float(x, topY, w, w, 180, -180, Arc2D.OPEN);
            pin.append(top, true);
            pin.quadTo(x + w, cyc + r * 0.55f, cx, tipY);
            pin.closePath();
            g2.setPaint(liquid(x, topY, w, tipY - topY));
            g2.fill(pin);
            clipShine(g2, pin, x, topY, w, tipY - topY);
            rim(g2, pin, Math.max(1f, d * 0.04f));
            // Loch
            float hr = w * 0.20f;
            Ellipse2D hole = new Ellipse2D.Float(cx - hr, cyc - hr, 2 * hr, 2 * hr);
            g2.setColor(DARK_HOLE);
            g2.fill(hole);
        }
    },

    CLOSE("Schließen") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.10f, sz = d - 2 * pad, cx = d / 2f, cy = d / 2f;
            Ellipse2D circ = new Ellipse2D.Float(pad, pad, sz, sz);
            g2.setPaint(new java.awt.GradientPaint(
                    pad, pad, WINE_RED, pad + sz, pad + sz, new Color(0x4D, 0x00, 0x10)));
            g2.fill(circ);
            clipShine(g2, circ, pad, pad, sz, sz);
            rim(g2, circ, Math.max(1f, d * 0.045f));
            g2.setColor(INK);
            g2.setStroke(round(d * 0.11f));
            float a = sz * 0.26f;
            g2.draw(new Line2D.Float(cx - a, cy - a, cx + a, cy + a));
            g2.draw(new Line2D.Float(cx - a, cy + a, cx + a, cy - a));
        }
    },

    MENU("Menü") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.18f, w = d - 2 * pad;
            float bh = Math.max(2f, d * 0.11f);
            float gap = (d - 2 * pad - 3 * bh) / 2f;
            g2.setPaint(liquid(pad, pad, w, d - 2 * pad));
            for (int i = 0; i < 3; i++) {
                float y = pad + i * (bh + gap);
                g2.fill(new RoundRectangle2D.Float(pad, y, w, bh, bh, bh));
            }
            // Glanz auf oberstem Balken
            g2.setColor(alpha(java.awt.Color.WHITE, 90));
            g2.fill(new RoundRectangle2D.Float(pad, pad, w * 0.5f, Math.max(1f, bh * 0.5f), bh, bh));
        }
    },

    DOWNLOAD("Download") {
        public void paintGlyph(Graphics2D g2, int dim) {
            arrow(g2, dim, true);
        }
    },

    UPLOAD("Upload") {
        public void paintGlyph(Graphics2D g2, int dim) {
            arrow(g2, dim, false);
        }
    },

    // ----- Fenster-Steuerung (für FFrame-Taskbar) -----

    MINIMIZE("Minimieren") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, cx = d / 2f, hw = d * 0.28f;
            float y = d * 0.66f, th = Math.max(2f, d * 0.11f);
            g2.setPaint(liquid(cx - hw, y, hw * 2, th));
            g2.fill(new RoundRectangle2D.Float(cx - hw, y - th / 2f, hw * 2, th, th, th));
            g2.setColor(alpha(java.awt.Color.WHITE, 90));
            g2.fill(new RoundRectangle2D.Float(cx - hw, y - th / 2f, hw, Math.max(1f, th * 0.5f), th, th));
        }
    },

    MAXIMIZE("Maximieren") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.20f, sz = d - 2 * pad;
            RoundRectangle2D box = new RoundRectangle2D.Float(pad, pad, sz, sz, d * 0.10f, d * 0.10f);
            g2.setPaint(liquid(pad, pad, sz, sz));
            g2.fill(box);
            clipShine(g2, box, pad, pad, sz, sz);
            rim(g2, box, Math.max(1f, d * 0.045f));
            // Innenausschnitt als „Fensterfläche"
            g2.setColor(DARK_HOLE);
            float ip = pad + Math.max(1.5f, d * 0.09f);
            float isz = sz - 2 * Math.max(1.5f, d * 0.09f);
            g2.fill(new RoundRectangle2D.Float(ip, ip + d * 0.02f, isz, isz - d * 0.02f, d * 0.06f, d * 0.06f));
        }
    },

    RESTORE("Wiederherstellen") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, sz = d * 0.44f, off = d * 0.12f;
            float frontX = d * 0.18f, frontY = d * 0.30f;
            // hinteres Fenster (nur oben/rechts sichtbar)
            RoundRectangle2D back = new RoundRectangle2D.Float(
                    frontX + off, frontY - off, sz, sz, d * 0.09f, d * 0.09f);
            g2.setColor(alpha(TURQUOISE, 150));
            g2.fill(back);
            rim(g2, back, Math.max(1f, d * 0.04f));
            // vorderes Fenster
            RoundRectangle2D front = new RoundRectangle2D.Float(frontX, frontY, sz, sz, d * 0.09f, d * 0.09f);
            g2.setPaint(liquid(frontX, frontY, sz, sz));
            g2.fill(front);
            clipShine(g2, front, frontX, frontY, sz, sz);
            rim(g2, front, Math.max(1f, d * 0.045f));
            g2.setColor(DARK_HOLE);
            float ip = Math.max(1.5f, d * 0.08f);
            g2.fill(new RoundRectangle2D.Float(frontX + ip, frontY + ip + d * 0.02f,
                    sz - 2 * ip, sz - 2 * ip - d * 0.02f, d * 0.05f, d * 0.05f));
        }
    },

    CAT("Katze") {
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, cx = d / 2f;
            // Kopf
            float hy = d * 0.30f, hw = d * 0.62f, hh = d * 0.50f;
            RoundRectangle2D head = new RoundRectangle2D.Float(
                    cx - hw / 2f, hy, hw, hh, hw * 0.5f, hh * 0.7f);
            // Ohren (Dreiecke) zuerst, damit der Kopf sie überdeckt
            Path2D earL = new Path2D.Float();
            earL.moveTo(cx - hw * 0.42f, hy + hh * 0.10f);
            earL.lineTo(cx - hw * 0.30f, hy - hh * 0.34f);
            earL.lineTo(cx - hw * 0.04f, hy + hh * 0.06f);
            earL.closePath();
            Path2D earR = new Path2D.Float();
            earR.moveTo(cx + hw * 0.42f, hy + hh * 0.10f);
            earR.lineTo(cx + hw * 0.30f, hy - hh * 0.34f);
            earR.lineTo(cx + hw * 0.04f, hy + hh * 0.06f);
            earR.closePath();
            g2.setPaint(liquidDiagonal(cx - hw / 2f, hy - hh * 0.34f, cx + hw / 2f, hy + hh));
            g2.fill(earL);
            g2.fill(earR);
            g2.fill(head);
            clipShine(g2, head, cx - hw / 2f, hy, hw, hh);
            rim(g2, head, Math.max(1f, d * 0.04f));
            // Augen
            g2.setColor(INK);
            float er = Math.max(1f, d * 0.055f), ey = hy + hh * 0.42f, ex = hw * 0.20f;
            g2.fill(new Ellipse2D.Float(cx - ex - er, ey - er, 2 * er, 2 * er));
            g2.fill(new Ellipse2D.Float(cx + ex - er, ey - er, 2 * er, 2 * er));
            // Näschen
            g2.setColor(alpha(WINE_RED, 235));
            float ny = hy + hh * 0.62f, nw = d * 0.07f;
            Path2D nose = new Path2D.Float();
            nose.moveTo(cx - nw, ny);
            nose.lineTo(cx + nw, ny);
            nose.lineTo(cx, ny + nw);
            nose.closePath();
            g2.fill(nose);
            // Schnurrhaare
            g2.setColor(alpha(INK, 200));
            g2.setStroke(round(Math.max(1f, d * 0.03f)));
            float wy = ny + nw * 0.4f;
            g2.draw(new Line2D.Float(cx - hw * 0.10f, wy, cx - hw * 0.52f, wy - d * 0.03f));
            g2.draw(new Line2D.Float(cx - hw * 0.10f, wy + d * 0.03f, cx - hw * 0.50f, wy + d * 0.06f));
            g2.draw(new Line2D.Float(cx + hw * 0.10f, wy, cx + hw * 0.52f, wy - d * 0.03f));
            g2.draw(new Line2D.Float(cx + hw * 0.10f, wy + d * 0.03f, cx + hw * 0.50f, wy + d * 0.06f));
        }
    };

    // ----------------------------------------------------------- Metadaten

    private final String label;

    FIconType(String label) {
        this.label = label;
    }

    /** Anzeigename (deutsch) für Tooltips/Demos. */
    public String getLabel() {
        return label;
    }

    /** Sucht einen Typ case-insensitiv per {@code name()}; {@code null} wenn unbekannt. */
    public static FIconType find(String name) {
        if (name == null) return null;
        for (FIconType t : values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    // ----------------------------------------------------------- gemeinsame Helfer

    private static BasicStroke round(float w) {
        return new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    /** Glanzstreifen, auf {@code shape} geclippt; stellt das Clip wieder her. */
    private static void clipShine(Graphics2D g2, Shape shape, float x, float y, float w, float h) {
        Shape oc = g2.getClip();
        g2.clip(shape);
        shine(g2, x, y, w, h);
        g2.setClip(oc);
    }

    /** Download-/Upload-Pfeil mit Ablage-U. */
    private static void arrow(Graphics2D g2, int dim, boolean down) {
        float d = dim, cx = d / 2f, shaftW = d * 0.15f;
        float headHalf = d * 0.19f, headLen = d * 0.20f;
        float aTop = d * 0.15f, aBot = d * 0.62f;
        float headBaseY = down ? aBot : (d * 0.74f - (aBot - aTop));
        // Wir definieren konkret je Richtung:
        float shaftTop, shaftBot, tipY, baseY;
        if (down) {
            shaftTop = aTop; shaftBot = d * 0.52f; baseY = shaftBot; tipY = baseY + headLen;
        } else {
            tipY = d * 0.14f; baseY = tipY + headLen; shaftTop = baseY; shaftBot = d * 0.66f;
        }
        g2.setPaint(liquid(cx - shaftW, aTop, shaftW * 2, d * 0.7f));
        g2.fill(new RoundRectangle2D.Float(cx - shaftW / 2f, shaftTop, shaftW, shaftBot - shaftTop, shaftW, shaftW));
        Path2D head = new Path2D.Float();
        head.moveTo(cx - headHalf, baseY);
        head.lineTo(cx + headHalf, baseY);
        head.lineTo(cx, tipY);
        head.closePath();
        g2.fill(head);
        // Ablage-U
        g2.setColor(alpha(INK, 215));
        g2.setStroke(new BasicStroke(d * 0.075f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float ty = d * 0.74f, by = d * 0.86f, hw = d * 0.27f;
        Path2D tray = new Path2D.Float();
        tray.moveTo(cx - hw, ty);
        tray.lineTo(cx - hw, by);
        tray.lineTo(cx + hw, by);
        tray.lineTo(cx + hw, ty);
        g2.draw(tray);
    }
}
