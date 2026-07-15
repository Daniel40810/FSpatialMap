package com.dan.ftooltip;

import com.dan.fstyle.FColors;
import com.dan.fstyle.FTheme;

import javax.swing.JComponent;
import javax.swing.Timer;
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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/**
 * FToolTip — Rich-Tooltip-Bubble im F-Stil (Laboratory Dark Look).
 *
 * Kein eigenstaendiges Fenster (kein JWindow): wird vom {@link FTooltipManager}
 * in den {@code JLayeredPane} des Root-Fensters eingehaengt (Popup-Layer) und
 * per {@link #fadeIn()}/{@link #fadeOut(Runnable)} ein-/ausgeblendet.
 *
 * Rendert Header (optional), Akzentbalken (optional), mehrzeilige Inhalte mit
 * fuehrenden Icons und einen Pfeil, der auf den Anker zeigt. Ein einziger
 * 16 ms-Timer treibt Alpha- und Scale-Overshoot-Animation.
 */
public class FTooltip extends JComponent implements FTheme.ThemeListener {

    /** Kante der Bubble, an der der Pfeil sitzt (zeigt zum Anker). */
    public enum Edge { TOP, BOTTOM, LEFT, RIGHT }

    // ------------------------------------------------------------------
    // Stil-Felder
    // ------------------------------------------------------------------
    public Color glassBody      = new Color(20, 26, 36, 235);
    public Color glassRim       = new Color(140, 200, 255, 170);
    public Color glassHighlight = new Color(255, 255, 255, 30);
    public Color shadowColor    = new Color(0, 0, 0, 90);
    public Color textColor      = new Color(235, 240, 248);
    public Color mutedColor     = new Color(150, 165, 185);

    public Font headerFont = new Font("Segoe UI", Font.BOLD, 12);
    public Font bodyFont   = new Font("Segoe UI", Font.PLAIN, 12);

    private int cornerRadius = 10;
    private int padding = 12;
    private int arrowSize = 8;
    private int iconSize = 14;
    private int lineGap = 4;
    private int maxTextWidth = 260;

    // ------------------------------------------------------------------
    // Zustand
    // ------------------------------------------------------------------
    private FTooltipContent content = new FTooltipContent();
    private Edge arrowEdge = Edge.BOTTOM;
    private int arrowOffset = 20;   // Position des Pfeils entlang der Kante

    private float alpha = 0f;
    private float alphaTarget = 0f;
    private float scale = 0.92f;
    private float scaleTarget = 0.92f;
    private float animationSpeed = 0.28f;

    private final Timer timer;
    private Runnable onHideComplete;

