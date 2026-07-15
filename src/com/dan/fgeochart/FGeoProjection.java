package com.dan.fgeochart;

/**
 * Verfuegbare Kartenprojektionen des FGeoChart.
 *
 * EQUIRECTANGULAR: Plate Carree mit cos(lat0)-Korrektur — schnell und fuer
 * regionale Karten (z. B. Timmendorfer Strand) voellig ausreichend.
 * MERCATOR: Web-Mercator-Y-Streckung — formtreuer bei groesseren Ausschnitten.
 */
public enum FGeoProjection {

    EQUIRECTANGULAR("Plate Carrée"),
    MERCATOR("Mercator");

    private final String label;

    FGeoProjection(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Projiziert Breitengrad auf eine vertikale Achse (vor Skalierung). */
    public double projectLat(double latDeg) {
        if (this == MERCATOR) {
            double phi = Math.toRadians(Math.max(-85.0, Math.min(85.0, latDeg)));
            return Math.toDegrees(Math.log(Math.tan(Math.PI / 4.0 + phi / 2.0)));
        }
        return latDeg;
    }

    /** Umkehrung von {@link #projectLat(double)}. */
    public double unprojectLat(double projected) {
        if (this == MERCATOR) {
            double y = Math.toRadians(projected);
            return Math.toDegrees(2.0 * Math.atan(Math.exp(y)) - Math.PI / 2.0);
        }
        return projected;
    }
}
