package com.dan.fgeochart;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Datenvertrag des FGeoChart: eine geordnete Menge von Layern.
 * Die Komponente rendert ausschliesslich aus diesem Model.
 */
public interface FGeoChartModel {

    int getLayerCount();

    FGeoLayer getLayer(int index);

    List<FGeoLayer> getLayers();

    FGeoLayer findLayer(String name);

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);
}
