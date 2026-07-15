package com.dan.flabel;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * FLabel — ein animiertes JLabel im "ReagenzglasBar"-Stil (Labor-Dark-Look).
 *
 * <p>Eigenständige Effekte:</p>
 * <ul>
 *   <li><b>Hover</b>: weiches Einblenden einer Glas-Hintergrundfläche
 *       (Türkis&rarr;Weinrot-Gradient), heller Glanzstreifen links,
 *       hellblauer Glasrand und ein flüssig wachsender Akzent-Balken unten.</li>
 *   <li><b>Ripple</b>: beim Klick breitet sich eine Welle vom Klickpunkt aus
 *       und verblasst &mdash; auf die abgerundete Fläche geclippt.</li>
 * </ul>
 *
 * <p>Voll JavaBean-konform: öffentlicher No-Arg-Konstruktor, Getter/Setter,
 * {@code repaint()} nach visuellen Änderungen, {@code revalidate()} bei
 * layout-relevanten Änderungen (Padding). Keine externen Abhängigkeiten,
 * nur Java SE Swing/AWT.</p>
 *
 * @author com.dan
 */
public class FLabel extends JLabel {

    // ---- ReagenzglasBar-Farbidentität (öffentlich für Palette/JAR-Zugriff) ----
    public Color liquidColorTop    = new Color(0x00, 0xB5, 0xAD); // Türkis (oben)
    public Color liquidColorBottom = new Color(0x8B, 0x00, 0x24); // Weinrot (unten)
    public Color glassRim          = new Color(140, 200, 255, 160);
    public Color glassBody         = new Color(180, 220, 255, 35);
    public Color glassHighlight    = new Color(255, 255, 255, 110);
    public Color shadowColor       = new Color(0, 0, 0, 70);
    public Color rippleColor       = new Color(200, 240, 255, 200);
    public Color hoverTextColor    = new Color(0xEA, 0xFB, 0xFF);

    // ---- Verhalten ----
    public boolean hoverEnabled  = true;
    public boolean rippleEnabled = true;
    public boolean accentVisible = true;   // wachsender Balken unten
    public boolean animated      = true;   // sanftes Shimmer im Akzent

    // ---- Geometrie ----
    public int arc            = 16;        // Eckenradius der Fläche
    public int padX           = 12;        // Innenabstand horizontal
    public int padY           = 7;         // Innenabstand vertikal
    public int accentHeight    = 3;        // Höhe des Akzent-Balkens
    public float rimStrokeWidth = 1.6f;

    // ---- Animations-Tuning ----
    public double hoverEaseIn   = 0.18;    // Geschwindigkeit Einblenden
    public double hoverEaseOut  = 0.14;    // Geschwindigkeit Ausblenden
    public double rippleSpeed   = 0.14;    // Easing der Wellen-Ausbreitung
    public double rippleFade    = 0.045;   // Alpha-Abbau pro Frame
    public float  bgAlpha       = 70f;     // Max-Deckkraft Glasfläche (0..255)

    // ---- interner Zustand ----
    private double hoverProgress = 0.0;    // 0 = aus, 1 = voll
    private boolean hovered      = false;
    private double waveOffset    = 0.0;
    private final List<Ripple> ripples = new ArrayList<Ripple>();
    private Color baseForeground = null;   // gemerkte Normalfarbe

    private final Timer effectTimer;

    // ============================================================== Konstruktoren

    public FLabel() {
        this("FLabel");
    }

