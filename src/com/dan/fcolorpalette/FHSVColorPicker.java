package com.dan.fcolorpalette;

import com.dan.fslider.FSlider;
import com.dan.fstyle.FColors;
import com.dan.fstyle.FFontRole;
import com.dan.fstyle.FTheme;
import com.dan.ftextfield.FTextfield;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Farbwahl-Panel: drei {@link FSlider} fuer Farbton/Saettigung/Helligkeit,
 * ein Live-Swatch mit Glas-Rand und ein Hex-Textfeld. Feuert
 * {@code "color"}-PropertyChange-Events bei jeder Aenderung.
 * <p>
 * Reine Anzeige-/Eingabekomponente ohne DB-Anbindung — der Aufrufer entscheidet,
 * was mit der gewaehlten {@link #getColor()} passiert (z. B. Model.createColor).
 */
public class FHSVColorPicker extends JPanel {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final FSlider hueSlider = new FSlider(0, 360, 0);
    private final FSlider satSlider = new FSlider(0, 100, 100);
    private final FSlider briSlider = new FSlider(0, 100, 100);
    private final FTextfield hexField = new FTextfield();
    private final SwatchPreview swatch = new SwatchPreview();

    private Color color = Color.WHITE;
    private boolean updatingFromColor;

    public FHSVColorPicker() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));

        swatch.setPreferredSize(new Dimension(200, 70));
        add(swatch, BorderLayout.NORTH);

        JPanel sliders = new JPanel();
        sliders.setOpaque(false);
        sliders.setLayout(new BoxLayout(sliders, BoxLayout.Y_AXIS));
        sliders.add(labeled("Farbton", hueSlider));
        sliders.add(labeled("Saettigung", satSlider));
        sliders.add(labeled("Helligkeit", briSlider));
        add(sliders, BorderLayout.CENTER);

        JPanel hexRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        hexRow.setOpaque(false);
        hexField.setLabelText("Hex");
        hexField.setColumns(9);
        hexField.setText(FColors.toHex(color));
        hexRow.add(hexField);
        add(hexRow, BorderLayout.SOUTH);

        hueSlider.addChangeListener(e -> onSliderChanged());
        satSlider.addChangeListener(e -> onSliderChanged());
        briSlider.addChangeListener(e -> onSliderChanged());

        hexField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyHexInput(); }
            @Override public void removeUpdate(DocumentEvent e) { applyHexInput(); }
            @Override public void changedUpdate(DocumentEvent e) { applyHexInput(); }
        });

        setColor(Color.WHITE);
    }

    private JComponent labeled(String text, FSlider slider) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        com.dan.flabel.FLabel label = new com.dan.flabel.FLabel(text);
        label.setPreferredSize(new Dimension(78, label.getPreferredSize().height));
        row.add(label, BorderLayout.WEST);
        row.add(slider, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, slider.getPreferredSize().height + 6));
        return row;
    }

    private void onSliderChanged() {
        if (updatingFromColor) {
            return;
        }
        float h = hueSlider.getValue() / 360f;
        float s = satSlider.getValue() / 100f;
        float b = briSlider.getValue() / 100f;
        Color c = Color.getHSBColor(h, s, b);
        applyColor(c, true);
    }

    private void applyHexInput() {
        if (updatingFromColor) {
            return;
        }
        String text = hexField.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        try {
            Color c = FColors.hex(text.trim());
            applyColor(c, false);
        } catch (Exception ignored) {
            // Eingabe (noch) kein gueltiges Hex — ignorieren, bis Nutzer fertig tippt
        }
    }

    private void applyColor(Color c, boolean fromSliders) {
        Color old = this.color;
        this.color = c;
        updatingFromColor = true;
        try {
            if (!fromSliders) {
                float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
                hueSlider.setValue(Math.round(hsb[0] * 360f));
                satSlider.setValue(Math.round(hsb[1] * 100f));
                briSlider.setValue(Math.round(hsb[2] * 100f));
            }
            hexField.setText(FColors.toHex(c).substring(0, 7)); // #RRGGBB, ohne Alpha
        } finally {
            updatingFromColor = false;
        }
        swatch.repaint();
        pcs.firePropertyChange("color", old, c);
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    public Color getColor() {
        return color;
    }

    public void setColor(Color c) {
        applyColor(c, false);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
        pcs.addPropertyChangeListener(propertyName, l);
    }

    // ------------------------------------------------------------------
    // Swatch-Vorschau (Glas-Look)
    // ------------------------------------------------------------------

    private final class SwatchPreview extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FTheme theme = FTheme.getInstance();
            int w = getWidth(), h = getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 16, 16);
            g2.setColor(color);
            g2.fill(shape);
            g2.setStroke(new java.awt.BasicStroke(1.6f));
            g2.setColor(theme.getGlassRim());
            g2.draw(shape);
            // Glanzstreifen links
            java.awt.Shape oldClip = g2.getClip();
            g2.clip(shape);
            g2.setColor(FColors.withAlpha(Color.WHITE, 60));
            g2.fillRect(2, 2, (w - 4) / 4, h - 4);
            g2.setClip(oldClip);
            g2.dispose();
        }
    }
}
