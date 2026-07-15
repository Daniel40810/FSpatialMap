package com.dan.fspatialmap;

import java.beans.PropertyChangeListener;

/**
 * Vertrag fuer den Kartenzustand von {@link FSpatialMapViewer}: Kartenmitte,
 * Zoomstufe und aktive Basiskarte. Aenderungen propagieren ueber
 * {@link PropertyChangeListener} ("centerLon", "centerLat", "zoom", "basemap").
 */
public interface FSpatialMapModel {

    double getCenterLon();

    double getCenterLat();

    /** Zoomstufe, 0 (Welt) .. 18 (Grundstueck), fraktional fuer weiche Animation. */
    double getZoom();

    FBasemapType getBasemap();

    void setCenter(double lon, double lat);

    void setZoom(double zoom);

    void setBasemap(FBasemapType basemap);

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);
}
