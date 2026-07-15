package com.dan.fbutton;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/**
 * FButton — ein animierter {@link JButton} im "ReagenzglasBar"-Stil
 * (Labor-Dark-Look), identisch mit dem Mal-Stack von {@code FLabel}.
 *
 * <p>Zusätzlich gegenüber FLabel:</p>
 * <ul>
 *   <li><b>Press-Sink</b>: beim Drücken schiebt sich die Fläche 2&nbsp;px
 *       nach rechts/unten und die Glasfüllung verdunkelt leicht.</li>
 *   <li><b>Hover-Volldeckung</b>: im Ruhezustand ist die Glasfläche schon
 *       schwach sichtbar (konfigurierbar), sodass der Button als klickbares
 *       Element erkennbar bleibt.</li>
 *   <li><b>Varianten</b>: {@link FButtonVariant} steuert das Farbpaar
 *       (PRIMARY, GHOST, DANGER, SUCCESS).</li>
 *   <li><b>FIcon-Slot</b>: optional wird ein {@link Icon} (z.&nbsp;B. ein über
 *       {@code FIconType.toImage()} erzeugter {@code ImageIcon}) links vom Text
 *       gerendert. Wer kein Icon braucht, lässt {@code setFIcon(null)}.</li>
 *   <li><b>FTheme-Live-Binding</b>: {@code bindTheme(Object fTheme)} verdrahtet
 *       den Button per {@link WeakReference} mit dem {@code FTheme}-Singleton
 *       aus {@code com.dan.fstyle}. Der Button muss nicht mehr nach jedem
 *       Theme-Wechsel manuell aktualisiert werden. Binding über Reflection, damit
 *       das JAR ohne {@code fstyle.jar} im Classpath kompilierbar bleibt.</li>
 * </ul>
 *
 * <p>Voll JavaBean-konform (NetBeans-Palette-tauglich).</p>
 *
 * @author com.dan
 */
public class FButton extends JButton {

    // ---- ReagenzglasBar-Farbidentität (öffentlich für Palette/JAR-Zugriff) ----
    public Color liquidColorTop    = new Color(0x00, 0xB5, 0xAD);
    public Color liquidColorBottom = new Color(0x8B, 0x00, 0x24);
    public Color glassRim          = new Color(140, 200, 255, 160);
    public Color glassBody         = new Color(180, 220, 255, 35);
    public Color glassHighlight    = new Color(255, 255, 255, 110);
    public Color shadowColor       = new Color(0,   0,   0,   70);
    public Color rippleColor       = new Color(200, 240, 255, 200);
    public Color normalTextColor   = new Color(0xCF, 0xE9, 0xF2);
    public Color hoverTextColor    = new Color(0xEA, 0xFB, 0xFF);
    public Color pressTextColor    = new Color(0xFF, 0xFF, 0xFF);

    // ---- Verhalten ----
    public boolean hoverEnabled  = true;
    public boolean rippleEnabled = true;
    public boolean accentVisible = true;
    public boolean animated      = true;

    // ---- Variante ----
    private FButtonVariant variant = FButtonVariant.PRIMARY;

    // ---- Geometrie ----
    public int   arc            = 16;
    public int   padX           = 18;
    public int   padY           = 8;
    public int   accentHeight   = 3;
    public float rimStrokeWidth = 1.6f;
    public int   iconGap        = 6;       // Abstand Icon → Text

    // ---- Alpha-Ebenen ----
    /** Deckkraft Glasfläche im Normalzustand (0..255). */
    public float restAlpha  = 18f;
    /** Max-Deckkraft Glasfläche beim Hover (0..255). */
    public float hoverAlpha = 70f;
    /** Abdunklung beim Drücken (0..255). */
    public float pressAlpha = 110f;

    // ---- Animations-Tuning ----
    public double hoverEaseIn  = 0.18;
    public double hoverEaseOut = 0.14;
    public double pressEase    = 0.28;
    public double rippleSpeed  = 0.14;
    public double rippleFade   = 0.045;

    // ---- FIcon-Slot ----
    private Icon fIcon = null;

    // ---- interner Zustand ----
    double hoverProgress = 0.0;
    double pressProgress = 0.0;
    boolean hovered      = false;
    boolean pressed      = false;
    double  waveOffset   = 0.0;
    final List<Ripple> ripples = new ArrayList<Ripple>();

    private Timer effectTimer;   // nicht final: wird in init() gesetzt

