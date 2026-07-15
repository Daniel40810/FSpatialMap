package com.dan.fcheckbox;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeListener;
import javax.swing.JCheckBox;
import javax.swing.Timer;

/**
 * FCheckBox – Glas-Checkbox im ReagenzglasBar / Laboratory-Dark-Stil.
 *
 * <p>Voll-custom gezeichnet (Box <em>und</em> Text). Zwei Varianten über
 * {@link FCheckBoxVariant}: abgerundete Glas-Box mit animiertem Haken oder
 * Kippschalter. Tri-State über {@link FCheckBoxModel} / {@link FCheckState}.</p>
 *
 * <p>Animation läuft über genau einen 16&nbsp;ms {@link Timer} mit dem
 * F-Easing {@code progress += (target - progress) * animationSpeed} und der
 * Settle-Schwelle {@code 0.004}. Aussehen-Presets über
 * {@link FProductCheckBoxStyler}.</p>
 */
public class FCheckBox extends JCheckBox {

    // ---- Farben (F-Standardidentität) -------------------------------------
    private Color liquidColorTop    = new Color(0x00, 0xB5, 0xAD); // Türkis
    private Color liquidColorBottom = new Color(0x8B, 0x00, 0x24); // Weinrot
    private Color glassRim          = new Color(140, 200, 255, 160);
    private Color glassBody         = new Color(180, 220, 255, 38);
    private Color glassHighlight    = new Color(255, 255, 255, 120);
    private Color checkColor        = new Color(255, 255, 255, 235);
    private Color textColor         = new Color(228, 238, 246);
    private Color focusGlowColor    = new Color(0x00, 0xB5, 0xAD);

    // ---- Geometrie --------------------------------------------------------
    private int boxSize    = 20;
    private int arc        = 8;
    private int iconTextGap = 10;
    private boolean textShadowVisible = true;

    // ---- Verhalten / Variante --------------------------------------------
    private FCheckBoxVariant variant = FCheckBoxVariant.BOX;
    private FCheckBoxModel model = new DefaultFCheckBoxModel();

    // ---- Animation --------------------------------------------------------
    private boolean animated = true;
    private double animationSpeed = 0.28;
    private static final double SETTLE = 0.004;

    private double checkP  = 0.0;  // Haken sichtbar (CHECKED)
    private double indetP  = 0.0;  // Strich sichtbar (INDETERMINATE)
    private double hoverP  = 0.0;  // Hover-Glow
    private double pressP  = 0.0;  // Press-Skalierung
    private double switchP = 0.0;  // Knopf-Position bei SWITCH

    private boolean hovered = false;
    private boolean pressed = false;

    private final Timer timer;

    private final PropertyChangeListener modelListener = evt -> {
        if (FCheckBoxModel.STATE_PROPERTY.equals(evt.getPropertyName())) {
            syncSuperSelected();
            ensureRunning();
            firePropertyChange("state", evt.getOldValue(), evt.getNewValue());
        }
    };

    public FCheckBox() {
        this("");
    }

