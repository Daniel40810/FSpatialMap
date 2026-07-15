package com.dan.fgeochart;

import com.dan.fmaplegend.FLayerGeometryType;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable Default-Implementierung des {@link FGeoChartModel}.
 * Aenderungen feuern PropertyChangeEvents ("layers"), auf die
 * das FGeoChart mit Repaint/Reveal-Animation reagiert.
 */
public class DefaultFGeoChartModel implements FGeoChartModel {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final List<FGeoLayer> layers = new ArrayList<FGeoLayer>();

    public DefaultFGeoChartModel() {
    }

    /** Fuegt einen Layer hinzu und feuert "layers". */
    public void addLayer(FGeoLayer layer) {
        if (layer != null) {
            layers.add(layer);
            pcs.firePropertyChange("layers", null, layer);
        }
    }

    /** Convenience: leeren Layer anlegen, hinzufuegen und zurueckgeben. */
    public FGeoLayer addLayer(String name, FLayerGeometryType type, java.awt.Color color) {
        FGeoLayer l = new FGeoLayer(name, type);
        if (color != null) {
            l.setColor(color);
        }
        addLayer(l);
        return l;
    }

    public void removeLayer(String name) {
        FGeoLayer found = findLayer(name);
        if (found != null) {
            layers.remove(found);
            pcs.firePropertyChange("layers", found, null);
        }
    }

    public void clear() {
        if (!layers.isEmpty()) {
            layers.clear();
            pcs.firePropertyChange("layers", null, null);
        }
    }

    /** Nach Bulk-Aenderungen an Layer-Inhalten manuell Neuzeichnung anstossen. */
    public void fireDataChanged() {
        pcs.firePropertyChange("layers", null, layers);
    }

    @Override
    public int getLayerCount() {
        return layers.size();
    }

    @Override
    public FGeoLayer getLayer(int index) {
        return layers.get(index);
    }

    @Override
    public List<FGeoLayer> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    @Override
    public FGeoLayer findLayer(String name) {
        for (FGeoLayer l : layers) {
            if (l.getName().equals(name)) {
                return l;
            }
        }
        return null;
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
