package com.dan.faccordion;

import com.dan.fstyle.FColors;
import com.dan.fstyle.FFontRole;
import com.dan.fstyle.FTheme;
import com.dan.ficons.FIconPaint;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Ausklappbare Sections mit Liquid-Fold-Animation im ReagenzglasBar-Stil.
 *
 * <p>Jede Section besteht aus einem Glas-Header (Titel + rotierendes
 * Chevron) und einem beliebigen Inhalts-{@link java.awt.Component}. Beim
 * Auf-/Zuklappen waechst bzw. schrumpft die enthuellte Hoehe ueber ein
 * Easing (16-ms-Timer); an der wandernden Kante liegt eine kurzlebige
 * Fluessigkeits-Welle (Doppel-Sinus, siehe ReagenzglasBar-Doku) mit
 * Tuerkis-&gt;Weinrot-Gradient — der Eindruck: der Inhalt "fliesst" beim
 * Erscheinen in die Section.</p>
 *
 * <p>Rendert ausschliesslich aus {@link FAccordionModel}; der Auf/Zu-Status
 * ist reiner Komponenten-Zustand (kein Model-Feld).</p>
 */
public class FAccordion extends JComponent {

    private static final float SETTLE_THRESHOLD = 0.003f;
    private static final float WAVE_BAND_PX = 16f;

    private FAccordionModel model;
    private final PropertyChangeHandler modelListener = new PropertyChangeHandler();

    private Component[] sectionContent = new Component[0];
    private Rectangle[] headerBounds = new Rectangle[0];
    private int[] contentPrefHeight = new int[0];

    private float[] progress = new float[0];
    private float[] target = new float[0];
    private float[] waveOffset = new float[0];
    private float[] waveAmp = new float[0];
    private float[] hoverProgress = new float[0];
    private float[] hoverTarget = new float[0];

