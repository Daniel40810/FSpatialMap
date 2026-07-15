package com.dan.fgeochart;

import com.dan.fstyle.FColors;
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
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FGeoChart — interaktive Geo-Karte im F-Stil (Laboratory Dark Look).
 *
 * Rendert {@link FGeoChartModel}-Layer (Punkte, Linien, Polygone in WGS84)
 * auf eine Java2D-Canvas mit Pan (Drag), Zoom (Mausrad, um den Cursor),
 * Feature-Picking (Hover-Glow + Glas-Popup) und gemalten Glass-Overlay-Controls
 * (Zoom +/−, Home). Theme-gebunden ueber {@link FTheme.ThemeListener}.
 */
public class FGeoChart extends JComponent implements FTheme.ThemeListener {

    // ------------------------------------------------------------------
    // Oeffentliche Stil-Felder (FBarChart-Konvention)
    // ------------------------------------------------------------------
    public Color backgroundDark = new Color(0x10, 0x16, 0x1E);
    public Color gridColor      = new Color(255, 255, 255, 22);
    public Color glassRim       = new Color(140, 200, 255, 160);
    public Color labelColor     = new Color(235, 240, 248);
    public Color mutedColor     = new Color(150, 165, 185);
    public Color hoverGlowColor = new Color(0x00B5AD);

