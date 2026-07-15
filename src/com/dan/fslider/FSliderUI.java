package com.dan.fslider;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/**
 * UI-Delegate fuer {@link FSlider}. Voll-custom (kein {@code super.paint}) &ndash;
 * begruendet, weil Track, Liquid, Thumb, Glow, Ripple und Skala als
 * zusammenhaengende Glas-Szene in einer festen Reihenfolge gemalt werden.
 *
 * Java 8 kompatibel.
 */
public class FSliderUI extends BasicSliderUI {

    private final FSlider f;

    public FSliderUI(FSlider slider) {
        super(slider);
        this.f = slider;
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
    }

    @Override
    protected Dimension getThumbSize() {
        int s = f.getThumbSize();
        return new Dimension(s, s);
    }

    // Voll-custom Paint: eigene Reihenfolge, keine Default-Ticks/Labels.
    @Override
    public void paint(Graphics g, JComponent c) {
        calculateGeometry();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        boolean horizontal = f.getOrientation() == JSlider.HORIZONTAL;
        FSliderVariant v = f.getVariant();
        int thickness = v.trackThickness;

        // ---- Track-Geometrie (schlanke, zentrierte Glasrinne) ------------
        Rectangle bar = computeBar(horizontal, thickness);

        // ---- Thumb-Position aus geglaetteter Fuellung --------------------
        double frac = f.displayFrac;
        int thumbC = horizontal
                ? bar.x + (int) Math.round(frac * bar.width)
                : bar.y + bar.height - (int) Math.round(frac * bar.height);
        if (f.getInverted()) {
            thumbC = horizontal
                    ? bar.x + bar.width - (int) Math.round(frac * bar.width)
                    : bar.y + (int) Math.round(frac * bar.height);
        }

        // ---- Gefuellte Sub-Flaeche (Liquid) ------------------------------
        Rectangle fill = computeFill(bar, horizontal, frac, thumbC);
        // Geometrie an die Komponente melden (fuer Blasen-Spawn im Timer)
        f.updateLiquidBounds(fill.x, fill.y, fill.width, fill.height, horizontal);

        // 1) Track-Untergrund (dunkles Glas) + Innenschatten + Rim
        paintTrackBase(g2, bar);

        // 2) Liquid mit wellenfoermiger Oberflaeche, Schaum und Blasen
        if (!fill.isEmpty()) {
            paintLiquid(g2, bar, fill, horizontal, v);
        }

        // 3) Glas-Sheen ueber dem gesamten Track
        paintTrackSheen(g2, bar, horizontal);

        // 4) Skala (nur TICKS-Variante)
        if (v.showTicks) {
            paintScale(g2, bar, horizontal);
        }

        // 5) Thumb: Glow-Aura, Glaskugel, Reflex, Ripple
        paintThumbGlass(g2, horizontal, bar, thumbC);

        // 6) Wert-Bubble ueber dem Thumb beim Ziehen
        if (f.isShowValueBubble() && f.pressProgress > 0.02) {
            paintValueBubble(g2, horizontal, bar, thumbC);
        }

        g2.dispose();
    }

    // =====================================================================
    //  Geometrie-Helfer
    // =====================================================================

    private Rectangle computeBar(boolean horizontal, int thickness) {
        Rectangle t = trackRect;
        if (horizontal) {
            int cy = t.y + t.height / 2;
            return new Rectangle(t.x, cy - thickness / 2, t.width, thickness);
        } else {
            int cx = t.x + t.width / 2;
            return new Rectangle(cx - thickness / 2, t.y, thickness, t.height);
        }
    }

    private Rectangle computeFill(Rectangle bar, boolean horizontal, double frac, int thumbC) {
        if (frac <= 0.0) return new Rectangle(0, 0, 0, 0);
        if (horizontal) {
            if (f.getInverted()) {
                int x = thumbC;
                return new Rectangle(x, bar.y, bar.x + bar.width - x, bar.height);
            } else {
                return new Rectangle(bar.x, bar.y, thumbC - bar.x, bar.height);
            }
        } else {
            if (f.getInverted()) {
                return new Rectangle(bar.x, bar.y, bar.width, thumbC - bar.y);
            } else {
                int y = thumbC;
                return new Rectangle(bar.x, y, bar.width, bar.y + bar.height - y);
            }
        }
    }

