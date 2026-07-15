package com.dan.fcombobox;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * UI-Delegate fuer {@link FScrollBar}. Zeichnet einen dezenten Glas-Track und
 * einen pillenfoermigen Thumb im Weinrot&rarr;Tuerkis-Verlauf mit Rim,
 * Glanzstreifen und weichem, ge-eastem Hover-Glow.
 */
public class FScrollBarUI extends BasicScrollBarUI {

    /** Pflicht-Factory fuer {@code UIManager}-Instanziierung. */
    public static ComponentUI createUI(JComponent c) {
        return new FScrollBarUI();
    }

    /** 0.0 = nicht gehovert, 1.0 = voll gehovert (ge-easter Wert). */
    private double hoverProgress = 0.0;
    private Timer animTimer;

    private FScrollBar bar() {
        return (FScrollBar) scrollbar;
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        scrollbar.setOpaque(false);
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        animTimer = new Timer(16, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { tick(); }
        });
        animTimer.setRepeats(true);
    }

    @Override
    protected void uninstallListeners() {
        if (animTimer != null) { animTimer.stop(); animTimer = null; }
        super.uninstallListeners();
    }

    @Override
    protected void setThumbRollover(boolean active) {
        boolean changed = active != isThumbRollover();
        super.setThumbRollover(active);
        if (changed) ensureRunning();
    }

    private void ensureRunning() {
        FScrollBar b = bar();
        if (b != null && !b.animated) {
            hoverProgress = isThumbRollover() ? 1.0 : 0.0;
            scrollbar.repaint();
            return;
        }
        if (animTimer != null && !animTimer.isRunning()) animTimer.start();
    }

    private void tick() {
        FScrollBar b = bar();
        double target = isThumbRollover() ? 1.0 : 0.0;
        double s = (b != null) ? b.animationSpeed : 0.20;
        hoverProgress += (target - hoverProgress) * s;
        if (Math.abs(target - hoverProgress) < 0.004) {
            hoverProgress = target;
            if (animTimer != null) animTimer.stop();
        }
        scrollbar.repaint();
    }

    // --------------------------------------------------------- Pfeil-Buttons

    @Override
    protected JButton createDecreaseButton(int orientation) { return zeroButton(); }

    @Override
    protected JButton createIncreaseButton(int orientation) { return zeroButton(); }

    private JButton zeroButton() {
        JButton b = new JButton();
        Dimension z = new Dimension(0, 0);
        b.setPreferredSize(z);
        b.setMinimumSize(z);
        b.setMaximumSize(z);
        b.setFocusable(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        return b;
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        FScrollBar b = bar();
        int len = (b != null) ? b.minimumThumbLength : 36;
        int th = (b != null) ? b.thickness : 12;
        return (scrollbar.getOrientation() == FScrollBar.VERTICAL)
                ? new Dimension(th, len)
                : new Dimension(len, th);
    }

    // --------------------------------------------------------------- Painting

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        FScrollBar b = bar();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int pad = b.thumbPadding;
            boolean vert = scrollbar.getOrientation() == FScrollBar.VERTICAL;
            float x = trackBounds.x + (vert ? pad : 0);
            float y = trackBounds.y + (vert ? 0 : pad);
            float w = trackBounds.width  - (vert ? pad * 2f : 0);
            float h = trackBounds.height - (vert ? 0 : pad * 2f);
            float r = Math.min(w, h);
            g2.setColor(b.trackColor);
            g2.fill(new RoundRectangle2D.Float(x, y, w, h, r, r));
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle tb) {
        if (tb.width <= 0 || tb.height <= 0) return;
        FScrollBar b = bar();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean vert = scrollbar.getOrientation() == FScrollBar.VERTICAL;
            int pad = b.thumbPadding;
            float x = tb.x + pad;
            float y = tb.y + pad;
            float w = tb.width  - pad * 2f;
            float h = tb.height - pad * 2f;
            float r = Math.min(w, h);
            float arc = Math.min(b.arc, r);
            double hover = clamp01(hoverProgress);

            // 1) Hover-Glow nach aussen.
            if (hover > 0.01) {
                int layers = 4;
                for (int i = layers; i >= 1; i--) {
                    float frac = (float) i / layers;
                    int alpha = (int) (60 * hover * (1f - frac));
                    if (alpha <= 0) continue;
                    g2.setColor(withAlpha(b.hoverGlowColor, alpha));
                    float sp = 1.5f + frac * 4f;
                    g2.fill(new RoundRectangle2D.Float(
                            x - sp, y - sp, w + sp * 2f, h + sp * 2f, arc + sp, arc + sp));
                }
            }

            Shape thumb = new RoundRectangle2D.Float(x, y, w, h, arc, arc);

            // 2) Verlauf Weinrot->Tuerkis (heller bei Hover).
            int boost = (int) (28 * hover);
            Color top = brighten(b.thumbColorTop, boost);
            Color bot = brighten(b.thumbColorBottom, boost / 2);
            if (vert) {
                g2.setPaint(new GradientPaint(0, y + h, bot, 0, y, top));
            } else {
                g2.setPaint(new GradientPaint(x, 0, bot, x + w, 0, top));
            }
            g2.fill(thumb);

            // 3) Glanzstreifen (links bei vertikal / oben bei horizontal).
            Shape oldClip = g2.getClip();
            g2.clip(thumb);
            if (vert) {
                float sw = Math.max(2f, w * 0.35f);
                g2.setPaint(new GradientPaint(
                        x, 0, withAlpha(b.thumbHighlight, b.thumbHighlight.getAlpha()),
                        x + sw, 0, withAlpha(b.thumbHighlight, 0)));
                g2.fill(new RoundRectangle2D.Float(x, y, sw, h, arc, arc));
            } else {
                float sh = Math.max(2f, h * 0.35f);
                g2.setPaint(new GradientPaint(
                        0, y, withAlpha(b.thumbHighlight, b.thumbHighlight.getAlpha()),
                        0, y + sh, withAlpha(b.thumbHighlight, 0)));
                g2.fill(new RoundRectangle2D.Float(x, y, w, sh, arc, arc));
            }
            g2.setClip(oldClip);

            // 4) Rim (heller bei Hover).
            g2.setColor(brighten(b.thumbRim, (int) (40 * hover)));
            g2.setStroke(new BasicStroke(1.2f + (float) (0.5 * hover)));
            g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1f, h - 1f, arc, arc));
        } finally {
            g2.dispose();
        }
    }

    // -------------------------------------------------------------- Farb-Utils

    private static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    private static Color withAlpha(Color c, int a) {
        a = a < 0 ? 0 : (a > 255 ? 255 : a);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private static Color brighten(Color c, int d) {
        return new Color(
                clampByte(c.getRed() + d),
                clampByte(c.getGreen() + d),
                clampByte(c.getBlue() + d),
                c.getAlpha());
    }

    private static int clampByte(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }
}