    // ------------------------------------------------------------------
    // Model & View-Zustand
    // ------------------------------------------------------------------
    private FGeoChartModel model = new DefaultFGeoChartModel();
    private final PropertyChangeListener modelListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            startReveal();
        }
    };

    private FGeoProjection projection = FGeoProjection.EQUIRECTANGULAR;

    private double centerLon = 10.78;   // Timmendorfer Strand als freundlicher Default
    private double centerLat = 53.99;
    private double scale = 4000.0;      // Pixel pro Grad Laenge (Zielwert)
    private double displayScale = 4000.0;

    private double minScale = 30.0;
    private double maxScale = 4_000_000.0;

    private boolean showGrid = true;
    private boolean showStatusBar = true;
    private boolean showZoomControls = true;
    private boolean showHoverPopup = true;
    private boolean animated = true;
    private float animationSpeed = 0.22f;

    private double homeLon = centerLon;
    private double homeLat = centerLat;
    private double homeScale = scale;

    // ------------------------------------------------------------------
    // Animations-Zustand (ein Timer, mehrere Progress-Felder)
    // ------------------------------------------------------------------
    private final Timer timer;
    private float revealProgress = 1f;       // 0..1 Feature-Einblendung
    private float hoverProgress = 0f;        // 0..1 Hover-Glow
    private float controlsHoverProgress = 0f;

    private double panTargetLon = centerLon;
    private double panTargetLat = centerLat;

    // ------------------------------------------------------------------
    // Interaktion
    // ------------------------------------------------------------------
    private Point dragStart;
    private double dragStartLon, dragStartLat;
    private boolean dragging;

    private FGeoFeature hoverFeature;
    private FGeoLayer hoverLayer;
    private Point hoverPoint;

    private int hoverControl = -1;   // 0=+, 1=−, 2=Home
    private int pressedControl = -1;

    private static final int CTRL_SIZE = 34;
    private static final int CTRL_PAD = 12;
    private static final int CTRL_GAP = 8;

    // ------------------------------------------------------------------
    // Konstruktor
    // ------------------------------------------------------------------
    public FGeoChart() {
        setOpaque(false);
        setPreferredSize(new Dimension(640, 440));
        setFont(new Font("Segoe UI", Font.PLAIN, 12));

        timer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tick();
            }
        });

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int c = controlAt(e.getPoint());
                if (c >= 0) {
                    pressedControl = c;
                    repaint();
                    return;
                }
                dragStart = e.getPoint();
                dragStartLon = centerLon;
                dragStartLat = centerLat;
                dragging = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (pressedControl >= 0) {
                    int c = controlAt(e.getPoint());
                    if (c == pressedControl) {
                        onControlClicked(c);
                    }
                    pressedControl = -1;
                    repaint();
                }
                dragging = false;
                dragStart = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging && dragStart != null) {
                    double dLonPerPx = 1.0 / (displayScale * lonStretch());
                    double dLatPerPx = 1.0 / displayScale;
                    centerLon = dragStartLon - (e.getX() - dragStart.x) * dLonPerPx;
                    centerLat = dragStartLat + (e.getY() - dragStart.y) * dLatPerPx;
                    panTargetLon = centerLon;
                    panTargetLat = centerLat;
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int c = controlAt(e.getPoint());
                if (c != hoverControl) {
                    hoverControl = c;
                    ensureRunning();
                }
                if (c < 0) {
                    updateHover(e.getPoint());
                } else if (hoverFeature != null) {
                    hoverFeature = null;
                    hoverLayer = null;
                    ensureRunning();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverControl = -1;
                if (hoverFeature != null) {
                    hoverFeature = null;
                    hoverLayer = null;
                }
                ensureRunning();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoomAt(e.getPoint(), e.getPreciseWheelRotation() < 0 ? 1.25 : 0.8);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);

        model.addPropertyChangeListener(modelListener);
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
        timer.stop();
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
        backgroundDark = t.getBackground();
        glassRim       = t.getGlassRim();
        labelColor     = t.getText();
        mutedColor     = t.getTextMuted();
        hoverGlowColor = t.getAccent();
        gridColor      = FColors.withAlpha(t.getText(), 22);
    }

    // ------------------------------------------------------------------
    // Model
    // ------------------------------------------------------------------
    public FGeoChartModel getModel() {
        return model;
    }

    public void setModel(FGeoChartModel newModel) {
        if (newModel == null || newModel == model) {
            return;
        }
        model.removePropertyChangeListener(modelListener);
        model = newModel;
        model.addPropertyChangeListener(modelListener);
        startReveal();
    }

    // ------------------------------------------------------------------
    // Oeffentliche View-API
    // ------------------------------------------------------------------

    /** Setzt Kartenmitte und Massstab in einem Rutsch (animiert, wenn animated). */
    public void setView(double lon, double lat, double pixelsPerDegree) {
        panTargetLon = lon;
        panTargetLat = lat;
        scale = clampScale(pixelsPerDegree);
        if (!animated) {
            centerLon = lon;
            centerLat = lat;
            displayScale = scale;
            repaint();
        } else {
            ensureRunning();
        }
    }

    /** Merkt aktuelle Sicht als Home-Position (Ziel des ⌂-Buttons). */
    public void setHomeView(double lon, double lat, double pixelsPerDegree) {
        homeLon = lon;
        homeLat = lat;
        homeScale = clampScale(pixelsPerDegree);
    }

    /** Zoomt die Sicht so, dass alle sichtbaren Features hineinpassen. */
    public void zoomToFit() {
        double lo = Double.MAX_VALUE, la = Double.MAX_VALUE;
        double hlo = -Double.MAX_VALUE, hla = -Double.MAX_VALUE;
        boolean any = false;
        for (FGeoLayer layer : model.getLayers()) {
            if (!layer.isVisible()) {
                continue;
            }
            for (FGeoFeature f : layer.getFeatures()) {
                lo = Math.min(lo, f.getMinLon());
                la = Math.min(la, f.getMinLat());
                hlo = Math.max(hlo, f.getMaxLon());
                hla = Math.max(hla, f.getMaxLat());
                any = true;
            }
        }
        if (!any) {
            return;
        }
        double cLon = (lo + hlo) / 2.0;
        double cLat = (la + hla) / 2.0;
        double spanLon = Math.max(1e-6, hlo - lo);
        double spanLat = Math.max(1e-6, hla - la);
        double stretch = Math.cos(Math.toRadians(cLat));
        double sx = (getWidth() > 0 ? getWidth() : 640) * 0.8 / (spanLon * stretch);
        double sy = (getHeight() > 0 ? getHeight() : 440) * 0.8 / spanLat;
        setView(cLon, cLat, Math.min(sx, sy));
    }

    public double getCenterLon()   { return centerLon; }
    public double getCenterLat()   { return centerLat; }
    public double getMapScale()    { return scale; }

    public FGeoProjection getProjection() {
        return projection;
    }

    public void setProjection(FGeoProjection p) {
        if (p != null && p != projection) {
            projection = p;
            repaint();
        }
    }

    // ------------------------------------------------------------------
    // JavaBean-Properties
    // ------------------------------------------------------------------
    public boolean isShowGrid()                  { return showGrid; }
    public void setShowGrid(boolean b)           { showGrid = b; repaint(); }

    public boolean isShowStatusBar()             { return showStatusBar; }
    public void setShowStatusBar(boolean b)      { showStatusBar = b; repaint(); }

    public boolean isShowZoomControls()          { return showZoomControls; }
    public void setShowZoomControls(boolean b)   { showZoomControls = b; repaint(); }

    public boolean isShowHoverPopup()            { return showHoverPopup; }
    public void setShowHoverPopup(boolean b)     { showHoverPopup = b; repaint(); }

    public boolean isAnimated()                  { return animated; }
    public void setAnimated(boolean b) {
        animated = b;
        if (!b) {
            displayScale = scale;
            centerLon = panTargetLon;
            centerLat = panTargetLat;
            revealProgress = 1f;
            timer.stop();
            repaint();
        }
    }

    public float getAnimationSpeed()             { return animationSpeed; }
    public void setAnimationSpeed(float s)       { animationSpeed = Math.max(0.02f, Math.min(0.9f, s)); }

    public Color getBackgroundDark()             { return backgroundDark; }
    public void setBackgroundDark(Color c)       { if (c != null) { backgroundDark = c; repaint(); } }

    public Color getGridColor()                  { return gridColor; }
    public void setGridColor(Color c)            { if (c != null) { gridColor = c; repaint(); } }

    public Color getGlassRim()                   { return glassRim; }
    public void setGlassRim(Color c)             { if (c != null) { glassRim = c; repaint(); } }

    public Color getLabelColor()                 { return labelColor; }
    public void setLabelColor(Color c)           { if (c != null) { labelColor = c; repaint(); } }

    public Color getHoverGlowColor()             { return hoverGlowColor; }
    public void setHoverGlowColor(Color c)       { if (c != null) { hoverGlowColor = c; repaint(); } }

    // ------------------------------------------------------------------
    // Animation
    // ------------------------------------------------------------------
    private void startReveal() {
        if (animated) {
            revealProgress = 0f;
            ensureRunning();
        } else {
            revealProgress = 1f;
        }
        repaint();
    }

    private void ensureRunning() {
        if (animated && !timer.isRunning()) {
            timer.start();
        }
        if (!animated) {
            repaint();
        }
    }

    private void tick() {
        boolean moving = false;

        if (revealProgress < 1f) {
            revealProgress += (1f - revealProgress) * animationSpeed;
            if (1f - revealProgress < 0.004f) {
                revealProgress = 1f;
            } else {
                moving = true;
            }
        }

        float hoverTarget = (hoverFeature != null) ? 1f : 0f;
        if (Math.abs(hoverTarget - hoverProgress) > 0.004f) {
            hoverProgress += (hoverTarget - hoverProgress) * 0.25f;
            moving = true;
        } else {
            hoverProgress = hoverTarget;
        }

        float ctrlTarget = (hoverControl >= 0) ? 1f : 0f;
        if (Math.abs(ctrlTarget - controlsHoverProgress) > 0.004f) {
            controlsHoverProgress += (ctrlTarget - controlsHoverProgress) * 0.25f;
            moving = true;
        } else {
            controlsHoverProgress = ctrlTarget;
        }

        if (Math.abs(scale - displayScale) > scale * 0.001) {
            displayScale += (scale - displayScale) * animationSpeed;
            moving = true;
        } else {
            displayScale = scale;
        }

        if (Math.abs(panTargetLon - centerLon) > 1e-9 || Math.abs(panTargetLat - centerLat) > 1e-9) {
            centerLon += (panTargetLon - centerLon) * animationSpeed;
            centerLat += (panTargetLat - centerLat) * animationSpeed;
            if (Math.abs(panTargetLon - centerLon) < 1e-7 && Math.abs(panTargetLat - centerLat) < 1e-7) {
                centerLon = panTargetLon;
                centerLat = panTargetLat;
            } else {
                moving = true;
            }
        }

        repaint();
        if (!moving) {
            timer.stop();
        }
    }

    // ------------------------------------------------------------------
    // Koordinaten-Transformation
    // ------------------------------------------------------------------
    private double lonStretch() {
        return Math.max(0.05, Math.cos(Math.toRadians(centerLat)));
    }

    /** WGS84 -> Bildschirm-X. */
    public double toScreenX(double lon) {
        return getWidth() / 2.0 + (lon - centerLon) * displayScale * lonStretch();
    }

    /** WGS84 -> Bildschirm-Y (Y waechst nach unten). */
    public double toScreenY(double lat) {
        double p  = projection.projectLat(lat);
        double pc = projection.projectLat(centerLat);
        return getHeight() / 2.0 - (p - pc) * displayScale;
    }

    /** Bildschirm-X -> Laengengrad. */
    public double toLon(double screenX) {
        return centerLon + (screenX - getWidth() / 2.0) / (displayScale * lonStretch());
    }

    /** Bildschirm-Y -> Breitengrad. */
    public double toLat(double screenY) {
        double pc = projection.projectLat(centerLat);
        double p  = pc - (screenY - getHeight() / 2.0) / displayScale;
        return projection.unprojectLat(p);
    }

    private void zoomAt(Point p, double factor) {
        double lonUnder = toLon(p.x);
        double latUnder = toLat(p.y);
        scale = clampScale(scale * factor);
        if (!animated) {
            displayScale = scale;
        }
        // Punkt unter der Maus fixieren
        double stretch = lonStretch();
        double newDisplay = animated ? displayScale : scale;
        // Ziel-Center so setzen, dass (lonUnder,latUnder) unter p bleibt (bezogen auf Zielscale)
        double targetStretchScale = scale * stretch;
        double tLon = lonUnder - (p.x - getWidth() / 2.0) / targetStretchScale;
        double pUnder = projection.projectLat(latUnder);
        double tLatProj = pUnder + (p.y - getHeight() / 2.0) / scale;
        double tLat = projection.unprojectLat(tLatProj);
        panTargetLon = tLon;
        panTargetLat = tLat;
        if (!animated) {
            centerLon = tLon;
            centerLat = tLat;
            repaint();
        } else {
            ensureRunning();
        }
        // newDisplay bewusst ungenutzt lassen — Kommentarvermeidung
        if (newDisplay < 0) { /* unreachable */ }
    }

    private double clampScale(double s) {
        return Math.max(minScale, Math.min(maxScale, s));
    }

    // ------------------------------------------------------------------
    // Hover / Picking
    // ------------------------------------------------------------------
    private void updateHover(Point p) {
        FGeoFeature best = null;
        FGeoLayer bestLayer = null;
        double bestDist = 14.0;   // Pick-Toleranz in Pixel

        for (int li = model.getLayerCount() - 1; li >= 0; li--) {
            FGeoLayer layer = model.getLayer(li);
            if (!layer.isVisible()) {
                continue;
            }
            for (FGeoFeature f : layer.getFeatures()) {
                double d = distanceToFeature(p, f, layer);
                if (d < bestDist) {
                    bestDist = d;
                    best = f;
                    bestLayer = layer;
                }
            }
        }

        if (best != hoverFeature) {
            hoverFeature = best;
            hoverLayer = bestLayer;
            ensureRunning();
        }
        hoverPoint = p;
        if (hoverFeature != null) {
            repaint();
        }
    }

    private double distanceToFeature(Point p, FGeoFeature f, FGeoLayer layer) {
        switch (f.getGeometryType()) {
            case POINT: {
                double dx = toScreenX(f.getLon(0)) - p.x;
                double dy = toScreenY(f.getLat(0)) - p.y;
                return Math.sqrt(dx * dx + dy * dy) - layer.getPointRadius();
            }
            case LINE:
            case NETWORK: {
                double min = Double.MAX_VALUE;
                for (int i = 0; i < f.getPointCount() - 1; i++) {
                    min = Math.min(min, distToSegment(p,
                            toScreenX(f.getLon(i)), toScreenY(f.getLat(i)),
                            toScreenX(f.getLon(i + 1)), toScreenY(f.getLat(i + 1))));
                }
                return min;
            }
            default: {   // POLYGON / RASTER: Innen = Treffer, sonst Randdistanz
                Path2D path = buildPath(f, true);
                if (path.contains(p)) {
                    return 0.0;
                }
                double min = Double.MAX_VALUE;
                int n = f.getPointCount();
                for (int i = 0; i < n; i++) {
                    int j = (i + 1) % n;
                    min = Math.min(min, distToSegment(p,
                            toScreenX(f.getLon(i)), toScreenY(f.getLat(i)),
                            toScreenX(f.getLon(j)), toScreenY(f.getLat(j))));
                }
                return min;
            }
        }
    }

    private static double distToSegment(Point p, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        double len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : ((p.x - x1) * dx + (p.y - y1) * dy) / len2;
        t = Math.max(0, Math.min(1, t));
        double px = x1 + t * dx, py = y1 + t * dy;
        return Math.sqrt((p.x - px) * (p.x - px) + (p.y - py) * (p.y - py));
    }

    // ------------------------------------------------------------------
    // Glass-Controls (Zoom +/−, Home)
    // ------------------------------------------------------------------
    private RoundRectangle2D controlBounds(int index) {
        int x = CTRL_PAD;
        int y = CTRL_PAD + index * (CTRL_SIZE + CTRL_GAP);
        return new RoundRectangle2D.Double(x, y, CTRL_SIZE, CTRL_SIZE, 10, 10);
    }

    private int controlAt(Point p) {
        if (!showZoomControls) {
            return -1;
        }
        for (int i = 0; i < 3; i++) {
            if (controlBounds(i).contains(p)) {
                return i;
            }
        }
        return -1;
    }

    private void onControlClicked(int c) {
        Point center = new Point(getWidth() / 2, getHeight() / 2);
        switch (c) {
            case 0: zoomAt(center, 1.5); break;
            case 1: zoomAt(center, 1.0 / 1.5); break;
            default: setView(homeLon, homeLat, homeScale); break;
        }
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            Shape frame = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 18, 18);

            // 1 — Hintergrund
            g2.setColor(backgroundDark);
            g2.fill(frame);

            Shape oldClip = g2.getClip();
            g2.clip(frame);

            // 2 — Grid
            if (showGrid) {
                paintGrid(g2, w, h);
            }

            // 3 — Layer
            for (FGeoLayer layer : model.getLayers()) {
                if (layer.isVisible()) {
                    paintLayer(g2, layer);
                }
            }

            // 4 — Hover-Glow + Popup
            if (hoverFeature != null && hoverLayer != null && hoverProgress > 0.02f) {
                paintHoverGlow(g2);
            }
            if (showHoverPopup && hoverFeature != null && hoverPoint != null && hoverProgress > 0.05f) {
                paintHoverPopup(g2, w, h);
            }

            // 5 — Overlays
            if (showZoomControls) {
                paintControls(g2);
            }
            if (showStatusBar) {
                paintStatusBar(g2, w, h);
            }

            g2.setClip(oldClip);

            // 6 — Glasrahmen aussen
            g2.setColor(glassRim);
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(frame);
        } finally {
            g2.dispose();
        }
    }

    private void paintGrid(Graphics2D g2, int w, int h) {
        double lonSpan = w / (displayScale * lonStretch());
        double step = niceStep(lonSpan / 6.0);

        g2.setColor(gridColor);
        g2.setStroke(new BasicStroke(1f));
        Font small = getFont().deriveFont(10f);
        g2.setFont(small);

        double startLon = Math.floor(toLon(0) / step) * step;
        for (double lon = startLon; toScreenX(lon) < w + step * displayScale; lon += step) {
            int x = (int) Math.round(toScreenX(lon));
            g2.drawLine(x, 0, x, h);
            g2.setColor(FColors.withAlpha(mutedColor, 120));
            g2.drawString(formatDeg(lon, true), x + 3, h - (showStatusBar ? 34 : 8));
            g2.setColor(gridColor);
        }
        double startLat = Math.floor(toLat(h) / step) * step;
        for (double lat = startLat; toScreenY(lat) > -step * displayScale; lat += step) {
            int y = (int) Math.round(toScreenY(lat));
            g2.drawLine(0, y, w, y);
            g2.setColor(FColors.withAlpha(mutedColor, 120));
            g2.drawString(formatDeg(lat, false), showZoomControls ? CTRL_PAD + CTRL_SIZE + 8 : 6, y - 3);
            g2.setColor(gridColor);
        }
    }

    private static double niceStep(double raw) {
        double mag = Math.pow(10, Math.floor(Math.log10(Math.max(1e-9, raw))));
        double norm = raw / mag;
        double nice;
        if (norm < 1.5)      nice = 1;
        else if (norm < 3.5) nice = 2;
        else if (norm < 7.5) nice = 5;
        else                 nice = 10;
        return nice * mag;
    }

    private static String formatDeg(double v, boolean lon) {
        String hemi = lon ? (v >= 0 ? "E" : "W") : (v >= 0 ? "N" : "S");
        double a = Math.abs(v);
        if (a >= 1) {
            return String.format("%.2f\u00B0%s", a, hemi);
        }
        return String.format("%.4f\u00B0%s", a, hemi);
    }

    private void paintLayer(Graphics2D g2, FGeoLayer layer) {
        float reveal = easeOut(revealProgress);
        for (FGeoFeature f : layer.getFeatures()) {
            Color c = layer.colorFor(f);
            switch (f.getGeometryType()) {
                case POINT:
                    paintPoint(g2, layer, f, c, reveal);
                    break;
                case LINE:
                case NETWORK:
                    paintLine(g2, layer, f, c, reveal);
                    break;
                default:
                    paintPolygon(g2, layer, f, c, reveal);
                    break;
            }
        }
    }

    private void paintPoint(Graphics2D g2, FGeoLayer layer, FGeoFeature f, Color c, float reveal) {
        double x = toScreenX(f.getLon(0));
        double y = toScreenY(f.getLat(0));
        if (!onScreen(x, y, 40)) {
            return;
        }
        double r = layer.getPointRadius() * reveal;
        if (r < 0.4) {
            return;
        }
        // Aussen-Halo
        g2.setColor(FColors.withAlpha(c, (int) (60 * reveal)));
        g2.fill(new Ellipse2D.Double(x - r * 1.8, y - r * 1.8, r * 3.6, r * 3.6));
        // Kern
        g2.setColor(FColors.withAlpha(c, (int) (235 * reveal)));
        g2.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        // Glanzpunkt (Glas-Look)
        g2.setColor(new Color(255, 255, 255, (int) (170 * reveal)));
        double gr = r * 0.45;
        g2.fill(new Ellipse2D.Double(x - r * 0.45, y - r * 0.55, gr, gr));
        // Rim
        g2.setColor(FColors.withAlpha(FColors.lighten(c, 0.4f), (int) (200 * reveal)));
        g2.setStroke(new BasicStroke(1.1f));
        g2.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
    }

    private void paintLine(Graphics2D g2, FGeoLayer layer, FGeoFeature f, Color c, float reveal) {
        Path2D p = buildPath(f, false);
        if (!p.intersects(-50, -50, getWidth() + 100, getHeight() + 100)) {
            return;
        }
        float wBase = layer.getStrokeWidth();
        // Unterlegter Glow
        g2.setColor(FColors.withAlpha(c, (int) (55 * reveal)));
        g2.setStroke(new BasicStroke(wBase + 5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(p);
        // Kernlinie
        g2.setColor(FColors.withAlpha(c, (int) (230 * reveal)));
        g2.setStroke(new BasicStroke(wBase, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(p);
    }

    private void paintPolygon(Graphics2D g2, FGeoLayer layer, FGeoFeature f, Color c, float reveal) {
        Path2D p = buildPath(f, true);
        if (!p.intersects(-50, -50, getWidth() + 100, getHeight() + 100)) {
            return;
        }
        g2.setColor(FColors.withAlpha(c, (int) (255 * layer.getFillAlpha() * reveal)));
        g2.fill(p);
        g2.setColor(FColors.withAlpha(FColors.lighten(c, 0.25f), (int) (220 * reveal)));
        g2.setStroke(new BasicStroke(layer.getStrokeWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(p);
    }

    private Path2D buildPath(FGeoFeature f, boolean close) {
        Path2D p = new Path2D.Double();
        int n = f.getPointCount();
        p.moveTo(toScreenX(f.getLon(0)), toScreenY(f.getLat(0)));
        for (int i = 1; i < n; i++) {
            p.lineTo(toScreenX(f.getLon(i)), toScreenY(f.getLat(i)));
        }
        if (close) {
            p.closePath();
        }
        return p;
    }

    private boolean onScreen(double x, double y, double margin) {
        return x >= -margin && y >= -margin && x <= getWidth() + margin && y <= getHeight() + margin;
    }

    private void paintHoverGlow(Graphics2D g2) {
        Color glow = FColors.withAlpha(hoverGlowColor, (int) (140 * hoverProgress));
        FGeoFeature f = hoverFeature;
        switch (f.getGeometryType()) {
            case POINT: {
                double x = toScreenX(f.getLon(0));
                double y = toScreenY(f.getLat(0));
                double r = hoverLayer.getPointRadius() + 5 + 3 * hoverProgress;
                g2.setColor(glow);
                g2.setStroke(new BasicStroke(2.4f));
                g2.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
                break;
            }
            default: {
                Path2D p = buildPath(f, f.getGeometryType() == com.dan.fmaplegend.FLayerGeometryType.POLYGON);
                g2.setColor(glow);
                g2.setStroke(new BasicStroke(hoverLayer.getStrokeWidth() + 3.5f,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(p);
                break;
            }
        }
    }

    /** Glas-Popup direkt auf die Canvas gemalt (Skill §7 — kein JWindow). */
    private void paintHoverPopup(Graphics2D g2, int w, int h) {
        FGeoFeature f = hoverFeature;
        Font titleFont = getFont().deriveFont(Font.BOLD, 12.5f);
        Font bodyFont  = getFont().deriveFont(11f);

        List<String> lines = new ArrayList<String>();
        String title = f.getName().isEmpty() ? ("Feature #" + f.getId()) : f.getName();
        lines.add(hoverLayer.getName() + "  \u00B7  " + f.getGeometryType().getLabel());
        for (Map.Entry<String, String> e : f.getAttributes().entrySet()) {
            lines.add(e.getKey() + ": " + e.getValue());
            if (lines.size() >= 5) {
                break;
            }
        }

        FontMetrics fmT = g2.getFontMetrics(titleFont);
        FontMetrics fmB = g2.getFontMetrics(bodyFont);
        int tw = fmT.stringWidth(title);
        for (String s : lines) {
            tw = Math.max(tw, fmB.stringWidth(s));
        }
        int pw = tw + 26;
        int ph = 14 + fmT.getHeight() + lines.size() * fmB.getHeight() + 8;

        int px = hoverPoint.x + 16;
        int py = hoverPoint.y - ph - 10;
        if (px + pw > w - 8) px = hoverPoint.x - pw - 16;
        if (py < 8)          py = hoverPoint.y + 20;
        px = Math.max(8, Math.min(w - pw - 8, px));
        py = Math.max(8, Math.min(h - ph - 8, py));

        float a = hoverProgress;
        RoundRectangle2D box = new RoundRectangle2D.Double(px, py, pw, ph, 12, 12);

        // Schatten
        g2.setColor(new Color(0, 0, 0, (int) (90 * a)));
        g2.fill(new RoundRectangle2D.Double(px + 2, py + 3, pw, ph, 12, 12));
        // Glasfuellung
        g2.setColor(FColors.withAlpha(FColors.lighten(backgroundDark, 0.18f), (int) (235 * a)));
        g2.fill(box);
        // Akzentstreifen links
        g2.setColor(FColors.withAlpha(hoverLayer.colorFor(f), (int) (255 * a)));
        g2.fill(new RoundRectangle2D.Double(px, py, 4, ph, 4, 4));
        // Rim
        g2.setColor(FColors.withAlpha(glassRim, (int) (180 * a)));
        g2.setStroke(new BasicStroke(1.1f));
        g2.draw(box);
        // Shine oben
        g2.setColor(new Color(255, 255, 255, (int) (26 * a)));
        g2.fill(new RoundRectangle2D.Double(px + 2, py + 2, pw - 4, ph * 0.35, 10, 10));

        int ty = py + 12 + fmT.getAscent();
        g2.setFont(titleFont);
        g2.setColor(FColors.withAlpha(labelColor, (int) (255 * a)));
        g2.drawString(title, px + 14, ty);
        ty += fmT.getDescent() + 2;
        g2.setFont(bodyFont);
        g2.setColor(FColors.withAlpha(mutedColor, (int) (230 * a)));
        for (String s : lines) {
            ty += fmB.getHeight();
            g2.drawString(s, px + 14, ty - fmB.getDescent());
        }
    }

    private void paintControls(Graphics2D g2) {
        String[] glyphs = {"+", "\u2212", "\u2302"};
        Font glyphFont = getFont().deriveFont(Font.BOLD, 16f);
        for (int i = 0; i < 3; i++) {
            RoundRectangle2D b = controlBounds(i);
            boolean hov = (i == hoverControl);
            boolean prs = (i == pressedControl);
            float lift = hov ? controlsHoverProgress : 0f;

            // Schatten
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fill(new RoundRectangle2D.Double(b.getX() + 1.5, b.getY() + 2.5,
                    b.getWidth(), b.getHeight(), 10, 10));
            // Glasfuellung
            Color body = FColors.lighten(backgroundDark, prs ? 0.10f : 0.16f + 0.10f * lift);
            g2.setColor(FColors.withAlpha(body, 225));
            g2.fill(b);
            // Rim + Hover-Akzent
            g2.setColor(hov
                    ? FColors.blend(glassRim, hoverGlowColor, 0.5f * lift + 0.2f)
                    : glassRim);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(b);
            // Shine
            g2.setColor(new Color(255, 255, 255, 28));
            g2.fill(new RoundRectangle2D.Double(b.getX() + 2, b.getY() + 2,
                    b.getWidth() - 4, b.getHeight() * 0.40, 8, 8));
            // Glyph
            g2.setFont(glyphFont);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(FColors.withAlpha(labelColor, prs ? 255 : 220));
            String s = glyphs[i];
            int gx = (int) (b.getCenterX() - fm.stringWidth(s) / 2.0);
            int gy = (int) (b.getCenterY() + fm.getAscent() / 2.0 - fm.getDescent() / 2.0);
            g2.drawString(s, gx, gy + (prs ? 1 : 0));
        }
    }

    private void paintStatusBar(Graphics2D g2, int w, int h) {
        int bh = 26;
        RoundRectangle2D bar = new RoundRectangle2D.Double(10, h - bh - 10, w - 20, bh, 10, 10);
        g2.setColor(FColors.withAlpha(FColors.lighten(backgroundDark, 0.12f), 210));
        g2.fill(bar);
        g2.setColor(FColors.withAlpha(glassRim, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(bar);
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fill(new RoundRectangle2D.Double(12, h - bh - 8, w - 24, bh * 0.4, 8, 8));

        g2.setFont(getFont().deriveFont(11f));
        FontMetrics fm = g2.getFontMetrics();
        int ty = h - 10 - (bh - fm.getAscent() + fm.getDescent()) / 2 - fm.getDescent() + 1;

        String left = "Lat " + String.format("%.4f", centerLat)
                    + "  |  Lon " + String.format("%.4f", centerLon);
        String right = "Zoom " + String.format("%.0f", displayScale) + " px/\u00B0"
                     + "  |  " + projection.getLabel();
        g2.setColor(FColors.withAlpha(labelColor, 220));
        g2.drawString(left, 22, ty);
        g2.setColor(FColors.withAlpha(mutedColor, 220));
        g2.drawString(right, w - 22 - fm.stringWidth(right), ty);
    }

    private static float easeOut(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }
}
