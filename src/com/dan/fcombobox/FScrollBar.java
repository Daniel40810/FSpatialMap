package com.dan.fcombobox;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JScrollBar;

/**
 * FScrollBar &mdash; eine moderne, animierte JScrollBar im ReagenzglasBar-Stil
 * (Weinrot&rarr;T&uuml;rkis Glasoptik) der {@code com.dan}-Bibliothek.
 *
 * <p>Visuelle Merkmale:</p>
 * <ul>
 *   <li>Dezenter, abgerundeter Glas-Track.</li>
 *   <li>Pillen-f&ouml;rmiger Thumb mit Weinrot&rarr;T&uuml;rkis-Verlauf,
 *       hellblauem Rim und Glanzstreifen.</li>
 *   <li>Weicher, ge-easter <b>Hover-Effekt</b>: t&uuml;rkiser Glow und
 *       hellerer Verlauf wenn die Maus &uuml;ber dem Thumb liegt.</li>
 *   <li>Keine klassischen Pfeil-Buttons (cleaner, moderner Look).</li>
 * </ul>
 *
 * <p>JavaBean-tauglich f&uuml;r die NetBeans-Palette: &ouml;ffentlicher
 * No-Arg-Konstruktor (vertikal), Properties mit Getter/Setter. Funktioniert
 * eigenst&auml;ndig in jeder {@code JScrollPane} oder &mdash; wie hier &mdash;
 * im Dropdown-Popup der {@link FComboBox}.</p>
 *
 * @author com.dan
 */
public class FScrollBar extends JScrollBar {

    // ---------------------------------------------------------------- Farben
    /** Obere Thumb-Farbe (T&uuml;rkis). */
    public Color thumbColorTop = new Color(0x00, 0xB5, 0xAD);
    /** Untere Thumb-Farbe (Weinrot). */
    public Color thumbColorBottom = new Color(0x8B, 0x00, 0x24);
    /** Hintergrund-Track (sehr dezent). */
    public Color trackColor = new Color(140, 200, 255, 20);
    /** Thumb-Rand / Outline (hellblau). */
    public Color thumbRim = new Color(140, 200, 255, 150);
    /** Glanzstreifen auf dem Thumb (wei&szlig;). */
    public Color thumbHighlight = new Color(255, 255, 255, 120);
    /** Farbe des Hover-Glows (t&uuml;rkis). */
    public Color hoverGlowColor = new Color(0x00, 0xB5, 0xAD);

    // ------------------------------------------------------------- Geometrie
    /** Dicke der Scrollbar (Querachse) in Pixeln. */
    public int thickness = 12;
    /** Innenabstand des Thumbs zum Track-Rand in Pixeln. */
    public int thumbPadding = 2;
    /** Eckenradius des Thumbs in Pixeln (Pille = halbe Dicke). */
    public int arc = 12;
    /** Mindestl&auml;nge des Thumbs in Pixeln. */
    public int minimumThumbLength = 36;

    // ------------------------------------------------------------ Animation
    /** Schaltet den Hover-&Uuml;bergang ein/aus. */
    public boolean animated = true;
    /** Easing-Geschwindigkeit des Hover-&Uuml;bergangs (0.05&ndash;0.40). */
    public double animationSpeed = 0.20;

    /** Vertikale Scrollbar (Palette-Standard). */
    public FScrollBar() {
        this(VERTICAL);
    }

    public FScrollBar(int orientation) {
        super(orientation);
        setOpaque(false);
        setUnitIncrement(16);
        setBorder(null);
        setUI(new FScrollBarUI());
    }

    /** H&auml;lt nach L&amp;F-Wechsel / Einh&auml;ngen in ein ScrollPane den eigenen UI-Delegate. */
    @Override
    public void updateUI() {
        setUI(new FScrollBarUI());
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (getOrientation() == VERTICAL) {
            d.width = thickness;
        } else {
            d.height = thickness;
        }
        return d;
    }

    // ----------------------------------------------------- Bean-Properties
    public Color getThumbColorTop() { return thumbColorTop; }
    public void setThumbColorTop(Color c) { this.thumbColorTop = c; repaint(); }

    public Color getThumbColorBottom() { return thumbColorBottom; }
    public void setThumbColorBottom(Color c) { this.thumbColorBottom = c; repaint(); }

    public Color getTrackColor() { return trackColor; }
    public void setTrackColor(Color c) { this.trackColor = c; repaint(); }

    public Color getThumbRim() { return thumbRim; }
    public void setThumbRim(Color c) { this.thumbRim = c; repaint(); }

    public Color getThumbHighlight() { return thumbHighlight; }
    public void setThumbHighlight(Color c) { this.thumbHighlight = c; repaint(); }

    public Color getHoverGlowColor() { return hoverGlowColor; }
    public void setHoverGlowColor(Color c) { this.hoverGlowColor = c; repaint(); }

    public int getThickness() { return thickness; }
    public void setThickness(int t) { this.thickness = Math.max(4, t); revalidate(); repaint(); }

    public int getThumbPadding() { return thumbPadding; }
    public void setThumbPadding(int p) { this.thumbPadding = Math.max(0, p); repaint(); }

    public int getArc() { return arc; }
    public void setArc(int arc) { this.arc = Math.max(0, arc); repaint(); }

    public int getMinimumThumbLength() { return minimumThumbLength; }
    public void setMinimumThumbLength(int len) { this.minimumThumbLength = Math.max(12, len); repaint(); }

    public boolean isAnimated() { return animated; }
    public void setAnimated(boolean animated) { this.animated = animated; }

    public double getAnimationSpeed() { return animationSpeed; }
    public void setAnimationSpeed(double s) {
        this.animationSpeed = s < 0.05 ? 0.05 : (s > 0.40 ? 0.40 : s);
    }
}
