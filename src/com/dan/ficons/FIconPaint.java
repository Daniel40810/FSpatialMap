package com.dan.ficons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

/**
 * Gemeinsame ReagenzglasBar-Farbidentität und Mal-Helfer für alle F-Style-Icons.
 * Reine Utility-Klasse — nicht instanziierbar.
 *
 * @author com.dan
 */
public final class FIconPaint {

    private FIconPaint() { }

    // ---- ReagenzglasBar-Farbidentität ----
    public static final Color TURQUOISE = new Color(0x00, 0xB5, 0xAD); // Türkis
    public static final Color WINE_RED  = new Color(0x8B, 0x00, 0x24); // Weinrot
    public static final Color GLASS_RIM = new Color(140, 200, 255, 150);
    public static final Color GLASS_BODY= new Color(180, 220, 255, 40);
    public static final Color HIGHLIGHT = new Color(255, 255, 255, 150);
    public static final Color INK       = new Color(0xEA, 0xFB, 0xFF); // helle "Tinte"
    public static final Color DARK_HOLE = new Color(0x12, 0x18, 0x20); // Aussparung

    /** Setzt die für F-Icons üblichen Rendering-Hints. */
    public static void hints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
    }

    /** Vertikaler Türkis(oben)&rarr;Weinrot(unten)-Verlauf über das Feld. */
    public static Paint liquid(float x, float y, float w, float h) {
        return new GradientPaint(x, y, TURQUOISE, x, y + h, WINE_RED);
    }

    /** Diagonaler Türkis&rarr;Weinrot-Verlauf (für längliche Symbole). */
    public static Paint liquidDiagonal(float x1, float y1, float x2, float y2) {
        return new GradientPaint(x1, y1, TURQUOISE, x2, y2, WINE_RED);
    }

    /** Feiner hellblauer Glasrand um eine Form. */
    public static void rim(Graphics2D g2, Shape s, float stroke) {
        Stroke os = g2.getStroke();
        Paint op = g2.getPaint();
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(GLASS_RIM);
        g2.draw(s);
        g2.setStroke(os);
        g2.setPaint(op);
    }

    /**
     * Diagonaler Glanzstreifen oben-links innerhalb einer geclippten Form.
     * Der Aufrufer setzt vorher das Clip auf die Symbolform.
     */
    public static void shine(Graphics2D g2, float x, float y, float w, float h) {
        Paint op = g2.getPaint();
        float sw = Math.max(1f, w * 0.45f);
        g2.setPaint(new GradientPaint(x, y, HIGHLIGHT,
                x + sw, y + h * 0.5f, new Color(255, 255, 255, 0)));
        g2.fillRect((int) x, (int) y, (int) Math.ceil(sw), (int) Math.ceil(h * 0.6f));
        g2.setPaint(op);
    }

    /** Rendert ein Glyph in ein frisches transparentes Bild. */
    public static BufferedImage render(FIcon icon, int dim) {
        BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            hints(g2);
            icon.paintGlyph(g2, dim);
        } finally {
            g2.dispose();
        }
        return img;
    }

    /** Hilfsfunktion: Farbe mit anderem Alpha. */
    public static Color alpha(Color c, int a) {
        a = a < 0 ? 0 : (a > 255 ? 255 : a);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