    public FCheckBox(String text) {
        super();
        setOpaque(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setRolloverEnabled(true);
        setFocusable(true);
        setText(text);
        setFont(new Font("Segoe UI", Font.PLAIN, 13));

        model.addPropertyChangeListener(modelListener);
        syncSuperSelected();

        timer = new Timer(16, e -> onTick());
        // Anfangszustand ohne Animation einrasten:
        snapToState();
    }

    // =======================================================================
    //  Model-API (primärer Datenvertrag)
    // =======================================================================

    public FCheckBoxModel getCheckModel() {
        return model;
    }

    /** Setzt das Model. Primärer Daten-API-Vertrag im F-Stil. */
    public void setCheckModel(FCheckBoxModel newModel) {
        if (newModel == null) {
            newModel = new DefaultFCheckBoxModel();
        }
        FCheckBoxModel old = this.model;
        if (old != null) {
            old.removePropertyChangeListener(modelListener);
        }
        this.model = newModel;
        this.model.addPropertyChangeListener(modelListener);
        syncSuperSelected();
        snapToState();
        repaint();
    }

    public FCheckState getState() {
        return model.getState();
    }

    public void setState(FCheckState state) {
        model.setState(state);
    }

    public boolean isIndeterminateAllowed() {
        return model.isIndeterminateAllowed();
    }

    public void setIndeterminateAllowed(boolean allowed) {
        model.setIndeterminateAllowed(allowed);
    }

    @Override
    public boolean isSelected() {
        // model ist während des super-Konstruktors evtl. noch null.
        return model != null ? model.isSelected() : super.isSelected();
    }

    @Override
    public void setSelected(boolean b) {
        if (model != null) {
            model.setSelected(b);
        } else {
            super.setSelected(b);
        }
    }

    private void syncSuperSelected() {
        // hält JCheckBox.isSelected() (für ButtonGroup/Introspection) im Einklang,
        // ohne die FCheckBox-Logik darüber zu steuern.
        boolean sel = model != null && model.isSelected();
        if (super.isSelected() != sel) {
            super.setSelected(sel);
        }
    }

    // =======================================================================
    //  Eingabe – voll selbst gesteuert (kein Default-Toggle der ButtonModel)
    // =======================================================================

    @Override
    protected void processMouseEvent(MouseEvent e) {
        switch (e.getID()) {
            case MouseEvent.MOUSE_ENTERED:
                hovered = true;
                ensureRunning();
                super.processMouseEvent(e);
                break;
            case MouseEvent.MOUSE_EXITED:
                hovered = false;
                pressed = false;
                ensureRunning();
                super.processMouseEvent(e);
                break;
            case MouseEvent.MOUSE_PRESSED:
                if (isEnabled()) {
                    pressed = true;
                    requestFocusInWindow();
                    ensureRunning();
                }
                // bewusst KEIN super → BasicButtonListener toggelt nicht doppelt
                break;
            case MouseEvent.MOUSE_RELEASED:
                if (pressed && isEnabled() && contains(e.getPoint())) {
                    performToggle();
                }
                pressed = false;
                ensureRunning();
                break;
            default:
                super.processMouseEvent(e);
        }
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED
                && e.getKeyCode() == KeyEvent.VK_SPACE
                && isEnabled()) {
            performToggle();
            e.consume();
            return;
        }
        super.processKeyEvent(e);
    }

    private void performToggle() {
        model.toggle();              // löst modelListener → repaint/animation aus
        fireActionPerformed(new java.awt.event.ActionEvent(
                this, java.awt.event.ActionEvent.ACTION_PERFORMED, "toggle"));
    }

    // =======================================================================
    //  Animation
    // =======================================================================

    private void onTick() {
        double cTarget = model.getState() == FCheckState.CHECKED ? 1.0 : 0.0;
        double iTarget = model.getState() == FCheckState.INDETERMINATE ? 1.0 : 0.0;
        double hTarget = hovered ? 1.0 : 0.0;
        double pTarget = pressed ? 1.0 : 0.0;
        double sTarget = switchTarget();

        checkP  = ease(checkP,  cTarget);
        indetP  = ease(indetP,  iTarget);
        hoverP  = ease(hoverP,  hTarget);
        pressP  = ease(pressP,  pTarget);
        switchP = ease(switchP, sTarget);

        if (settled(checkP, cTarget) && settled(indetP, iTarget)
                && settled(hoverP, hTarget) && settled(pressP, pTarget)
                && settled(switchP, sTarget)) {
            checkP = cTarget; indetP = iTarget; hoverP = hTarget;
            pressP = pTarget; switchP = sTarget;
            timer.stop();
        }
        repaint();
    }

    private double switchTarget() {
        switch (model.getState()) {
            case CHECKED:       return 1.0;
            case INDETERMINATE: return 0.5;
            default:            return 0.0;
        }
    }

