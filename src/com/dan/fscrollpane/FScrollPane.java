package com.dan.fscrollpane;

import com.dan.fspatialmap.FLayerPanel;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.AbstractBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Glas-ScrollPane im ReagenzglasBar-Look: abgerundeter dunkler Koerper,
 * hellblauer Glas-Rim, Shine-Streifen sowie schlanke Tuerkis-Scrollbars
 * ({@link FScrollBarUI}). Standard-Wrapper fuer {@link FTable}.
 *
 * <p>{@code updateUI()} reinstalliert die Scrollbar-Delegates, damit Swing
 * sie bei einem L&amp;F-Wechsel nicht still auf den Default zurueckdreht.</p>
 */
public class FScrollPane extends JScrollPane {

    private Color bodyColor   = new Color(0x141A24);
    private Color rimColor    = new Color(140, 200, 255, 150);
    private Color accentColor = new Color(0x00B5AD);
    private int   arc         = 18;
    private final int pad     = 6;

    public FScrollPane() {
        super();
        init();
    }

    public FScrollPane(Component view) {
        super(view);
        init();
    }

    private void init() {
        setOpaque(false);
        getViewport().setOpaque(false);
        setBorder(new GlassBorder());
        setViewportBorder(null);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        installScrollBars();
        syncBodyFromView();
    }

    private void installScrollBars() {
        JScrollBar v = getVerticalScrollBar();
        JScrollBar h = getHorizontalScrollBar();
        if (v != null) {
            v.setUI(new FScrollBarUI(accentColor));
            v.setOpaque(false);
            v.setPreferredSize(new java.awt.Dimension(12, 0));
        }
        if (h != null) {
            h.setUI(new FScrollBarUI(accentColor));
            h.setOpaque(false);
            h.setPreferredSize(new java.awt.Dimension(0, 12));
        }
    }

    /** Uebernimmt Body-/Akzentfarbe von einer eingebetteten FTable. */
    private void syncBodyFromView() {
        Component view = (getViewport() != null) ? getViewport().getView() : null;
        if (view instanceof FLayerPanel) {
            FLayerPanel t = (FLayerPanel) view;
//            bodyColor   = t.getBodyColor();
//            accentColor = t.getAccentColor();
            installScrollBars();
        }
    }

    @Override
    public void setViewportView(Component view) {
        super.setViewportView(view);
        syncBodyFromView();
        repaint();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // Delegates erneut installieren — sonst still auf Default zurueckgesetzt
        installScrollBars();
        setBorder(new GlassBorder());
        setOpaque(false);
        if (getViewport() != null) {
            getViewport().setOpaque(false);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Schatten (mehrschichtig, weicher Versatz)
        for (int i = 4; i >= 1; i--) {
            g2.setColor(new Color(0, 0, 0, 10 + i * 6));
            g2.fillRoundRect(pad - i, pad - i + 2, w - 2 * pad + 2 * i, h - 2 * pad + 2 * i, arc + i, arc + i);
        }

        // dunkler Glaskoerper
        int bx = pad, by = pad, bw = w - 2 * pad, bh = h - 2 * pad;
        g2.setColor(bodyColor);
        g2.fillRoundRect(bx, by, bw, bh, arc, arc);

        // Glasfuellung (oben heller)
        g2.setPaint(new java.awt.GradientPaint(0, by, new Color(180, 220, 255, 16),
                0, by + bh, new Color(180, 220, 255, 0)));
        g2.fillRoundRect(bx, by, bw, bh, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }

    // ---- Properties ------------------------------------------------------
    public Color getBodyColor() { return bodyColor; }
    public void setBodyColor(Color c) { if (c != null) { bodyColor = c; repaint(); } }

    public Color getRimColor() { return rimColor; }
    public void setRimColor(Color c) { if (c != null) { rimColor = c; repaint(); } }

    public Color getAccentColor() { return accentColor; }
    public void setAccentColor(Color c) {
        if (c != null) { accentColor = c; installScrollBars(); repaint(); }
    }

    public int getArc() { return arc; }
    public void setArc(int arc) { this.arc = Math.max(0, arc); repaint(); }

    // ---- Glas-Rim als Border (liegt ueber dem Viewport) ------------------
    private final class GlassBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(pad + 3, pad + 3, pad + 3, pad + 3);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(pad + 3, pad + 3, pad + 3, pad + 3);
            return insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int bx = pad, by = pad, bw = width - 2 * pad, bh = height - 2 * pad;

            // Shine-Streifen oben (clip auf abgerundeten Koerper)
            java.awt.Shape oldClip = g2.getClip();
            g2.clip(new java.awt.geom.RoundRectangle2D.Float(bx, by, bw, bh, arc, arc));
            g2.setPaint(new java.awt.GradientPaint(bx, by, new Color(255, 255, 255, 26),
                    bx, by + Math.max(10, bh / 6), new Color(255, 255, 255, 0)));
            g2.fillRect(bx, by, bw, Math.max(10, bh / 6));
            g2.setClip(oldClip);

            // Glas-Rim
            g2.setStroke(new java.awt.BasicStroke(1.4f));
            g2.setColor(rimColor);
            g2.drawRoundRect(bx, by, bw - 1, bh - 1, arc, arc);

            g2.dispose();
        }
    }
}