    // =====================================================================
    //  Track-Untergrund
    // =====================================================================

    private void paintTrackBase(Graphics2D g2, Rectangle bar) {
        int arc = Math.min(f.getArc(), Math.min(bar.width, bar.height));
        RoundRectangle2D shape =
                new RoundRectangle2D.Float(bar.x, bar.y, bar.width, bar.height, arc, arc);

        // Weicher Schlagschatten
        int so = f.getShadowOffset();
        if (so > 0) {
            Color sh = f.getShadowColor();
            for (int i = so; i >= 1; i--) {
                float a = (sh.getAlpha() / 255f) * (i / (float) so) * 0.5f;
                g2.setColor(new Color(sh.getRed(), sh.getGreen(), sh.getBlue(),
                        Math.max(0, Math.min(255, (int) (a * 255)))));
                g2.fill(new RoundRectangle2D.Float(bar.x, bar.y + i, bar.width, bar.height, arc, arc));
            }
        }

        // Dunkle Glasrinne mit vertikalem Tiefen-Gradient
        Color base = f.getTrackColor();
        g2.setPaint(new GradientPaint(
                bar.x, bar.y, darker(base, 0.15f),
                bar.x, bar.y + bar.height, lighter(base, 0.08f)));
        g2.fill(shape);

        // Innenschatten oben (Vertiefung)
        Shape oldClip = g2.getClip();
        g2.setClip(shape);
        g2.setPaint(new GradientPaint(
                bar.x, bar.y, new Color(0, 0, 0, 90),
                bar.x, bar.y + Math.max(4, bar.height / 2), new Color(0, 0, 0, 0)));
        g2.fill(shape);
        g2.setClip(oldClip);

        // Glas-Rim
        g2.setStroke(new BasicStroke(f.getRimStrokeWidth()));
        g2.setColor(f.getGlassRim());
        g2.draw(shape);
    }

    private void paintTrackSheen(Graphics2D g2, Rectangle bar, boolean horizontal) {
        int arc = Math.min(f.getArc(), Math.min(bar.width, bar.height));
        RoundRectangle2D shape =
                new RoundRectangle2D.Float(bar.x, bar.y, bar.width, bar.height, arc, arc);
        Shape oldClip = g2.getClip();
        g2.setClip(shape);
        Color hi = f.getGlassHighlight();
        // Schmaler Glanzstreifen an der Oberkante (horizontal) bzw. linken Kante (vertikal)
        if (horizontal) {
            g2.setPaint(new GradientPaint(
                    bar.x, bar.y, new Color(hi.getRed(), hi.getGreen(), hi.getBlue(), hi.getAlpha()),
                    bar.x, bar.y + Math.max(3, bar.height / 2), new Color(hi.getRed(), hi.getGreen(), hi.getBlue(), 0)));
        } else {
            g2.setPaint(new GradientPaint(
                    bar.x, bar.y, new Color(hi.getRed(), hi.getGreen(), hi.getBlue(), hi.getAlpha()),
                    bar.x + Math.max(3, bar.width / 2), bar.y, new Color(hi.getRed(), hi.getGreen(), hi.getBlue(), 0)));
        }
        g2.fill(shape);
        g2.setClip(oldClip);
    }

    // =====================================================================
    //  Liquid mit Welle, Schaum, Blasen
    // =====================================================================

