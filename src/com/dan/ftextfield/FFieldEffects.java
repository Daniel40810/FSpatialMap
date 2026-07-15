package com.dan.ftextfield;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

/**
 * Gekapselte Effekt-Logik fuer moderne Textfelder im <b>ReagenzglasBar-Glasstil</b>
 * der {@code com.dan}-Bibliothek (Weinrot&rarr;Tuerkis Glasoptik) &ndash; passend
 * zu {@link com.dan.fcombobox.FComboBox}.
 *
 * <p>Da {@link javax.swing.JTextField} und {@link javax.swing.JPasswordField}
 * verschiedene Basisklassen besitzen, liegt die gemeinsame Logik hier als
 * wiederverwendbare Hilfsklasse. {@code FTextfield} und {@code FPasswordfield}
 * halten je eine Instanz und delegieren Painting und Animation dorthin.</p>
 *
 * <p>Visuelle Merkmale (analog FComboBox):</p>
 * <ul>
 *   <li>Halbtransparenter Glaskoerper mit hellblauem Rand und linkem Glanzstreifen.</li>
 *   <li>Liquid-Akzentlinie am unteren Rand (Weinrot&rarr;Tuerkis), waechst bei Fokus/Hover
 *       &ndash; dient zugleich als Fokus-Indikator.</li>
 *   <li>Weicher, ge-easter Tuerkis-Glow bei Hover/Fokus.</li>
 *   <li>Floating-Label: Platzhalter gleitet bei Fokus/Inhalt animiert nach oben.</li>
 *   <li>Dezenter Liquid-Ripple ausgehend von der Klickposition.</li>
 * </ul>
 *
 * <p>Alle Animationen laufen ueber einen einzelnen {@link javax.swing.Timer}
 * mit derselben Easing-Sprache wie FComboBox
 * ({@code progress += (target - progress) * animationSpeed}).</p>
 *
 * @author com.dan
 */
public class FFieldEffects {

    /** Eine einzelne, sich ausbreitende und verblassende Liquid-Welle. */
    private static final class Ripple {
        float x, y;
        float radius;
        float maxRadius;
        float alpha;   // 0.0 .. 1.0
    }

    private final JTextComponent comp;

    // ----------------------------------------------------------------- Farben
    /** Obere Akzentfarbe (Tuerkis) &mdash; ReagenzglasBar-Identitaet. */
    private Color liquidColorTop = new Color(0x00, 0xB5, 0xAD);
    /** Untere Akzentfarbe (Weinrot) &mdash; ReagenzglasBar-Identitaet. */
    private Color liquidColorBottom = new Color(0x8B, 0x00, 0x24);
    /** Glasfuellung des Koerpers (fast transparent). */
    private Color glassBody = new Color(180, 220, 255, 32);
    /** Glasrand / Outline (hellblau). */
    private Color glassRim = new Color(140, 200, 255, 160);
    /** Linker Glanzstreifen (weiss). */
    private Color glassHighlight = new Color(255, 255, 255, 110);
    /** Farbe des Hover-/Fokus-Glows (tuerkis). */
    private Color hoverGlowColor = new Color(0x00, 0xB5, 0xAD);
    /** Textfarbe (hell, fuer dunkle Hintergruende). */
    private Color textColor = new Color(0xF0, 0xF6, 0xFF);
    /** Ruhefarbe des Platzhalters (blasses Glas). */
    private Color placeholderColor = new Color(190, 208, 224, 150);
    /** Farbe des angedockten (oben schwebenden) Labels. */
    private Color labelColor = new Color(0x00, 0xB5, 0xAD);
    /** Grundfarbe des Klick-Ripples (Alpha wird animiert ueberlagert). */
    private Color rippleColor = new Color(0x00, 0xB5, 0xAD);

    // -------------------------------------------------------------- Geometrie
    /** Eckenradius in Pixeln. */
    private int arc = 16;
    /** Strichstaerke des Glasrands in Pixeln. */
    private float rimStrokeWidth = 2.0f;
    /** Innenabstand links/rechts fuer den Text in Pixeln. */
    private int horizontalPadding = 14;
    /** Reservierter Platz oben fuer das schwebende Label. */
    private int padTop = 22;
    /** Reservierter Platz unten fuer die Liquid-Akzentlinie. */
    private int padBottom = 14;
    /** Skalierung der Label-Schrift im angedockten Zustand (0.4&ndash;1.0). */
    private float labelFontScale = 0.78f;

    // ------------------------------------------------------------- Animation
    /** Schaltet alle Animationen ein/aus. */
    private boolean animated = true;
    /** Easing-Geschwindigkeit der Uebergaenge (0.05&ndash;0.40). */
    private double animationSpeed = 0.18;

