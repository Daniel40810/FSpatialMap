package com.dan.fdal.sdo;

import com.dan.fgeochart.FGeoFeature;
import java.sql.Array;
import java.sql.SQLException;
import java.sql.Struct;

/**
 * Wandelt eine Oracle {@code SDO_GEOMETRY}-Spalte (gelesen als generisches
 * {@link java.sql.Struct}, kein Treiber-spezifischer {@code oracle.sql.STRUCT}
 * noetig) in ein {@link FGeoFeature} um.
 * <p>
 * <b>Abgedeckt (Phase 2):</b> einfache, nicht-zusammengesetzte 2D/3D-Geometrien -
 * POINT (SDO_GTYPE-Code 1), LINESTRING (Code 2, ein Element) und POLYGON
 * (Code 3, nur Außenring, keine Loecher). Z-Koordinaten werden gelesen, aber
 * verworfen (FGeoFeature ist 2D/lon-lat).
 * <p>
 * <b>Nicht abgedeckt:</b> zusammengesetzte Kurven (Kreisboegen), Polygone mit
 * Loechern, MULTI*-Geometrien und Geometry Collections - dafuer wirft
 * {@link #toFeature(long, String, Struct)} eine {@link SdoParseException} mit
 * Klartext-Grund, statt stillschweigend falsche Geometrie zu erzeugen.
 */
public final class SdoGeometryParser {

    private SdoGeometryParser() {
    }

    public static FGeoFeature toFeature(long id, String name, Struct sdoGeometry) {
        if (sdoGeometry == null) {
            return null;
        }
        try {
            Object[] attrs = sdoGeometry.getAttributes();
            if (attrs.length < 5) {
                throw new SdoParseException("SDO_GEOMETRY-Struct hat nur " + attrs.length
                        + " Attribute, erwartet 5 (GTYPE, SRID, POINT, ELEM_INFO, ORDINATES).");
            }
            long gtype = toLong(attrs[0]);
            int dims = (int) (gtype / 1000);
            int typeCode = (int) (gtype % 100);
            if (dims < 2) {
                dims = 2;
            }

            switch (typeCode) {
                case 1:
                    return toPoint(id, name, attrs, dims);
                case 2:
                    return toLine(id, name, attrs, dims);
                case 3:
                    return toPolygon(id, name, attrs, dims);
                default:
                    throw new SdoParseException("Nicht unterstuetzter SDO_GTYPE-Code " + typeCode
                            + " (nur POINT=1, LINE=2, POLYGON=3 - keine MULTI*/Collection).");
            }
        } catch (SQLException ex) {
            throw new SdoParseException("SDO_GEOMETRY konnte nicht gelesen werden (gid=" + id + ")", ex);
        }
    }

    private static FGeoFeature toPoint(long id, String name, Object[] attrs, int dims) throws SQLException {
        if (attrs[2] instanceof Struct) {
            Object[] p = ((Struct) attrs[2]).getAttributes();
            double x = toDouble(p[0]);
            double y = toDouble(p[1]);
            return FGeoFeature.point(id, name, x, y);
        }
        double[] pairs = readOrdinatesAsLonLat(attrs[4], dims);
        if (pairs.length < 2) {
            throw new SdoParseException("POINT-Geometrie ohne Koordinaten (gid=" + id + ").");
        }
        return FGeoFeature.point(id, name, pairs[0], pairs[1]);
    }

    private static FGeoFeature toLine(long id, String name, Object[] attrs, int dims) throws SQLException {
        requireSimpleSingleElement(attrs[3], id, "LINE", 2);
        double[] pairs = readOrdinatesAsLonLat(attrs[4], dims);
        return FGeoFeature.line(id, name, pairs);
    }

    private static FGeoFeature toPolygon(long id, String name, Object[] attrs, int dims) throws SQLException {
        int[] elemInfo = readElemInfo(attrs[3]);
        if (elemInfo.length != 3 || (elemInfo[1] != 1003 && elemInfo[1] != 3)) {
            throw new SdoParseException("POLYGON-Geometrie mit Loechern oder mehreren Elementen "
                    + "wird in Phase 2 nicht unterstuetzt (gid=" + id + ", SDO_ELEM_INFO-Laenge="
                    + elemInfo.length + "). Nur einfacher Aussenring.");
        }
        double[] pairs = readOrdinatesAsLonLat(attrs[4], dims);
        return FGeoFeature.polygon(id, name, pairs);
    }

    private static void requireSimpleSingleElement(Object elemInfoAttr, long id, String kind, int expectedInterpretation)
            throws SQLException {
        int[] elemInfo = readElemInfo(elemInfoAttr);
        if (elemInfo.length != 3 || elemInfo[2] != expectedInterpretation) {
            throw new SdoParseException(kind + "-Geometrie ist zusammengesetzt (compound curve o.ae.) "
                    + "und wird in Phase 2 nicht unterstuetzt (gid=" + id + ").");
        }
    }

    private static int[] readElemInfo(Object elemInfoAttr) throws SQLException {
        if (!(elemInfoAttr instanceof Array)) {
            throw new SdoParseException("SDO_ELEM_INFO fehlt oder hat unerwarteten Typ.");
        }
        Object raw = ((Array) elemInfoAttr).getArray();
        int len = java.lang.reflect.Array.getLength(raw);
        int[] out = new int[len];
        for (int i = 0; i < len; i++) {
            out[i] = (int) toLong(java.lang.reflect.Array.get(raw, i));
        }
        return out;
    }

    private static double[] readOrdinatesAsLonLat(Object ordinatesAttr, int dims) throws SQLException {
        if (!(ordinatesAttr instanceof Array)) {
            throw new SdoParseException("SDO_ORDINATES fehlt oder hat unerwarteten Typ.");
        }
        Object raw = ((Array) ordinatesAttr).getArray();
        int len = java.lang.reflect.Array.getLength(raw);
        int n = len / dims;
        double[] out = new double[n * 2];
        for (int i = 0; i < n; i++) {
            out[i * 2] = toDouble(java.lang.reflect.Array.get(raw, i * dims));
            out[i * 2 + 1] = toDouble(java.lang.reflect.Array.get(raw, i * dims + 1));
        }
        return out;
    }

    private static double toDouble(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        throw new SdoParseException("Unerwarteter Ordinaten-Werttyp: " + (o == null ? "null" : o.getClass()));
    }

    private static long toLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        throw new SdoParseException("Unerwarteter Zahlenwert-Typ: " + (o == null ? "null" : o.getClass()));
    }
}