    private void paintLiquid(Graphics2D g2, Rectangle bar, Rectangle fill,
                             boolean horizontal, FSliderVariant v) {
        int arc = Math.min(f.getArc(), Math.min(bar.width, bar.height));

        // Clip auf die Track-Rundung, damit Liquid nicht ueber die Glaskante blutet.
        Area clip = new Area(new RoundRectangle2D.Float(
                bar.x, bar.y, bar.width, bar.height, arc, arc));
        Shape oldClip = g2.getClip();
        g2.clip(clip);

        // Wellenamplitude an die Track-Dicke koppeln (thin bars -> kleine Welle).
        double effAmp = Math.min(f.waveAmp, Math.max(1.5, v.trackThickness * 0.16));

        // Oberkante des Liquids = wellenfoermige Linie.
        int surfaceY = fill.y;
        Path2D path = new Path2D.Float();
        int steps = Math.max(2, fill.width);
        path.moveTo(fill.x, surfaceY + waveAt(0, effAmp));
        for (int i = 1; i <= steps; i++) {
            double frac = i / (double) steps;
            float x = fill.x + i;
            path.lineTo(x, surfaceY + waveAt(frac, effAmp));
        }
        // untere/rechte Begrenzung schliessen
        path.lineTo(fill.x + fill.width, fill.y + fill.height);
        path.lineTo(fill.x, fill.y + fill.height);
        path.closePath();

        // Vertikaler Liquid-Gradient (oben heller/tuerkis -> unten satter/weinrot).
        Color top = f.getLiquidColorTop();
        Color bot = f.getLiquidColorBottom();
        g2.setPaint(new GradientPaint(
                fill.x, surfaceY - (float) effAmp, top,
                fill.x, fill.y + fill.height, bot));
        g2.fill(path);

        // Innerer Glanzstreifen links im Liquid (Zylinderwirkung).
        g2.setPaint(new GradientPaint(
                fill.x, fill.y, new Color(255, 255, 255, 60),
                fill.x + Math.max(4, fill.width / 3), fill.y, new Color(255, 255, 255, 0)));
        g2.fill(path);

        // Schaumkrone + Blasen nur bei entsprechender Variante.
        if (v.showBubbles) {
            paintFoam(g2, fill, surfaceY, effAmp);
            paintBubbles(g2);
        }

        g2.setClip(oldClip);
    }

    private double waveAt(double frac, double amp) {
        return Math.sin(frac * Math.PI * 2.5 + f.waveOffset) * amp
             + Math.sin(frac * Math.PI * 1.3 + f.waveOffset * 0.7) * amp * 0.4;
    }

    private void paintFoam(Graphics2D g2, Rectangle fill, int surfaceY, double amp) {
        int foam = Math.max(3, fill.width / 14);
        double bw = fill.width / (double) foam;
        Color fc = f.getFoamColor();
        for (int i = 0; i < foam; i++) {
            double fx = fill.x + i * bw + waveAt(i / (double) foam, amp) * 1.2;
            double fy = surfaceY + waveAt(i / (double) foam + 0.5, amp);
            double br = bw * 0.6;
            float alpha = (float) (0.45 + 0.3 * Math.sin(f.waveOffset + i));
            alpha = Math.max(0.12f, Math.min(0.8f, alpha));
            g2.setColor(new Color(fc.getRed(), fc.getGreen(), fc.getBlue(),
                    (int) (alpha * fc.getAlpha())));
            g2.fill(new Ellipse2D.Double(fx, fy - br * 0.55, br, br * 0.7));
        }
    }

    private void paintBubbles(Graphics2D g2) {
        for (FSlider.Bubble b : f.bubbles) {
            int a = (int) (Math.max(0f, Math.min(1f, b.life)) * 150);
            if (a <= 0) continue;
            g2.setColor(new Color(200, 240, 255, a));
            g2.fill(new Ellipse2D.Float(b.x - b.r, b.y - b.r, b.r * 2, b.r * 2));
            // kleiner Lichtpunkt oben-links
            g2.setColor(new Color(255, 255, 255, Math.min(180, a + 40)));
            float gr = b.r * 0.5f;
            g2.fill(new Ellipse2D.Float(b.x - b.r * 0.5f, b.y - b.r * 0.5f, gr, gr));
        }
    }

    // =====================================================================
    //  Thumb (Glaskugel)
    // =====================================================================

