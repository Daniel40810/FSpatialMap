package com.dan.fspatialmap;

/**
 * Event-Vertrag fuer Interaktionen mit {@link FSpatialMapViewer}.
 * Beide Methoden haben Default-Leerimplementierungen, damit Aufrufer nur
 * das brauchen, was sie interessiert (Lambda-tauglich ueber Einzelmethode
 * ist wegen zwei Methoden nicht moeglich, daher Adapter-Stil per default).
 */
public interface FSpatialMapListener {

    /** Einfacher Klick (kein Drag) auf die Karte, in WGS84-Grad. */
    default void mapClicked(double lon, double lat) {
    }

    /**
     * Zusaetzlich zu {@link #mapClicked} gefeuert, wenn der Klick ein Feature
     * des Vektor-Overlays getroffen hat (siehe {@link FSpatialMapViewer#setVectorModel}).
     */
    default void featureClicked(com.dan.fgeochart.FGeoLayer layer, com.dan.fgeochart.FGeoFeature feature) {
    }

    /**
     * Zusaetzlich zu {@link #mapClicked} gefeuert, wenn der Klick eine Zeile des
     * ColorAdmin-Live-Layers getroffen hat (siehe {@link FSpatialMapViewer#setColorAdminModel}).
     * Eignet sich, um per Kartenklick die Auswahl in einem begleitenden
     * {@code FLayerStyler}-Panel zu setzen (Karte -&gt; Editor, zusaetzlich zu Editor -&gt; Karte).
     */
    default void featureRowClicked(com.dan.flayerstyler.FFeatureRow row) {
    }

    /** Kartenmitte und/oder Zoom haben sich geaendert (Pan/Zoom, auch waehrend Animation). */
    default void viewChanged(double centerLon, double centerLat, double zoom) {
    }
}
