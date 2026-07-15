package com.dan.fslider;

import com.dan.fstyle.FTheme;

import javax.swing.BoundedRangeModel;
import javax.swing.JSlider;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * <h2>FSlider &ndash; ReagenzglasBar / Laboratory Dark Look</h2>
 *
 * Ein Custom-{@link JSlider} im F-Style. Der gefuellte Bereich des Tracks ist keine
 * platte Gradient-Flaeche, sondern eine <b>lebendige Fluessigkeit</b>: eine wellenfoermig
 * animierte Oberflaeche mit Schaumkrone und aufsteigenden Blasen. Der Thumb ist eine
 * gleaserne Kugel mit Reflex, Hover-Glow (pulsierend) und Ripple beim Ziehen.
 *
 * <p>Architektur folgt dem F-Style-Vertrag:
 * <ul>
 *   <li>Ein einziger 16&nbsp;ms {@link Timer} treibt alle Animationen
 *       (Glide, Hover, Press, Welle, Blasen, Ripples).</li>
 *   <li>Gemalt wird ueber den UI-Delegate {@link FSliderUI}.</li>
 *   <li>Farben binden an {@link FTheme}; explizite Setter ueberschreiben das Theme.</li>
 *   <li>Feste Auswahl (Erscheinungsbild) als Enum {@link FSliderVariant}.</li>
 * </ul>
 *
 * Java 8 kompatibel.
 */
public class FSlider extends JSlider {

    // ---- Farben (Liquid) -------------------------------------------------
    private Color liquidColorTop    = new Color(0x00, 0xB5, 0xAD); // Tuerkis (theme.primary)
    private Color liquidColorBottom = new Color(0x8B, 0x00, 0x24); // Weinrot (theme.accent)

    // ---- Farben (Glas) ---------------------------------------------------
    private Color glassRim       = new Color(0x8C, 0xC8, 0xFF, 160);
    private Color glassBody      = new Color(0xB4, 0xDC, 0xFF, 35);
    private Color glassHighlight = new Color(0xFF, 0xFF, 0xFF, 110);
    private Color shadowColor    = new Color(0, 0, 0, 70);
    private Color rippleColor    = new Color(0xC8, 0xF0, 0xFF, 200);
    private Color glowColor      = new Color(0x00, 0xB5, 0xAD, 120);
    private Color foamColor      = new Color(255, 255, 255, 200);

    // ---- Farben (Track-Untergrund / Text) --------------------------------
    private Color trackColor = new Color(0x12, 0x16, 0x20);
    private Color textColor  = new Color(0xCF, 0xE9, 0xF2);
    private Color tickColor  = new Color(0x8F, 0xA9, 0xB6);

    // Wird true, sobald ein FSliderStyler (oder manueller Setter) die Liquid-/Glas-Farben
    // gesetzt hat. Verhindert, dass syncFromTheme() diese Werte anschliessend wieder
    // ueberschreibt (z.B. beim naechsten addNotify() oder Theme-Wechsel).
    private boolean customColorsLocked = false;

    // ---- Geometrie -------------------------------------------------------
    private FSliderVariant variant = FSliderVariant.STANDARD;
    private int   arc            = 16;
    private int   thumbSize      = 22;
    private float rimStrokeWidth = 1.6f;
    private int   shadowOffset   = 4;

    // ---- Label-Formatierung (TICKS-Variante & Wert-Bubble) ---------------
    private FSliderLabelFormatter tickLabelFormatter = null; // null = roher Integer-Wert

    // ---- Flags -----------------------------------------------------------
    private boolean hoverEnabled = true;
    private boolean rippleEnabled = true;
    private boolean glowEnabled  = true;
    private boolean animated     = true;
    private boolean showValueBubble = true; // Glas-Tooltip ueber dem Thumb beim Ziehen
    private boolean idleWaveEnabled = false; // false = Welle nur bei Hover/Drag/Glide (CPU-sparend)

    // ---- Animations-Parameter --------------------------------------------
    private double hoverEaseIn  = 0.18;
    private double hoverEaseOut = 0.14;
    private double glideSpeed   = 0.22;
    private double rippleSpeed  = 0.14;
    private double rippleFade   = 0.045;
    private double waveSpeed    = 0.07;

