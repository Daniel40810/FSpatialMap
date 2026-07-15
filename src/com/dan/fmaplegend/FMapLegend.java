package com.dan.fmaplegend;

import com.dan.fstyle.FAnimationConfig;
import com.dan.fstyle.FColors;
import com.dan.fstyle.FFontRole;
import com.dan.fstyle.FTheme;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FMapLegend — interaktive Legende fuer Spatial-Layer im Laboratory-Dark-Look.
 *
 * Jede Zeile: Geometrie-Glyph (FLayerGeometryType implements FIcon), Layer-Name,
 * Farbbalken (eine Farbe oder Verlauf der distinkten Feature-Farben aus
 * ColorAdminDAO.loadLayerColorMap()) und ein glasierter Sichtbarkeits-Toggle.
 *
 * Hover zeigt ein On-Canvas-Detail-Popup (Feature-Anzahl, Bounds) — gemalt,
 * kein JWindow. Ausgeblendete Layer werden gedimmt. Ein 16ms-Timer treibt
 * Zeilen-Hover, Toggle-Knopf-Glide, Dimmen und Popup-Fade.
 *
 * Tastatur: Pfeil auf/ab waehlt Zeile, Leertaste/Enter schaltet um.
 */
public class FMapLegend extends JComponent {

    // ------------------------------------------------------------------
    // Zeilen-Animationszustand
    // ------------------------------------------------------------------

    private static final class RowState {
        double hover = 0.0;      // 0..1
        double toggle = 1.0;     // 0..1 (1 = sichtbar), animiert Knopf + Dimmen
        double popup = 0.0;      // 0..1 Popup-Fade
    }

    // ------------------------------------------------------------------
    // Model + Listener
    // ------------------------------------------------------------------

    private DefaultFMapLegendModel model = new DefaultFMapLegendModel();