    public FTooltip() {
        setOpaque(false);
        setFocusable(false);
        timer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tick();
            }
        });
        applyTheme(FTheme.getInstance());
    }

    // ------------------------------------------------------------------
    // Theme-Binding
    // ------------------------------------------------------------------
    @Override
    public void addNotify() {
        super.addNotify();
        FTheme.getInstance().addThemeListener(this);
    }

    @Override
    public void removeNotify() {
        FTheme.getInstance().removeThemeListener(this);
        super.removeNotify();
    }

    @Override
    public void themeChanged(FTheme theme) {
        applyTheme(theme);
        repaint();
    }

    private void applyTheme(FTheme t) {
        if (t == null) {
            return;
        }
        glassBody = FColors.withAlpha(FColors.darken(t.getSurface(), 0.35f), 235);
        glassRim = t.getGlassRim();
        glassHighlight = FColors.withAlpha(t.getGlassHighlight(), 30);
        shadowColor = FColors.withAlpha(t.getShadow(), 90);
        textColor = t.getText();
        mutedColor = t.getTextMuted();
    }

    // ------------------------------------------------------------------
    // Content / Layout
    // ------------------------------------------------------------------
    public void setContent(FTooltipContent content) {
        this.content = (content == null) ? new FTooltipContent() : content;
        Dimension d = computeBubbleSize();
        setSize(d);
        setPreferredSize(d);
        repaint();
    }

    public FTooltipContent getContent() {
        return content;
    }

    /** Setzt Pfeil-Kante und Offset (vom Manager anhand des Ankers berechnet). */
    public void setArrow(Edge edge, int offset) {
        this.arrowEdge = (edge == null) ? Edge.BOTTOM : edge;
        this.arrowOffset = offset;
        repaint();
    }

    public int getMaxTextWidth()             { return maxTextWidth; }
    public void setMaxTextWidth(int w)       { maxTextWidth = Math.max(60, w); }

    public float getAnimationSpeed()         { return animationSpeed; }
    public void setAnimationSpeed(float s)   { animationSpeed = Math.max(0.05f, Math.min(0.9f, s)); }

    public Color getGlassBody()              { return glassBody; }
    public void setGlassBody(Color c)        { if (c != null) { glassBody = c; repaint(); } }

    public Color getGlassRim()               { return glassRim; }
    public void setGlassRim(Color c)         { if (c != null) { glassRim = c; repaint(); } }

    /** Berechnet die benoetigte Groesse inkl. Pfeil-Reserve auf allen Seiten. */
    public Dimension computeBubbleSize() {
        Graphics2D probe = probeGraphics();
        FontMetrics fmH = probe.getFontMetrics(headerFont);
        FontMetrics fmB = probe.getFontMetrics(bodyFont);

        int textW = 0;
        if (content.hasHeader()) {
            textW = Math.max(textW, fmH.stringWidth(content.getHeader()));
        }
        for (FTooltipLine line : content.getLines()) {
            int w = fmB.stringWidth(line.getText());
            if (line.hasIcon()) {
                w += iconSize + 6;
            }
            textW = Math.max(textW, w);
        }
        textW = Math.min(textW, maxTextWidth);

        int bubbleW = textW + padding * 2 + (content.hasAccent() ? 6 : 0);
        int bubbleH = padding * 2;
        if (content.hasHeader()) {
            bubbleH += fmH.getHeight() + 6;
        }
        int lineH = Math.max(fmB.getHeight(), iconSize);
        int n = content.getLines().size();
        bubbleH += n * lineH + Math.max(0, n - 1) * lineGap;
        if (bubbleH < padding * 2 + fmB.getHeight()) {
            bubbleH = padding * 2 + fmB.getHeight();
        }

        probe.dispose();
        // Platz fuer den Pfeil auf jeder Seite reservieren (max. genutzte Seite bestimmt sich beim Paint)
        return new Dimension(bubbleW + arrowSize, bubbleH + arrowSize);
    }

    private Graphics2D probeGraphics() {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        return img.createGraphics();
    }

    // ------------------------------------------------------------------
    // Sichtbarkeits-Animation
    // ------------------------------------------------------------------
    public void fadeIn() {
        onHideComplete = null;
        alphaTarget = 1f;
        scaleTarget = 1f;
        ensureRunning();
    }

    public void fadeOut(Runnable onComplete) {
        onHideComplete = onComplete;
        alphaTarget = 0f;
        scaleTarget = 0.92f;
        ensureRunning();
    }

    /** Sofort unsichtbar, ohne Animation (z. B. bei hartem Reset). */
    public void resetHidden() {
        alpha = 0f;
        alphaTarget = 0f;
        scale = 0.92f;
        scaleTarget = 0.92f;
        onHideComplete = null;
        timer.stop();
    }

    private void ensureRunning() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    private void tick() {
        boolean moving = false;

        if (Math.abs(alphaTarget - alpha) > 0.004f) {
            alpha += (alphaTarget - alpha) * animationSpeed;
            moving = true;
        } else {
            alpha = alphaTarget;
        }

        if (Math.abs(scaleTarget - scale) > 0.002f) {
            scale += (scaleTarget - scale) * animationSpeed;
            moving = true;
        } else {
            scale = scaleTarget;
        }

        repaint();

        if (!moving) {
            timer.stop();
            if (alphaTarget == 0f && onHideComplete != null) {
                Runnable r = onHideComplete;
                onHideComplete = null;
                r.run();
            }
        }
    }

    public boolean isFadingOut() {
        return alphaTarget == 0f;
    }

    public float getCurrentAlpha() {
        return alpha;
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        if (alpha <= 0.01f || content.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Composite oldComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            // Scale-Overshoot um das Bubble-Zentrum
            Rectangle bubble = bubbleRect();
            double cx = bubble.getCenterX();
            double cy = bubble.getCenterY();
            AffineTransform oldTx = g2.getTransform();
            g2.translate(cx, cy);
            g2.scale(scale, scale);
            g2.translate(-cx, -cy);

            paintBubble(g2, bubble);

            g2.setTransform(oldTx);
            g2.setComposite(oldComposite);
        } finally {
            g2.dispose();
        }
    }

    private Rectangle bubbleRect() {
        int x = 0, y = 0, w = getWidth(), h = getHeight();
        switch (arrowEdge) {
            case TOP:    y += arrowSize; h -= arrowSize; break;
            case BOTTOM: h -= arrowSize; break;
            case LEFT:   x += arrowSize; w -= arrowSize; break;
            case RIGHT:  w -= arrowSize; break;
            default:     break;
        }
        return new Rectangle(x, y, Math.max(1, w), Math.max(1, h));
    }

    private void paintBubble(Graphics2D g2, Rectangle b) {
        RoundRectangle2D shape = new RoundRectangle2D.Double(b.x, b.y, b.width, b.height, cornerRadius, cornerRadius);

        // Schatten
        g2.setColor(shadowColor);
        g2.fill(new RoundRectangle2D.Double(b.x + 2, b.y + 3, b.width, b.height, cornerRadius, cornerRadius));

        // Glaskoerper
        g2.setPaint(new GradientPaint(0, b.y, FColors.lighten(glassBody, 0.05f),
                0, b.y + b.height, glassBody));
        g2.fill(shape);

        // Shine oben
        g2.setColor(glassHighlight);
        g2.fill(new RoundRectangle2D.Double(b.x + 2, b.y + 2, b.width - 4, b.height * 0.35, cornerRadius - 2, cornerRadius - 2));

        // Akzentbalken links
        int textX = b.x + padding + (content.hasAccent() ? 6 : 0);
        if (content.hasAccent()) {
            Shape oldClip = g2.getClip();
            g2.clip(shape);
            g2.setColor(content.getAccentColor());
            g2.fillRect(b.x, b.y, 4, b.height);
            g2.setClip(oldClip);
        }

        // Rim
        g2.setColor(glassRim);
        g2.setStroke(new BasicStroke(1.1f));
        g2.draw(shape);

        // Pfeil
        paintArrow(g2, b);

        // Inhalt
        paintContent(g2, b, textX);
    }

    private void paintArrow(Graphics2D g2, Rectangle b) {
        Path2D arrow = new Path2D.Double();
        switch (arrowEdge) {
            case BOTTOM: {
                int ax = b.x + clampOffset(arrowOffset, b.width);
                int ay = b.y + b.height;
                arrow.moveTo(ax - arrowSize, ay);
                arrow.lineTo(ax + arrowSize, ay);
                arrow.lineTo(ax, ay + arrowSize);
                arrow.closePath();
                break;
            }
            case TOP: {
                int ax = b.x + clampOffset(arrowOffset, b.width);
                int ay = b.y;
                arrow.moveTo(ax - arrowSize, ay);
                arrow.lineTo(ax + arrowSize, ay);
                arrow.lineTo(ax, ay - arrowSize);
                arrow.closePath();
                break;
            }
            case RIGHT: {
                int ax = b.x + b.width;
                int ay = b.y + clampOffset(arrowOffset, b.height);
                arrow.moveTo(ax, ay - arrowSize);
                arrow.lineTo(ax, ay + arrowSize);
                arrow.lineTo(ax + arrowSize, ay);
                arrow.closePath();
                break;
            }
            default: { // LEFT
                int ax = b.x;
                int ay = b.y + clampOffset(arrowOffset, b.height);
                arrow.moveTo(ax, ay - arrowSize);
                arrow.lineTo(ax, ay + arrowSize);
                arrow.lineTo(ax - arrowSize, ay);
                arrow.closePath();
                break;
            }
        }
        g2.setColor(glassBody);
        g2.fill(arrow);
        g2.setColor(glassRim);
        g2.setStroke(new BasicStroke(1.1f));
        g2.draw(arrow);
    }

    private int clampOffset(int off, int extent) {
        return Math.max(arrowSize + 2, Math.min(extent - arrowSize - 2, off));
    }

    private void paintContent(Graphics2D g2, Rectangle b, int textX) {
        int y = b.y + padding;
        FontMetrics fmH = g2.getFontMetrics(headerFont);
        FontMetrics fmB = g2.getFontMetrics(bodyFont);

        if (content.hasHeader()) {
            g2.setFont(headerFont);
            g2.setColor(textColor);
            g2.drawString(clip(g2, content.getHeader(), b.x + b.width - textX - padding), textX, y + fmH.getAscent());
            y += fmH.getHeight() + 6;
            g2.setColor(FColors.withAlpha(glassRim, 90));
            g2.drawLine(textX, y - 3, b.x + b.width - padding, y - 3);
        }

        g2.setFont(bodyFont);
        int lineH = Math.max(fmB.getHeight(), iconSize);
        int maxTextW = b.x + b.width - textX - padding;

        for (FTooltipLine line : content.getLines()) {
            int lx = textX;
            int cy = y + lineH / 2;
            if (line.hasIcon()) {
                Graphics2D ig = (Graphics2D) g2.create();
                ig.translate(lx, cy - iconSize / 2);
                ig.setColor(mutedColor);
                line.getIcon().paintGlyph(ig, iconSize);
                ig.dispose();
                lx += iconSize + 6;
            }
            g2.setColor(textColor);
            String txt = clip(g2, line.getText(), maxTextW - (lx - textX));
            g2.drawString(txt, lx, y + fmB.getAscent() + (lineH - fmB.getHeight()) / 2);
            y += lineH + lineGap;
        }
    }

    private String clip(Graphics2D g2, String s, int maxW) {
        if (maxW <= 0) {
            return s;
        }
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(s) <= maxW) {
            return s;
        }
        String ell = "\u2026";
        while (s.length() > 1 && fm.stringWidth(s + ell) > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s + ell;
    }
}
