package com.dan.fmaplegend;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Mutable Standard-Implementierung von FMapLegendModel.
 *
 * DAL-Bruecke: {@link #applyFeatureColors(String, Map)} nimmt direkt das
 * Ergebnis von {@code ColorAdminDAO.loadLayerColorMap(layer)} entgegen und
 * setzt die distinkten Feature-Farben als Verlaufsbalken des Eintrags.
 */
public class DefaultFMapLegendModel implements FMapLegendModel {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final List<FLayerEntry> entries = new ArrayList<FLayerEntry>();

    public DefaultFMapLegendModel() {
    }

    // ------------------------------------------------------------------
    // Struktur
    // ------------------------------------------------------------------

    public void addLayer(FLayerEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("entry darf nicht null sein");
        }
        if (findByLayerName(entry.getLayerName()) != null) {
            throw new IllegalArgumentException(
                    "Layer bereits vorhanden: " + entry.getLayerName());
        }
        entries.add(entry);
        pcs.firePropertyChange("entries", null, entry);
    }

    /** Bequem-Variante fuer den haeufigsten Fall. */
    public FLayerEntry addLayer(String layerName, String displayName,
                                FLayerGeometryType type, Color color) {
        FLayerEntry e = new FLayerEntry(layerName, displayName, type, color);
        addLayer(e);
        return e;
    }

    public void removeLayer(String layerName) {
        FLayerEntry e = findByLayerName(layerName);
        if (e != null) {
            entries.remove(e);
            pcs.firePropertyChange("entries", e, null);
        }
    }

    public void clear() {
        if (!entries.isEmpty()) {
            entries.clear();
            pcs.firePropertyChange("entries", null, null);
        }
    }

    // ------------------------------------------------------------------
    // DAL-Bruecke
    // ------------------------------------------------------------------

    /**
     * Uebernimmt das Ergebnis von ColorAdminDAO.loadLayerColorMap(layer):
     * die distinkten Feature-Farben werden zum Verlaufsbalken des Eintrags,
     * die Feature-Anzahl wird gesetzt.
     */
    public void applyFeatureColors(String layerName, Map<String, Color> featureColorMap) {
        FLayerEntry e = findByLayerName(layerName);
        if (e == null || featureColorMap == null) {
            return;
        }
        e.setBarColors(featureColorMap.values());
        e.setFeatureCount(featureColorMap.size());
        pcs.firePropertyChange("entries", null, e);
    }

    // ------------------------------------------------------------------
    // FMapLegendModel
    // ------------------------------------------------------------------

    @Override
    public List<FLayerEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public FLayerEntry findByLayerName(String layerName) {
        if (layerName == null) {
            return null;
        }
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getLayerName().equalsIgnoreCase(layerName)) {
                return entries.get(i);
            }
        }
        return null;
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public void setLayerVisible(String layerName, boolean visible) {
        FLayerEntry e = findByLayerName(layerName);
        if (e == null || e.isVisible() == visible) {
            return;
        }
        e.setVisible(visible);
        pcs.firePropertyChange("visibility", null, e);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}