    // ---- Live-Animationszustand (vom UI gelesen) -------------------------
    double  displayFrac  = 0.0;   // geglaettete Fuellung 0..1 (folgt dem echten Wert)
    double  hoverProgress = 0.0;  // 0..1
    double  pressProgress = 0.0;  // 0..1
    double  waveOffset   = 0.0;   // laufende Phase der Oberflaechenwelle
    double  waveAmp      = 2.0;   // aktuelle Wellenamplitude (durch Aktivitaet gedaempft)
    double  glowPulse    = 0.0;   // 0..2PI fuer pulsierenden Hover-Glow
    boolean hovering     = false;
    boolean dragging     = false;

    final List<Bubble> bubbles = new ArrayList<Bubble>();
    final List<Ripple> ripples = new ArrayList<Ripple>();
    private final Random rng = new Random();

    // Vom UI je Paint gesetzte Fuell-Geometrie (Komponenten-Koordinaten),
    // damit der Timer Blasen an der richtigen Stelle spawnen/steigen lassen kann.
    private final Rectangle liquidBounds = new Rectangle();
    private boolean liquidHorizontal = true;

    private Timer timer;
    private FTheme.ThemeListener themeListener;

    // =====================================================================
    //  Konstruktoren
    // =====================================================================

    public FSlider() {
        super();
        init();
    }

    public FSlider(int orientation) {
        super(orientation);
        init();
    }

    public FSlider(int min, int max) {
        super(min, max);
        init();
    }

    public FSlider(int min, int max, int value) {
        super(min, max, value);
        init();
    }

    public FSlider(int orientation, int min, int max, int value) {
        super(orientation, min, max, value);
        init();
    }

    public FSlider(BoundedRangeModel model) {
        super(model);
        init();
    }