    // ---- Theme-Binding (Reflection, kein harter Import von fstyle) ----
    private WeakReference<Object> themeRef = null;
    private PropertyChangeListener themeListener = null;

    // ============================================================= Konstruktoren

    public FButton() {
        this("FButton");
    }

    public FButton(String text) {
        super(text);
        init();
    }

    private void init() {
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setHorizontalAlignment(SwingConstants.CENTER);
        setForeground(normalTextColor);
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        applyPadding();

        effectTimer = new Timer(16, e -> tick());

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (!hoverEnabled) return;
                hovered = true;
                ensureRunning();
            }
            @Override public void mouseExited(MouseEvent e) {
                hovered  = false;
                pressed  = false;
                ensureRunning();
            }
            @Override public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                pressed = true;
                if (rippleEnabled) spawnRipple(e.getX(), e.getY());
                ensureRunning();
            }
            @Override public void mouseReleased(MouseEvent e) {
                pressed = false;
                ensureRunning();
            }
        });
    }

    // ============================================================= Animations-Loop

    private void ensureRunning() {
        if (!effectTimer.isRunning()) effectTimer.start();
    }

    private void tick() {
        boolean busy = false;

        // Hover-Easing
        double htarget = hovered ? 1.0 : 0.0;
        double hease   = hovered ? hoverEaseIn : hoverEaseOut;
        double hdiff   = htarget - hoverProgress;
        if (Math.abs(hdiff) > 0.004) {
            hoverProgress += hdiff * hease;
            busy = true;
        } else {
            hoverProgress = htarget;
        }

        // Press-Easing
        double ptarget = pressed ? 1.0 : 0.0;
        double pdiff   = ptarget - pressProgress;
        if (Math.abs(pdiff) > 0.004) {
            pressProgress += pdiff * pressEase;
            busy = true;
        } else {
            pressProgress = ptarget;
        }

        // Shimmer
        if (animated && hoverProgress > 0.01) {
            waveOffset += 0.06;
            busy = true;
        }

        // Ripples
        if (!ripples.isEmpty()) {
            for (Iterator<Ripple> it = ripples.iterator(); it.hasNext();) {
                Ripple r = it.next();
                r.radius += (r.maxRadius - r.radius) * rippleSpeed;
                r.alpha  -= (float) rippleFade;
                if (r.alpha <= 0f) it.remove();
            }
            busy = true;
        }

        // Textfarbe glatt überblenden
        Color tc = blendColor(normalTextColor,
                pressed ? pressTextColor : hoverTextColor,
                (float) Math.max(pressProgress, hoverProgress));
        setForeground(tc);

        repaint();
        if (!busy) effectTimer.stop();
    }

    private void spawnRipple(int x, int y) {
        int w = getWidth(), h = getHeight();
        double dx = Math.max(x, w - x);
        double dy = Math.max(y, h - y);
        Ripple r  = new Ripple();
        r.x = x; r.y = y;
        r.radius    = Math.max(6f, Math.min(w, h) * 0.12f);
        r.maxRadius = (float) Math.hypot(dx, dy) + 8f;
        r.alpha     = 1.0f;
        ripples.add(r);
    }

    // ============================================================= Painting

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth(), h = getHeight();
            float hp = clampF((float) hoverProgress);
            float pp = clampF((float) pressProgress);

            // Press-Sink: Fläche 2px verschoben
            int sinkX = (int) (pp * 2);
            int sinkY = (int) (pp * 2);

            float inset = Math.max(1f, rimStrokeWidth);
            RoundRectangle2D shape = new RoundRectangle2D.Float(
                    inset + sinkX, inset + sinkY,
                    w - 2 * inset - sinkX, h - 2 * inset - sinkY,
                    arc, arc);

            // Effektive bgAlpha: im Ruhezustand schwach sichtbar, beim Hover/Press voll
            float bgA;
            if (variant == FButtonVariant.GHOST) {
                bgA = restAlpha * (1 - hp) + hoverAlpha * hp;
            } else {
                bgA = restAlpha + (hoverAlpha - restAlpha) * hp + (pressAlpha - hoverAlpha) * pp;
            }
            bgA = clampF(bgA / 255f) * 255f;

            // 1 · Schatten (nur bei Hover/Press)
            if (hp > 0.01f || pp > 0.01f) drawShadow(g2, w, h, inset, sinkX, sinkY, Math.max(hp, pp));

            // 2 · Glasfläche
            drawGlassFill(g2, shape, h, inset, sinkY, bgA, pp);

            // 3 · Ripples
            if (!ripples.isEmpty()) drawRipples(g2, shape);

            // 4 · Glanzstreifen
            if (hp > 0.01f || pp > 0.01f) drawHighlight(g2, shape, w, h, Math.max(hp, pp));

            // 5 · Glasrand
            drawRim(g2, shape, Math.max(restAlpha / 255f, Math.max(hp, pp)));

            // 6 · Akzent-Balken unten
            if (accentVisible) drawAccent(g2, w, h, inset, sinkX, hp, pp);

            // 7 · FIcon links
            if (fIcon != null) drawFIcon(g2, w, h, sinkX, sinkY);

        } finally {
            g2.dispose();
        }

        // 8 · Text (super, damit Icon-/Text-Layout von JButton verarbeitet wird)
        super.paintComponent(g);
    }

    // ---- Paint-Helfer (1:1 aus FLabel, erweitert um Press-Parameter) --------

    private void drawShadow(Graphics2D g2, int w, int h,
                            float inset, int sx, int sy, float a) {
        Composite oc = g2.getComposite();
        for (int i = 1; i <= 4; i++) {
            float al = (0.04f + i * 0.015f) * a;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampF(al)));
            g2.setColor(shadowColor);
            g2.fill(new RoundRectangle2D.Float(
                    inset + sx + i, inset + sy + i + 1,
                    w - 2 * inset - sx, h - 2 * inset - sy, arc, arc));
        }
        g2.setComposite(oc);
    }

    private void drawGlassFill(Graphics2D g2, Shape shape, int h,
                               float inset, int sy, float bgA, float pp) {
        Shape oldClip = g2.getClip();
        g2.clip(shape);

        Color top = withAlpha(variant.top,    (int) bgA);
        Color bot = withAlpha(variant.bottom, (int) bgA);

        // Press: Gradient invertieren (Farbwechsel beim Drücken)
        if (pp > 0.01f) {
            top = blend(top, withAlpha(variant.bottom, (int) bgA), pp);
            bot = blend(bot, withAlpha(variant.top,    (int) bgA), pp);
        }

        Paint op = g2.getPaint();
        g2.setPaint(new GradientPaint(0, inset + sy, top, 0, h - inset, bot));
        g2.fill(shape);

        // dünne Glasfüllung
        g2.setColor(withAlpha(glassBody, (int) (glassBody.getAlpha() * Math.max(bgA / 255f, 0.08f))));
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
            g2.fillOval((int) (r.x - r.radius), (int) (r.y - r.radius), (int) d, (int) d);
        }
        g2.setComposite(oc);
        g2.setClip(oldClip);
    }

    private void drawHighlight(Graphics2D g2, Shape shape, int w, int h, float a) {
        Shape oldClip = g2.getClip();
        g2.clip(shape);
        int stripeW = Math.max(4, (int) (w * 0.22));
        Paint op = g2.getPaint();
        g2.setPaint(new GradientPaint(
                0, 0, withAlpha(glassHighlight, (int) (glassHighlight.getAlpha() * a)),
                stripeW, 0, withAlpha(glassHighlight, 0)));
        g2.fillRect(0, 0, stripeW, h);
        g2.setPaint(op);
        g2.setClip(oldClip);
    }

    private void drawRim(Graphics2D g2, Shape shape, float a) {
        Stroke os = g2.getStroke();
        g2.setStroke(new BasicStroke(rimStrokeWidth));
        g2.setColor(withAlpha(glassRim, (int) (glassRim.getAlpha() * Math.max(a, 0.25f))));
        g2.draw(shape);
        g2.setStroke(os);
    }

    private void drawAccent(Graphics2D g2, int w, int h, float inset,
                            int sx, float hp, float pp) {
        int barH = Math.max(1, accentHeight);
        float fullW = w - 2 * (inset + 4) - sx;
        float fillW = fullW * Math.max(hp, 0.3f);  // immer mindestens 30% sichtbar
        float x0    = inset + 4 + sx;
        float y0    = h - inset - barH - 2;

        // Shimmer-Wackeln
        if (animated) {
            float wob = (float) Math.sin(waveOffset) * 1.5f * hp;
            fillW = Math.max(0f, Math.min(fullW, fillW + wob));
        }

        // Press: Balken voll
        if (pp > 0.01f) fillW = Math.min(fullW, fillW + (fullW - fillW) * pp);

        Shape bar = new RoundRectangle2D.Float(x0, y0, fillW, barH, barH, barH);
        Paint op  = g2.getPaint();
        g2.setPaint(new GradientPaint(
                x0, 0, variant.bottom, x0 + Math.max(1, fullW), 0, variant.top));
        g2.fill(bar);

        // Glanzlinie oben auf dem Balken
        g2.setColor(new Color(255, 255, 255, 90));
        g2.fill(new RoundRectangle2D.Float(x0, y0, fillW, Math.max(1, barH / 2f), barH, barH));
        g2.setPaint(op);
    }

    private void drawFIcon(Graphics2D g2, int w, int h, int sinkX, int sinkY) {
        int iw = fIcon.getIconWidth();
        int ih = fIcon.getIconHeight();
        // Links ausrichten, vertikal zentrieren; Text-Offset wird von super übernommen
        int ix = padX + sinkX;
        int iy = (h - ih) / 2 + sinkY;
        fIcon.paintIcon(this, g2, ix, iy);
    }

    // ============================================================= FTheme-Binding
    // Reflection-basiert: kein harter Import von com.dan.fstyle.
    // FTheme muss folgende public-Methoden haben:
    //   addPropertyChangeListener(PropertyChangeListener)
    //   removePropertyChangeListener(PropertyChangeListener)
    //   Color getLiquidColorTop()
    //   Color getLiquidColorBottom()
    //   Color getGlassRim()
    //   Color getGlassBody()
    //   Color getGlassHighlight()

    /**
     * Bindet den Button an den {@code FTheme}-Singleton aus {@code com.dan.fstyle}.
     * Übergibt man {@code null}, wird das Binding gelöst.
     *
     * <pre>{@code
     *   // Typische Verwendung:
     *   FTheme theme = FTheme.getInstance();
     *   btn.bindTheme(theme);
     * }</pre>
     *
     * @param fTheme Instanz von {@code com.dan.fstyle.FTheme} (oder {@code null}).
     */
    public void bindTheme(Object fTheme) {
        // altes Binding sauber lösen
        if (themeRef != null && themeRef.get() != null && themeListener != null) {
            try {
                themeRef.get().getClass()
                        .getMethod("removePropertyChangeListener", PropertyChangeListener.class)
                        .invoke(themeRef.get(), themeListener);
            } catch (Exception ignored) { }
        }
        themeRef      = null;
        themeListener = null;

        if (fTheme == null) return;

        themeListener = new PropertyChangeListener() {
            @Override public void propertyChange(PropertyChangeEvent e) {
                applyTheme(e.getSource());
            }
        };
        themeRef = new WeakReference<Object>(fTheme);
        try {
            fTheme.getClass()
                  .getMethod("addPropertyChangeListener", PropertyChangeListener.class)
                  .invoke(fTheme, themeListener);
        } catch (Exception ignored) { }

        applyTheme(fTheme);   // sofort anwenden
    }

    private void applyTheme(Object theme) {
        if (theme == null) return;
        try {
            Class<?> c = theme.getClass();
            Color t = (Color) c.getMethod("getLiquidColorTop").invoke(theme);
            Color b = (Color) c.getMethod("getLiquidColorBottom").invoke(theme);
            Color r = (Color) c.getMethod("getGlassRim").invoke(theme);
            Color gb = (Color) c.getMethod("getGlassBody").invoke(theme);
            Color gh = (Color) c.getMethod("getGlassHighlight").invoke(theme);
            if (t  != null) { liquidColorTop    = t;  }
            if (b  != null) { liquidColorBottom  = b;  }
            if (r  != null) { glassRim           = r;  }
            if (gb != null) { glassBody          = gb; }
            if (gh != null) { glassHighlight     = gh; }
            // Variant-Farben nur bei PRIMARY nachführen (DANGER/SUCCESS haben eigene Palette)
            if (variant == FButtonVariant.PRIMARY && t != null && b != null) {
                variant = FButtonVariant.PRIMARY; // Farben via liquidColorTop/Bottom
                liquidColorTop    = t;
                liquidColorBottom = b;
            }
            repaint();
        } catch (Exception ignored) { }
    }

    // ============================================================= Padding / Größe

    private void applyPadding() {
        // Wenn ein FIcon gesetzt ist, links extra Platz für Icon + Gap
        int leftPad = (fIcon != null) ? padX + fIcon.getIconWidth() + iconGap : padX;
        super.setBorder(new EmptyBorder(padY, leftPad, padY, padX));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width  += 4;
        d.height += 4;
        return d;
    }

    // ============================================================= Helpers

    private static float clampF(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static Color withAlpha(Color c, int a) {
        a = a < 0 ? 0 : (a > 255 ? 255 : a);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private static Color blend(Color a, Color b, float t) {
        t = clampF(t);
        return new Color(
                (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    private static Color blendColor(Color a, Color b, float t) {
        return blend(a, b, clampF(t));
    }

    // ============================================================= Ripple

    static final class Ripple {
        float x, y, radius, maxRadius, alpha;
    }

    // ============================================================= JavaBean API

    public FButtonVariant getVariant() { return variant; }
    public void setVariant(FButtonVariant v) {
        if (v == null) return;
        this.variant = v;
        repaint();
    }

    public Icon getFIcon() { return fIcon; }
    public void setFIcon(Icon icon) {
        this.fIcon = icon;
        applyPadding();
        revalidate();
        repaint();
    }

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

    public Color getNormalTextColor() { return normalTextColor; }
    public void setNormalTextColor(Color c) { this.normalTextColor = c; repaint(); }

    public Color getHoverTextColor() { return hoverTextColor; }
    public void setHoverTextColor(Color c) { this.hoverTextColor = c; repaint(); }

    public Color getPressTextColor() { return pressTextColor; }
    public void setPressTextColor(Color c) { this.pressTextColor = c; repaint(); }

    public boolean isHoverEnabled() { return hoverEnabled; }
    public void setHoverEnabled(boolean b) { this.hoverEnabled = b; if (!b) hovered = false; repaint(); }

    public boolean isRippleEnabled() { return rippleEnabled; }
    public void setRippleEnabled(boolean b) { this.rippleEnabled = b; }

    public boolean isAccentVisible() { return accentVisible; }
    public void setAccentVisible(boolean b) { this.accentVisible = b; repaint(); }

    public boolean isAnimated() { return animated; }
    public void setAnimated(boolean b) { this.animated = b; if (b && hoverProgress > 0.01) ensureRunning(); }

    public int getArc() { return arc; }
    public void setArc(int v) { this.arc = Math.max(0, v); repaint(); }

    public int getPadX() { return padX; }
    public void setPadX(int v) { this.padX = Math.max(0, v); applyPadding(); revalidate(); repaint(); }

    public int getPadY() { return padY; }
    public void setPadY(int v) { this.padY = Math.max(0, v); applyPadding(); revalidate(); repaint(); }

    public int getAccentHeight() { return accentHeight; }
    public void setAccentHeight(int v) { this.accentHeight = Math.max(1, v); repaint(); }

    public int getIconGap() { return iconGap; }
    public void setIconGap(int v) { this.iconGap = Math.max(0, v); applyPadding(); revalidate(); repaint(); }

    public float getRimStrokeWidth() { return rimStrokeWidth; }
    public void setRimStrokeWidth(float v) { this.rimStrokeWidth = Math.max(0.5f, v); repaint(); }

    public float getRestAlpha() { return restAlpha; }
    public void setRestAlpha(float v) { this.restAlpha = clampF(v / 255f) * 255f; repaint(); }

    public float getHoverAlpha() { return hoverAlpha; }
    public void setHoverAlpha(float v) { this.hoverAlpha = clampF(v / 255f) * 255f; repaint(); }

    public float getPressAlpha() { return pressAlpha; }
    public void setPressAlpha(float v) { this.pressAlpha = clampF(v / 255f) * 255f; repaint(); }

    public double getHoverEaseIn() { return hoverEaseIn; }
    public void setHoverEaseIn(double v) { this.hoverEaseIn = clamp(v); }

    public double getHoverEaseOut() { return hoverEaseOut; }
    public void setHoverEaseOut(double v) { this.hoverEaseOut = clamp(v); }

    public double getPressEase() { return pressEase; }
    public void setPressEase(double v) { this.pressEase = clamp(v); }

    public double getRippleSpeed() { return rippleSpeed; }
    public void setRippleSpeed(double v) { this.rippleSpeed = clamp(v); }

    public double getRippleFade() { return rippleFade; }
    public void setRippleFade(double v) { this.rippleFade = clamp(v); }

    private static double clamp(double v) {
        return v < 0.02 ? 0.02 : (v > 1.0 ? 1.0 : v);
    }
}
