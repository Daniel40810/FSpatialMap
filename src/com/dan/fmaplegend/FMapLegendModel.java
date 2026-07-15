package com.dan.fmaplegend;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Datenvertrag der FMapLegend: geordnete Liste von Layer-Eintraegen.
 * Aenderungen propagieren ueber PropertyChangeSupport mit den
 * Property-Namen "entries" (Strukturaenderung) und "visibility".
 */
public interface FMapLegendModel {

    List<FLayerEntry> getEntries();

    FLayerEntry findByLayerName(String layerName);

    int size();

    /** Sichtbarkeit umschalten; feuert "visibility" mit dem Eintrag als newValue. */
    void setLayerVisible(String layerName, boolean visible);

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);
}
