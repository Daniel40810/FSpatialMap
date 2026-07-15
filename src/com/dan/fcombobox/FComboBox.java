package com.dan.fcombobox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComboBox;
import javax.swing.Timer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * FComboBox &mdash; eine moderne, animierte JComboBox im ReagenzglasBar-Stil
 * (Weinrot&rarr;T&uuml;rkis Glasoptik) der {@code com.dan}-Bibliothek.
 *
 * <p>Visuelle Merkmale:</p>
 * <ul>
 *   <li>Halbtransparenter Glask&ouml;rper mit hellblauem Rand und linkem Glanzstreifen.</li>
 *   <li>Liquid-Akzentlinie am unteren Rand (Weinrot&rarr;T&uuml;rkis Verlauf).</li>
 *   <li>Weicher, ge-easter <b>Hover-Effekt</b>: t&uuml;rkiser Glow und helleres
 *       Rim wenn die Maus &uuml;ber der Box liegt.</li>
 *   <li>Animierter Chevron-Pfeil, der sich beim &Ouml;ffnen dreht.</li>
 *   <li>Dunkles, abgerundetes Popup mit t&uuml;rkis hinterlegter Auswahl.</li>
 * </ul>
 *
 * <p>Die Klasse ist als JavaBean f&uuml;r die NetBeans-Palette ausgelegt:
 * &ouml;ffentlicher No-Arg-Konstruktor, JavaBean-Properties mit Getter/Setter,
 * und ein Roh-Typ {@code JComboBox} (kein generischer Parameter), damit der
 * NetBeans-Forms-Generator keine Typkonflikte erzeugt.</p>
 *
 * @author com.dan
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FComboBox extends JComboBox {

    // ---------------------------------------------------------------- Farben
    /** Obere Akzentfarbe (T&uuml;rkis) &mdash; ReagenzglasBar-Identit&auml;t. */
    public Color liquidColorTop = new Color(0x00, 0xB5, 0xAD);
    /** Untere Akzentfarbe (Weinrot) &mdash; ReagenzglasBar-Identit&auml;t. */
    public Color liquidColorBottom = new Color(0x8B, 0x00, 0x24);
    /** Glasf&uuml;llung des K&ouml;rpers (deckend). */
    public Color glassBody = new Color(18, 22, 34, 255);
    /** Glasrand / Outline (hellblau). */
    public Color glassRim = new Color(140, 200, 255, 160);
    /** Linker Glanzstreifen (wei&szlig;). */
    public Color glassHighlight = new Color(255, 255, 255, 110);
    /** Farbe des Hover-Glows (t&uuml;rkis). */
    public Color hoverGlowColor = new Color(0x00, 0xB5, 0xAD);
    /** Textfarbe des angezeigten Wertes. */
    public Color textColor = new Color(0xF0, 0xF6, 0xFF);
    /** Hintergrund des Dropdown-Popups (dunkel). */
    public Color popupBackground = new Color(0x12, 0x14, 0x1E);
    /** Farbe des Chevron-Pfeils. */
    public Color arrowColor = new Color(0xBF, 0xEE, 0xEB);

    // ------------------------------------------------------------- Geometrie
    /** Eckenradius der Box in Pixeln. */
    public int arc = 16;
    /** Strichst&auml;rke des Glasrands in Pixeln. */
    public float rimStrokeWidth = 2.0f;
    /** Innenabstand links/rechts f&uuml;r den Text in Pixeln. */
    public int horizontalPadding = 12;

    // ------------------------------------------------------------ Animation
    /** Schaltet alle Animationen (Hover, &Ouml;ffnen) ein/aus. */
    public boolean animated = true;
    /** Easing-Geschwindigkeit der Hover-/&Ouml;ffnen-&Uuml;berg&auml;nge (0.05&ndash;0.40). */
    public double animationSpeed = 0.18;

    // -------------------------------------------------------- interner State
    /** 0.0 = nicht gehovert, 1.0 = voll gehovert (ge-easter Wert). */
    private double hoverProgress = 0.0;
    /** 0.0 = geschlossen, 1.0 = ge&ouml;ffnet (ge-easter Wert). */
    private double openProgress = 0.0;

    private boolean hovered = false;
    private boolean open = false;

    private final Timer animTimer;

    public FComboBox() {
        super();
        setOpaque(false);
        setFocusable(true);
        setMaximumRowCount(10);
        // Heavyweight-Popup: liegt als eigenes Fenster garantiert ueber
        // allen animierten Geschwister-Komponenten (F-Buttons/-Slider
        // malen sonst ueber das leichtgewichtige Popup).
        setLightWeightPopupEnabled(false);
        setFont(getFont().deriveFont(13f));
        // BasicComboBoxUI faerbt den angezeigten Wert mit getForeground() ein.
        setForeground(textColor);

        // Eigener UI-Delegate (Glas-Painting, Pfeil, Popup).
        setUI(new FComboBoxUI());

        // Hover-Erkennung.
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { setHoveredState(true); }
            @Override public void mouseExited(MouseEvent e)  { setHoveredState(false); }
        });

        // &Ouml;ffnen/Schlie&szlig;en treibt den Pfeil und den Glow.
        addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e)   { open = true;  ensureRunning(); }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { open = false; ensureRunning(); }
            @Override public void popupMenuCanceled(PopupMenuEvent e)            { open = false; ensureRunning(); }
        });

        // 60-fps-Timer fuer weiche Uebergaenge.
        animTimer = new Timer(16, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { tick(); }
        });
        animTimer.setRepeats(true);
    }

    /**
     * Convenience-Konstruktor, der Items direkt befüllt.
     * Ermöglicht Nutzung via Reflection (z. B. in Demo-Frames).
     */
    public FComboBox(Object[] items) {
        this();
        if (items != null) {
            for (Object item : items) {
                addItem(item);
            }
        }
    }

    // ------------------------------------------------------- Animations-Loop
    private void ensureRunning() {
        if (animated) {
            if (!animTimer.isRunning()) animTimer.start();
        } else {
            // Ohne Animation Ziel sofort uebernehmen.
            hoverProgress = hovered ? 1.0 : 0.0;
            openProgress  = open    ? 1.0 : 0.0;
            repaint();
        }
    }

    private void tick() {
        double hTarget = hovered ? 1.0 : 0.0;
        double oTarget = open    ? 1.0 : 0.0;
        double s = clamp(animationSpeed, 0.05, 0.40);

        hoverProgress += (hTarget - hoverProgress) * s;
        openProgress  += (oTarget - openProgress)  * s;

        boolean settled =
                Math.abs(hTarget - hoverProgress) < 0.004 &&
                Math.abs(oTarget - openProgress)  < 0.004;

        if (settled) {
            hoverProgress = hTarget;
            openProgress  = oTarget;
            animTimer.stop();
        }
        repaint();
    }

    /**
     * Setzt den Hover-Zustand; auch vom Pfeil-Button des UI-Delegates
     * aufgerufen, da Kind-Komponenten mit eigenen MouseListenern die
     * Enter/Exit-Events der Combo sonst verschlucken.
     */
    void setHoveredState(boolean h) {
        if (hovered != h) {
            hovered = h;
            ensureRunning();
        }
    }

    /** Vom UI-Delegate gelesener Hover-Fortschritt (0.0&ndash;1.0). */
    double getHoverProgress() { return clamp(hoverProgress, 0.0, 1.0); }
    /** Vom UI-Delegate gelesener &Ouml;ffnungs-Fortschritt (0.0&ndash;1.0). */
    double getOpenProgress()  { return clamp(openProgress,  0.0, 1.0); }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        // Etwas Luft fuer Glas/Rim.
        d.height = Math.max(d.height, 38);
        d.width  = Math.max(d.width, 140);
        return d;
    }

    @Override
    public Dimension getMaximumSize() {
        // Hoehe fixieren: sonst streckt z. B. BoxLayout die Box vertikal,
        // der quadratische Pfeil-Button wird riesig und verdraengt den Text.
        Dimension d = getPreferredSize();
        d.width = Integer.MAX_VALUE;
        return d;
    }

    // ----------------------------------------------------- Bean-Properties
    public Color getLiquidColorTop() { return liquidColorTop; }
    public void setLiquidColorTop(Color c) { this.liquidColorTop = c; repaint(); }

    public Color getLiquidColorBottom() { return liquidColorBottom; }
    public void setLiquidColorBottom(Color c) { this.liquidColorBottom = c; repaint(); }

    public Color getGlassBody() { return glassBody; }
    public void setGlassBody(Color c) { this.glassBody = c; repaint(); }

    public Color getGlassRim() { return glassRim; }
    public void setGlassRim(Color c) { this.glassRim = c; repaint(); }

    public Color getGlassHighlight() { return glassHighlight; }
    public void setGlassHighlight(Color c) { this.glassHighlight = c; repaint(); }

    public Color getHoverGlowColor() { return hoverGlowColor; }
    public void setHoverGlowColor(Color c) { this.hoverGlowColor = c; repaint(); }

    public Color getTextColor() { return textColor; }
    public void setTextColor(Color c) { this.textColor = c; setForeground(c); repaint(); }

    public Color getPopupBackground() { return popupBackground; }
    public void setPopupBackground(Color c) { this.popupBackground = c; repaint(); }

    public Color getArrowColor() { return arrowColor; }
    public void setArrowColor(Color c) { this.arrowColor = c; repaint(); }

    public int getArc() { return arc; }
    public void setArc(int arc) { this.arc = Math.max(0, arc); repaint(); }

    public float getRimStrokeWidth() { return rimStrokeWidth; }
    public void setRimStrokeWidth(float w) { this.rimStrokeWidth = Math.max(0.5f, w); repaint(); }

    public int getHorizontalPadding() { return horizontalPadding; }
    public void setHorizontalPadding(int p) { this.horizontalPadding = Math.max(0, p); revalidate(); repaint(); }

    public boolean isAnimated() { return animated; }
    public void setAnimated(boolean animated) {
        this.animated = animated;
        ensureRunning();
    }

    public double getAnimationSpeed() { return animationSpeed; }
    public void setAnimationSpeed(double s) { this.animationSpeed = clamp(s, 0.05, 0.40); }
}