    // ---------------------------------------------------------- interner State
    private double hoverProgress = 0.0;   // 0 = nicht gehovert, 1 = gehovert
    private double focusProgress = 0.0;   // 0 = ohne Fokus, 1 = fokussiert
    private double labelProgress = 0.0;   // 0 = ruhend im Feld, 1 = oben angedockt

    private boolean hovered = false;
    private boolean focused = false;

    private final List<Ripple> ripples = new ArrayList<Ripple>();
    private final Timer animTimer;

    public FFieldEffects(JTextComponent component) {
        this.comp = component;
        animTimer = new Timer(16, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { tick(); }
        });
        animTimer.setRepeats(true);
        install();
    }

    // =========================================================================
    //  Installation
    // =========================================================================
    private void install() {
        comp.setOpaque(false);
        comp.setDoubleBuffered(true);
        comp.setForeground(textColor);
        comp.setBorder(new InsetBorder());

        comp.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { focused = true;  updateLabelTarget(); ensureRunning(); }
            @Override public void focusLost(FocusEvent e)   { focused = false; updateLabelTarget(); ensureRunning(); }
        });

        comp.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true;  ensureRunning(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; ensureRunning(); }
            @Override public void mousePressed(MouseEvent e) { spawnRipple(e.getX(), e.getY()); }
        });

        comp.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { updateLabelTarget(); }
            @Override public void removeUpdate(DocumentEvent e)  { updateLabelTarget(); }
            @Override public void changedUpdate(DocumentEvent e) { updateLabelTarget(); }
        });

        // Anfangszustand sofort uebernehmen (z.B. vorbelegter Text).
        labelProgress = labelTarget();
        comp.repaint();
    }

    // =========================================================================
    //  Animations-Loop (eine Easing-Schleife wie in FComboBox)
    // =========================================================================
    private boolean hasContent() {
        return comp.getDocument() != null && comp.getDocument().getLength() > 0;
    }

    private double labelTarget() { return (focused || hasContent()) ? 1.0 : 0.0; }

    private void updateLabelTarget() {
        if (!animated) {
            labelProgress = labelTarget();
            comp.repaint();
        } else {
            ensureRunning();
        }
    }

    private void ensureRunning() {
        if (animated) {
            if (!animTimer.isRunning()) {
                animTimer.start();
            }
        } else {
            hoverProgress = hovered ? 1.0 : 0.0;
            focusProgress = focused ? 1.0 : 0.0;
            labelProgress = labelTarget();
            ripples.clear();
            comp.repaint();
        }
    }

    private void tick() {
        double s = clamp(animationSpeed, 0.05, 0.40);

        double hTarget = hovered ? 1.0 : 0.0;
        double fTarget = focused ? 1.0 : 0.0;
        double lTarget = labelTarget();

        hoverProgress += (hTarget - hoverProgress) * s;
        focusProgress += (fTarget - focusProgress) * s;
        labelProgress += (lTarget - labelProgress) * s;

        // Ripples fortschreiben.
        for (Iterator<Ripple> it = ripples.iterator(); it.hasNext(); ) {
            Ripple r = it.next();
            r.radius += (r.maxRadius - r.radius) * 0.16f;
            r.alpha -= 0.045f;
            if (r.alpha <= 0f) {
                it.remove();
            }
        }

        boolean settled =
                Math.abs(hTarget - hoverProgress) < 0.004 &&
                Math.abs(fTarget - focusProgress) < 0.004 &&
                Math.abs(lTarget - labelProgress) < 0.004 &&
                ripples.isEmpty();

        if (settled) {
            hoverProgress = hTarget;
            focusProgress = fTarget;
            labelProgress = lTarget;
            animTimer.stop();
        }
        comp.repaint();
    }

    private void spawnRipple(int x, int y) {
        if (!animated) {
            return;
        }
        int w = comp.getWidth();
        int h = comp.getHeight();
        double dx = Math.max(x, w - x);
        double dy = Math.max(y, h - y);
        Ripple r = new Ripple();
        r.x = x;
        r.y = y;
        r.radius = 0f;
        r.maxRadius = (float) Math.hypot(dx, dy);
        r.alpha = 1f;
        ripples.add(r);
        ensureRunning();
        comp.repaint();
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /** Liefert {@code max(hoverProgress, focusProgress)} &ndash; hebt die Box optisch an. */
    private double lift() { return Math.max(clamp(hoverProgress, 0, 1), clamp(focusProgress, 0, 1)); }

    // =========================================================================
    //  Painting (von den Komponenten aufgerufen)
    // =========================================================================

    private RoundRectangle2D bodyShape(int w, int h) {
        return new RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, arc, arc);
    }

    /**
     * Schritt 1&ndash;4 (hinter dem Text): Glow, Glaskoerper, Glanzstreifen,
     * Liquid-Akzentlinie und Ripples.
     */
    public void paintGlassBackground(Graphics2D g2, int w, int h) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double lift = lift();

        Shape body = bodyShape(w, h);

        // 1) Weicher Glow als aeusserer Halo (nur bei Hover/Fokus). Auf den
        //    Ring ausserhalb des Koerpers geclippt, damit der Innenraum klar bleibt.
        if (lift > 0.01) {
            float maxSpread = 8f;
            java.awt.geom.Area halo = new java.awt.geom.Area(new RoundRectangle2D.Float(
                    -maxSpread, -maxSpread, w + maxSpread * 2f, h + maxSpread * 2f,
                    arc + maxSpread, arc + maxSpread));
            halo.subtract(new java.awt.geom.Area(body));
            Shape oldHaloClip = g2.getClip();
            g2.clip(halo);
            int layers = 5;
            for (int i = layers; i >= 1; i--) {
                float frac = (float) i / layers;
                int alpha = (int) (70 * lift * (1f - frac));
                if (alpha <= 0) continue;
                g2.setColor(withAlpha(hoverGlowColor, alpha));
                float spread = 2f + frac * 6f;
                g2.fill(new RoundRectangle2D.Float(
                        -spread, -spread, w + spread * 2f, h + spread * 2f,
                        arc + spread, arc + spread));
            }
            g2.setClip(oldHaloClip);
        }

        // 2) Glasfuellung (leicht heller bei Hover/Fokus).
        g2.setColor(brighten(glassBody, (int) (18 * lift)));
        g2.fill(body);

        Shape oldClip = g2.getClip();
        g2.clip(body);

        // 3) Linker Glanzstreifen (vertikaler weisser Verlauf).
        int stripeW = Math.max(6, (int) (w * 0.18));
        g2.setPaint(new GradientPaint(
                0, 0, withAlpha(glassHighlight, glassHighlight.getAlpha()),
                stripeW, 0, withAlpha(glassHighlight, 0)));
        g2.fillRect(2, 2, stripeW, h - 4);

        // 4) Ripples (Liquid-Puls vom Klickpunkt), sauber auf den Koerper geclippt.
        if (!ripples.isEmpty()) {
            Composite oldComp = g2.getComposite();
            for (Ripple r : ripples) {
                float a = Math.max(0f, Math.min(1f, r.alpha)) * 0.28f;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
                g2.setColor(rippleColor);
                g2.fill(new Ellipse2D.Float(r.x - r.radius, r.y - r.radius, r.radius * 2, r.radius * 2));
            }
            g2.setComposite(oldComp);
        }

        // 5) Liquid-Akzentlinie unten (Weinrot->Tuerkis), waechst bei Fokus/Hover.
        float baseLine = 2.4f;
        float lineH = (float) (baseLine + 2.6 * lift);
        float lineY = h - lineH - 2.5f;
        g2.setPaint(new GradientPaint(0, lineY, liquidColorBottom, w, lineY, liquidColorTop));
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, (float) (0.55 + 0.45 * lift)));
        g2.fill(new RoundRectangle2D.Float(6f, lineY, w - 12f, lineH, lineH, lineH));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        g2.setClip(oldClip);

        if (oldAA != null) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        }
    }

    /** Schritt 5 (ueber dem Text): Glasrand (heller bei Hover/Fokus, Richtung Tuerkis). */
    public void paintGlassRim(Graphics2D g2, int w, int h) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Stroke oldStroke = g2.getStroke();
        Paint oldPaint = g2.getPaint();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double lift = lift();
        Color rim = blend(glassRim, liquidColorTop, (float) (0.55 * lift));
        rim = brighten(rim, (int) (40 * lift));
        g2.setColor(rim);
        g2.setStroke(new BasicStroke(rimStrokeWidth + (float) (0.6 * lift)));
        g2.draw(new RoundRectangle2D.Float(1f, 1f, w - 2.5f, h - 2.5f, arc, arc));

        g2.setStroke(oldStroke);
        g2.setPaint(oldPaint);
        if (oldAA != null) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        }
    }

    /** Schritt 6 (ganz oben): das gleitende Label / der Platzhalter. */
    public void paintFloatingLabel(Graphics2D g2, int w, int h) {
        if (getLabelText().isEmpty()) {
            return;
        }
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Font oldFont = g2.getFont();
        Color oldColor = g2.getColor();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        float p = (float) clamp(labelProgress, 0.0, 1.0);

        Font base = comp.getFont();
        float restSize = base.getSize2D();
        float floatSize = restSize * labelFontScale;
        Font labelFont = base.deriveFont(lerp(restSize, floatSize, p));
        g2.setFont(labelFont);
        FontMetrics fm = g2.getFontMetrics(labelFont);

        int restCenterY = (padTop + (h - padBottom)) / 2;  // gleiche Hoehe wie der Text
        int floatCenterY = padTop / 2 + 1;                 // oben, ueber dem Text
        float centerY = lerp(restCenterY, floatCenterY, p);
        float baseline = centerY + (fm.getAscent() - fm.getDescent()) / 2f;

        float x = horizontalPadding;
        g2.setColor(lerpColor(placeholderColor, labelColor, p));
        g2.drawString(getLabelText(), x, baseline);

        g2.setFont(oldFont);
        g2.setColor(oldColor);
        if (oldAA != null) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        }
    }

    // =========================================================================
    //  Farb- und Interpolations-Utilities (identisch zu FComboBoxUI)
    // =========================================================================
    private static Color withAlpha(Color c, int a) {
        a = a < 0 ? 0 : (a > 255 ? 255 : a);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private static Color brighten(Color c, int d) {
        return new Color(clampByte(c.getRed() + d), clampByte(c.getGreen() + d),
                clampByte(c.getBlue() + d), c.getAlpha());
    }

    private static Color blend(Color a, Color b, float t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return new Color(
                (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    private static int clampByte(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
                Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    // =========================================================================
    //  Leerer Border &ndash; liefert nur die Insets; die Optik malt die Komponente
    // =========================================================================
    private final class InsetBorder implements Border {
        @Override public Insets getBorderInsets(Component c) {
            return new Insets(padTop, horizontalPadding, padBottom, horizontalPadding);
        }
        @Override public boolean isBorderOpaque() { return false; }
        @Override public void paintBorder(Component c, java.awt.Graphics g, int x, int y, int w, int h) {
            // bewusst leer &ndash; gezeichnet wird in paintGlassRim().
        }
    }

    // =========================================================================
    //  JavaBean-Properties (Getter/Setter)
    // =========================================================================
    public String getLabelText() { return labelText == null ? "" : labelText; }
    private String labelText = "";
    public void setLabelText(String labelText) { this.labelText = labelText; comp.repaint(); }

    public Color getLiquidColorTop() { return liquidColorTop; }
    public void setLiquidColorTop(Color c) { this.liquidColorTop = c; comp.repaint(); }

    public Color getLiquidColorBottom() { return liquidColorBottom; }
    public void setLiquidColorBottom(Color c) { this.liquidColorBottom = c; comp.repaint(); }

    public Color getGlassBody() { return glassBody; }
    public void setGlassBody(Color c) { this.glassBody = c; comp.repaint(); }

    public Color getGlassRim() { return glassRim; }
    public void setGlassRim(Color c) { this.glassRim = c; comp.repaint(); }

    public Color getGlassHighlight() { return glassHighlight; }
    public void setGlassHighlight(Color c) { this.glassHighlight = c; comp.repaint(); }

    public Color getHoverGlowColor() { return hoverGlowColor; }
    public void setHoverGlowColor(Color c) { this.hoverGlowColor = c; comp.repaint(); }

    public Color getTextColor() { return textColor; }
    public void setTextColor(Color c) { this.textColor = c; comp.setForeground(c); comp.repaint(); }

    public Color getPlaceholderColor() { return placeholderColor; }
    public void setPlaceholderColor(Color c) { this.placeholderColor = c; comp.repaint(); }

    public Color getLabelColor() { return labelColor; }
    public void setLabelColor(Color c) { this.labelColor = c; comp.repaint(); }

    public Color getRippleColor() { return rippleColor; }
    public void setRippleColor(Color c) { this.rippleColor = c; comp.repaint(); }

    public int getArc() { return arc; }
    public void setArc(int arc) { this.arc = Math.max(0, arc); comp.repaint(); }

    public float getRimStrokeWidth() { return rimStrokeWidth; }
    public void setRimStrokeWidth(float w) { this.rimStrokeWidth = Math.max(0.5f, w); comp.repaint(); }

    public int getHorizontalPadding() { return horizontalPadding; }
    public void setHorizontalPadding(int p) { this.horizontalPadding = Math.max(0, p); comp.revalidate(); comp.repaint(); }

    public float getLabelFontScale() { return labelFontScale; }
    public void setLabelFontScale(float s) { this.labelFontScale = Math.max(0.4f, Math.min(1f, s)); comp.repaint(); }

    public boolean isAnimated() { return animated; }
    public void setAnimated(boolean animated) {
        this.animated = animated;
        ensureRunning();
    }

    public double getAnimationSpeed() { return animationSpeed; }
    public void setAnimationSpeed(double s) { this.animationSpeed = clamp(s, 0.05, 0.40); }
}