    private double ease(double cur, double target) {
        if (!animated) {
            return target;
        }
        return cur + (target - cur) * animationSpeed;
    }

    private boolean settled(double cur, double target) {
        return Math.abs(target - cur) < SETTLE;
    }

    private void ensureRunning() {
        if (animated) {
            if (!timer.isRunning()) {
                timer.start();
            }
        } else {
            snapToState();
            repaint();
        }
    }

    private void snapToState() {
        checkP  = model != null && model.getState() == FCheckState.CHECKED ? 1.0 : 0.0;
        indetP  = model != null && model.getState() == FCheckState.INDETERMINATE ? 1.0 : 0.0;
        hoverP  = hovered ? 1.0 : 0.0;
        pressP  = pressed ? 1.0 : 0.0;
        switchP = switchTarget();
    }

    // =======================================================================
    //  Painting
    // =======================================================================

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        // Originalzustand sichern wird durch g.create()/dispose() abgedeckt.
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Composite original = g2.getComposite();
            if (!isEnabled()) {
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.45f));
            }

            int h = getHeight();
            int controlW = (variant == FCheckBoxVariant.SWITCH)
                    ? Math.round(boxSize * 1.8f) : boxSize;
            int controlH = (variant == FCheckBoxVariant.SWITCH)
                    ? Math.round(boxSize * 0.72f) : boxSize;
            int bx = 1;
            int by = (h - controlH) / 2;

            if (variant == FCheckBoxVariant.SWITCH) {
                paintSwitch(g2, bx, by, controlW, controlH);
            } else {
                paintBox(g2, bx, by, controlW, controlH);
            }

            paintText(g2, bx + controlW + iconTextGap, h);

            g2.setComposite(original);
        } finally {
            g2.dispose();
        }
    }

    private void paintBox(Graphics2D g2, int x, int y, int w, int hh) {
        float shrink = (float) (pressP * 1.5);
        float fx = x + shrink, fy = y + shrink;
        float fw = w - 2 * shrink, fh = hh - 2 * shrink;

        RoundRectangle2D box = new RoundRectangle2D.Float(fx, fy, fw, fh, arc, arc);

        paintHoverHalo(g2, box, fx, fy, fw, fh);

        // Glas-Körper
        g2.setColor(glassBody);
        g2.fill(box);

        // Accent-Füllung (Haken/Strich-Hintergrund)
        double fillP = Math.max(checkP, indetP);
        if (fillP > 0.001) {
            Shape oldClip = g2.getClip();
            g2.clip(box);
            Paint gp = new GradientPaint(0, fy, withAlpha(liquidColorTop, (int) (fillP * 255)),
                    0, fy + fh, withAlpha(liquidColorBottom, (int) (fillP * 255)));
            g2.setPaint(gp);
            g2.fill(box);
            // linker Glanzstreifen
            g2.setColor(withAlpha(glassHighlight, (int) (fillP * 70)));
            g2.fillRect((int) fx, (int) fy, (int) (fw * 0.28f), (int) fh);
            g2.setClip(oldClip);
        }

        // Rim
        g2.setColor(glassRim);
        g2.setStroke(new BasicStroke(1.6f));
        g2.draw(new RoundRectangle2D.Float(fx + 0.5f, fy + 0.5f,
                fw - 1f, fh - 1f, arc, arc));

        // oberer Glanz
        g2.setColor(withAlpha(glassHighlight, 110));
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawLine((int) (fx + arc * 0.4f), (int) (fy + 2),
                (int) (fx + fw - arc * 0.4f), (int) (fy + 2));

        // Fokus-Ring
        paintFocusRing(g2, fx - 2, fy - 2, fw + 4, fh + 4, arc + 2);

        // Haken
        if (checkP > 0.001) {
            paintCheck(g2, fx, fy, fw, fh, checkP);
        }
        // Strich (indeterminate)
        if (indetP > 0.001) {
            paintDash(g2, fx, fy, fw, fh, indetP);
        }
    }

    private void paintSwitch(Graphics2D g2, int x, int y, int w, int hh) {
        float fx = x, fy = y;
        float fw = w, fh = hh;
        float r = fh; // Pille: voll abgerundet

        RoundRectangle2D track = new RoundRectangle2D.Float(fx, fy, fw, fh, r, r);
        paintHoverHalo(g2, track, fx, fy, fw, fh);

        // Track: Glas → Accent je nach switchP
        g2.setColor(glassBody);
        g2.fill(track);
        if (switchP > 0.001) {
            Shape oldClip = g2.getClip();
            g2.clip(track);
            Paint gp = new GradientPaint(fx, fy, withAlpha(liquidColorTop, (int) (switchP * 230)),
                    fx + fw, fy, withAlpha(liquidColorBottom, (int) (switchP * 230)));
            g2.setPaint(gp);
            g2.fill(track);
            g2.setClip(oldClip);
        }

        // Rim
        g2.setColor(glassRim);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new RoundRectangle2D.Float(fx + 0.5f, fy + 0.5f, fw - 1f, fh - 1f, r, r));

        paintFocusRing(g2, fx - 2, fy - 2, fw + 4, fh + 4, r + 2);

        // Knopf
        float knobD = fh - 4;
        float travel = fw - knobD - 4;
        float kx = fx + 2 + (float) (switchP * travel);
        float ky = fy + 2;
        // Schatten
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval((int) kx, (int) (ky + 1), (int) knobD, (int) knobD);
        // Knopf
        g2.setColor(new Color(245, 250, 255, 240));
        g2.fillOval((int) kx, (int) ky, (int) knobD, (int) knobD);
        // Glanz
        g2.setColor(withAlpha(glassHighlight, 160));
        g2.fillOval((int) (kx + knobD * 0.18f), (int) (ky + knobD * 0.16f),
                (int) (knobD * 0.4f), (int) (knobD * 0.3f));
    }

    private void paintHoverHalo(Graphics2D g2, Shape inner,
                                float fx, float fy, float fw, float fh) {
        if (hoverP <= 0.001) {
            return;
        }
        // Glow auf Halo-Ring beschränken (Area-Subtraktion), damit er nicht
        // in die Box blutet und den Kontrast ruiniert.
        float pad = 4f;
        Area halo = new Area(new RoundRectangle2D.Float(
                fx - pad, fy - pad, fw + 2 * pad, fh + 2 * pad, arc + pad, arc + pad));
        halo.subtract(new Area(inner));
        g2.setColor(withAlpha(focusGlowColor, (int) (hoverP * 50)));
        g2.fill(halo);
    }

    private void paintFocusRing(Graphics2D g2, float x, float y,
                                float w, float h, float r) {
        if (!hasFocus()) {
            return;
        }
        g2.setColor(withAlpha(focusGlowColor, 130));
        g2.setStroke(new BasicStroke(1.6f));
        g2.draw(new RoundRectangle2D.Float(x, y, w, h, r, r));
    }

    private void paintCheck(Graphics2D g2, float bx, float by,
                            float bw, float bh, double p) {
        // Drei Kontrollpunkte des Hakens, relativ zur Box.
        Point2D.Float p0 = new Point2D.Float(bx + bw * 0.26f, by + bh * 0.52f);
        Point2D.Float p1 = new Point2D.Float(bx + bw * 0.44f, by + bh * 0.70f);
        Point2D.Float p2 = new Point2D.Float(bx + bw * 0.76f, by + bh * 0.30f);

        double l1 = p0.distance(p1);
        double l2 = p1.distance(p2);
        double total = l1 + l2;
        double drawn = p * total;

        Path2D path = new Path2D.Float();
        path.moveTo(p0.x, p0.y);
        if (drawn <= l1) {
            double t = l1 == 0 ? 0 : drawn / l1;
            path.lineTo(lerp(p0.x, p1.x, t), lerp(p0.y, p1.y, t));
        } else {
            path.lineTo(p1.x, p1.y);
            double t = l2 == 0 ? 0 : (drawn - l1) / l2;
            path.lineTo(lerp(p1.x, p2.x, t), lerp(p1.y, p2.y, t));
        }
        g2.setColor(checkColor);
        g2.setStroke(new BasicStroke(Math.max(1.8f, bw * 0.12f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
    }

    private void paintDash(Graphics2D g2, float bx, float by,
                           float bw, float bh, double p) {
        float cy = by + bh / 2f;
        float half = (bw * 0.28f) * (float) p;
        float cx = bx + bw / 2f;
        g2.setColor(checkColor);
        g2.setStroke(new BasicStroke(Math.max(1.8f, bw * 0.12f), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        g2.drawLine((int) (cx - half), (int) cy, (int) (cx + half), (int) cy);
    }

    private void paintText(Graphics2D g2, int tx, int h) {
        String text = getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        if (textShadowVisible) {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawString(text, tx + 1, ty + 1);
        }
        g2.setColor(textColor);
        g2.drawString(text, tx, ty);
    }

    // =======================================================================
    //  Helpers
    // =======================================================================

    private static float lerp(float a, float b, double t) {
        return (float) (a + (b - a) * t);
    }

    private static Color withAlpha(Color c, int a) {
        a = Math.max(0, Math.min(255, a));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    @Override
    public Dimension getPreferredSize() {
        int controlW = (variant == FCheckBoxVariant.SWITCH)
                ? Math.round(boxSize * 1.8f) : boxSize;
        int controlH = (variant == FCheckBoxVariant.SWITCH)
                ? Math.round(boxSize * 0.72f) : boxSize;
        int w = controlW + 4;
        int h = Math.max(controlH + 6, boxSize + 6);
        String text = getText();
        if (text != null && !text.isEmpty()) {
            FontMetrics fm = getFontMetrics(getFont());
            w += iconTextGap + fm.stringWidth(text) + 4;
            h = Math.max(h, fm.getHeight() + 6);
        }
        return new Dimension(w, h);
    }

    // =======================================================================
    //  JavaBean-Properties
    // =======================================================================

    public FCheckBoxVariant getVariant() { return variant; }
    public void setVariant(FCheckBoxVariant v) {
        this.variant = (v == null) ? FCheckBoxVariant.BOX : v;
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

    public Color getCheckColor() { return checkColor; }
    public void setCheckColor(Color c) { this.checkColor = c; repaint(); }

    public Color getTextColor() { return textColor; }
    public void setTextColor(Color c) {
        this.textColor = c;
        setForeground(c); // Konsistenz mit Introspection
        repaint();
    }

    public Color getFocusGlowColor() { return focusGlowColor; }
    public void setFocusGlowColor(Color c) { this.focusGlowColor = c; repaint(); }

    public int getBoxSize() { return boxSize; }
    public void setBoxSize(int s) {
        this.boxSize = Math.max(10, s);
        revalidate();
        repaint();
    }

    public int getArc() { return arc; }
    public void setArc(int a) { this.arc = Math.max(0, a); repaint(); }

    public int getIconTextGap() { return iconTextGap; }
    public void setIconTextGap(int g) {
        this.iconTextGap = Math.max(0, g);
        revalidate();
        repaint();
    }

    public boolean isTextShadowVisible() { return textShadowVisible; }
    public void setTextShadowVisible(boolean b) { this.textShadowVisible = b; repaint(); }

    public boolean isAnimated() { return animated; }
    public void setAnimated(boolean b) {
        this.animated = b;
        if (!b) {
            if (timer != null) timer.stop();
            snapToState();
            repaint();
        }
    }

    public double getAnimationSpeed() { return animationSpeed; }
    public void setAnimationSpeed(double s) {
        this.animationSpeed = Math.max(0.05, Math.min(0.6, s));
    }
}