    private final Timer timer = new Timer(16, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            tick();
        }
    });

    private final List<FAccordionSectionListener> sectionListeners = new ArrayList<FAccordionSectionListener>();

    private boolean multipleOpen = true;
    private boolean collapsible = true;
    private double animationSpeed = 0.16;
    private int headerHeight = 40;
    private int sectionSpacing = 7;
    private int arc = 14;
    private int headerPadX = 14;
    private float waveAmplitude = 6f;
    private FChevronPosition chevronPosition = FChevronPosition.RIGHT;

    private Color liquidColorTop;
    private Color liquidColorBottom;
    private Color headerBackground;
    private Color textColor;

    private boolean userLiquidColorTop;
    private boolean userLiquidColorBottom;
    private boolean userHeaderBackground;
    private boolean userTextColor;

    private int lastPreferredHeight = -1;

    private final FTheme.ThemeListener themeListener = new FTheme.ThemeListener() {
        @Override
        public void themeChanged(FTheme theme) {
            applyThemeDefaults();
        }
    };

    public FAccordion() {
        setOpaque(false);
        setLayout(null);
        setFocusable(false);

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = headerIndexAt(e.getPoint());
                if (idx >= 0) {
                    toggleSection(idx);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateHover(headerIndexAt(e.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateHover(-1);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        applyThemeDefaults();
        setModel(new DefaultFAccordionModel());
        setPreferredSize(new Dimension(360, 200));
    }

    // ------------------------------------------------------------------
    // Theme-Bindung
    // ------------------------------------------------------------------

    @Override
    public void addNotify() {
        super.addNotify();
        FTheme.getInstance().addThemeListener(themeListener);
        applyThemeDefaults();
    }

    @Override
    public void removeNotify() {
        FTheme.getInstance().removeThemeListener(themeListener);
        super.removeNotify();
    }

    private void applyThemeDefaults() {
        FTheme theme = FTheme.getInstance();
        if (!userLiquidColorTop) {
            liquidColorTop = theme.getPrimary();
        }
        if (!userLiquidColorBottom) {
            liquidColorBottom = theme.getAccent();
        }
        if (!userHeaderBackground) {
            headerBackground = theme.getSurface();
        }
        if (!userTextColor) {
            textColor = theme.getText();
        }
        repaint();
    }

    // ------------------------------------------------------------------
    // Model
    // ------------------------------------------------------------------

    public FAccordionModel getModel() {
        return model;
    }

    public void setModel(FAccordionModel model) {
        FAccordionModel old = this.model;
        if (old != null) {
            old.removePropertyChangeListener(modelListener);
        }
        this.model = model;
        if (model != null) {
            model.addPropertyChangeListener(modelListener);
        }
        rebuildFromModel();
        firePropertyChange("model", old, model);
    }

    private final class PropertyChangeHandler implements java.beans.PropertyChangeListener {
        @Override
        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            rebuildFromModel();
        }
    }

    private void rebuildFromModel() {
        removeAll();
        int n = (model == null) ? 0 : model.getSectionCount();

        sectionContent = new Component[n];
        headerBounds = new Rectangle[n];
        contentPrefHeight = new int[n];
        progress = new float[n];
        target = new float[n];
        waveOffset = new float[n];
        waveAmp = new float[n];
        hoverProgress = new float[n];
        hoverTarget = new float[n];

        for (int i = 0; i < n; i++) {
            Component c = model.getContentAt(i);
            sectionContent[i] = c;
            add(c);
            headerBounds[i] = new Rectangle();
        }
        lastPreferredHeight = -1;
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------------
    // Section-Zustand (Live, nicht im Model)
    // ------------------------------------------------------------------

    private void checkIndex(int index) {
        if (model == null || index < 0 || index >= model.getSectionCount()) {
            throw new IndexOutOfBoundsException("Section-Index: " + index);
        }
    }

    public int getSectionCount() {
        return model == null ? 0 : model.getSectionCount();
    }

    /**
     * Kleinster konsistenter Section-Count zwischen Model und den intern
     * angelegten Animations-Arrays. Schuetzt Layout/Paint gegen einen kurzen
     * Zwischenzustand, falls Model-Aenderungen ausnahmsweise nicht auf dem
     * EDT erfolgen (Swing-Konvention: Model-Mutationen gehoeren auf den EDT).
     */
    private int activeSectionCount() {
        return Math.min(getSectionCount(), sectionContent.length);
    }

    public boolean isSectionOpen(int index) {
        checkIndex(index);
        return target[index] == 1f;
    }

    public int[] getOpenSections() {
        List<Integer> open = new ArrayList<Integer>();
        for (int i = 0; i < target.length; i++) {
            if (target[i] == 1f) {
                open.add(Integer.valueOf(i));
            }
        }
        int[] result = new int[open.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = open.get(i).intValue();
        }
        return result;
    }

    public void setOpenSections(int... indices) {
        for (int i = 0; i < target.length; i++) {
            if (target[i] == 1f) {
                target[i] = 0f;
                fireClosing(i);
            }
        }
        if (indices != null) {
            for (int idx : indices) {
                checkIndex(idx);
                target[idx] = 1f;
                fireOpening(idx);
                if (!multipleOpen) {
                    break;
                }
            }
        }
        ensureRunning();
    }

    public void toggleSection(int index) {
        if (isSectionOpen(index)) {
            closeSection(index);
        } else {
            openSection(index);
        }
    }

    public void openSection(int index) {
        checkIndex(index);
        if (target[index] == 1f) {
            return;
        }
        if (!multipleOpen) {
            for (int i = 0; i < target.length; i++) {
                if (i != index && target[i] == 1f) {
                    target[i] = 0f;
                    fireClosing(i);
                }
            }
        }
        target[index] = 1f;
        fireOpening(index);
        ensureRunning();
    }

    public void closeSection(int index) {
        checkIndex(index);
        if (target[index] == 0f) {
            return;
        }
        if (!collapsible) {
            int openCount = 0;
            for (float t : target) {
                if (t == 1f) {
                    openCount++;
                }
            }
            if (openCount <= 1) {
                return;
            }
        }
        target[index] = 0f;
        fireClosing(index);
        ensureRunning();
    }

    // ------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------

    public void addSectionListener(FAccordionSectionListener l) {
        sectionListeners.add(l);
    }

    public void removeSectionListener(FAccordionSectionListener l) {
        sectionListeners.remove(l);
    }

    private void fireOpening(int i) {
        for (FAccordionSectionListener l : sectionListeners) {
            l.sectionOpening(i);
        }
    }

    private void fireOpened(int i) {
        for (FAccordionSectionListener l : sectionListeners) {
            l.sectionOpened(i);
        }
    }

    private void fireClosing(int i) {
        for (FAccordionSectionListener l : sectionListeners) {
            l.sectionClosing(i);
        }
    }

    private void fireClosed(int i) {
        for (FAccordionSectionListener l : sectionListeners) {
            l.sectionClosed(i);
        }
    }

    // ------------------------------------------------------------------
    // Animation
    // ------------------------------------------------------------------

    private void ensureRunning() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    private void tick() {
        boolean animating = false;
        int n = progress.length;
        float speed = (float) animationSpeed;

        for (int i = 0; i < n; i++) {
            float before = progress[i];
            float diff = target[i] - progress[i];
            if (Math.abs(diff) > SETTLE_THRESHOLD) {
                progress[i] += diff * speed;
                waveOffset[i] += 0.20f;
                waveAmp[i] = Math.min(waveAmplitude, waveAmp[i] + 0.6f);
                animating = true;
            } else if (progress[i] != target[i]) {
                progress[i] = target[i];
            } else if (waveAmp[i] > 0.05f) {
                waveAmp[i] *= 0.90f;
                waveOffset[i] += 0.20f;
                animating = true;
            } else {
                waveAmp[i] = 0f;
            }

            if (before != target[i] && progress[i] == target[i]) {
                if (target[i] == 1f) {
                    fireOpened(i);
                } else {
                    fireClosed(i);
                }
            }

            float hoverDiff = hoverTarget[i] - hoverProgress[i];
            if (Math.abs(hoverDiff) > 0.004f) {
                hoverProgress[i] += hoverDiff * 0.2f;
                animating = true;
            } else {
                hoverProgress[i] = hoverTarget[i];
            }
        }

        updatePreferredSizeIfNeeded();
        repaint();
        if (!animating) {
            timer.stop();
        }
    }

    private void updateHover(int index) {
        for (int i = 0; i < hoverTarget.length; i++) {
            hoverTarget[i] = (i == index) ? 1f : 0f;
        }
        setCursor(java.awt.Cursor.getPredefinedCursor(
                index >= 0 ? java.awt.Cursor.HAND_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
        ensureRunning();
    }

    private static float waveY(float frac, float amp, float offset) {
        return (float) (Math.sin(frac * Math.PI * 2.5 + offset) * amp
                + Math.sin(frac * Math.PI * 1.3 + offset * 0.7) * amp * 0.4);
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private int headerIndexAt(Point p) {
        for (int i = 0; i < headerBounds.length; i++) {
            if (headerBounds[i] != null && headerBounds[i].contains(p)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void doLayout() {
        int n = activeSectionCount();
        int width = getWidth();
        int y = 0;
        for (int i = 0; i < n; i++) {
            headerBounds[i].setBounds(0, y, width, headerHeight);
            y += headerHeight;

            int prefH = sectionContent[i].getPreferredSize().height;
            contentPrefHeight[i] = prefH;
            int revealed = Math.round(prefH * progress[i]);
            sectionContent[i].setBounds(0, y, width, Math.max(revealed, 0));
            y += Math.max(revealed, 0);

            if (i < n - 1) {
                y += sectionSpacing;
            }
        }
    }

    private int computeTotalHeight() {
        int n = activeSectionCount();
        int total = 0;
        for (int i = 0; i < n; i++) {
            total += headerHeight;
            int prefH = (sectionContent[i] != null) ? sectionContent[i].getPreferredSize().height : 0;
            total += Math.round(prefH * progress[i]);
            if (i < n - 1) {
                total += sectionSpacing;
            }
        }
        return Math.max(total, 0);
    }

    private void updatePreferredSizeIfNeeded() {
        int h = computeTotalHeight();
        if (h != lastPreferredHeight) {
            lastPreferredHeight = h;
            revalidate();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        int n = activeSectionCount();
        int w = 320;
        for (int i = 0; i < n; i++) {
            if (sectionContent[i] != null) {
                w = Math.max(w, sectionContent[i].getPreferredSize().width);
            }
        }
        return new Dimension(w, computeTotalHeight());
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FIconPaint.hints(g2);
            int n = activeSectionCount();
            for (int i = 0; i < n; i++) {
                paintSectionCard(g2, i);
                paintHeader(g2, i);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FIconPaint.hints(g2);
            int n = activeSectionCount();
            for (int i = 0; i < n; i++) {
                if (waveAmp[i] > 0.05f) {
                    paintLiquidWaveBand(g2, i);
                }
            }
        } finally {
            g2.dispose();
        }
    }

    private Shape buildCardShape(int i) {
        Rectangle hb = headerBounds[i];
        int revealed = Math.round(contentPrefHeight[i] * progress[i]);
        RoundRectangle2D headerShape = new RoundRectangle2D.Float(
                hb.x, hb.y, Math.max(hb.width, 1), headerHeight + arc, arc, arc);
        if (revealed <= 0) {
            return headerShape;
        }
        Rectangle2D contentShape = new Rectangle2D.Float(
                hb.x, hb.y + headerHeight, Math.max(hb.width, 1), revealed);
        Area card = new Area(headerShape);
        card.add(new Area(contentShape));
        return card;
    }

    private void paintSectionCard(Graphics2D g2, int i) {
        Shape card = buildCardShape(i);
        g2.setPaint(FColors.withAlpha(headerBackground, 46));
        g2.fill(card);
        FIconPaint.rim(g2, card, 1.4f);
    }

    private void paintHeader(Graphics2D g2, int i) {
        Rectangle hb = headerBounds[i];
        float p = progress[i];
        float hover = hoverProgress[i];

        Color accent = FColors.blend(liquidColorBottom, liquidColorTop, p);

        // Linker Akzentbalken (zeigt Auf/Zu-Fortschritt)
        Rectangle2D.Float bar = new Rectangle2D.Float(hb.x + 3, hb.y + 6, 4, headerHeight - 12);
        g2.setPaint(accent);
        g2.fill(bar);

        // Hover-Tint ueber dem Header
        if (hover > 0.01f) {
            g2.setPaint(FColors.withAlpha(liquidColorTop, (int) (hover * 26)));
            g2.fill(new Rectangle2D.Float(hb.x, hb.y, hb.width, headerHeight));
        }

        // Chevron
        float chevronSize = 11f;
        float chevronCy = hb.y + headerHeight / 2f;
        float chevronCx = (chevronPosition == FChevronPosition.RIGHT)
                ? hb.x + hb.width - headerPadX - chevronSize / 2f
                : hb.x + headerPadX + 12 + chevronSize / 2f;
        paintChevron(g2, chevronCx, chevronCy, chevronSize, p, accent);

        // Titel
        String title = (model != null) ? model.getTitleAt(i) : "";
        if (title != null) {
            Font font = FTheme.getInstance().getFont(FFontRole.BUTTON);
            g2.setFont(font);
            g2.setColor(textColor);
            FontMetrics fm = g2.getFontMetrics();
            int textX = (chevronPosition == FChevronPosition.RIGHT)
                    ? hb.x + headerPadX + 12
                    : hb.x + headerPadX + 24;
            int textY = hb.y + (headerHeight - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(title, textX, textY);
        }
    }

    private void paintChevron(Graphics2D g2, float cx, float cy, float size, float progress, Color color) {
        Graphics2D g = (Graphics2D) g2.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(cx, cy);
            g.rotate(Math.toRadians(progress * 180.0));
            float h = size / 2f;
            GeneralPath path = new GeneralPath();
            path.moveTo(-h, -h * 0.5f);
            path.lineTo(0, h * 0.5f);
            path.lineTo(h, -h * 0.5f);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(color);
            g.draw(path);
        } finally {
            g.dispose();
        }
    }

    private void paintLiquidWaveBand(Graphics2D g2, int i) {
        Rectangle hb = headerBounds[i];
        int prefH = contentPrefHeight[i];
        int revealed = Math.round(prefH * progress[i]);
        int boundaryY = hb.y + headerHeight + revealed;
        int width = getWidth();
        if (width <= 0) {
            return;
        }
        float amp = waveAmp[i];
        float offset = waveOffset[i];

        int steps = Math.max(8, width / 6);
        Path2D.Float path = new Path2D.Float();
        path.moveTo(0, boundaryY + waveY(0, amp, offset));
        for (int s = 1; s <= steps; s++) {
            float frac = (float) s / steps;
            path.lineTo(frac * width, boundaryY + waveY(frac, amp, offset));
        }
        path.lineTo(width, boundaryY - WAVE_BAND_PX);
        path.lineTo(0, boundaryY - WAVE_BAND_PX);
        path.closePath();

        Composite oldComposite = g2.getComposite();
        GradientPaint grad = new GradientPaint(
                0, boundaryY - WAVE_BAND_PX, liquidColorTop,
                0, boundaryY, liquidColorBottom);
        g2.setPaint(grad);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        g2.fill(path);
        g2.setComposite(oldComposite);

        g2.setStroke(new BasicStroke(1.4f));
        g2.setColor(FColors.withAlpha(liquidColorTop, 170));
        g2.draw(path);
    }

    // ------------------------------------------------------------------
    // Bean-Properties
    // ------------------------------------------------------------------

    public boolean isMultipleOpen() {
        return multipleOpen;
    }

    public void setMultipleOpen(boolean multipleOpen) {
        this.multipleOpen = multipleOpen;
    }

    public boolean isCollapsible() {
        return collapsible;
    }

    public void setCollapsible(boolean collapsible) {
        this.collapsible = collapsible;
    }

    public double getAnimationSpeed() {
        return animationSpeed;
    }

    public void setAnimationSpeed(double animationSpeed) {
        this.animationSpeed = Math.max(0.02, Math.min(1.0, animationSpeed));
    }

    public int getHeaderHeight() {
        return headerHeight;
    }

    public void setHeaderHeight(int headerHeight) {
        this.headerHeight = Math.max(20, headerHeight);
        revalidate();
        repaint();
    }

    public int getSectionSpacing() {
        return sectionSpacing;
    }

    public void setSectionSpacing(int sectionSpacing) {
        this.sectionSpacing = Math.max(0, sectionSpacing);
        revalidate();
        repaint();
    }

    public int getArc() {
        return arc;
    }

    public void setArc(int arc) {
        this.arc = Math.max(0, arc);
        repaint();
    }

    public float getWaveAmplitude() {
        return waveAmplitude;
    }

    public void setWaveAmplitude(float waveAmplitude) {
        this.waveAmplitude = Math.max(0f, waveAmplitude);
    }

    public FChevronPosition getChevronPosition() {
        return chevronPosition;
    }

    public void setChevronPosition(FChevronPosition chevronPosition) {
        this.chevronPosition = (chevronPosition != null) ? chevronPosition : FChevronPosition.RIGHT;
        repaint();
    }

    public Color getLiquidColorTop() {
        return liquidColorTop;
    }

    public void setLiquidColorTop(Color liquidColorTop) {
        this.liquidColorTop = liquidColorTop;
        this.userLiquidColorTop = true;
        repaint();
    }

    public Color getLiquidColorBottom() {
        return liquidColorBottom;
    }

    public void setLiquidColorBottom(Color liquidColorBottom) {
        this.liquidColorBottom = liquidColorBottom;
        this.userLiquidColorBottom = true;
        repaint();
    }

    public Color getHeaderBackground() {
        return headerBackground;
    }

    public void setHeaderBackground(Color headerBackground) {
        this.headerBackground = headerBackground;
        this.userHeaderBackground = true;
        repaint();
    }

    public Color getTextColor() {
        return textColor;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
        this.userTextColor = true;
        repaint();
    }
}
