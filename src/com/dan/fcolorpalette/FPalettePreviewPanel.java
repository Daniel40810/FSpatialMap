package com.dan.fcolorpalette;

import com.dan.fdal.color.ColorDTO;
import com.dan.fdal.color.PaletteDTO;
import com.dan.fslider.FSlider;
import com.dan.fstyle.FColors;
import com.dan.fstyle.FFontRole;
import com.dan.fstyle.FTheme;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rendert die aktuell im Model geordnete Palette in drei Modi
 * ({@link FPreviewMode}): linearer Verlauf, interaktive Heatmap (Schieberegler
 * -&gt; interpolierte Farbe via {@link PaletteDTO#colorAt(double)}) und
 * Balkendiagramm-Demo. Reines Anzeige-Panel, liest nur die uebergebene Farbliste.
 */
public class FPalettePreviewPanel extends JPanel {

    private FPreviewMode mode = FPreviewMode.GRADIENT;
    private List<ColorDTO> colors = Collections.emptyList();
    private final Canvas canvas = new Canvas();
    private final FSlider heatSlider = new FSlider(0, 100, 50);

    public FPalettePreviewPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 6));
        add(canvas, BorderLayout.CENTER);
        heatSlider.addChangeListener(e -> canvas.repaint());
        add(heatSlider, BorderLayout.SOUTH);
        heatSlider.setVisible(false);
    }

    public void setMode(FPreviewMode mode) {
        this.mode = mode;
        heatSlider.setVisible(mode == FPreviewMode.HEATMAP);
        canvas.repaint();
    }

    public FPreviewMode getMode() {
        return mode;
    }

    public void setColors(List<ColorDTO> colors) {
        this.colors = colors == null ? Collections.emptyList() : colors;
        canvas.repaint();
    }

    private final class Canvas extends JComponent {
        Canvas() {
            setPreferredSize(new java.awt.Dimension(220, 160));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FTheme theme = FTheme.getInstance();
            int w = getWidth(), h = getHeight();

            g2.setColor(FColors.withAlpha(theme.getSurface(), 160));
            g2.fillRoundRect(0, 0, w, h, 14, 14);

            if (colors.isEmpty()) {
                g2.setFont(theme.getFont(FFontRole.BODY));
                g2.setColor(theme.getTextMuted());
                g2.drawString("Keine Palette ausgewaehlt", 14, h / 2);
                g2.dispose();
                return;
            }

            switch (mode) {
                case GRADIENT:
                    paintGradient(g2, w, h);
                    break;
                case HEATMAP:
                    paintHeatmap(g2, w, h);
                    break;
                case BARS:
                    paintBars(g2, w, h);
                    break;
            }
            g2.dispose();
        }

        private void paintGradient(Graphics2D g2, int w, int h) {
            int pad = 12;
            int gw = w - 2 * pad, gh = h - 2 * pad;
            if (colors.size() == 1) {
                g2.setColor(colors.get(0).toColor());
                g2.fillRoundRect(pad, pad, gw, gh, 10, 10);
                return;
            }
            int band = gw / (colors.size() - 1);
            for (int i = 0; i < colors.size() - 1; i++) {
                GradientPaint gp = new GradientPaint(
                        pad + i * band, 0, colors.get(i).toColor(),
                        pad + (i + 1) * band, 0, colors.get(i + 1).toColor());
                g2.setPaint(gp);
                int segW = (i == colors.size() - 2) ? (gw - i * band) : band;
                g2.fillRect(pad + i * band, pad, segW + 1, gh);
            }
            g2.setColor(FTheme.getInstance().getGlassRim());
            g2.setStroke(new java.awt.BasicStroke(1.4f));
            g2.drawRoundRect(pad, pad, gw, gh, 10, 10);
        }

        private void paintHeatmap(Graphics2D g2, int w, int h) {
            PaletteDTO tmp = new PaletteDTO(0, "preview", null, colors);
            double t = heatSlider.getValue() / 100.0;
            ColorDTO c = tmp.colorAt(t);
            int pad = 12;
            g2.setColor(c.toColor());
            g2.fillRoundRect(pad, pad, w - 2 * pad, h - 2 * pad - 20, 12, 12);
            g2.setColor(FTheme.getInstance().getText());
            g2.setFont(FTheme.getInstance().getFont(FFontRole.CAPTION));
            g2.drawString(String.format("t = %.2f  →  %s", t, c.getColorName()), pad, h - pad);
        }

        private void paintBars(Graphics2D g2, int w, int h) {
            int pad = 12;
            int n = Math.min(colors.size(), 8);
            int gap = 6;
            int barW = (w - 2 * pad - (n - 1) * gap) / n;
            int baseY = h - pad;
            int maxBarH = h - 2 * pad;
            for (int i = 0; i < n; i++) {
                int barH = (int) (maxBarH * (0.35 + 0.65 * ((i + 1.0) / n)));
                int x = pad + i * (barW + gap);
                g2.setColor(colors.get(i).toColor());
                g2.fillRoundRect(x, baseY - barH, barW, barH, 6, 6);
            }
        }
    }
}
