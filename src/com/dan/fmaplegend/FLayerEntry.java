package com.dan.fmaplegend;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Ein Layer-Eintrag der FMapLegend.
 *
 * Der Farbbalken kann eine oder mehrere Farben zeigen — bei mehreren wird
 * ein Verlauf gemalt (z. B. die distinkten Feature-Farben eines Layers aus
 * ColorAdminDAO.loadLayerColorMap()).
 */
public final class FLayerEntry {

    private final String layerName;          // technischer Name (z. B. "STADTTEILE_KOELN")
    private String displayName;              // Anzeigename
    private FLayerGeometryType geometryType;
    private List<Color> barColors = new ArrayList<Color>();
    private long featureCount = -1;          // -1 = unbekannt
    private String boundsText = null;        // z. B. "10.6°E .. 10.9°E / 53.9°N .. 54.1°N"
    private boolean visible = true;

    public FLayerEntry(String layerName, FLayerGeometryType type, Color color) {
        this(layerName, layerName, type, color);
    }

    public FLayerEntry(String layerName, String displayName,
                       FLayerGeometryType type, Color color) {
        if (layerName == null || layerName.trim().isEmpty()) {
            throw new IllegalArgumentException("layerName darf nicht leer sein");
        }
        this.layerName = layerName;
        this.displayName = (displayName != null) ? displayName : layerName;
        this.geometryType = (type != null) ? type : FLayerGeometryType.POLYGON;
        if (color != null) {
            this.barColors.add(color);
        }
    }

    // ------------------------------------------------------------------

    public String getLayerName() {
        return layerName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = (displayName != null) ? displayName : layerName;
    }

    public FLayerGeometryType getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(FLayerGeometryType type) {
        this.geometryType = (type != null) ? type : FLayerGeometryType.POLYGON;
    }

    /** Unveraenderliche Farbliste des Balkens (evtl. leer). */
    public List<Color> getBarColors() {
        return Collections.unmodifiableList(barColors);
    }

    public void setBarColor(Color c) {
        barColors.clear();
        if (c != null) {
            barColors.add(c);
        }
    }

    /**
     * Setzt mehrere Balkenfarben (Verlauf). Duplikate werden entfernt,
     * Reihenfolge bleibt erhalten. Praktisch fuer die distinkten Werte aus
     * {@code dao.loadLayerColorMap(layer).values()}.
     */
    public void setBarColors(Collection<Color> colors) {
        barColors.clear();
        if (colors != null) {
            barColors.addAll(new LinkedHashSet<Color>(colors));
        }
    }

    public long getFeatureCount() {
        return featureCount;
    }

    public void setFeatureCount(long featureCount) {
        this.featureCount = featureCount;
    }

    public String getBoundsText() {
        return boundsText;
    }

    public void setBoundsText(String boundsText) {
        this.boundsText = boundsText;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public String toString() {
        return "FLayerEntry[" + layerName + ", " + geometryType + ", visible=" + visible + "]";
    }
}
