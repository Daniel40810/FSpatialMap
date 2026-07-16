package com.dan.fspatialmap;

import com.dan.fdal.color.ColorDTO;
import com.dan.fdal.color.PaletteDTO;
import com.dan.fgeochart.FGeoChartModel;
import com.dan.fgeochart.FGeoFeature;
import com.dan.fgeochart.FGeoLayer;
import com.dan.flayerstyler.FFeatureRow;
import com.dan.flayerstyler.FLayerStylerModel;
import com.dan.fmaplegend.FLayerGeometryType;
import com.dan.fstyle.FTheme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * WMS-Kartenviewer im F-Style ("Laboratory Dark"): rastert Kacheln eines
 * WMS-Dienstes (Default: GDI-SH Geoserver) client-seitig im Slippy-Map-Schema,
 * cached sie zweistufig ({@link FTileCache}) und bietet Pan (Drag), Zoom
 * (Mausrad + Buttons, mit weicher Animation um den Cursor) sowie Klick-Events
 * ueber {@link FSpatialMapListener}.
 * <p>
 * Phase 1 der FSpatialMap-Familie: reiner Rasterviewer. Vektor-Overlays
 * (Oracle Spatial / {@code com.dan.fgeochart}) und ColorAdmin-Faerbung folgen
 * als eigene Ausbaustufe.
 */
public class FSpatialMapViewer extends JComponent implements FTheme.ThemeListener {

    private static final double ZOOM_EASE = 0.25;
    private static final double ZOOM_SETTLE = 0.002;
    private static final int CLICK_TOLERANCE_PX = 4;

    private FSpatialMapModel model = new DefaultFSpatialMapModel();
    private FTileCache cache = new FTileCache();
    private FTileFetcher fetcher = new FWmsTileFetcher();
    private ExecutorService loaderPool;
    private final Set<String> pendingLoads = new HashSet<>();
    private final List<FSpatialMapListener> listeners = new ArrayList<>();

    private double displayZoom = model.getZoom();
    private double targetZoom = model.getZoom();
    private Timer zoomTimer;

    private int pressX, pressY;
    private double pressCenterLon, pressCenterLat;
    private boolean dragging;

    private FMapTool currentTool = FMapTool.PAN;
    private final List<double[]> measurePoints = new ArrayList<>();
    private final com.dan.ftooltip.FTooltip identifyTooltip = new com.dan.ftooltip.FTooltip();
    private com.dan.ftooltip.FTooltipContent identifyContent;
    private int identifyAnchorX;
    private int identifyAnchorY;

    private Color glassRim = new Color(140, 200, 255, 150);
    private Color attributionColor = new Color(0xEA, 0xFB, 0xFF, 160);
    private Color crosshairColor = new Color(0x00, 0xB5, 0xAD);
    private boolean showAttribution = true;
    private boolean showZoomControls = true;
    private boolean showCoordinateReadout = true;
    private Double lastClickLon;
    private Double lastClickLat;

    private FGeoChartModel vectorModel;
    private final PropertyChangeListener vectorModelListener = evt -> repaint();
    private static final double HIT_TOLERANCE_PX = 6.0;
    private final Map<FGeoLayer, PaletteDTO> heatmapPalettes = new IdentityHashMap<>();
    private final Map<FGeoLayer, Float> layerOpacity = new IdentityHashMap<>();

    private FLayerStylerModel colorAdminModel;
    private FColorAdminLayerStyle colorAdminStyle = FColorAdminLayerStyle.defaults();
    private final PropertyChangeListener colorAdminModelListener = evt -> repaint();

