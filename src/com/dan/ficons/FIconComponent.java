package com.dan.ficons;

import javax.swing.JComponent;
import javax.swing.Timer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Animierte F-Style-Icon-Komponente im ReagenzglasBar-Look.
 *
 * <p>Trägt ein {@link FIcon} (Standard: ein {@link FIconType}) und ergänzt drei
 * Effekte über einen einzigen 16&nbsp;ms-Timer mit exponentiellem Easing:</p>
 * <ul>
 *   <li><b>Hover-Glow</b>: weicher Türkis-Halo, der das Symbol umringt — per
 *       {@link Area}-Subtraktion auf einen Ring geclippt, damit der Glow das
 *       Symbol nicht überdeckt und den Kontrast nicht ruiniert.</li>
 *   <li><b>Press-Feedback</b>: kurzes Einsinken (Skalierung &lt; 1).</li>
 *   <li><b>Aktivierungs-Puls</b>: bei {@code active == true} pulsiert der Glow
 *       sanft weiter — etwa für ein aktives Toolbar-/Toggle-Icon.</li>
 * </ul>
 *
 * <p>Voll JavaBean-konform (public no-arg Konstruktor, Getter/Setter,
 * {@code repaint()}/{@code revalidate()} nach Änderungen). Klickbar wie ein
 * Button via {@link #addActionListener(ActionListener)}.</p>
 *
 * @author com.dan
 */
public class FIconComponent extends JComponent {

    // ---- Inhalt ----
    private FIcon icon = FIconType.SETTINGS;
    private FIconType type = FIconType.SETTINGS;   // gespiegelt, falls per Enum gesetzt

    // ---- Farb-/Verhaltens-Properties (öffentlich für Palette/JAR) ----
    public Color glowColor       = new Color(0x00, 0xB5, 0xAD); // Türkis
    public Color activeGlowColor = new Color(0x00, 0xB5, 0xAD);
    public boolean hoverGlowEnabled = true;
    public boolean pressEnabled     = true;
    public boolean active           = false;       // dauerhafter Puls
    public boolean animated         = true;

    // ---- Geometrie / Tuning ----
    public int iconGap        = 7;        // Rand zwischen Symbol und Komponenten-Kante (Platz für Glow)
    public int glowLayers     = 6;        // Anzahl Glow-Schichten (weicher = mehr)
    public float maxGlowAlpha = 120f;     // Spitzen-Deckkraft des Glow (0..255)
    public float hoverLift    = 0.07f;    // Skalierungs-Plus bei Hover
    public float pressSink     = 0.06f;   // Skalierungs-Minus bei Press
    public double easeIn       = 0.22;    // Easing Hover ein
    public double easeOut      = 0.16;    // Easing Hover aus
    public double pulseSpeed   = 0.09;    // Geschwindigkeit Aktivierungs-Puls

    // ---- interner Zustand ----
    private double hoverP = 0.0;
    private double pressP = 0.0;
    private boolean hovered = false;
    private boolean pressing = false;
    private double pulsePhase = 0.0;

    private final Timer timer;
    private final List<ActionListener> actionListeners = new ArrayList<ActionListener>();

    // ===================================================== Konstruktoren

    public FIconComponent() {
        this(FIconType.SETTINGS);
    }

    public FIconComponent(FIconType type) {
        setType(type);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(32, 32));

        timer = new Timer(16, e -> tick());

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                hovered = true; ensureRunning();
            }
            @Override public void mouseExited(MouseEvent e) {
                hovered = false; pressing = false; ensureRunning();
            }
            @Override public void mousePressed(MouseEvent e) {
                if (pressEnabled && isEnabled()) { pressing = true; ensureRunning(); }
            }
            @Override public void mouseReleased(MouseEvent e) {
                boolean wasPressing = pressing;
                pressing = false;
                ensureRunning();
                if (wasPressing && isEnabled() && contains(e.getPoint())) fireAction();
            }
        });
    }

    // ===================================================== Animation

    private void ensureRunning() {
        if (!timer.isRunning()) timer.start();
    }

    private void tick() {
        boolean busy = false;

        // Hover-Easing
        double ht = hovered ? 1.0 : 0.0;
        double he = hovered ? easeIn : easeOut;
        if (Math.abs(ht - hoverP) > 0.004) { hoverP += (ht - hoverP) * he; busy = true; }
        else hoverP = ht;

        // Press-Easing
        double pt = pressing ? 1.0 : 0.0;
        if (Math.abs(pt - pressP) > 0.004) { pressP += (pt - pressP) * 0.35; busy = true; }
        else pressP = pt;

        // Aktivierungs-Puls
        if (animated && active) { pulsePhase += pulseSpeed; busy = true; }

        repaint();
        if (!busy) timer.stop();
    }

    /** Aktueller Glow-Anteil 0..1 aus Hover und (pulsierendem) Aktiv-Zustand. */
    private double glowAmount() {
        double pulse = 0.0;
        if (active) {
            double base = animated ? (0.55 + 0.45 * 0.5 * (1 + Math.sin(pulsePhase))) : 0.85;
            pulse = base;
        }
        return Math.max(hoverGlowEnabled ? hoverP : 0.0, pulse);
    }

    // ===================================================== Painting

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int dim = Math.max(4, Math.min(w, h) - 2 * iconGap);
            int ox = (w - dim) / 2;
            int oy = (h - dim) / 2;
            float cx = w / 2f, cy = h / 2f;

            // 1 · Glow-Halo (nur außen, per Area-Ring)
            double glow = glowAmount();
            if (glow > 0.01) drawGlow(g2, cx, cy, dim, (float) glow);

            // 2 · Symbol mit Hover-Lift / Press-Sink
            float scale = 1f + (float) (hoverLift * hoverP) - (float) (pressSink * pressP);
            g2.translate(cx, cy);
            g2.scale(scale, scale);
            g2.translate(-cx, -cy);
            g2.translate(ox, oy);

            if (!isEnabled()) {
                g2.setComposite(java.awt.AlphaComposite.getInstance(
                        java.awt.AlphaComposite.SRC_OVER, 0.4f));
            }
            icon.paintGlyph(g2, dim);
        } finally {
            g2.dispose();
        }
    }

    private void drawGlow(Graphics2D g2, float cx, float cy, int dim, float amt) {
        Color base = active ? activeGlowColor : glowColor;
        float coreR = dim * 0.5f;                 // Symbol-Radius (Aussparung)
        float maxR  = coreR + iconGap + dim * 0.10f;

        // Ring = äußere Scheibe minus Symbol-Kern → Glow bleibt außen
        Area ring = new Area(new Ellipse2D.Float(cx - maxR, cy - maxR, 2 * maxR, 2 * maxR));
        ring.subtract(new Area(new Ellipse2D.Float(
                cx - coreR * 0.92f, cy - coreR * 0.92f, 2 * coreR * 0.92f, 2 * coreR * 0.92f)));

        java.awt.Shape oc = g2.getClip();
        g2.clip(ring);
        int layers = Math.max(2, glowLayers);
        for (int i = layers; i >= 1; i--) {
            float t = i / (float) layers;          // 1 außen .. 0 innen
            float r = coreR + (maxR - coreR) * t;
            int a = (int) (maxGlowAlpha * amt * (1f - t) * 0.9f);
            if (a <= 0) continue;
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), a));
            g2.fill(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r));
        }
        g2.setClip(oc);
    }

    // ===================================================== Action-API

    public void addActionListener(ActionListener l) {
        if (l != null) actionListeners.add(l);
    }

    public void removeActionListener(ActionListener l) {
        actionListeners.remove(l);
    }

    private void fireAction() {
        java.awt.event.ActionEvent ev = new java.awt.event.ActionEvent(
                this, java.awt.event.ActionEvent.ACTION_PERFORMED, "icon");
        for (ActionListener l : new ArrayList<ActionListener>(actionListeners)) {
            l.actionPerformed(ev);
        }
    }

    // ===================================================== JavaBean API

    public FIcon getIcon() { return icon; }
    public void setIcon(FIcon icon) {
        this.icon = (icon != null) ? icon : FIconType.SETTINGS;
        if (!(icon instanceof FIconType)) this.type = null;
        repaint();
    }

    /** Setzt das Symbol über die Standard-Bibliothek (Palette-Dropdown). */
    public FIconType getType() { return type; }
    public void setType(FIconType type) {
        FIconType t = (type != null) ? type : FIconType.SETTINGS;
        this.type = t;
        this.icon = t;
        repaint();
    }

    public Color getGlowColor() { return glowColor; }
    public void setGlowColor(Color c) { this.glowColor = c; repaint(); }

    public Color getActiveGlowColor() { return activeGlowColor; }
    public void setActiveGlowColor(Color c) { this.activeGlowColor = c; repaint(); }

    public boolean isHoverGlowEnabled() { return hoverGlowEnabled; }
    public void setHoverGlowEnabled(boolean b) { this.hoverGlowEnabled = b; repaint(); }

    public boolean isPressEnabled() { return pressEnabled; }
    public void setPressEnabled(boolean b) { this.pressEnabled = b; }

    public boolean isActive() { return active; }
    public void setActive(boolean b) {
        this.active = b;
        if (b) ensureRunning();
        repaint();
    }

    public boolean isAnimated() { return animated; }
    public void setAnimated(boolean b) {
        this.animated = b;
        if (b && active) ensureRunning();
    }

    public int getIconGap() { return iconGap; }
    public void setIconGap(int v) { this.iconGap = Math.max(0, v); revalidate(); repaint(); }

    public int getGlowLayers() { return glowLayers; }
    public void setGlowLayers(int v) { this.glowLayers = Math.max(2, v); repaint(); }

    public float getMaxGlowAlpha() { return maxGlowAlpha; }
    public void setMaxGlowAlpha(float v) { this.maxGlowAlpha = clamp(v, 0, 255); repaint(); }

    public float getHoverLift() { return hoverLift; }
    public void setHoverLift(float v) { this.hoverLift = clamp(v, 0, 0.5f); }

    public float getPressSink() { return pressSink; }
    public void setPressSink(float v) { this.pressSink = clamp(v, 0, 0.5f); }

    public double getEaseIn() { return easeIn; }
    public void setEaseIn(double v) { this.easeIn = clampD(v, 0.02, 1.0); }

    public double getEaseOut() { return easeOut; }
    public void setEaseOut(double v) { this.easeOut = clampD(v, 0.02, 1.0); }

    public double getPulseSpeed() { return pulseSpeed; }
    public void setPulseSpeed(double v) { this.pulseSpeed = clampD(v, 0.01, 0.5); }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
    private static double clampD(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
