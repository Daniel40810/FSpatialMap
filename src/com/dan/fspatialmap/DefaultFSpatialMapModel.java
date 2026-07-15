package com.dan.fspatialmap;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Default-Implementierung von {@link FSpatialMapModel}.
 * Startzustand: Kiel-Zentrum, Zoom 11, farbige DTK25.
 */
public class DefaultFSpatialMapModel implements FSpatialMapModel {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private double centerLon = 10.1228;
    private double centerLat = 54.3233;
    private double zoom = 11.0;
    private FBasemapType basemap = FBasemapType.DTK25_FARBE;

    @Override
    public double getCenterLon() {
        return centerLon;
    }

    @Override
    public double getCenterLat() {
        return centerLat;
    }

    @Override
    public double getZoom() {
        return zoom;
    }

    @Override
    public FBasemapType getBasemap() {
        return basemap;
    }

    @Override
    public void setCenter(double lon, double lat) {
        double oldLon = this.centerLon;
        double oldLat = this.centerLat;
        this.centerLon = lon;
        this.centerLat = Math.max(-85.05112878, Math.min(85.05112878, lat));
        pcs.firePropertyChange("centerLon", oldLon, this.centerLon);
        pcs.firePropertyChange("centerLat", oldLat, this.centerLat);
    }

    @Override
    public void setZoom(double zoom) {
        double old = this.zoom;
        this.zoom = Math.max(0.0, Math.min(18.0, zoom));
        pcs.firePropertyChange("zoom", old, this.zoom);
    }

    @Override
    public void setBasemap(FBasemapType basemap) {
        FBasemapType old = this.basemap;
        this.basemap = basemap;
        pcs.firePropertyChange("basemap", old, basemap);
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
