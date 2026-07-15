package com.dan.fradiobutton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/**
 * FRadioButton — ReagenzglasBar-Stil JRadioButton.
 *
 * Merkmale:
 *  - Kreis-Indikator mit Glasrand (glassRim) und Gradient-Fill bei Selektion
 *  - Hover-Halo animiert (easing, 16 ms Timer)
 *  - Ripple-Effekt bei Klick
 *  - Turquoise/Weinrot Gradient-Kern wenn selected
 *  - Rollover über setRolloverEnabled(true) aktiviert
 *  - Vollständig als JavaBean / NetBeans-Palette-Komponente nutzbar
 */
public class FRadioButton extends JRadioButton {

    // ── Farben ──────────────────────────────────────────────────────────────
    public Color liquidColorTop    = new Color(0x00, 0xB5, 0xAD);
    public Color liquidColorBottom = new Color(0x8B, 0x00, 0x24);
    public Color glassRim          = new Color(140, 200, 255, 160);
    public Color glassBody         = new Color(180, 220, 255, 35);
    public Color glassHighlight    = new Color(255, 255, 255, 110);
    public Color hoverColor        = new Color(0x00, 0xB5, 0xAD, 50);
    public Color textColor         = new Color(220, 235, 255);
    public Color rippleColor       = new Color(0x00, 0xB5, 0xAD, 120);

    // ── Geometrie ────────────────────────────────────────────────────────────
    public int circleSize    = 18;
    public int circleGap     = 6;    // Abstand Kreis ↔ Text
    public int hoverHaloSize = 26;   // Durchmesser des Hover-Halos

    // ── Animation ───────────────────────────────────────────────────────────
    public boolean animated       = true;
    public float   animationSpeed = 0.12f;

    // ── Interner Zustand ─────────────────────────────────────────────────────
    private float hoverProgress  = 0f;
    private float selectProgress = 0f;

    // Ripple
    private float   rippleRadius  = 0f;
    private float   rippleAlpha   = 0f;
    private int     rippleX, rippleY;
    private boolean rippleRunning = false;

    private final Timer animTimer;

    // ── Konstruktoren ─────────────────────────────────────────────────────────
    public FRadioButton() {
        this("Option");
    }

    public FRadioButton(String text) {
        super(text);
        setOpaque(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setRolloverEnabled(true);          // ← PFLICHT für getModel().isRollover()
        setForeground(textColor);
        setFont(new Font("Segoe UI", Font.PLAIN, 13));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        animTimer = new Timer(16, e -> tick());

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { ensureRunning(); }
            @Override public void mouseExited(MouseEvent e)  { ensureRunning(); }
            @Override public void mousePressed(MouseEvent e) {
                startRipple(e.getX(), e.getY());
            }
        });

