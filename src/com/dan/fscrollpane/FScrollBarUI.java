package com.dan.fscrollpane;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Schlanker Scrollbar-Delegate im F-Stil: kein Track-Hintergrund, runder
 * Tuerkis-Thumb mit Glanzkante, keine Pfeil-Buttons. Wird von
 * {@link FScrollPane} auf vertikale + horizontale Scrollbar installiert.
 */
public class FScrollBarUI extends BasicScrollBarUI {

    private Color thumbColor = new Color(0x00B5AD);
    private int   thickness  = 10;

    public FScrollBarUI() { }

    public FScrollBarUI(Color thumbColor) {
        if (thumbColor != null) {
            this.thumbColor = thumbColor;
        }
    }

    public static ComponentUI createUI(JComponent c) {
        return new FScrollBarUI();
    }

    public void setThumbColor(Color c)  { if (c != null) this.thumbColor = c; }
    public void setThickness(int t)     { this.thickness = Math.max(6, t); }

    @Override
    protected void configureScrollBarColors() {
        trackColor = new Color(0, 0, 0, 0);
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return new Dimension(thickness, 28);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) { return zeroButton(); }

    @Override
    protected JButton createIncreaseButton(int orientation) { return zeroButton(); }

    private JButton zeroButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        b.setFocusable(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        // bewusst leer: transparenter Track passt zum Glaskoerper
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int pad = 2;
        int x = r.x + pad;
        int y = r.y + pad;
        int w = r.width  - pad * 2;
        int h = r.height - pad * 2;
        int arc = Math.min(w, h);

        boolean hovered = isThumbRollover();
        Color top = hovered ? brighten(thumbColor, 0.18f) : thumbColor;
        Color bot = hovered ? thumbColor : darken(thumbColor, 0.25f);

        g2.setPaint(new java.awt.GradientPaint(x, y, top, x, y + h, bot));
        g2.fillRoundRect(x, y, w, h, arc, arc);

        // Glanzkante links/oben
        g2.setColor(new Color(255, 255, 255, hovered ? 90 : 55));
        g2.fillRoundRect(x + 1, y + 1, Math.max(2, w / 3), Math.max(2, h - 2), arc, arc);

        g2.dispose();
    }

    private static Color brighten(Color c, float f) {
        int r = (int) Math.min(255, c.getRed()   + 255 * f);
        int g = (int) Math.min(255, c.getGreen() + 255 * f);
        int b = (int) Math.min(255, c.getBlue()  + 255 * f);
        return new Color(r, g, b);
    }

    private static Color darken(Color c, float f) {
        int r = (int) Math.max(0, c.getRed()   * (1 - f));
        int g = (int) Math.max(0, c.getGreen() * (1 - f));
        int b = (int) Math.max(0, c.getBlue()  * (1 - f));
        return new Color(r, g, b);
    }
}
