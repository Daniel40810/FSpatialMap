package com.dan.fcombobox;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

/**
 * UI-Delegate fuer {@link FComboBox}. Zeichnet den Glaskoerper im
 * ReagenzglasBar-Stil, den animierten Chevron-Pfeil sowie ein dunkles,
 * abgerundetes Popup mit tuerkis hinterlegter Auswahl.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FComboBoxUI extends BasicComboBoxUI {

    /** Pflicht-Factory fuer {@code UIManager}-Instanziierung. */
    public static ComponentUI createUI(JComponent c) {
        return new FComboBoxUI();
    }

    private FComboBox combo() {
        return (FComboBox) comboBox;
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        // Innenabstand, damit Text nicht am Rim klebt; rechts Platz fuer Pfeil.
        FComboBox c = combo();
        int pad = (c != null) ? c.horizontalPadding : 12;
        comboBox.setBorder(BorderFactory.createEmptyBorder(6, pad, 6, 30));
        comboBox.setOpaque(false);
    }

    @Override
    protected ListCellRenderer createRenderer() {
        return new FCellRenderer();
    }

    @Override
    protected JButton createArrowButton() {
        ChevronButton b = new ChevronButton();
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusable(false);
        return b;
    }

    @Override
    protected ComboPopup createPopup() {
        return new FComboPopup(comboBox);
    }

    // --------------------------------------------------------------- Painting

    @Override
    public void update(Graphics g, JComponent c) {
        // Kein Standard-Hintergrund-Fill: alles selbst zeichnen.
        paint(g, c);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        FComboBox box = combo();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            int w = c.getWidth();
            int h = c.getHeight();
            int arc = box.arc;
            // Solider, deckender Hintergrund – verhindert Durchscheinen.
            g2.setColor(box.glassBody.getAlpha() < 255
                    ? new Color(box.glassBody.getRed(), box.glassBody.getGreen(),
                                box.glassBody.getBlue(), 255)
                    : box.glassBody);
            g2.fill(new RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, arc, arc));
            paintGlass(g2, box);
        } finally {
            g2.dispose();
        }
        if (!comboBox.isEditable()) {
            Rectangle r = rectangleForCurrentValue();
            paintCurrentValue(g, r, false);
        }
    }

    private void paintGlass(Graphics2D g2, FComboBox box) {
        int w = comboBox.getWidth();
        int h = comboBox.getHeight();
        int arc = box.arc;
        double hover = box.getHoverProgress();
        double open = box.getOpenProgress();
        double lift = Math.max(hover, open); // beides hebt die Box optisch an

        // 1) Weicher Glow nach aussen (nur bei Hover/Offen sichtbar).
        if (lift > 0.01) {
            int layers = 5;
            for (int i = layers; i >= 1; i--) {
                float frac = (float) i / layers;
                int alpha = (int) (70 * lift * (1f - frac) );
                if (alpha <= 0) continue;
                g2.setColor(withAlpha(box.hoverGlowColor, alpha));
                float spread = 2f + frac * 6f;
                g2.fill(new RoundRectangle2D.Float(
                        -spread, -spread,
                        w + spread * 2f, h + spread * 2f,
                        arc + spread, arc + spread));
            }
        }

        Shape body = new RoundRectangle2D.Float(
                1f, 1f, w - 2f, h - 2f, arc, arc);

        // 2) Glasfuellung (leicht heller bei Hover).
        int bodyBoost = (int) (18 * lift);
        g2.setColor(brighten(box.glassBody, bodyBoost));
        g2.fill(body);

        // 3) Linker Glanzstreifen (vertikaler weisser Verlauf).
        Shape oldClip = g2.getClip();
        g2.clip(body);
        int stripeW = Math.max(6, (int) (w * 0.18));
        g2.setPaint(new GradientPaint(
                0, 0, withAlpha(box.glassHighlight, alphaOf(box.glassHighlight)),
                stripeW, 0, withAlpha(box.glassHighlight, 0)));
        g2.fillRect(2, 2, stripeW, h - 4);

        // 4) Liquid-Akzentlinie unten (Weinrot->Tuerkis), waechst bei Hover.
        float baseLine = 2.4f;
        float lineH = (float) (baseLine + 2.6 * lift);
        float lineY = h - lineH - 2.5f;
        g2.setPaint(new GradientPaint(
                0, lineY, box.liquidColorBottom,
                w, lineY, box.liquidColorTop));
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, (float) (0.55 + 0.45 * lift)));
        g2.fill(new RoundRectangle2D.Float(
                6f, lineY, w - 12f, lineH, lineH, lineH));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setClip(oldClip);

        // 5) Glasrand (heller bei Hover, Richtung Tuerkis).
        Color rim = blend(box.glassRim, box.liquidColorTop, (float) (0.55 * lift));
        rim = brighten(rim, (int) (40 * lift));
        g2.setColor(rim);
        g2.setStroke(new BasicStroke(box.rimStrokeWidth + (float) (0.6 * lift)));
        g2.draw(new RoundRectangle2D.Float(
                1f, 1f, w - 2.5f, h - 2.5f, arc, arc));
    }

    // ----------------------------------------------------------- Chevron-Pfeil

    /** Transparenter Drop-Down-Button, der einen animierten Chevron malt. */
    private final class ChevronButton extends JButton {

        ChevronButton() {
            // Hover an die Combo weiterreichen: der Button verschluckt
            // sonst die Enter/Exit-Events der Combo.
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { combo().setHoveredState(true); }
                @Override public void mouseExited(java.awt.event.MouseEvent e)  { combo().setHoveredState(false); }
            });
        }

        @Override
        public void paintComponent(Graphics g) {
            FComboBox box = combo();
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int cx = w / 2;
                int cy = h / 2;
                double open = box.getOpenProgress();
                double hover = box.getHoverProgress();

                // Chevron-Geometrie; dreht sich beim Oeffnen um 180 Grad.
                double size = 5.0;
                double rot = Math.PI * open; // 0 -> nach unten, PI -> nach oben
                double[][] pts = {{-size, -size * 0.55}, {0, size * 0.55}, {size, -size * 0.55}};
                GeneralPath path = new GeneralPath();
                for (int i = 0; i < pts.length; i++) {
                    double rx = pts[i][0] * Math.cos(rot) - pts[i][1] * Math.sin(rot);
                    double ry = pts[i][0] * Math.sin(rot) + pts[i][1] * Math.cos(rot);
                    if (i == 0) path.moveTo(cx + rx, cy + ry);
                    else path.lineTo(cx + rx, cy + ry);
                }

                Color col = brighten(box.arrowColor, (int) (35 * hover));
                g2.setColor(col);
                g2.setStroke(new BasicStroke(2.2f + (float) (0.5 * hover),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(path);
            } finally {
                g2.dispose();
            }
        }
    }

    // -------------------------------------------------------------- Renderer

    /** Renderer fuer Popup-Eintraege: dunkel, tuerkis Auswahl, weisser Text. */
    private final class FCellRenderer extends javax.swing.JLabel implements ListCellRenderer {
        private boolean sel;

        FCellRenderer() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            FComboBox box = combo();
            setText(value == null ? "" : value.toString());
            setFont(box.getFont());
            this.sel = isSelected;
            setForeground(box.textColor);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            FComboBox box = combo();
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                // Solider Hintergrund pro Zelle, damit nichts durchscheint.
                g2.setColor(box.popupBackground);
                g2.fillRect(0, 0, w, h);
                if (sel) {
                    g2.setPaint(new GradientPaint(
                            0, 0, withAlpha(box.liquidColorTop, 70),
                            w, 0, withAlpha(box.liquidColorBottom, 70)));
                    g2.fill(new RoundRectangle2D.Float(3, 2, w - 6, h - 4, 10, 10));
                    g2.setColor(withAlpha(box.liquidColorTop, 200));
                    g2.fillRoundRect(3, 2, 4, h - 4, 4, 4);
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    // ----------------------------------------------------------------- Popup

    /** Dunkles, abgerundetes Popup mit Glas-Hintergrund. */
    private final class FComboPopup extends BasicComboPopup {
        FComboPopup(JComboBox c) {
            super(c);
        }

        @Override
        protected void configureList() {
            super.configureList();
            FComboBox box = combo();
            list.setBackground(box.popupBackground);
            list.setForeground(box.textColor);
            list.setSelectionBackground(box.popupBackground);
            list.setSelectionForeground(box.textColor);
            list.setOpaque(true);
        }

        @Override
        protected void configurePopup() {
            super.configurePopup();
            FComboBox box = combo();
            setOpaque(true);
            setBackground(box.popupBackground);
            setBorder(BorderFactory.createLineBorder(box.glassRim, 1, true));
        }

        @Override
        protected JScrollPane createScroller() {
            JScrollPane sp = super.createScroller();
            FComboBox box = combo();
            // Eigene FScrollBar mit der Farbpalette der Box.
            FScrollBar vbar = new FScrollBar(FScrollBar.VERTICAL);
            vbar.setThumbColorTop(box.liquidColorTop);
            vbar.setThumbColorBottom(box.liquidColorBottom);
            vbar.setHoverGlowColor(box.hoverGlowColor);
            sp.setVerticalScrollBar(vbar);
            sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            return sp;
        }

        @Override
        protected void configureScroller() {
            super.configureScroller();
            scroller.setOpaque(false);
            scroller.getViewport().setOpaque(true);
            scroller.getViewport().setBackground(combo().popupBackground);
            scroller.setBorder(BorderFactory.createEmptyBorder());
        }
    }

    // -------------------------------------------------------------- Farb-Utils

    private static int alphaOf(Color c) { return c.getAlpha(); }

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

    private static Color blend(Color a, Color b, float t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return new Color(
                (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    private static int clampByte(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }
}