    private final PropertyChangeListener modelListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("visibility".equals(evt.getPropertyName())) {
                ensureRunning();
                FLayerEntry e = (FLayerEntry) evt.getNewValue();
                fireVisibilityChanged(e);
            } else {
                syncRowStates();
                revalidate();
            }
            repaint();
        }
    };

    private final FTheme.ThemeListener themeListener = new FTheme.ThemeListener() {
        @Override
        public void themeChanged(FTheme theme) {
            repaint();
        }
    };

    // ------------------------------------------------------------------
    // Bean-Properties
    // ------------------------------------------------------------------

    private String title = "Layer";
    private boolean showTitle = true;
    private boolean showDetailsPopup = true;
    private boolean paintOwnFrame = true;    // Glasrahmen um die ganze Legende
    private int rowHeight = 36;
    private int iconSize = 22;
    private int colorBarWidth = 54;
    private boolean animated = true;

    // ------------------------------------------------------------------
    // Laufzeitzustand
    // ------------------------------------------------------------------

    private Timer timer;
    private final Map<String, RowState> rowStates = new HashMap<String, RowState>();
    private int hoverRow = -1;
    private int focusRow = -1;
    private int pressRow = -1;

    // ------------------------------------------------------------------
    // Konstruktion
    // ------------------------------------------------------------------

    public FMapLegend() {
        setOpaque(false);
        setFocusable(true);
        model.addPropertyChangeListener(modelListener);
        installTimer();
        installMouse();
        installKeyboard();
    }

    private void installTimer() {
        int interval = FTheme.getInstance().getAnimationConfig().timerIntervalMs;
        if (interval <= 0) {
            interval = 16;
        }
        timer = new Timer(interval, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tick();
            }
        });
        timer.setRepeats(true);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        FTheme.getInstance().addThemeListener(themeListener);
    }

    @Override
    public void removeNotify() {
        FTheme.getInstance().removeThemeListener(themeListener);
        if (timer != null) {
            timer.stop();
        }
        super.removeNotify();
    }

    // ------------------------------------------------------------------
    // Animation
    // ------------------------------------------------------------------

    private void tick() {
        FAnimationConfig anim = FTheme.getInstance().getAnimationConfig();
        boolean busy = false;
        List<FLayerEntry> entries = model.getEntries();

        for (int i = 0; i < entries.size(); i++) {
            FLayerEntry e = entries.get(i);
            RowState rs = stateFor(e);

            busy |= ease(rs, 'h', (i == hoverRow || i == focusRow) ? 1.0 : 0.0, anim);
            busy |= ease(rs, 't', e.isVisible() ? 1.0 : 0.0, anim);
            boolean popupOn = showDetailsPopup && i == hoverRow && hasDetails(e);
            busy |= ease(rs, 'p', popupOn ? 1.0 : 0.0, anim);
        }

        repaint();
        if (!busy) {
            timer.stop();
        }
    }

    private boolean ease(RowState rs, char field, double target, FAnimationConfig anim) {
        double current = (field == 'h') ? rs.hover : (field == 't') ? rs.toggle : rs.popup;
        double dist = target - current;
        if (Math.abs(dist) < anim.settleThreshold) {
            set(rs, field, target);
            return false;
        }
        if (!animated) {
            set(rs, field, target);
            return false;
        }
        double speed = (dist > 0) ? anim.hoverEaseIn : anim.hoverEaseOut;
        set(rs, field, current + dist * speed);
        return true;
    }

    private void set(RowState rs, char field, double v) {
        if (field == 'h') {
            rs.hover = v;
        } else if (field == 't') {
            rs.toggle = v;
        } else {
            rs.popup = v;
        }
    }

    private RowState stateFor(FLayerEntry e) {
        RowState rs = rowStates.get(e.getLayerName());
        if (rs == null) {
            rs = new RowState();
            rs.toggle = e.isVisible() ? 1.0 : 0.0;
            rowStates.put(e.getLayerName(), rs);
        }
        return rs;
    }

    private void syncRowStates() {
        // Verwaiste Zustaende entfernen
        List<FLayerEntry> entries = model.getEntries();
        Map<String, RowState> keep = new HashMap<String, RowState>();
        for (int i = 0; i < entries.size(); i++) {
            String key = entries.get(i).getLayerName();
            RowState rs = rowStates.get(key);
            keep.put(key, (rs != null) ? rs : new RowState());
        }
        rowStates.clear();
        rowStates.putAll(keep);
    }

    private boolean hasDetails(FLayerEntry e) {
        return e.getFeatureCount() >= 0 || e.getBoundsText() != null;
    }

    private void ensureRunning() {
        if (timer != null && !timer.isRunning()) {
            timer.start();
        }
    }

    // ------------------------------------------------------------------
    // Eingabe
    // ------------------------------------------------------------------

    private void installMouse() {
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int r = rowAt(e.getY());
                if (r != hoverRow) {
                    hoverRow = r;
                    ensureRunning();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverRow = -1;
                ensureRunning();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                pressRow = rowAt(e.getY());
                focusRow = pressRow;
                ensureRunning();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int r = rowAt(e.getY());
                if (r >= 0 && r == pressRow) {
                    toggleRow(r);
                }
                pressRow = -1;
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void installKeyboard() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int n = model.size();
                if (n == 0) {
                    return;
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                        focusRow = Math.min(n - 1, Math.max(0, focusRow + 1));
                        ensureRunning();
                        break;
                    case KeyEvent.VK_UP:
                        focusRow = Math.max(0, (focusRow < 0) ? 0 : focusRow - 1);
                        ensureRunning();
                        break;
                    case KeyEvent.VK_SPACE:
                    case KeyEvent.VK_ENTER:
                        if (focusRow >= 0) {
                            toggleRow(focusRow);
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void toggleRow(int row) {
        List<FLayerEntry> entries = model.getEntries();
        if (row < 0 || row >= entries.size()) {
            return;
        }
        FLayerEntry e = entries.get(row);
        model.setLayerVisible(e.getLayerName(), !e.isVisible());
    }

    private int rowAt(int y) {
        int top = headerHeight();
        int idx = (y - top) / rowHeight;
        return (y >= top && idx >= 0 && idx < model.size()) ? idx : -1;
    }

    private int headerHeight() {
        return showTitle ? 30 : 8;
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        int h = headerHeight() + model.size() * rowHeight + 10;
        return new Dimension(250, Math.max(60, h));
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            FTheme theme = FTheme.getInstance();
            int w = getWidth();
            int h = getHeight();

            if (paintOwnFrame) {
                paintFrame(g2, theme, w, h);
            }
            if (showTitle) {
                paintTitle(g2, theme, w);
            }

            List<FLayerEntry> entries = model.getEntries();
            for (int i = 0; i < entries.size(); i++) {
                paintRow(g2, theme, entries.get(i), i, w);
            }

            // Popup zuletzt (liegt ueber allem)
            for (int i = 0; i < entries.size(); i++) {
                RowState rs = stateFor(entries.get(i));
                if (rs.popup > 0.03) {
                    paintPopup(g2, theme, entries.get(i), i, rs.popup, w);
                }
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintFrame(Graphics2D g2, FTheme theme, int w, int h) {
        int arc = theme.getGeometryConfig().arc;
        RoundRectangle2D frame = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, arc, arc);
        // Schatten
        g2.setColor(FColors.withAlpha(theme.getShadow(), 60));
        g2.fill(new RoundRectangle2D.Float(3, 4, w - 3, h - 3, arc, arc));
        // Glasflaeche auf Surface
        g2.setColor(FColors.withAlpha(theme.getSurface(), 235));
        g2.fill(frame);
        g2.setColor(FColors.withAlpha(theme.getGlassBody(),
                (int) theme.getGlassFillAlpha()));
        g2.fill(frame);
        // Glanz oben
        Shape oldClip = g2.getClip();
        g2.clip(frame);
        g2.setPaint(new LinearGradientPaint(0, 1, 0, h * 0.25f,
                new float[]{0f, 1f},
                new Color[]{FColors.withAlpha(theme.getGlassHighlight(), 60),
                        FColors.withAlpha(theme.getGlassHighlight(), 0)}));
        g2.fillRect(1, 1, w - 3, (int) (h * 0.25f));
        g2.setClip(oldClip);
        // Rim
        g2.setStroke(new BasicStroke(theme.getGeometryConfig().rimStrokeWidth));
        g2.setColor(theme.getGlassRim());
        g2.draw(frame);
    }

    private void paintTitle(Graphics2D g2, FTheme theme, int w) {
        Font f = theme.getFont(FFontRole.TITLE);
        g2.setFont(f);
        g2.setColor(theme.getText());
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, 12, 8 + fm.getAscent());
        // Trennlinie
        g2.setColor(FColors.withAlpha(theme.getGlassRim(), 90));
        g2.drawLine(10, headerHeight() - 2, w - 10, headerHeight() - 2);
    }

    private void paintRow(Graphics2D g2, FTheme theme, FLayerEntry e, int row, int w) {
        RowState rs = stateFor(e);
        int y = headerHeight() + row * rowHeight;
        int cy = y + rowHeight / 2;

        // Dimm-Faktor: unsichtbare Layer 40 % Alpha
        float dim = (float) (0.40 + 0.60 * rs.toggle);

        // Hover-Tint der Zeile
        if (rs.hover > 0.02) {
            g2.setColor(FColors.withAlpha(theme.getPrimary(), (int) (26 * rs.hover)));
            g2.fill(new RoundRectangle2D.Float(6, y + 2, w - 12, rowHeight - 4, 10, 10));
        }
        // Fokus-Rahmen
        if (row == focusRow && isFocusOwner()) {
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(FColors.withAlpha(theme.getGlow(), 160));
            g2.draw(new RoundRectangle2D.Float(6, y + 2, w - 12, rowHeight - 4, 10, 10));
        }

        // 1) Geometrie-Glyph
        int ix = 12;
        int iy = cy - iconSize / 2;
        Graphics2D ig = (Graphics2D) g2.create(ix, iy, iconSize, iconSize);
        ig.setComposite(java.awt.AlphaComposite.getInstance(
                java.awt.AlphaComposite.SRC_OVER, dim));
        e.getGeometryType().paintGlyph(ig, iconSize);
        ig.dispose();

        // 2) Layer-Name
        Font f = theme.getFont(FFontRole.LABEL);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(FColors.withAlpha(theme.getText(), (int) (255 * dim)));
        int nameX = ix + iconSize + 10;
        int nameMaxW = w - nameX - colorBarWidth - 54 - 16;
        String name = ellipsize(e.getDisplayName(), fm, nameMaxW);
        g2.drawString(name, nameX, cy + (fm.getAscent() - fm.getDescent()) / 2);

        // 3) Farbbalken
        int barX = w - colorBarWidth - 54;
        int barH = 10;
        paintColorBar(g2, theme, e, barX, cy - barH / 2, colorBarWidth, barH, dim);

        // 4) Glas-Toggle
        paintToggle(g2, theme, rs, w - 46, cy - 9, 34, 18);
    }

    private void paintColorBar(Graphics2D g2, FTheme theme, FLayerEntry e,
                               int x, int y, int w, int h, float dim) {
        RoundRectangle2D bar = new RoundRectangle2D.Float(x, y, w, h, h, h);
        List<Color> colors = e.getBarColors();
        if (colors.isEmpty()) {
            g2.setColor(FColors.withAlpha(theme.getTextMuted(), (int) (60 * dim)));
            g2.fill(bar);
        } else if (colors.size() == 1) {
            g2.setColor(FColors.withAlpha(colors.get(0), (int) (255 * dim)));
            g2.fill(bar);
        } else {
            int n = Math.min(colors.size(), 12);   // Verlauf auf 12 Stops begrenzen
            float[] fr = new float[n];
            Color[] cs = new Color[n];
            for (int i = 0; i < n; i++) {
                fr[i] = (float) i / (n - 1);
                cs[i] = FColors.withAlpha(colors.get(i), (int) (255 * dim));
            }
            g2.setPaint(new LinearGradientPaint(x, y, x + w, y, fr, cs));
            g2.fill(bar);
        }
        // Mini-Glanz + Rim
        Shape oldClip = g2.getClip();
        g2.clip(bar);
        g2.setColor(FColors.withAlpha(theme.getGlassHighlight(), (int) (70 * dim)));
        g2.fillRect(x + 1, y + 1, w - 2, h / 2 - 1);
        g2.setClip(oldClip);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(FColors.withAlpha(theme.getGlassRim(), (int) (140 * dim)));
        g2.draw(bar);
    }

    private void paintToggle(Graphics2D g2, FTheme theme, RowState rs,
                             int x, int y, int w, int h) {
        RoundRectangle2D track = new RoundRectangle2D.Float(x, y, w, h, h, h);
        // Track: interpoliert von Muted zu Primary
        Color trackCol = FColors.blend(
                FColors.withAlpha(theme.getTextMuted(), 90),
                FColors.withAlpha(theme.getPrimary(), 170),
                (float) rs.toggle);
        g2.setColor(trackCol);
        g2.fill(track);
        // Glaskoerper + Rim
        g2.setColor(FColors.withAlpha(theme.getGlassBody(), 70));
        g2.fill(track);
        g2.setStroke(new BasicStroke(1.2f));
        g2.setColor(FColors.withAlpha(theme.getGlassRim(), 160));
        g2.draw(track);

        // Knopf gleitet mit rs.toggle
        float knobR = h / 2f - 2f;
        float knobX = (float) (x + 2 + knobR + rs.toggle * (w - 4 - 2 * knobR));
        float knobY = y + h / 2f;
        Ellipse2D knob = new Ellipse2D.Float(knobX - knobR, knobY - knobR, 2 * knobR, 2 * knobR);
        g2.setPaint(new LinearGradientPaint(
                knobX, knobY - knobR, knobX, knobY + knobR,
                new float[]{0f, 1f},
                new Color[]{FColors.withAlpha(theme.getGlassHighlight(), 230),
                        FColors.withAlpha(theme.getGlassBody(), 200)}));
        g2.fill(knob);
        g2.setColor(theme.getGlassRim());
        g2.draw(knob);
        // Glanzpunkt
        g2.setColor(FColors.withAlpha(theme.getGlassHighlight(), 200));
        g2.fill(new Ellipse2D.Float(knobX - knobR * 0.45f, knobY - knobR * 0.65f,
                knobR * 0.7f, knobR * 0.5f));
    }

    private void paintPopup(Graphics2D g2, FTheme theme, FLayerEntry e,
                            int row, double alpha, int w) {
        Font f = theme.getFont(FFontRole.CAPTION);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();

        String line1 = e.getGeometryType().getLabel()
                + ((e.getFeatureCount() >= 0) ? "  ·  " + e.getFeatureCount() + " Features" : "");
        String line2 = e.getBoundsText();

        int tw = fm.stringWidth(line1);
        if (line2 != null) {
            tw = Math.max(tw, fm.stringWidth(line2));
        }
        int lines = (line2 != null) ? 2 : 1;
        int padX = 9, padY = 6;
        int pw = tw + 2 * padX;
        int ph = lines * fm.getHeight() + 2 * padY;

        int y = headerHeight() + row * rowHeight;
        float px = w - pw - 12;
        float py = y + rowHeight - 2;
        // Unten einklemmen: notfalls oberhalb der Zeile
        if (py + ph > getHeight() - 4) {
            py = y - ph + 2;
        }
        px = Math.max(4, px);
        py = Math.max(4, py);

        int a = (int) (255 * alpha);
        RoundRectangle2D box = new RoundRectangle2D.Float(px, py, pw, ph, 12, 12);
        // Schatten, Glas, Rim
        g2.setColor(FColors.withAlpha(theme.getShadow(), (int) (90 * alpha)));
        g2.fill(new RoundRectangle2D.Float(px + 2, py + 3, pw, ph, 12, 12));
        g2.setColor(FColors.withAlpha(theme.getSurface(), (int) (245 * alpha)));
        g2.fill(box);
        g2.setColor(FColors.withAlpha(theme.getGlassBody(), (int) (80 * alpha)));
        g2.fill(box);
        g2.setStroke(new BasicStroke(1.2f));
        g2.setColor(FColors.withAlpha(theme.getGlassRim(), (int) (200 * alpha)));
        g2.draw(box);

        g2.setColor(FColors.withAlpha(theme.getText(), a));
        g2.drawString(line1, px + padX, py + padY + fm.getAscent());
        if (line2 != null) {
            g2.setColor(FColors.withAlpha(theme.getTextMuted(), a));
            g2.drawString(line2, px + padX, py + padY + fm.getHeight() + fm.getAscent());
        }
    }

    private static String ellipsize(String s, FontMetrics fm, int maxW) {
        if (fm.stringWidth(s) <= maxW || s.length() < 2) {
            return s;
        }
        String ell = "…";
        int i = s.length() - 1;
        while (i > 1 && fm.stringWidth(s.substring(0, i) + ell) > maxW) {
            i--;
        }
        return s.substring(0, i) + ell;
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    public DefaultFMapLegendModel getModel() {
        return model;
    }

    public void setModel(DefaultFMapLegendModel newModel) {
        if (newModel == null) {
            throw new IllegalArgumentException("model darf nicht null sein");
        }
        model.removePropertyChangeListener(modelListener);
        model = newModel;
        model.addPropertyChangeListener(modelListener);
        rowStates.clear();
        syncRowStates();
        revalidate();
        repaint();
    }

    public void addMapLegendListener(FMapLegendListener l) {
        listenerList.add(FMapLegendListener.class, l);
    }

    public void removeMapLegendListener(FMapLegendListener l) {
        listenerList.remove(FMapLegendListener.class, l);
    }

    private void fireVisibilityChanged(FLayerEntry e) {
        Object[] ls = listenerList.getListenerList();
        for (int i = ls.length - 2; i >= 0; i -= 2) {
            if (ls[i] == FMapLegendListener.class) {
                ((FMapLegendListener) ls[i + 1]).layerVisibilityChanged(e);
            }
        }
    }

    // ------------------------------------------------------------------
    // Bean-Getter/Setter
    // ------------------------------------------------------------------

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = (title != null) ? title : "";
        repaint();
    }

    public boolean isShowTitle() {
        return showTitle;
    }

    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
        revalidate();
        repaint();
    }

    public boolean isShowDetailsPopup() {
        return showDetailsPopup;
    }

    public void setShowDetailsPopup(boolean showDetailsPopup) {
        this.showDetailsPopup = showDetailsPopup;
        repaint();
    }

    public boolean isPaintOwnFrame() {
        return paintOwnFrame;
    }

    public void setPaintOwnFrame(boolean paintOwnFrame) {
        this.paintOwnFrame = paintOwnFrame;
        repaint();
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public void setRowHeight(int rowHeight) {
        this.rowHeight = Math.max(24, rowHeight);
        revalidate();
        repaint();
    }

    public int getIconSize() {
        return iconSize;
    }

    public void setIconSize(int iconSize) {
        this.iconSize = Math.max(12, iconSize);
        repaint();
    }

    public int getColorBarWidth() {
        return colorBarWidth;
    }

    public void setColorBarWidth(int colorBarWidth) {
        this.colorBarWidth = Math.max(20, colorBarWidth);
        repaint();
    }

    public boolean isAnimated() {
        return animated;
    }

    public void setAnimated(boolean animated) {
        this.animated = animated;
        ensureRunning();
        repaint();
    }
}
