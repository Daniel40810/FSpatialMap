package com.dan.ftextfield;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JTextField;

/**
 * Modernes {@link JTextField} im <b>ReagenzglasBar-Glasstil</b> der
 * {@code com.dan}-Bibliothek (Weinrot&rarr;Tuerkis Glasoptik), abgestimmt auf
 * {@link com.dan.fcombobox.FComboBox}.
 *
 * <p>Bietet ein gleitendes Floating-Label, einen Tuerkis-Glow bei Hover/Fokus,
 * eine wachsende Liquid-Akzentlinie als Fokus-Indikator und einen dezenten
 * Klick-Ripple. Die gesamte Effekt-Logik liegt in {@link FFieldEffects} und
 * wird mit {@link FPasswordfield} geteilt.</p>
 *
 * <p>NetBeans-tauglich: oeffentlicher No-Arg-Konstruktor, JavaBean-Properties,
 * keine Konstruktor-Seiteneffekte mit externen Dateien.</p>
 *
 * @author com.dan
 */
public class FTextfield extends JTextField {

    private static final long serialVersionUID = 1L;

    /** Gemeinsame Glas-Effekt-Engine (Floating-Label, Hover/Fokus, Ripple). */
    protected final FFieldEffects fx = new FFieldEffects(this);

    public FTextfield() {
        super();
        initLook();
    }

    public FTextfield(String labelText) {
        super();
        initLook();
        setLabelText(labelText);
    }

    private void initLook() {
        setFont(getFont().deriveFont(15f));
        setCaretColor(new Color(0x00, 0xB5, 0xAD));
        setSelectionColor(new Color(0x00, 0xB5, 0xAD, 90));
        setSelectedTextColor(new Color(0xF0, 0xF6, 0xFF));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(220, d.width), Math.max(54, d.height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D bg = (Graphics2D) g.create();
        try {
            fx.paintGlassBackground(bg, getWidth(), getHeight());
        } finally {
            bg.dispose();
        }

        // Standard-Textdarstellung (Text, Caret, Auswahl) ueber dem Glas.
        super.paintComponent(g);

        Graphics2D top = (Graphics2D) g.create();
        try {
            fx.paintGlassRim(top, getWidth(), getHeight());
            fx.paintFloatingLabel(top, getWidth(), getHeight());
        } finally {
            top.dispose();
        }
    }

    @Override
    protected void paintBorder(Graphics g) {
        // Rand wird in paintComponent (paintGlassRim) gezeichnet.
    }

    // ---- JavaBean-Properties (an die Effekt-Engine delegiert) ---------------
    public String getLabelText() { return fx.getLabelText(); }
    public void setLabelText(String labelText) { fx.setLabelText(labelText); }

    public Color getLiquidColorTop() { return fx.getLiquidColorTop(); }
    public void setLiquidColorTop(Color c) { fx.setLiquidColorTop(c); }

    public Color getLiquidColorBottom() { return fx.getLiquidColorBottom(); }
    public void setLiquidColorBottom(Color c) { fx.setLiquidColorBottom(c); }

    public Color getGlassBody() { return fx.getGlassBody(); }
    public void setGlassBody(Color c) { fx.setGlassBody(c); }

    public Color getGlassRim() { return fx.getGlassRim(); }
    public void setGlassRim(Color c) { fx.setGlassRim(c); }

    public Color getGlassHighlight() { return fx.getGlassHighlight(); }
    public void setGlassHighlight(Color c) { fx.setGlassHighlight(c); }

    public Color getHoverGlowColor() { return fx.getHoverGlowColor(); }
    public void setHoverGlowColor(Color c) { fx.setHoverGlowColor(c); }

    public Color getTextColor() { return fx.getTextColor(); }
    public void setTextColor(Color c) { fx.setTextColor(c); }

    public Color getPlaceholderColor() { return fx.getPlaceholderColor(); }
    public void setPlaceholderColor(Color c) { fx.setPlaceholderColor(c); }

    public Color getLabelColor() { return fx.getLabelColor(); }
    public void setLabelColor(Color c) { fx.setLabelColor(c); }

    public Color getRippleColor() { return fx.getRippleColor(); }
    public void setRippleColor(Color c) { fx.setRippleColor(c); }

    public int getArc() { return fx.getArc(); }
    public void setArc(int arc) { fx.setArc(arc); }

    public float getRimStrokeWidth() { return fx.getRimStrokeWidth(); }
    public void setRimStrokeWidth(float w) { fx.setRimStrokeWidth(w); }

    public int getHorizontalPadding() { return fx.getHorizontalPadding(); }
    public void setHorizontalPadding(int p) { fx.setHorizontalPadding(p); }

    public float getLabelFontScale() { return fx.getLabelFontScale(); }
    public void setLabelFontScale(float s) { fx.setLabelFontScale(s); }

    public boolean isAnimated() { return fx.isAnimated(); }
    public void setAnimated(boolean animated) { fx.setAnimated(animated); }

    public double getAnimationSpeed() { return fx.getAnimationSpeed(); }
    public void setAnimationSpeed(double s) { fx.setAnimationSpeed(s); }
}