        // Selektion (z.B. durch ButtonGroup-Wechsel) → Animation anstoßen
        addItemListener(e -> ensureRunning());
    }

    // ── Animation ─────────────────────────────────────────────────────────────
    private void ensureRunning() {
        if (animated && !animTimer.isRunning()) animTimer.start();
    }

    private void tick() {
        boolean hovered    = getModel().isRollover();
        float hoverTarget  = hovered     ? 1f : 0f;
        float selectTarget = isSelected() ? 1f : 0f;

        hoverProgress  += (hoverTarget  - hoverProgress)  * animationSpeed;
        selectProgress += (selectTarget - selectProgress) * animationSpeed;

        if (rippleRunning) {
            rippleRadius += 1.8f;
            rippleAlpha  -= 0.035f;
            if (rippleAlpha <= 0f) { rippleAlpha = 0f; rippleRunning = false; }
        }

        repaint();

        boolean settled = Math.abs(hoverProgress  - hoverTarget)  < 0.004f
                       && Math.abs(selectProgress - selectTarget) < 0.004f
                       && !rippleRunning;
        if (settled) {
            hoverProgress  = hoverTarget;
            selectProgress = selectTarget;
            animTimer.stop();
        }
    }

    private void startRipple(int x, int y) {
        rippleX       = x;
        rippleY       = y;
        rippleRadius  = 0f;
        rippleAlpha   = 0.6f;
        rippleRunning = true;
        ensureRunning();
    }

    // ── Painting ──────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int h  = getHeight();
        int cy = h / 2;
        int cx = circleSize / 2 + 2;   // horizontale Mitte des Kreises

        // ── 1. Ripple ──
        if (rippleAlpha > 0f && animated) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, rippleAlpha * 0.4f));
            g2.setColor(rippleColor);
            int rr = (int) rippleRadius;
            g2.fillOval(cx - rr, cy - rr, rr * 2, rr * 2);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        // ── 2. Hover-Halo ──
        if (hoverProgress > 0.01f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hoverProgress * 0.55f));
            g2.setColor(hoverColor);
            int halo = hoverHaloSize;
            g2.fillOval(cx - halo / 2, cy - halo / 2, halo, halo);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        // ── 3. Kreis-Schatten ──
        for (int i = 3; i >= 1; i--) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f * i));
            g2.setColor(Color.BLACK);
            g2.fillOval(cx - circleSize / 2 + i, cy - circleSize / 2 + i, circleSize, circleSize);
        }
        g2.setComposite(AlphaComposite.SrcOver);

        // ── 4. Glas-Körper ──
        g2.setColor(glassBody);
        g2.fillOval(cx - circleSize / 2, cy - circleSize / 2, circleSize, circleSize);

        // ── 5. Gradient-Kern (Selektion) ──
        if (selectProgress > 0.01f) {
            int innerR = Math.max(1, (int)(circleSize * 0.32f));
            int ix = cx - innerR;
            int iy = cy - innerR;
            int iw = innerR * 2;

            // Clip auf Innenkreis beschränken
            Shape innerClip = new Ellipse2D.Float(
                cx - circleSize / 2f + 2, cy - circleSize / 2f + 2,
                circleSize - 4f,          circleSize - 4f);
            g2.setClip(innerClip);

            GradientPaint gp = new GradientPaint(
                ix, iy + iw, blendToTransparent(liquidColorBottom, 1f - selectProgress),
                ix, iy,      blendToTransparent(liquidColorTop,    1f - selectProgress)
            );
            g2.setPaint(gp);
            g2.fillOval(ix, iy, iw, iw);
            g2.setClip(null);

            // Glanzpunkt
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f * selectProgress));
            g2.setColor(Color.WHITE);
            int hp = Math.max(1, innerR / 2);
            g2.fillOval(cx - hp / 2 - 2, cy - hp / 2 - 2, hp, hp);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        // ── 6. Glas-Rand ──
        g2.setPaint(glassRim);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawOval(cx - circleSize / 2, cy - circleSize / 2, circleSize - 1, circleSize - 1);

        // ── 7. Glas-Highlight-Bogen oben-links ──
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        g2.setColor(glassHighlight);
        g2.setStroke(new BasicStroke(1.5f));
        int hx = cx - circleSize / 2 + 3;
        int hy = cy - circleSize / 2 + 2;
        g2.drawArc(hx, hy, circleSize / 3, circleSize / 3, 120, 110);
        g2.setComposite(AlphaComposite.SrcOver);

        // ── 8. Text ──
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textX = cx + circleSize / 2 + circleGap;
        int textY = cy + fm.getAscent() / 2 - 1;

        // Leichter Schatten
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawString(getText(), textX + 1, textY + 1);

        // Text (disabled → gedimmt)
        float textAlpha = isEnabled() ? 1.0f : 0.4f;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textAlpha));
        g2.setColor(getForeground());
        g2.drawString(getText(), textX, textY);
        g2.setComposite(AlphaComposite.SrcOver);

        g2.dispose();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    /** Blendet eine Farbe hin zu Transparent (alpha=0) abhängig von t ∈ [0,1]. */
    private static Color blendToTransparent(Color c, float t) {
        t = Math.max(0f, Math.min(1f, t));
        // t=0 → volle Farbe, t=1 → komplett transparent
        int alpha = (int)(c.getAlpha() * (1f - t));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    // ── Preferred Size ────────────────────────────────────────────────────────
    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int textW = (getText() == null) ? 0 : fm.stringWidth(getText());
        int w = 2 + circleSize + circleGap + textW + 8;
        int h = Math.max(circleSize + 8, fm.getHeight() + 8);
        return new Dimension(w, h);
    }

    // ── JavaBean Getter/Setter ─────────────────────────────────────────────────
    public Color getLiquidColorTop()    { return liquidColorTop; }
    public void  setLiquidColorTop(Color c) { liquidColorTop = c; repaint(); }

    public Color getLiquidColorBottom() { return liquidColorBottom; }
    public void  setLiquidColorBottom(Color c) { liquidColorBottom = c; repaint(); }

    public Color getGlassRim()  { return glassRim; }
    public void  setGlassRim(Color c) { glassRim = c; repaint(); }

    public Color getGlassBody() { return glassBody; }
    public void  setGlassBody(Color c) { glassBody = c; repaint(); }

    public Color getGlassHighlight() { return glassHighlight; }
    public void  setGlassHighlight(Color c) { glassHighlight = c; repaint(); }

    public Color getHoverColor() { return hoverColor; }
    public void  setHoverColor(Color c) { hoverColor = c; repaint(); }

    public Color getTextColor()  { return textColor; }
    public void  setTextColor(Color c) { textColor = c; setForeground(c); repaint(); }

    public Color getRippleColor() { return rippleColor; }
    public void  setRippleColor(Color c) { rippleColor = c; }

    public int  getCircleSize()  { return circleSize; }
    public void setCircleSize(int s) { circleSize = Math.max(10, Math.min(40, s)); revalidate(); repaint(); }

    public int  getCircleGap()   { return circleGap; }
    public void setCircleGap(int g) { circleGap = Math.max(2, g); revalidate(); repaint(); }

    public int  getHoverHaloSize() { return hoverHaloSize; }
    public void setHoverHaloSize(int s) { hoverHaloSize = Math.max(circleSize, s); repaint(); }

    public boolean isAnimated()   { return animated; }
    public void setAnimated(boolean b) {
        animated = b;
        if (!b) animTimer.stop();
    }

    public float getAnimationSpeed() { return animationSpeed; }
    public void  setAnimationSpeed(float s) {
        animationSpeed = Math.max(0.01f, Math.min(0.5f, s));
    }

    /**
     * Setzt das Gradient-Farbschema für den Selektions-Kern.
     * Kann auch per {@link FButtonGroup#setColorScheme(Color, Color)} propagiert werden.
     */
    public void setColorScheme(Color top, Color bottom) {
        this.liquidColorTop    = top;
        this.liquidColorBottom = bottom;
        repaint();
    }
}