    public FLabel(String text) {
        super(text);
        setOpaque(false);
        setForeground(new Color(0xCF, 0xE9, 0xF2));
        setHorizontalAlignment(SwingConstants.CENTER);
        applyPadding();

        // Ein Timer treibt Hover-Easing, Ripple-Ausbreitung und Shimmer.
        // Läuft nur, solange Arbeit anfällt (start/stop on demand) — wichtig,
        // wenn viele FLabels gleichzeitig auf einem Panel liegen.
        effectTimer = new Timer(16, e -> tick());

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (!hoverEnabled) return;
                hovered = true;
                ensureRunning();
            }
            @Override public void mouseExited(MouseEvent e) {
                hovered = false;
                ensureRunning();
            }
            @Override public void mousePressed(MouseEvent e) {
                if (!rippleEnabled || !isEnabled()) return;
                spawnRipple(e.getX(), e.getY());
                ensureRunning();
            }
        });
    }

    // ============================================================== Animations-Loop

    private void ensureRunning() {
        if (!effectTimer.isRunning()) effectTimer.start();
    }

    private void tick() {
        boolean busy = false;

        // Hover-Progress easen
        double target = hovered ? 1.0 : 0.0;
        double ease   = hovered ? hoverEaseIn : hoverEaseOut;
        double diff   = target - hoverProgress;
        if (Math.abs(diff) > 0.001) {
            hoverProgress += diff * ease;
            busy = true;
        } else {
            hoverProgress = target;
        }

        // Shimmer nur, wenn sichtbar & animiert
        if (animated && hoverProgress > 0.01) {
            waveOffset += 0.06;
            busy = true;
        }

        // Ripples vorantreiben
        if (!ripples.isEmpty()) {
            for (Iterator<Ripple> it = ripples.iterator(); it.hasNext();) {
                Ripple r = it.next();
                r.radius += (r.maxRadius - r.radius) * rippleSpeed;
                r.alpha  -= (float) rippleFade;
                if (r.alpha <= 0f) it.remove();
            }
            busy = true;
        }

        repaint();
        if (!busy) effectTimer.stop();
    }

    private void spawnRipple(int x, int y) {
        int w = getWidth(), h = getHeight();
        double dx = Math.max(x, w - x);
        double dy = Math.max(y, h - y);
        Ripple r = new Ripple();
        r.x = x; r.y = y;
        r.radius = Math.max(6f, Math.min(w, h) * 0.12f);
        r.maxRadius = (float) Math.hypot(dx, dy) + 8f;
        r.alpha = 1.0f;
        ripples.add(r);
    }

    // ============================================================== Painting

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth(), h = getHeight();
            float hp = (float) clamp(hoverProgress, 0.0, 1.0);

            float inset = Math.max(1f, rimStrokeWidth);
            RoundRectangle2D shape = new RoundRectangle2D.Float(
                    inset, inset, w - 2 * inset, h - 2 * inset, arc, arc);

            // 1 · Schatten (nur bei Hover)
            if (hp > 0.01f) drawShadow(g2, w, h, inset, hp);

            // 2 · Glas-Hintergrundfläche (Türkis→Weinrot, transparent)
            if (hp > 0.01f) drawGlassFill(g2, shape, (int) (h), inset, hp);

            // 3 · Ripples — auf die Fläche geclippt
            if (!ripples.isEmpty()) drawRipples(g2, shape);

            // 4 · Glanzstreifen links (im Glas)
            if (hp > 0.01f) drawHighlight(g2, shape, w, h, hp);

            // 5 · Glasrand
            if (hp > 0.01f) drawRim(g2, shape, hp);

            // 6 · Akzent-Balken unten (flüssig wachsend)
            if (accentVisible && hp > 0.01f) drawAccent(g2, w, h, inset, hp);
        } finally {
            g2.dispose();
        }

        // 7 · Text & Icon zuletzt, oben drauf
        super.paintComponent(g);
    }

    private void drawShadow(Graphics2D g2, int w, int h, float inset, float hp) {
        Composite oc = g2.getComposite();
        for (int i = 1; i <= 4; i++) {
            float a = (0.04f + i * 0.015f) * hp;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(a)));
            g2.setColor(shadowColor);
            g2.fill(new RoundRectangle2D.Float(
                    inset + i, inset + i + 1,
                    w - 2 * inset, h - 2 * inset, arc, arc));
        }
        g2.setComposite(oc);
    }

    private void drawGlassFill(Graphics2D g2, Shape shape, int h, float inset, float hp) {
        Shape oldClip = g2.getClip();
        g2.clip(shape);

        // Flüssigkeits-Gradient mit hover-skaliertem Alpha
        int a = (int) clamp(bgAlpha * hp, 0, 255);
        Color top = withAlpha(liquidColorTop, a);
        Color bot = withAlpha(liquidColorBottom, a);
        Paint op = g2.getPaint();
        g2.setPaint(new GradientPaint(0, inset, top, 0, h - inset, bot));
        g2.fill(shape);

        // dünne Glasfüllung darüber
        g2.setColor(withAlpha(glassBody, (int) (glassBody.getAlpha() * hp)));
        g2.fill(shape);

        g2.setPaint(op);
        g2.setClip(oldClip);
    }

    private void drawRipples(Graphics2D g2, Shape shape) {
        Shape oldClip = g2.getClip();
        g2.clip(shape);
        Composite oc = g2.getComposite();
        for (Ripple r : ripples) {
            float a = clampF(r.alpha * (rippleColor.getAlpha() / 255f));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g2.setColor(rippleColor);
            float d = r.radius * 2f;
            g2.fillOval((int) (r.x - r.radius), (int) (r.y - r.radius),
                    (int) d, (int) d);
        }
        g2.setComposite(oc);
        g2.setClip(oldClip);
    }

    private void drawHighlight(Graphics2D g2, Shape shape, int w, int h, float hp) {
        Shape oldClip = g2.getClip();
        g2.clip(shape);
        int stripeW = Math.max(4, (int) (w * 0.22));
        Paint op = g2.getPaint();
        g2.setPaint(new GradientPaint(
                0, 0, withAlpha(glassHighlight, (int) (glassHighlight.getAlpha() * hp)),
                stripeW, 0, withAlpha(glassHighlight, 0)));
        g2.fillRect(0, 0, stripeW, h);
        g2.setPaint(op);
        g2.setClip(oldClip);
    }

    private void drawRim(Graphics2D g2, Shape shape, float hp) {
        Stroke os = g2.getStroke();
        g2.setStroke(new BasicStroke(rimStrokeWidth));
        g2.setColor(withAlpha(glassRim, (int) (glassRim.getAlpha() * hp)));
        g2.draw(shape);
        g2.setStroke(os);
    }

    private void drawAccent(Graphics2D g2, int w, int h, float inset, float hp) {
        int barH = Math.max(1, accentHeight);
        float fullW = w - 2 * (inset + 4);
        float fillW = fullW * hp;
        float x0 = inset + 4;
        float y0 = h - inset - barH - 2;

        // dezentes Shimmer-Wackeln in der Breite
        if (animated) {
            float wob = (float) Math.sin(waveOffset) * 1.5f * hp;
            fillW = Math.max(0f, Math.min(fullW, fillW + wob));
        }

        Shape bar = new RoundRectangle2D.Float(x0, y0, fillW, barH, barH, barH);
        Paint op = g2.getPaint();
        g2.setPaint(new GradientPaint(
                x0, 0, liquidColorBottom, x0 + fullW, 0, liquidColorTop));
        g2.fill(bar);

        // feiner Glanz auf dem Balken
        g2.setColor(new Color(255, 255, 255, (int) (90 * hp)));
        g2.fill(new RoundRectangle2D.Float(x0, y0, fillW, Math.max(1, barH / 2f),
                barH, barH));
        g2.setPaint(op);
    }

    // ============================================================== Helpers

    private void applyPadding() {
        super.setBorder(new EmptyBorder(padY, padX, padY, padX));
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static float clampF(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static Color withAlpha(Color c, int a) {
        a = a < 0 ? 0 : (a > 255 ? 255 : a);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        // etwas Luft, damit Glasrand/Schatten nicht angeschnitten werden
        d.width  += 4;
        d.height += 4;
        return d;
    }

    /** Kleine Welle für den Ripple-Effekt. */
    private static final class Ripple {
        float x, y;
        float radius;
        float maxRadius;
        float alpha;
    }

    // ============================================================== JavaBean API

    public Color getLiquidColorTop() { return liquidColorTop; }
    public void setLiquidColorTop(Color c) { this.liquidColorTop = c; repaint(); }

    public Color getLiquidColorBottom() { return liquidColorBottom; }
    public void setLiquidColorBottom(Color c) { this.liquidColorBottom = c; repaint(); }

    public Color getGlassRim() { return glassRim; }
    public void setGlassRim(Color c) { this.glassRim = c; repaint(); }

    public Color getGlassBody() { return glassBody; }
    public void setGlassBody(Color c) { this.glassBody = c; repaint(); }

    public Color getGlassHighlight() { return glassHighlight; }
    public void setGlassHighlight(Color c) { this.glassHighlight = c; repaint(); }

    public Color getShadowColor() { return shadowColor; }
    public void setShadowColor(Color c) { this.shadowColor = c; repaint(); }

    public Color getRippleColor() { return rippleColor; }
    public void setRippleColor(Color c) { this.rippleColor = c; repaint(); }

    public Color getHoverTextColor() { return hoverTextColor; }
    public void setHoverTextColor(Color c) { this.hoverTextColor = c; repaint(); }

    public boolean isHoverEnabled() { return hoverEnabled; }
    public void setHoverEnabled(boolean b) { this.hoverEnabled = b; if (!b) { hovered = false; } repaint(); }

    public boolean isRippleEnabled() { return rippleEnabled; }
    public void setRippleEnabled(boolean b) { this.rippleEnabled = b; }

    public boolean isAccentVisible() { return accentVisible; }
    public void setAccentVisible(boolean b) { this.accentVisible = b; repaint(); }

    public boolean isAnimated() { return animated; }
    public void setAnimated(boolean b) {
        this.animated = b;
        if (b && hoverProgress > 0.01) ensureRunning();
    }

    public int getArc() { return arc; }
    public void setArc(int v) { this.arc = Math.max(0, v); repaint(); }

    public int getPadX() { return padX; }
    public void setPadX(int v) { this.padX = Math.max(0, v); applyPadding(); revalidate(); repaint(); }

    public int getPadY() { return padY; }
    public void setPadY(int v) { this.padY = Math.max(0, v); applyPadding(); revalidate(); repaint(); }

    public int getAccentHeight() { return accentHeight; }
    public void setAccentHeight(int v) { this.accentHeight = Math.max(1, v); repaint(); }

    public float getRimStrokeWidth() { return rimStrokeWidth; }
    public void setRimStrokeWidth(float v) { this.rimStrokeWidth = Math.max(0.5f, v); repaint(); }

    public double getHoverEaseIn() { return hoverEaseIn; }
    public void setHoverEaseIn(double v) { this.hoverEaseIn = clamp(v, 0.02, 1.0); }

    public double getHoverEaseOut() { return hoverEaseOut; }
    public void setHoverEaseOut(double v) { this.hoverEaseOut = clamp(v, 0.02, 1.0); }

    public double getRippleSpeed() { return rippleSpeed; }
    public void setRippleSpeed(double v) { this.rippleSpeed = clamp(v, 0.02, 0.5); }

    public double getRippleFade() { return rippleFade; }
    public void setRippleFade(double v) { this.rippleFade = clamp(v, 0.005, 0.2); }

    public float getBgAlpha() { return bgAlpha; }
    public void setBgAlpha(float v) { this.bgAlpha = (float) clamp(v, 0, 255); repaint(); }

    /** Erlaubt das Überschreiben des Standard-Paddings via eigenem Border. */
    @Override
    public void setBorder(javax.swing.border.Border border) {
        super.setBorder(border);
    }
}