    private void paintThumbGlass(Graphics2D g2, boolean horizontal, Rectangle bar, int thumbC) {
        int s = f.getThumbSize();
        int cx, cy;
        if (horizontal) {
            cx = thumbC;
            cy = bar.y + bar.height / 2;
        } else {
            cx = bar.x + bar.width / 2;
            cy = thumbC;
        }
        int r = s / 2;

        // --- Hover-Glow-Aura (pulsierend) unter dem Thumb ---
        if (f.isGlowEnabled() && f.hoverProgress > 0.01) {
            double pulse = 0.75 + 0.25 * Math.sin(f.glowPulse);
            float haloR = (float) (r + 6 + 5 * f.hoverProgress * pulse);
            Color glow = f.getGlowColor();
            int ga = (int) (glow.getAlpha() * f.hoverProgress * pulse);
            RadialGradientPaint halo = new RadialGradientPaint(
                    new Point2D.Float(cx, cy), haloR,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{
                            new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), Math.min(255, ga)),
                            new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), Math.min(255, ga / 2)),
                            new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0)});
            g2.setPaint(halo);
            g2.fill(new Ellipse2D.Float(cx - haloR, cy - haloR, haloR * 2, haloR * 2));
        }

        // --- Schlagschatten ---
        int so = f.getShadowOffset();
        if (so > 0) {
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fill(new Ellipse2D.Float(cx - r, cy - r + so * 0.6f, s, s));
        }

        // --- Glaskoerper als radialer Verlauf (Reflex oben-links) ---
        Color top = f.getLiquidColorTop();
        Color bot = f.getLiquidColorBottom();
        Point2D center = new Point2D.Float(cx - r * 0.35f, cy - r * 0.4f);
        float rad = s * 0.85f;
        RadialGradientPaint body = new RadialGradientPaint(
                center, rad,
                new float[]{0f, 0.35f, 0.72f, 1f},
                new Color[]{
                        new Color(255, 255, 255, 235),
                        lighter(top, 0.25f),
                        top,
                        darker(bot, 0.10f)});
        g2.setPaint(body);
        Ellipse2D sphere = new Ellipse2D.Float(cx - r, cy - r, s, s);
        g2.fill(sphere);

        // --- Ripple (auf Kugel geclippt) ---
        if (!f.ripples.isEmpty()) {
            Shape oldClip = g2.getClip();
            g2.clip(sphere);
            Color rc = f.getRippleColor();
            for (FSlider.Ripple rp : f.ripples) {
                int a = (int) (Math.max(0f, rp.alpha) * rc.getAlpha());
                if (a <= 0) continue;
                g2.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), a));
                g2.fill(new Ellipse2D.Float(rp.x - rp.radius, rp.y - rp.radius,
                        rp.radius * 2, rp.radius * 2));
            }
            g2.setClip(oldClip);
        }

        // --- Oberer Glanz-Reflex (Highlight-Kappe) ---
        Color hi = f.getGlassHighlight();
        g2.setPaint(new GradientPaint(
                cx, cy - r, new Color(255, 255, 255, Math.min(220, hi.getAlpha() + 90)),
                cx, cy, new Color(255, 255, 255, 0)));
        g2.fill(new Ellipse2D.Float(cx - r * 0.7f, cy - r * 0.85f, s * 0.7f, s * 0.6f));

        // --- Rim ---
        float rw = Math.max(1.2f, f.getRimStrokeWidth() + 0.6f);
        g2.setStroke(new BasicStroke(rw));
        g2.setColor(f.getGlassRim());
        g2.draw(sphere);

        // --- Press-Einsenkung: dunkler Innenring ---
        if (f.pressProgress > 0.02) {
            int pa = (int) (60 * f.pressProgress);
            g2.setColor(new Color(0, 0, 0, pa));
            g2.setStroke(new BasicStroke(1.4f));
            g2.draw(new Ellipse2D.Float(cx - r + 2, cy - r + 2, s - 4, s - 4));
        }
    }

    // =====================================================================
    //  Skala (TICKS-Variante) & Wert-Bubble
    // =====================================================================

    private void paintScale(Graphics2D g2, Rectangle bar, boolean horizontal) {
        int min = f.getMinimum();
        int max = f.getMaximum();
        int range = max - min;
        if (range <= 0) return;

        int major = f.getMajorTickSpacing();
        if (major <= 0) major = Math.max(1, range / 5); // sinnvolle Default-Teilung
        int minor = f.getMinorTickSpacing();

        g2.setColor(f.getTickColor());
        g2.setStroke(new BasicStroke(1.2f));

        // Minor-Ticks (kurz)
        if (minor > 0) {
            for (int val = min; val <= max; val += minor) {
                drawTick(g2, bar, horizontal, valFrac(val, min, range), 4, 90);
            }
        }
        // Major-Ticks (lang) + Labels
        Font lblFont = f.getFont().deriveFont(Font.PLAIN, 10f);
        g2.setFont(lblFont);
        FontMetrics fm = g2.getFontMetrics();
        for (int val = min; val <= max; val += major) {
            double vf = valFrac(val, min, range);
            drawTick(g2, bar, horizontal, vf, 7, 200);
            String txt = f.formatValue(val);
            drawTickLabel(g2, bar, horizontal, vf, txt, fm);
        }
    }

    private double valFrac(int val, int min, int range) {
        double frac = (val - min) / (double) range;
        return f.getInverted() ? 1.0 - frac : frac;
    }

    private void drawTick(Graphics2D g2, Rectangle bar, boolean horizontal,
                          double frac, int len, int alpha) {
        Color tc = f.getTickColor();
        g2.setColor(new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), alpha));
        if (horizontal) {
            int x = bar.x + (int) Math.round(frac * bar.width);
            int y0 = bar.y + bar.height + 3;
            g2.drawLine(x, y0, x, y0 + len);
        } else {
            int y = bar.y + bar.height - (int) Math.round(frac * bar.height);
            if (f.getInverted()) y = bar.y + (int) Math.round(frac * bar.height);
            int x0 = bar.x + bar.width + 3;
            g2.drawLine(x0, y, x0 + len, y);
        }
    }

    private void drawTickLabel(Graphics2D g2, Rectangle bar, boolean horizontal,
                               double frac, String txt, FontMetrics fm) {
        Color tc = f.getTickColor();
        g2.setColor(new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), 230));
        if (horizontal) {
            int x = bar.x + (int) Math.round(frac * bar.width) - fm.stringWidth(txt) / 2;
            int y = bar.y + bar.height + 12 + fm.getAscent();
            g2.drawString(txt, x, y);
        } else {
            int y = bar.y + bar.height - (int) Math.round(frac * bar.height) + fm.getAscent() / 2 - 1;
            if (f.getInverted()) y = bar.y + (int) Math.round(frac * bar.height) + fm.getAscent() / 2 - 1;
            int x = bar.x + bar.width + 13;
            g2.drawString(txt, x, y);
        }
    }

    private void paintValueBubble(Graphics2D g2, boolean horizontal, Rectangle bar, int thumbC) {
        String txt = f.formatValue(f.getValue());
        Font font = f.getFont().deriveFont(Font.BOLD, 11f);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(txt);
        int padX = 8, padY = 4;
        int bw = tw + padX * 2;
        int bh = fm.getHeight() + padY * 2;
        int r = f.getThumbSize() / 2;

        int bx, by;
        if (horizontal) {
            bx = thumbC - bw / 2;
            by = bar.y + bar.height / 2 - r - bh - 6;
        } else {
            bx = bar.x + bar.width / 2 - r - bw - 8;
            by = thumbC - bh / 2;
        }
        bx = Math.max(0, Math.min(bx, f.getWidth() - bw));
        by = Math.max(0, by);

        float alpha = (float) Math.min(1.0, f.pressProgress * 1.2);

        // Glas-Bubble
        RoundRectangle2D rr = new RoundRectangle2D.Float(bx, by, bw, bh, 10, 10);
        g2.setColor(new Color(20, 28, 38, (int) (210 * alpha)));
        g2.fill(rr);
        g2.setStroke(new BasicStroke(1.2f));
        Color rim = f.getGlassRim();
        g2.setColor(new Color(rim.getRed(), rim.getGreen(), rim.getBlue(), (int) (rim.getAlpha() * alpha)));
        g2.draw(rr);

        Color tx = f.getTextColor();
        g2.setColor(new Color(tx.getRed(), tx.getGreen(), tx.getBlue(), (int) (255 * alpha)));
        g2.drawString(txt, bx + padX, by + padY + fm.getAscent());
    }

    // =====================================================================
    //  Farbhelfer (Java 8, ohne FColors-Abhaengigkeit im Hot-Path)
    // =====================================================================

    private static Color lighter(Color c, float amt) {
        int r = (int) Math.min(255, c.getRed() + 255 * amt);
        int g = (int) Math.min(255, c.getGreen() + 255 * amt);
        int b = (int) Math.min(255, c.getBlue() + 255 * amt);
        return new Color(r, g, b, c.getAlpha());
    }

    private static Color darker(Color c, float amt) {
        int r = (int) Math.max(0, c.getRed() * (1 - amt));
        int g = (int) Math.max(0, c.getGreen() * (1 - amt));
        int b = (int) Math.max(0, c.getBlue() * (1 - amt));
        return new Color(r, g, b, c.getAlpha());
    }
}
