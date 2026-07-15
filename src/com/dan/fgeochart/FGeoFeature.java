package com.dan.fgeochart;

import com.dan.fmaplegend.FLayerGeometryType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ein einzelnes Geo-Feature: Punkt, Linie oder Polygon in WGS84-Koordinaten
 * (Lon/Lat, SRID 8307). Koordinaten liegen als flaches double-Array vor:
 * [lon0, lat0, lon1, lat1, ...].
 *
 * Immutable bis auf die Attribut-Map (defensive Kopie beim Bau).
 */
public final class FGeoFeature {

    private final long id;
    private final String name;
    private final FLayerGeometryType geometryType;
    private final double[] coords;
    private final Map<String, String> attributes;
    private final double value;

    private final double minLon, minLat, maxLon, maxLat;

    public FGeoFeature(long id, String name, FLayerGeometryType geometryType,
                       double[] lonLatPairs, double value, Map<String, String> attributes) {
        if (lonLatPairs == null || lonLatPairs.length < 2 || lonLatPairs.length % 2 != 0) {
            throw new IllegalArgumentException("lonLatPairs muss gerade Laenge >= 2 haben");
        }
        this.id = id;
        this.name = name == null ? "" : name;
        this.geometryType = geometryType == null ? FLayerGeometryType.POINT : geometryType;
        this.coords = lonLatPairs.clone();
        this.value = value;
        Map<String, String> attr = new LinkedHashMap<String, String>();
        if (attributes != null) {
            attr.putAll(attributes);
        }
        this.attributes = Collections.unmodifiableMap(attr);

        double lo = Double.MAX_VALUE, la = Double.MAX_VALUE;
        double hlo = -Double.MAX_VALUE, hla = -Double.MAX_VALUE;
        for (int i = 0; i < coords.length; i += 2) {
            if (coords[i] < lo)      lo = coords[i];
            if (coords[i] > hlo)     hlo = coords[i];
            if (coords[i + 1] < la)  la = coords[i + 1];
            if (coords[i + 1] > hla) hla = coords[i + 1];
        }
        this.minLon = lo; this.maxLon = hlo;
        this.minLat = la; this.maxLat = hla;
    }

    /** Convenience: einzelner Punkt. */
    public static FGeoFeature point(long id, String name, double lon, double lat) {
        return new FGeoFeature(id, name, FLayerGeometryType.POINT,
                new double[]{lon, lat}, 0.0, null);
    }

    /** Convenience: einzelner Punkt mit Wert (fuer Farb-Mapping). */
    public static FGeoFeature point(long id, String name, double lon, double lat, double value) {
        return new FGeoFeature(id, name, FLayerGeometryType.POINT,
                new double[]{lon, lat}, value, null);
    }

    /** Convenience: Linienzug. */
    public static FGeoFeature line(long id, String name, double[] lonLatPairs) {
        return new FGeoFeature(id, name, FLayerGeometryType.LINE, lonLatPairs, 0.0, null);
    }

    /** Convenience: geschlossenes Polygon (erster Punkt muss nicht wiederholt werden). */
    public static FGeoFeature polygon(long id, String name, double[] lonLatPairs) {
        return new FGeoFeature(id, name, FLayerGeometryType.POLYGON, lonLatPairs, 0.0, null);
    }

    public long getId()                          { return id; }
    public String getName()                      { return name; }
    public FLayerGeometryType getGeometryType()  { return geometryType; }
    public double getValue()                     { return value; }
    public Map<String, String> getAttributes()   { return attributes; }

    /** Anzahl Koordinatenpaare. */
    public int getPointCount()                   { return coords.length / 2; }
    public double getLon(int index)              { return coords[index * 2]; }
    public double getLat(int index)              { return coords[index * 2 + 1]; }

    public double getMinLon() { return minLon; }
    public double getMinLat() { return minLat; }
    public double getMaxLon() { return maxLon; }
    public double getMaxLat() { return maxLat; }

    /** Schwerpunkt (einfaches Mittel der Stuetzpunkte). */
    public double getCenterLon() { return (minLon + maxLon) / 2.0; }
    public double getCenterLat() { return (minLat + maxLat) / 2.0; }

    @Override
    public String toString() {
        return "FGeoFeature{id=" + id + ", name='" + name + "', type="
                + geometryType + ", points=" + getPointCount() + "}";
    }
}