    private void init() {
        setOpaque(false);
        setFocusable(true);
        syncFromTheme(FTheme.getInstance());

        displayFrac = currentFrac();

        // Ein einziger 16ms-Timer fuer ALLE Animationen.
        timer = new Timer(16, e -> tick());

        // Hover-Tracking
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (!isEnabled()) return;
                hovering = true;
                ensureRunning();
            }
            @Override public void mouseExited(MouseEvent e) {
                hovering = false;
                ensureRunning();
            }
            @Override public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                dragging = true;
                spawnRipple(e.getX(), e.getY());
                ensureRunning();
            }
            @Override public void mouseReleased(MouseEvent e) {
                dragging = false;
                ensureRunning();
            }
        });

        // Wertaenderung -> Glide-Animation anstossen
        addChangeListener(e -> ensureRunning());

        // Theme-Bindung (Lifecycle in add/removeNotify)
        themeListener = new FTheme.ThemeListener() {
            @Override public void themeChanged(FTheme t) {
                syncFromTheme(t);
                repaint();
            }
        };

        setUI(new FSliderUI(this));
    }

    /** Zieht die aktuellen Laboratory-Dark-Farben aus dem Theme. */
    private void syncFromTheme(FTheme t) {
        if (t == null) return;
        if (!customColorsLocked) {
            liquidColorTop    = t.getPrimary();       // Tuerkis
            liquidColorBottom = t.getAccent();        // Weinrot
            glassRim          = t.getGlassRim();
            glowColor         = t.getGlow();
        }
        glassBody         = t.getGlassBody();
        glassHighlight    = t.getGlassHighlight();
        shadowColor       = t.getShadow();
        rippleColor       = t.getRipple();
        textColor         = t.getText();
        tickColor         = t.getTextMuted();
        trackColor        = t.getBackground();
    }

    // =====================================================================
    //  UI-Delegate-Vertrag
    // =====================================================================

    @Override
    public void updateUI() {
        // Delegate erneut installieren, KEIN super.updateUI() (setzt sonst Default zurueck).
        setUI(new FSliderUI(this));
        updateLabelUIs();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        FTheme.getInstance().addThemeListener(themeListener);
        syncFromTheme(FTheme.getInstance());
        if (animated && isShowing()) ensureRunning();
    }

    @Override
    public void removeNotify() {
        FTheme.getInstance().removeThemeListener(themeListener);
        if (timer != null) timer.stop();
        super.removeNotify();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        int extra = (variant != null && variant.showTicks) ? 26 : 0;
        if (getOrientation() == JSlider.HORIZONTAL) {
            return new Dimension(Math.max(d.width, 160),
                    Math.max(thumbSize + 14 + extra, d.height));
        } else {
            return new Dimension(Math.max(thumbSize + 14 + extra, d.width),
                    Math.max(d.height, 160));
        }
    }

    // =====================================================================
    //  Animation
    // =====================================================================

    private double currentFrac() {
        int range = getMaximum() - getMinimum();
        if (range <= 0) return 0.0;
        return (getValue() - getMinimum()) / (double) range;
    }

    private void ensureRunning() {
        if (timer != null && !timer.isRunning()) timer.start();
    }

    private void tick() {
        boolean busy = false;

        // 1) Glide der Fuellung Richtung echtem Wert.
        //    Beim Ziehen folgt der Thumb dem Finger 1:1 (kein Nachlaufen).
        double target = currentFrac();
        double dFrac = target - displayFrac;
        if (dragging) {
            displayFrac = target;
        } else if (Math.abs(dFrac) > 0.0006) {
            displayFrac += dFrac * glideSpeed;
            busy = true;
        } else {
            displayFrac = target;
        }

        // 2) Hover ein-/ausblenden
        double hTarget = (hovering && hoverEnabled && isEnabled()) ? 1.0 : 0.0;
        double dHover = hTarget - hoverProgress;
        if (Math.abs(dHover) > 0.004) {
            hoverProgress += dHover * (hTarget > hoverProgress ? hoverEaseIn : hoverEaseOut);
            busy = true;
        } else {
            hoverProgress = hTarget;
        }

        // 3) Press-Zustand
        double pTarget = dragging ? 1.0 : 0.0;
        double dPress = pTarget - pressProgress;
        if (Math.abs(dPress) > 0.004) {
            pressProgress += dPress * 0.25;
            busy = true;
        } else {
            pressProgress = pTarget;
        }

        // 4) Glow-Puls (nur relevant solange Hover sichtbar)
        if (hoverProgress > 0.01 && glowEnabled) {
            glowPulse += 0.12;
            if (glowPulse > Math.PI * 2) glowPulse -= Math.PI * 2;
            busy = true;
        }

        // 5) Wellenbewegung + Amplitudendaempfung.
        //    CPU-Sparmodus (Default): Welle laeuft nur bei Hover/Drag/Glide.
        //    Ist keine Aktivitaet vorhanden, klingt die Amplitude ab und friert
        //    dann ein -> der Timer darf stoppen (siehe stopIfIdle-Idiom unten).
        boolean liquidActive = animated && variant != null;
        boolean waveActive = liquidActive
                && (dragging || hovering || Math.abs(dFrac) > 0.0006 || idleWaveEnabled);
        if (waveActive) {
            waveOffset += waveSpeed;
            waveAmp = Math.min(5.0, waveAmp + 0.06); // aufpeitschen bei Aktivitaet
            busy = true;
        } else if (liquidActive && waveAmp > 1.42) {
            waveAmp = Math.max(1.4, waveAmp * 0.90);   // zuegig einpendeln
            waveOffset += waveSpeed * 0.3;             // sanftes Nachschwingen beim Abklingen
            busy = true;
        }
        // sonst: Welle steht still (letzter Frame bleibt gemalt) -> spart CPU

        // 6) Blasen: nur bei Aktivitaet neu spawnen, bestehende immer zu Ende animieren.
        if (liquidActive && variant.showBubbles && !liquidBounds.isEmpty()) {
            if (waveActive && displayFrac > 0.02 && rng.nextFloat() < 0.22f) spawnBubble();
            updateBubbles();
            busy = busy || !bubbles.isEmpty();
        } else if (!bubbles.isEmpty()) {
            bubbles.clear();
        }

        // 7) Ripples
        if (!ripples.isEmpty()) {
            Iterator<Ripple> it = ripples.iterator();
            while (it.hasNext()) {
                Ripple r = it.next();
                r.radius += r.maxRadius * rippleSpeed;
                r.alpha  -= rippleFade;
                if (r.alpha <= 0f || r.radius >= r.maxRadius) it.remove();
            }
            busy = busy || !ripples.isEmpty();
        }

        repaint();

        // Timer anhalten, wenn wirklich nichts mehr zu tun ist
        if (!busy) timer.stop();
    }

    private void spawnBubble() {
        Rectangle b = liquidBounds;
        if (b.isEmpty()) return;
        Bubble bub = new Bubble();
        bub.r = 1.5f + rng.nextFloat() * 3.0f;
        bub.speed = 0.3f + rng.nextFloat() * 0.6f;
        bub.life = 1f;
        bub.wobble = rng.nextFloat() * 6f;
        if (liquidHorizontal) {
            // Blasen entlang der gefuellten Laenge, steigen zur Track-Oberkante
            bub.x = b.x + rng.nextInt(Math.max(1, b.width));
            bub.y = b.y + b.height - bub.r;
            bub.targetY = b.y + bub.r;
        } else {
            bub.x = b.x + rng.nextInt(Math.max(1, b.width));
            bub.y = b.y + b.height - bub.r;
            bub.targetY = b.y + bub.r;
        }
        bubbles.add(bub);
    }

    private void updateBubbles() {
        Iterator<Bubble> it = bubbles.iterator();
        while (it.hasNext()) {
            Bubble b = it.next();
            b.life -= 0.02f;
            b.y -= b.speed * 1.6f;
            b.wobble += 0.25f;
            b.x += (float) (Math.sin(b.wobble) * 0.7f);
            if (b.life <= 0f || b.y <= b.targetY) it.remove();
        }
    }

    private void spawnRipple(int x, int y) {
        if (!rippleEnabled) return;
        Ripple r = new Ripple();
        r.x = x; r.y = y;
        r.radius = thumbSize * 0.35f;
        r.maxRadius = thumbSize * 1.6f;
        r.alpha = 0.55f;
        ripples.add(r);
    }

    // Vom UI je Paint gesetzt: aktuelle Fuell-Geometrie fuer Partikelspawn.
    void updateLiquidBounds(int x, int y, int w, int h, boolean horizontal) {
        liquidBounds.setBounds(x, y, Math.max(0, w), Math.max(0, h));
        liquidHorizontal = horizontal;
    }

    // =====================================================================
    //  Interne Partikel-Datenklassen
    // =====================================================================

    static final class Bubble {
        float x, y, r, speed, life, wobble, targetY;
    }

    static final class Ripple {
        float x, y, radius, maxRadius, alpha;
    }

    // =====================================================================
    //  JavaBean-Properties (Getter/Setter)
    // =====================================================================

    public Color getLiquidColorTop() { return liquidColorTop; }
    public void setLiquidColorTop(Color c) { this.liquidColorTop = c; this.customColorsLocked = true; repaint(); }

    public Color getLiquidColorBottom() { return liquidColorBottom; }
    public void setLiquidColorBottom(Color c) { this.liquidColorBottom = c; this.customColorsLocked = true; repaint(); }

    public Color getGlassRim() { return glassRim; }
    public void setGlassRim(Color c) { this.glassRim = c; this.customColorsLocked = true; repaint(); }

    public Color getGlassBody() { return glassBody; }
    public void setGlassBody(Color c) { this.glassBody = c; repaint(); }

    public Color getGlassHighlight() { return glassHighlight; }
    public void setGlassHighlight(Color c) { this.glassHighlight = c; repaint(); }

    public Color getShadowColor() { return shadowColor; }
    public void setShadowColor(Color c) { this.shadowColor = c; repaint(); }

    public Color getRippleColor() { return rippleColor; }
    public void setRippleColor(Color c) { this.rippleColor = c; repaint(); }

    public Color getGlowColor() { return glowColor; }
    public void setGlowColor(Color c) { this.glowColor = c; this.customColorsLocked = true; repaint(); }

    /** Wenn {@code true}, ueberschreibt {@code syncFromTheme()} Liquid-/Glas-/Glow-Farben nicht mehr
     *  (z.B. nach Anwendung eines {@link FSliderStyler}-Presets). */
    public boolean isCustomColorsLocked() { return customColorsLocked; }
    public void setCustomColorsLocked(boolean locked) { this.customColorsLocked = locked; }

    public Color getFoamColor() { return foamColor; }
    public void setFoamColor(Color c) { this.foamColor = c; repaint(); }

    public Color getTrackColor() { return trackColor; }
    public void setTrackColor(Color c) { this.trackColor = c; repaint(); }

    public Color getTextColor() { return textColor; }
    public void setTextColor(Color c) { this.textColor = c; repaint(); }

    public Color getTickColor() { return tickColor; }
    public void setTickColor(Color c) { this.tickColor = c; repaint(); }

    public FSliderVariant getVariant() { return variant; }
    public void setVariant(FSliderVariant v) {
        this.variant = (v == null) ? FSliderVariant.STANDARD : v;
        if (!this.variant.showBubbles) bubbles.clear();
        revalidate();
        repaint();
        ensureRunning();
    }

    public int getArc() { return arc; }
    public void setArc(int a) { this.arc = Math.max(0, a); repaint(); }

    public int getThumbSize() { return thumbSize; }
    public void setThumbSize(int s) { this.thumbSize = Math.max(10, s); revalidate(); repaint(); }

    public float getRimStrokeWidth() { return rimStrokeWidth; }
    public void setRimStrokeWidth(float w) { this.rimStrokeWidth = Math.max(0f, w); repaint(); }

    public int getShadowOffset() { return shadowOffset; }
    public void setShadowOffset(int o) { this.shadowOffset = Math.max(0, o); repaint(); }

    public boolean isHoverEnabled() { return hoverEnabled; }
    public void setHoverEnabled(boolean b) { this.hoverEnabled = b; if (!b) hovering = false; repaint(); }

    public boolean isRippleEnabled() { return rippleEnabled; }
    public void setRippleEnabled(boolean b) { this.rippleEnabled = b; if (!b) ripples.clear(); repaint(); }

    public boolean isGlowEnabled() { return glowEnabled; }
    public void setGlowEnabled(boolean b) { this.glowEnabled = b; repaint(); }

    public boolean isAnimated() { return animated; }
    public void setAnimated(boolean b) {
        this.animated = b;
        if (b) ensureRunning();
        else if (timer != null) { timer.stop(); bubbles.clear(); repaint(); }
    }

    public boolean isShowValueBubble() { return showValueBubble; }
    public void setShowValueBubble(boolean b) { this.showValueBubble = b; repaint(); }

    /**
     * Formatter fuer Tick-Labels (TICKS-Variante) und die Wert-Bubble.
     * {@code null} (Default) zeigt den rohen Integer-Wert.
     * @see FSliderLabelFormatter#percent()
     * @see FSliderLabelFormatter#suffix(String)
     * @see FSliderLabelFormatter#decimal(int, int)
     */
    public FSliderLabelFormatter getTickLabelFormatter() { return tickLabelFormatter; }
    public void setTickLabelFormatter(FSliderLabelFormatter f) {
        this.tickLabelFormatter = f;
        repaint();
    }

    /** Package-private Helfer: formatiert ueber den gesetzten Formatter oder als roher Integer. */
    String formatValue(int value) {
        return (tickLabelFormatter != null) ? tickLabelFormatter.format(value) : Integer.toString(value);
    }

    /**
     * Wenn {@code false} (Default): die Liquid-Welle laeuft nur waehrend Hover,
     * Drag oder Glide-Bewegung und friert danach ein &ndash; spart CPU bei vielen
     * Slidern auf einem Panel. Wenn {@code true}: die Welle plaetschert dauerhaft,
     * auch im Ruhezustand (lebendigerer, aber teurerer Look).
     */
    public boolean isIdleWaveEnabled() { return idleWaveEnabled; }
    public void setIdleWaveEnabled(boolean b) {
        this.idleWaveEnabled = b;
        if (b) ensureRunning();
    }

    public double getHoverEaseIn() { return hoverEaseIn; }
    public void setHoverEaseIn(double d) { this.hoverEaseIn = d; }

    public double getHoverEaseOut() { return hoverEaseOut; }
    public void setHoverEaseOut(double d) { this.hoverEaseOut = d; }

    public double getGlideSpeed() { return glideSpeed; }
    public void setGlideSpeed(double d) { this.glideSpeed = Math.max(0.02, Math.min(1.0, d)); }

    public double getRippleSpeed() { return rippleSpeed; }
    public void setRippleSpeed(double d) { this.rippleSpeed = d; }

    public double getRippleFade() { return rippleFade; }
    public void setRippleFade(double d) { this.rippleFade = d; }

    public double getWaveSpeed() { return waveSpeed; }
    public void setWaveSpeed(double d) { this.waveSpeed = d; }

    // Package-private Lese-Helfer fuer den UI-Delegate
    double surfaceWave(double frac) {
        // Zwei ueberlagerte Sinuswellen fuer plastische, nicht-monotone Oberflaeche.
        return Math.sin(frac * Math.PI * 2.5 + waveOffset) * waveAmp
             + Math.sin(frac * Math.PI * 1.3 + waveOffset * 0.7) * waveAmp * 0.4;
    }
}