    public FSpatialMapViewer() {
        setOpaque(true);
        setBackground(new Color(0x12, 0x14, 0x1E));
        setPreferredSize(new java.awt.Dimension(640, 480));
        setFocusable(true);
        installInteraction();
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void addNotify() {
        super.addNotify();
        FTheme.getInstance().addThemeListener(this);
        loaderPool = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "FSpatialMapViewer-tile-loader");
            t.setDaemon(true);
            return t;
        });
        model.addPropertyChangeListener(modelListener);
    }

    @Override
    public void removeNotify() {
        FTheme.getInstance().removeThemeListener(this);
        model.removePropertyChangeListener(modelListener);
        if (vectorModel != null) {
            vectorModel.removePropertyChangeListener(vectorModelListener);
        }
        if (colorAdminModel != null) {
            colorAdminModel.removePropertyChangeListener(colorAdminModelListener);
        }
        if (loaderPool != null) {
            loaderPool.shutdownNow();
            loaderPool = null;
        }
        if (zoomTimer != null) {
            zoomTimer.stop();
        }
        super.removeNotify();
    }

    @Override
    public void themeChanged(FTheme theme) {
        repaint();
    }

    private final PropertyChangeListener modelListener = evt -> {
        if ("zoom".equals(evt.getPropertyName())) {
            targetZoom = model.getZoom();
            ensureZoomTimerRunning();
        } else {
            repaint();
            fireViewChanged();
        }
    };

    // ------------------------------------------------------------------
    // Bean-Properties
    // ------------------------------------------------------------------

    public FSpatialMapModel getModel() {
        return model;
    }

    public void setModel(FSpatialMapModel model) {
        if (this.model != null) {
            this.model.removePropertyChangeListener(modelListener);
        }
        this.model = model;
        this.displayZoom = model.getZoom();
        this.targetZoom = model.getZoom();
        model.addPropertyChangeListener(modelListener);
        repaint();
    }

    public FTileCache getCache() {
        return cache;
    }

    public void setCache(FTileCache cache) {
        this.cache = cache;
        repaint();
    }

    public FTileFetcher getFetcher() {
        return fetcher;
    }

    /** Austauschbar - z.B. Stub-Fetcher in Headless-Tests ohne Netzwerkzugriff. */
    public void setFetcher(FTileFetcher fetcher) {
        this.fetcher = fetcher;
        cache.clearMemory();
        pendingLoads.clear();
        repaint();
    }

    public Color getGlassRim() {
        return glassRim;
    }

    public void setGlassRim(Color glassRim) {
        this.glassRim = glassRim;
        repaint();
    }

    public Color getAttributionColor() {
        return attributionColor;
    }

    public void setAttributionColor(Color attributionColor) {
        this.attributionColor = attributionColor;
        repaint();
    }

    public Color getCrosshairColor() {
        return crosshairColor;
    }

    public void setCrosshairColor(Color crosshairColor) {
        this.crosshairColor = crosshairColor;
        repaint();
    }

    public boolean isShowAttribution() {
        return showAttribution;
    }

    public void setShowAttribution(boolean showAttribution) {
        this.showAttribution = showAttribution;
        repaint();
    }

    public boolean isShowZoomControls() {
        return showZoomControls;
    }

    public void setShowZoomControls(boolean showZoomControls) {
        this.showZoomControls = showZoomControls;
        repaint();
    }

    public boolean isShowCoordinateReadout() {
        return showCoordinateReadout;
    }

    public void setShowCoordinateReadout(boolean showCoordinateReadout) {
        this.showCoordinateReadout = showCoordinateReadout;
        repaint();
    }

    /** Vektor-Overlay (GIS-Layer aus Oracle SDO_GEOMETRY o.ae.) - optional, {@code null} = keins. */
    public FGeoChartModel getVectorModel() {
        return vectorModel;
    }

    /**
     * Setzt/entfernt das Vektor-Overlay. Gemalt wird mit eigener Web-Mercator-Mathematik
     * (nicht ueber {@code FGeoChart}), daher exakt pixelsynchron zu den WMS-Kacheln.
     * Layer-Sichtbarkeit, Farbe/Rampe, Strichbreite, Punktradius und Fuell-Alpha werden
     * aus {@link FGeoLayer} respektiert.
     */
    public void setVectorModel(FGeoChartModel vectorModel) {
        if (this.vectorModel != null) {
            this.vectorModel.removePropertyChangeListener(vectorModelListener);
        }
        this.vectorModel = vectorModel;
        if (vectorModel != null) {
            vectorModel.addPropertyChangeListener(vectorModelListener);
        }
        repaint();
    }

    /**
     * Faerbt einen Layer des Vektor-Overlays als Heatmap: statt der festen
     * Layer-Farbe/2-Stufen-Rampe wird {@link FGeoFeature#getValue()} auf
     * [0..1] normiert (ueber {@link FGeoLayer#getMinValue()}/{@link FGeoLayer#getMaxValue()}
     * - vorher per {@code layer.setValueRange(...)} setzen oder
     * {@link FHeatmapApplier#autoNormalize(FGeoLayer)} nutzen) und durch die
     * uebergebene ColorAdmin-{@link PaletteDTO} gemappt ({@code palette.colorAt(t)}).
     * {@code null} entfernt die Heatmap-Faerbung wieder (Layer faellt zurueck
     * auf {@code layer.colorFor(feature)}).
     */
    public void setLayerPalette(FGeoLayer layer, PaletteDTO palette) {
        if (palette == null) {
            heatmapPalettes.remove(layer);
        } else {
            heatmapPalettes.put(layer, palette);
        }
        repaint();
    }

    public void clearLayerPalette(FGeoLayer layer) {
        setLayerPalette(layer, null);
    }

    public PaletteDTO getLayerPalette(FGeoLayer layer) {
        return heatmapPalettes.get(layer);
    }

    /**
     * Layer-Transparenz zusaetzlich zur Sichtbarkeit ({@link FGeoLayer#setVisible}):
     * {@code 0.0} = unsichtbar, {@code 1.0} (Default) = voll deckend. Multipliziert
     * sich auf den Alpha-Kanal von Fuellung UND Rand/Strich - anders als
     * {@code FGeoLayer.fillAlpha}, das nur Polygon-Fuellungen betrifft, wirkt dies
     * auf Punkte/Linien/Polygone gleichermassen. Gedacht fuer {@link FLayerPanel}.
     */
    public void setLayerOpacity(FGeoLayer layer, float opacity01) {
        layerOpacity.put(layer, Math.max(0f, Math.min(1f, opacity01)));
        repaint();
    }

    public float getLayerOpacity(FGeoLayer layer) {
        Float v = layerOpacity.get(layer);
        return v != null ? v : 1.0f;
    }

    public void clearLayerOpacity(FGeoLayer layer) {
        layerOpacity.remove(layer);
        repaint();
    }

    /**
     * ColorAdmin-Live-Layer: zeigt die Features des aktuell in {@code model}
     * ausgewaehlten Layers (siehe {@code FLayerStylerModel#getSelectedLayer()}),
     * gefaerbt exakt wie im gekoppelten {@code FLayerStyler}-Editor-Panel.
     * Farbzuweisungen (auch Layer-Wechsel) erscheinen sofort auf der Karte,
     * da auf jede {@code PropertyChangeEvent} des Models mit {@code repaint()}
     * reagiert wird. {@code null} entfernt den Layer wieder.
     */
    public FLayerStylerModel getColorAdminModel() {
        return colorAdminModel;
    }

    public void setColorAdminModel(FLayerStylerModel colorAdminModel) {
        if (this.colorAdminModel != null) {
            this.colorAdminModel.removePropertyChangeListener(colorAdminModelListener);
        }
        this.colorAdminModel = colorAdminModel;
        if (colorAdminModel != null) {
            colorAdminModel.addPropertyChangeListener(colorAdminModelListener);
        }
        repaint();
    }

    public FColorAdminLayerStyle getColorAdminStyle() {
        return colorAdminStyle;
    }

    public void setColorAdminStyle(FColorAdminLayerStyle colorAdminStyle) {
        this.colorAdminStyle = colorAdminStyle != null ? colorAdminStyle : FColorAdminLayerStyle.defaults();
        repaint();
    }

    // ------------------------------------------------------------------
    // Navigation API
    // ------------------------------------------------------------------

    public void setView(double lon, double lat, double zoom) {
        model.setCenter(lon, lat);
        model.setZoom(zoom);
    }

    public void zoomIn() {
        zoomAround(getWidth() / 2.0, getHeight() / 2.0, +1.0);
    }

    public void zoomOut() {
        zoomAround(getWidth() / 2.0, getHeight() / 2.0, -1.0);
    }

    /** Aktives Werkzeug fuer Mausklicks (siehe {@link FMapTool}). Wechsel loescht laufende Messung/Popup. */
    public FMapTool getTool() {
        return currentTool;
    }

    public void setTool(FMapTool tool) {
        this.currentTool = tool != null ? tool : FMapTool.PAN;
        measurePoints.clear();
        identifyContent = null;
        repaint();
    }

    public void clearMeasurement() {
        measurePoints.clear();
        repaint();
    }

    public int getMeasurePointCount() {
        return measurePoints.size();
    }

    /** Gesamtstrecke der aktuellen Messung in Metern (Summe der Teilstrecken, Haversine). */
    public double getMeasureTotalMeters() {
        double total = 0;
        for (int i = 1; i < measurePoints.size(); i++) {
            double[] a = measurePoints.get(i - 1);
            double[] b = measurePoints.get(i);
            total += FMercatorMath.haversineMeters(a[0], a[1], b[0], b[1]);
        }
        return total;
    }

    private void addMeasurePoint(int screenX, int screenY) {
        measurePoints.add(new double[]{screenXToLon(screenX), screenYToLat(screenY)});
        repaint();
    }

    /** Zentrale Klick-Weiche nach Werkzeug - genutzt von der echten Maus UND von {@link #simulateClickForTest}. */
    private void handleClickRelease(int screenX, int screenY, boolean rightButton) {
        if (currentTool == FMapTool.MEASURE) {
            if (rightButton) {
                clearMeasurement();
            } else {
                addMeasurePoint(screenX, screenY);
            }
            return;
        }
        if (currentTool == FMapTool.IDENTIFY) {
            showIdentifyPopup(screenX, screenY);
        } else if (identifyContent != null) {
            identifyContent = null;
            repaint();
        }
        fireClicked(screenX, screenY);
    }

    private void showIdentifyPopup(int screenX, int screenY) {
        identifyAnchorX = screenX;
        identifyAnchorY = screenY;

        FeatureHit hit = hitTestVectorLayers(screenX, screenY);
        FFeatureRow rowHit = hit == null ? hitTestColorAdminLayer(screenX, screenY) : null;

        com.dan.ftooltip.FTooltipContent content;
        if (hit != null) {
            FGeoFeature f = hit.getFeature();
            content = new com.dan.ftooltip.FTooltipContent()
                    .withHeader(f.getName() != null ? f.getName() : ("Feature #" + f.getId()))
                    .withAccent(com.dan.ftooltip.FTooltipContent.ACCENT_INFO)
                    .addLine("Layer: " + hit.getLayer().getName())
                    .addLine("Geometrie: " + f.getGeometryType());
            if (f.getValue() != 0.0) {
                content = content.addLine("Wert: " + f.getValue());
            }
            for (Map.Entry<String, String> e : f.getAttributes().entrySet()) {
                content = content.addLine(e.getKey() + ": " + e.getValue());
            }
        } else if (rowHit != null) {
            FGeoFeature f = rowHit.getFeature();
            content = new com.dan.ftooltip.FTooltipContent()
                    .withHeader(f.getName() != null ? f.getName() : ("Feature #" + f.getId()))
                    .withAccent(com.dan.ftooltip.FTooltipContent.ACCENT_SUCCESS)
                    .addLine(rowHit.isColored() ? "Farbe: " + rowHit.getColor().getColorName() : "Keine ColorAdmin-Zuweisung")
                    .addLine(rowHit.isUnlimited() ? "Gueltigkeit: unbefristet"
                            : "Gueltig: " + rowHit.getValidFrom() + " bis " + rowHit.getValidTo());
        } else {
            content = com.dan.ftooltip.FTooltipContent.info("Kein Feature an dieser Position.");
        }
        identifyContent = content;
        repaint();
    }

    public void addSpatialMapListener(FSpatialMapListener l) {
        listeners.add(l);
    }

    public void removeSpatialMapListener(FSpatialMapListener l) {
        listeners.remove(l);
    }

    /**
     * Package-sichtbarer Test-Hook: simuliert einen Karten-Klick, ohne ein echtes
     * {@link MouseEvent} mit {@code getLocationOnScreen()} zu benoetigen (das
     * scheitert in einer Headless-JVM ohne Fenster-Peer). Nur fuer Tests gedacht.
     */
    void simulateClickForTest(int screenX, int screenY) {
        handleClickRelease(screenX, screenY, false);
    }

    /** Test-Hook: aktueller Identify-Popup-Inhalt (package-private, nur fuer Tests). */
    com.dan.ftooltip.FTooltipContent getIdentifyContentForTest() {
        return identifyContent;
    }

    private void fireClicked(int screenX, int screenY) {
        double lon = screenXToLon(screenX);
        double lat = screenYToLat(screenY);
        lastClickLon = lon;
        lastClickLat = lat;
        for (FSpatialMapListener l : listeners) {
            l.mapClicked(lon, lat);
        }
        FeatureHit hit = hitTestVectorLayers(screenX, screenY);
        if (hit != null) {
            for (FSpatialMapListener l : listeners) {
                l.featureClicked(hit.getLayer(), hit.getFeature());
            }
        }
        FFeatureRow rowHit = hitTestColorAdminLayer(screenX, screenY);
        if (rowHit != null) {
            for (FSpatialMapListener l : listeners) {
                l.featureRowClicked(rowHit);
            }
        }
        repaint();
    }

    private void fireViewChanged() {
        for (FSpatialMapListener l : listeners) {
            l.viewChanged(model.getCenterLon(), model.getCenterLat(), model.getZoom());
        }
    }

    // ------------------------------------------------------------------
    // Koordinatentransformation (weltraum-basiert auf displayZoom)
    // ------------------------------------------------------------------

    private double originWorldX() {
        return FMercatorMath.lonToWorldX(model.getCenterLon(), displayZoom) - getWidth() / 2.0;
    }

    private double originWorldY() {
        return FMercatorMath.latToWorldY(model.getCenterLat(), displayZoom) - getHeight() / 2.0;
    }

    public double screenXToLon(double sx) {
        return FMercatorMath.worldXToLon(originWorldX() + sx, displayZoom);
    }

    public double screenYToLat(double sy) {
        return FMercatorMath.worldYToLat(originWorldY() + sy, displayZoom);
    }

    public double lonToScreenX(double lon) {
        return FMercatorMath.lonToWorldX(lon, displayZoom) - originWorldX();
    }

    public double latToScreenY(double lat) {
        return FMercatorMath.latToWorldY(lat, displayZoom) - originWorldY();
    }

    // ------------------------------------------------------------------
    // Interaktion: Pan (Drag), Zoom (Rad + Buttons), Klick
    // ------------------------------------------------------------------

    private void installInteraction() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                pressX = e.getX();
                pressY = e.getY();
                pressCenterLon = model.getCenterLon();
                pressCenterLat = model.getCenterLat();
                dragging = false;
                if (isOverZoomControls(e.getX(), e.getY())) {
                    return;
                }
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                if (handleZoomControlClick(e.getX(), e.getY())) {
                    return;
                }
                if (dragging) {
                    return;
                }
                handleClickRelease(e.getX(), e.getY(), javax.swing.SwingUtilities.isRightMouseButton(e));
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - pressX;
                int dy = e.getY() - pressY;
                if (Math.abs(dx) > CLICK_TOLERANCE_PX || Math.abs(dy) > CLICK_TOLERANCE_PX) {
                    dragging = true;
                }
                double startWorldX = FMercatorMath.lonToWorldX(pressCenterLon, displayZoom);
                double startWorldY = FMercatorMath.latToWorldY(pressCenterLat, displayZoom);
                double newLon = FMercatorMath.worldXToLon(startWorldX - dx, displayZoom);
                double newLat = FMercatorMath.worldYToLat(startWorldY - dy, displayZoom);
                model.setCenter(newLon, newLat);
            }
        });
        addMouseWheelListener((MouseWheelListener) (MouseWheelEvent e) -> {
            double dir = -Math.signum(e.getPreciseWheelRotation());
            zoomAround(e.getX(), e.getY(), dir);
        });
    }

    private void zoomAround(double mouseX, double mouseY, double delta) {
        double oldZoom = displayZoom;
        double newTargetZoom = Math.max(0.0, Math.min(18.0, targetZoom + delta));
        if (newTargetZoom == targetZoom) {
            return;
        }
        double cursorWorldXOld = originWorldX() + mouseX;
        double cursorWorldYOld = originWorldY() + mouseY;
        double cursorLon = FMercatorMath.worldXToLon(cursorWorldXOld, oldZoom);
        double cursorLat = FMercatorMath.worldYToLat(cursorWorldYOld, oldZoom);

        double cursorWorldXNew = FMercatorMath.lonToWorldX(cursorLon, newTargetZoom);
        double cursorWorldYNew = FMercatorMath.latToWorldY(cursorLat, newTargetZoom);
        double desiredCenterWorldX = cursorWorldXNew - (mouseX - getWidth() / 2.0);
        double desiredCenterWorldY = cursorWorldYNew - (mouseY - getHeight() / 2.0);
        double newCenterLon = FMercatorMath.worldXToLon(desiredCenterWorldX, newTargetZoom);
        double newCenterLat = FMercatorMath.worldYToLat(desiredCenterWorldY, newTargetZoom);

        model.setCenter(newCenterLon, newCenterLat);
        model.setZoom(newTargetZoom);
    }

    private void ensureZoomTimerRunning() {
        if (zoomTimer == null) {
            zoomTimer = new Timer(16, e -> {
                displayZoom += (targetZoom - displayZoom) * ZOOM_EASE;
                if (Math.abs(targetZoom - displayZoom) < ZOOM_SETTLE) {
                    displayZoom = targetZoom;
                    zoomTimer.stop();
                }
                repaint();
                fireViewChanged();
            });
        }
        if (!zoomTimer.isRunning()) {
            zoomTimer.start();
        }
    }

    // ------------------------------------------------------------------
    // Zoom-Buttons (oben rechts, gemalt - kein separates JComponent noetig)
    // ------------------------------------------------------------------

    private java.awt.Rectangle zoomInBounds() {
        return new java.awt.Rectangle(getWidth() - 40, 12, 28, 28);
    }

    private java.awt.Rectangle zoomOutBounds() {
        return new java.awt.Rectangle(getWidth() - 40, 44, 28, 28);
    }

    private boolean isOverZoomControls(int x, int y) {
        return showZoomControls && (zoomInBounds().contains(x, y) || zoomOutBounds().contains(x, y));
    }

    private boolean handleZoomControlClick(int x, int y) {
        if (!showZoomControls) {
            return false;
        }
        if (zoomInBounds().contains(x, y)) {
            zoomIn();
            return true;
        }
        if (zoomOutBounds().contains(x, y)) {
            zoomOut();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            paintTiles(g2);
            if (vectorModel != null) {
                paintVectorLayers(g2);
            }
            if (colorAdminModel != null) {
                paintColorAdminLayer(g2);
            }
            if (!measurePoints.isEmpty()) {
                paintMeasurement(g2);
            }
            if (showCoordinateReadout) {
                paintCrosshairAndReadout(g2);
            }
            if (showZoomControls) {
                paintZoomControls(g2);
            }
            if (showAttribution) {
                paintAttribution(g2);
            }
            paintGlassRim(g2);
            if (identifyContent != null) {
                paintIdentifyPopup(g2);
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintTiles(Graphics2D g2) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        int tileZoom = (int) Math.round(displayZoom);
        tileZoom = Math.max(0, Math.min(18, tileZoom));
        double scale = Math.pow(2.0, displayZoom - tileZoom);
        int tilesPerSide = 1 << tileZoom;

        double originX = originWorldX();
        double originY = originWorldY();

        double tileWorldSize = FMercatorMath.TILE_SIZE * scale;
        int minTx = (int) Math.floor(originX / tileWorldSize) - 1;
        int maxTx = (int) Math.floor((originX + w) / tileWorldSize) + 1;
        int minTy = (int) Math.floor(originY / tileWorldSize) - 1;
        int maxTy = (int) Math.floor((originY + h) / tileWorldSize) + 1;

        FBasemapType basemap = model.getBasemap();
        for (int ty = minTy; ty <= maxTy; ty++) {
            if (ty < 0 || ty >= tilesPerSide) {
                continue;
            }
            for (int tx = minTx; tx <= maxTx; tx++) {
                int wrappedX = ((tx % tilesPerSide) + tilesPerSide) % tilesPerSide;
                double sx = tx * tileWorldSize - originX;
                double sy = ty * tileWorldSize - originY;
                BufferedImage img = cache.getFromMemory(basemap, tileZoom, wrappedX, ty);
                if (img != null) {
                    g2.drawImage(img, (int) Math.round(sx), (int) Math.round(sy),
                            (int) Math.ceil(tileWorldSize) + 1, (int) Math.ceil(tileWorldSize) + 1, null);
                } else {
                    paintTilePlaceholder(g2, sx, sy, tileWorldSize);
                    requestTile(basemap, tileZoom, wrappedX, ty);
                }
            }
        }
    }

    private void paintTilePlaceholder(Graphics2D g2, double sx, double sy, double size) {
        g2.setColor(new Color(0x1B, 0x25, 0x30));
        g2.fillRect((int) sx, (int) sy, (int) Math.ceil(size), (int) Math.ceil(size));
        g2.setColor(new Color(140, 200, 255, 40));
        g2.drawRect((int) sx, (int) sy, (int) size, (int) size);
    }

    private void requestTile(FBasemapType basemap, int z, int x, int y) {
        String key = FTileCache.key(basemap, z, x, y);
        if (!pendingLoads.add(key) || loaderPool == null) {
            return;
        }
        loaderPool.submit(() -> {
            try {
                BufferedImage img = cache.readFromDisk(basemap, z, x, y);
                if (img == null) {
                    img = fetcher.fetchTile(basemap, z, x, y);
                    cache.writeToDisk(basemap, z, x, y, img);
                }
                BufferedImage loaded = img;
                SwingUtilities.invokeLater(() -> {
                    cache.putMemory(basemap, z, x, y, loaded);
                    pendingLoads.remove(key);
                    repaint();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> pendingLoads.remove(key));
            }
        });
    }

    private void paintCrosshairAndReadout(Graphics2D g2) {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        g2.setColor(crosshairColor);
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawLine(cx - 7, cy, cx + 7, cy);
        g2.drawLine(cx, cy - 7, cx, cy + 7);

        String text = String.format(java.util.Locale.GERMANY, "%.5f\u00B0, %.5f\u00B0  \u00B7  Zoom %.1f",
                model.getCenterLon(), model.getCenterLat(), displayZoom);
        g2.setFont(getFont() != null ? getFont().deriveFont(Font.PLAIN, 11f) : new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(8, getHeight() - 26, g2.getFontMetrics().stringWidth(text) + 16, 18, 8, 8);
        g2.setColor(attributionColor);
        g2.drawString(text, 16, getHeight() - 12);
    }

    private void paintZoomControls(Graphics2D g2) {
        paintZoomButton(g2, zoomInBounds(), "+");
        paintZoomButton(g2, zoomOutBounds(), "\u2212");
    }

    private void paintZoomButton(Graphics2D g2, java.awt.Rectangle r, String label) {
        g2.setPaint(new java.awt.GradientPaint(r.x, r.y, new Color(0x00, 0xB5, 0xAD),
                r.x, r.y + r.height, new Color(0x8B, 0x00, 0x24)));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g2.setColor(glassRim);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g2.setColor(Color.WHITE);
        g2.setFont(getFont() != null ? getFont().deriveFont(Font.BOLD, 15f) : new Font("SansSerif", Font.BOLD, 15));
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(label)) / 2;
        int ty = r.y + (r.height + fm.getAscent()) / 2 - 2;
        g2.drawString(label, tx, ty);
    }

    private void paintAttribution(Graphics2D g2) {
        String text = model.getBasemap().getAttribution();
        g2.setFont(getFont() != null ? getFont().deriveFont(Font.PLAIN, 9.5f) : new Font("SansSerif", Font.PLAIN, 10));
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRect(getWidth() - tw - 14, getHeight() - 18, tw + 10, 16);
        g2.setColor(attributionColor);
        g2.drawString(text, getWidth() - tw - 9, getHeight() - 6);
    }

    private void paintGlassRim(Graphics2D g2) {
        g2.setColor(glassRim);
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }

    // ------------------------------------------------------------------
    // Werkzeuge (Phase 6): Messung + Identify-Popup
    // ------------------------------------------------------------------

    private void paintMeasurement(Graphics2D g2) {
        g2.setColor(crosshairColor);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[]{8f, 6f}, 0f));
        Path2D path = new Path2D.Double();
        for (int i = 0; i < measurePoints.size(); i++) {
            double sx = lonToScreenX(measurePoints.get(i)[0]);
            double sy = latToScreenY(measurePoints.get(i)[1]);
            if (i == 0) {
                path.moveTo(sx, sy);
            } else {
                path.lineTo(sx, sy);
            }
            g2.fill(new Ellipse2D.Double(sx - 4, sy - 4, 8, 8));
        }
        g2.draw(path);

        if (measurePoints.size() >= 2) {
            double[] last = measurePoints.get(measurePoints.size() - 1);
            String text = formatDistance(getMeasureTotalMeters());
            double lx = lonToScreenX(last[0]) + 12;
            double ly = latToScreenY(last[1]) - 10;
            g2.setFont(getFont() != null ? getFont().deriveFont(Font.BOLD, 12f) : new Font("SansSerif", Font.BOLD, 12));
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect((int) lx - 4, (int) ly - fm.getAscent() - 2, tw + 8, fm.getHeight() + 4, 6, 6);
            g2.setColor(Color.WHITE);
            g2.drawString(text, (float) lx, (float) ly);
        }
    }

    private static String formatDistance(double meters) {
        if (meters < 1000) {
            return Math.round(meters) + " m";
        }
        return String.format(java.util.Locale.GERMANY, "%.2f km", meters / 1000.0);
    }

    private void paintIdentifyPopup(Graphics2D g2) {
        identifyTooltip.setContent(identifyContent);
        java.awt.Dimension size = identifyTooltip.computeBubbleSize();
        identifyTooltip.setSize(size);

        int x = identifyAnchorX + 14;
        int y = identifyAnchorY + 14;
        if (x + size.width > getWidth()) {
            x = identifyAnchorX - size.width - 14;
        }
        if (y + size.height > getHeight()) {
            y = identifyAnchorY - size.height - 14;
        }
        Graphics2D popupG = (Graphics2D) g2.create(x, y, size.width, size.height);
        try {
            identifyTooltip.paint(popupG);
        } finally {
            popupG.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Vektor-Overlay (GIS-Layer) - eigenes Rendering, Web-Mercator-synchron
    // ------------------------------------------------------------------

    private void paintVectorLayers(Graphics2D g2) {
        // Sichtbaren Lon/Lat-Ausschnitt einmal pro Repaint berechnen (nicht pro Feature) -
        // 10% Puffer, damit am Rand anschneidende Features nicht vorzeitig verschwinden.
        double viewMinLon = screenXToLon(0);
        double viewMaxLon = screenXToLon(getWidth());
        double viewMinLat = screenYToLat(getHeight());
        double viewMaxLat = screenYToLat(0);
        double lonPad = (viewMaxLon - viewMinLon) * 0.1;
        double latPad = (viewMaxLat - viewMinLat) * 0.1;
        viewMinLon -= lonPad; viewMaxLon += lonPad;
        viewMinLat -= latPad; viewMaxLat += latPad;

        for (FGeoLayer layer : vectorModel.getLayers()) {
            if (!layer.isVisible()) {
                continue;
            }
            for (FGeoFeature feature : layer.getFeatures()) {
                if (feature.getMaxLon() < viewMinLon || feature.getMinLon() > viewMaxLon
                        || feature.getMaxLat() < viewMinLat || feature.getMinLat() > viewMaxLat) {
                    continue; // ausserhalb des sichtbaren Ausschnitts - billiger BBox-Check statt Projektion
                }
                paintFeature(g2, layer, feature);
            }
        }
    }

    private void paintFeature(Graphics2D g2, FGeoLayer layer, FGeoFeature feature) {
        float opacity = getLayerOpacity(layer);
        if (opacity <= 0f) {
            return;
        }
        FLayerGeometryType type = feature.getGeometryType();
        Color base = resolveFeatureColor(layer, feature);
        Color color = opacity >= 1f ? base : withAlpha(base, (base.getAlpha() / 255f) * opacity);
        float fillAlpha = layer.getFillAlpha() * opacity;
        if (type == FLayerGeometryType.POINT) {
            paintPointPrimitive(g2, feature, color, layer.getPointRadius());
        } else if (type == FLayerGeometryType.LINE) {
            paintLinePrimitive(g2, feature, color, layer.getStrokeWidth());
        } else if (type == FLayerGeometryType.POLYGON) {
            paintPolygonPrimitive(g2, feature, color, fillAlpha, layer.getStrokeWidth());
        }
        // RASTER/NETWORK: kein generisches Rendering in Phase 2 - eigene Layer-Typen
        // (z.B. Kachelraster, Netzwerkgraph) brauchen eigene Darstellungslogik.
    }

    /** Heatmap-Palette (falls ueber {@link #setLayerPalette} gesetzt) hat Vorrang vor der Layer-eigenen Farbe/Rampe. */
    private Color resolveFeatureColor(FGeoLayer layer, FGeoFeature feature) {
        PaletteDTO palette = heatmapPalettes.get(layer);
        if (palette == null) {
            return layer.colorFor(feature);
        }
        double t = FHeatmapApplier.normalize(feature.getValue(), layer.getMinValue(), layer.getMaxValue());
        ColorDTO c = palette.colorAt(t);
        return c != null ? c.toColor() : layer.colorFor(feature);
    }

    // ------------------------------------------------------------------
    // ColorAdmin-Live-Layer (Phase 3) - malt die Zeilen des aktuell in
    // FLayerStylerModel ausgewaehlten Layers, exakt wie im FLayerStyler-Panel gefaerbt
    // ------------------------------------------------------------------

    private void paintColorAdminLayer(Graphics2D g2) {
        List<FFeatureRow> rows = colorAdminModel.getFeatureRows();
        if (rows == null) {
            return;
        }
        for (FFeatureRow row : rows) {
            if (colorAdminStyle.respectValidity && !isRowCurrentlyValid(row)) {
                continue;
            }
            paintFeatureRow(g2, row);
        }
    }

    private void paintFeatureRow(Graphics2D g2, FFeatureRow row) {
        FGeoFeature feature = row.getFeature();
        Color color = row.isColored() ? row.getColor().toColor() : colorAdminStyle.uncoloredColor;
        FLayerGeometryType type = feature.getGeometryType();
        if (type == FLayerGeometryType.POINT) {
            paintPointPrimitive(g2, feature, color, colorAdminStyle.pointRadius);
        } else if (type == FLayerGeometryType.LINE) {
            paintLinePrimitive(g2, feature, color, colorAdminStyle.strokeWidth);
        } else if (type == FLayerGeometryType.POLYGON) {
            paintPolygonPrimitive(g2, feature, color, colorAdminStyle.fillAlpha, colorAdminStyle.strokeWidth);
        }
    }

    private boolean isRowCurrentlyValid(FFeatureRow row) {
        if (row.isUnlimited()) {
            return true;
        }
        java.time.LocalDate ref = colorAdminStyle.referenceDate != null
                ? colorAdminStyle.referenceDate : java.time.LocalDate.now();
        java.time.LocalDate from = row.getValidFrom();
        java.time.LocalDate to = row.getValidTo();
        if (from != null && ref.isBefore(from)) {
            return false;
        }
        if (to != null && ref.isAfter(to)) {
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Gemeinsame Geometrie-Malprimitive (Vektor-Overlay + ColorAdmin-Layer)
    // ------------------------------------------------------------------

    private void paintPointPrimitive(Graphics2D g2, FGeoFeature feature, Color color, double radius) {
        double sx = lonToScreenX(feature.getLon(0));
        double sy = latToScreenY(feature.getLat(0));
        g2.setColor(color);
        g2.fill(new Ellipse2D.Double(sx - radius, sy - radius, radius * 2, radius * 2));
        g2.setColor(glassRim);
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new Ellipse2D.Double(sx - radius, sy - radius, radius * 2, radius * 2));
    }

    private void paintLinePrimitive(Graphics2D g2, FGeoFeature feature, Color color, float strokeWidth) {
        Path2D path = featurePath(feature, false);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
    }

    private void paintPolygonPrimitive(Graphics2D g2, FGeoFeature feature, Color color, float fillAlpha, float strokeWidth) {
        Path2D path = featurePath(feature, true);
        g2.setColor(withAlpha(color, fillAlpha));
        g2.fill(path);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(strokeWidth));
        g2.draw(path);
    }

    private Path2D featurePath(FGeoFeature feature, boolean closed) {
        Path2D.Double path = new Path2D.Double();
        int n = feature.getPointCount();
        for (int i = 0; i < n; i++) {
            double sx = lonToScreenX(feature.getLon(i));
            double sy = latToScreenY(feature.getLat(i));
            if (i == 0) {
                path.moveTo(sx, sy);
            } else {
                path.lineTo(sx, sy);
            }
        }
        if (closed) {
            path.closePath();
        }
        return path;
    }

    private static Color withAlpha(Color c, float alpha01) {
        int a = Math.max(0, Math.min(255, Math.round(alpha01 * 255f)));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    /**
     * Sucht das oberste (zuletzt gemalte, d.h. letzter sichtbarer Layer) Feature
     * unter dem Klickpunkt. Punkte/Linien haben eine Toleranz von
     * {@link #HIT_TOLERANCE_PX}, Polygone werden per {@code contains()} getroffen.
     */
    private FeatureHit hitTestVectorLayers(int screenX, int screenY) {
        if (vectorModel == null) {
            return null;
        }
        List<FGeoLayer> layers = vectorModel.getLayers();
        for (int li = layers.size() - 1; li >= 0; li--) {
            FGeoLayer layer = layers.get(li);
            if (!layer.isVisible()) {
                continue;
            }
            for (FGeoFeature feature : layer.getFeatures()) {
                if (featureHit(feature, screenX, screenY)) {
                    return new FeatureHit(layer, feature);
                }
            }
        }
        return null;
    }

    private boolean featureHit(FGeoFeature feature, int screenX, int screenY) {
        FLayerGeometryType type = feature.getGeometryType();
        if (type == FLayerGeometryType.POINT) {
            double sx = lonToScreenX(feature.getLon(0));
            double sy = latToScreenY(feature.getLat(0));
            return Point2D.distance(sx, sy, screenX, screenY) <= HIT_TOLERANCE_PX + 4;
        } else if (type == FLayerGeometryType.LINE) {
            int n = feature.getPointCount();
            for (int i = 0; i < n - 1; i++) {
                double x1 = lonToScreenX(feature.getLon(i));
                double y1 = latToScreenY(feature.getLat(i));
                double x2 = lonToScreenX(feature.getLon(i + 1));
                double y2 = latToScreenY(feature.getLat(i + 1));
                if (Line2D.ptSegDist(x1, y1, x2, y2, screenX, screenY) <= HIT_TOLERANCE_PX) {
                    return true;
                }
            }
            return false;
        } else if (type == FLayerGeometryType.POLYGON) {
            return featurePath(feature, true).contains(screenX, screenY);
        }
        return false;
    }

    /** Sucht die oberste ColorAdmin-Zeile unter dem Klickpunkt (letzte Zeile zuerst = zuletzt gemalt). */
    private FFeatureRow hitTestColorAdminLayer(int screenX, int screenY) {
        if (colorAdminModel == null) {
            return null;
        }
        List<FFeatureRow> rows = colorAdminModel.getFeatureRows();
        if (rows == null) {
            return null;
        }
        for (int i = rows.size() - 1; i >= 0; i--) {
            FFeatureRow row = rows.get(i);
            if (colorAdminStyle.respectValidity && !isRowCurrentlyValid(row)) {
                continue;
            }
            if (featureHit(row.getFeature(), screenX, screenY)) {
                return row;
            }
        }
        return null;
    }

    /** Ergebnis eines Klick-Hit-Tests gegen das Vektor-Overlay. */
    public static final class FeatureHit {
        private final FGeoLayer layer;
        private final FGeoFeature feature;

        FeatureHit(FGeoLayer layer, FGeoFeature feature) {
            this.layer = layer;
            this.feature = feature;
        }

        public FGeoLayer getLayer() {
            return layer;
        }

        public FGeoFeature getFeature() {
            return feature;
        }
    }
}
