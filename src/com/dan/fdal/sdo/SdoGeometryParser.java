package com.dan.fdal.sdo;

import com.dan.fgeochart.FGeoFeature;
import java.sql.Array;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wandelt eine Oracle {@code SDO_GEOMETRY}-Spalte (gelesen als generisches
 * {@link java.sql.Struct}, kein Treiber-spezifischer {@code oracle.sql.STRUCT}
 * noetig) in {@link FGeoFeature}s um.
 * <p>
 * <b>Abgedeckt:</b> POINT (SDO_GTYPE-Code 1), LINESTRING (Code 2, ein
 * einfaches Element) und POLYGON/MULTIPOLYGON (Code 3 und 7) mit beliebig
 * vielen Aussenringen. Jeder Aussenring (SDO_ETYPE 1003) wird als eigenes
 * {@link FGeoFeature} zurueckgegeben (gleiche id/name) - so werden auch
 * "echte" Multipolygone (z.B. eine Gemeinde mit Exklave) korrekt als mehrere
 * Flaechen gezeichnet. <b>Innenringe/Loecher (SDO_ETYPE 2003) werden bewusst
 * verworfen</b> statt gerendert (FGeoFeature kennt nur einen einzelnen Ring
 * pro Feature) - ein Ausschnitt mit einer Enklave wird also als volle
 * Flaeche statt mit Aussparung gezeichnet. Z-Koordinaten werden gelesen,
 * aber verworfen (FGeoFeature ist 2D/lon-lat).
 * <p>
 * <b>Nicht abgedeckt:</b> zusammengesetzte Kurven (Kreisboegen, SDO_ETYPE
 * 1005/2005 mit Bogensegmenten) und Geometry Collections - dafuer wirft
 * {@link #toFeatures(long, String, Struct)} eine {@link SdoParseException}
 * mit Klartext-Grund, statt stillschweigend falsche Geometrie zu erzeugen.
 */
public final class SdoGeometryParser {

    private static final int ETYPE_EXTERIOR_RING = 1003;
    private static final int ETYPE_INTERIOR_RING = 2003;

    private SdoGeometryParser() {
    }

    /**
     * @return 0..n Features fuer eine DB-Zeile (n &gt; 1 bei Multipolygonen/
     *         Polygonen mit mehreren Aussenringen), nie {@code null}.
     */
    public static List<FGeoFeature> toFeatures(long id, String name, Struct sdoGeometry) {
        if (sdoGeometry == null) {
            return Collections.emptyList();
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
                case 7: // MULTIPOLYGON - strukturell identisch zu POLYGON (mehrere Aussenringe)
                    return toPolygons(id, name, attrs, dims);
                default:
                    throw new SdoParseException("Nicht unterstuetzter SDO_GTYPE-Code " + typeCode
                            + " (nur POINT=1, LINE=2, POLYGON=3, MULTIPOLYGON=7).");
            }
        } catch (SQLException ex) {
            throw new SdoParseException("SDO_GEOMETRY konnte nicht gelesen werden (gid=" + id + ")", ex);
        }
    }

    /** @deprecated liefert nur das erste Feature - Aufrufer sollten {@link #toFeatures} nutzen. */
    @Deprecated
    public static FGeoFeature toFeature(long id, String name, Struct sdoGeometry) {
        List<FGeoFeature> features = toFeatures(id, name, sdoGeometry);
        return features.isEmpty() ? null : features.get(0);
    }

    private static List<FGeoFeature> toPoint(long id, String name, Object[] attrs, int dims) throws SQLException {
        if (attrs[2] instanceof Struct) {
            Object[] p = ((Struct) attrs[2]).getAttributes();
            double x = toDouble(p[0]);
            double y = toDouble(p[1]);
            return Collections.singletonList(FGeoFeature.point(id, name, x, y));
        }
        double[] flat = readOrdinates(attrs[4]);
        if (flat.length < dims) {
            throw new SdoParseException("POINT-Geometrie ohne Koordinaten (gid=" + id + ").");
        }
        double[] pairs = toLonLatPairs(flat, 0, 1, dims);
        return Collections.singletonList(FGeoFeature.point(id, name, pairs[0], pairs[1]));
    }

    private static List<FGeoFeature> toLine(long id, String name, Object[] attrs, int dims) throws SQLException {
        int[] elemInfo = readElemInfo(attrs[3]);
        if (elemInfo.length != 3 || elemInfo[2] != 2) {
            throw new SdoParseException("LINE-Geometrie ist zusammengesetzt (compound curve o.ae.) "
                    + "oder hat mehrere Elemente - nicht unterstuetzt (gid=" + id + ").");
        }
        double[] flat = readOrdinates(attrs[4]);
        double[] pairs = toLonLatPairs(flat, 0, flat.length / dims, dims);
        return Collections.singletonList(FGeoFeature.line(id, name, pairs));
    }

    /**
     * Walkt SDO_ELEM_INFO komplett durch: jeder Aussenring (ETYPE 1003)
     * ergibt ein eigenes FGeoFeature; Innenringe (ETYPE 2003, Loecher)
     * werden uebersprungen. Deckt damit POLYGON-mit-Loechern genauso ab
     * wie echte MULTIPOLYGONE (mehrere Aussenringe hintereinander).
     */
    private static List<FGeoFeature> toPolygons(long id, String name, Object[] attrs, int dims) throws SQLException {
        int[] elemInfo = readElemInfo(attrs[3]);
        if (elemInfo.length == 0 || elemInfo.length % 3 != 0) {
            throw new SdoParseException("SDO_ELEM_INFO hat ungueltige Laenge " + elemInfo.length
                    + " (gid=" + id + ").");
        }
        double[] flat = readOrdinates(attrs[4]);
        int totalPoints = flat.length / dims;
        int elementCount = elemInfo.length / 3;

        List<FGeoFeature> result = new ArrayList<FGeoFeature>();
        for (int i = 0; i < elementCount; i++) {
            int offset = elemInfo[i * 3];
            int etype = elemInfo[i * 3 + 1];
            int interpretation = elemInfo[i * 3 + 2];

            if (interpretation != 1) {
                throw new SdoParseException("Ring mit SDO_INTERPRETATION=" + interpretation
                        + " (Kreisbogen o.ae.) wird nicht unterstuetzt (gid=" + id + ").");
            }
            if (etype != ETYPE_EXTERIOR_RING && etype != ETYPE_INTERIOR_RING) {
                throw new SdoParseException("Nicht unterstuetzter SDO_ETYPE=" + etype
                        + " (gid=" + id + ").");
            }
            if (etype == ETYPE_INTERIOR_RING) {
                continue; // Loch/Enklave bewusst verworfen (siehe Klassenkommentar)
            }

            int startPoint = (offset - 1) / dims;
            int endPoint = (i + 1 < elementCount) ? (elemInfo[(i + 1) * 3] - 1) / dims : totalPoints;
            if (startPoint < 0 || endPoint > totalPoints || endPoint <= startPoint) {
                throw new SdoParseException("Ungueltiger Ring-Offset (gid=" + id + ", element=" + i + ").");
            }
            double[] ring = toLonLatPairs(flat, startPoint, endPoint - startPoint, dims);
            result.add(FGeoFeature.polygon(id, name, ring));
        }
        return result;
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

    /** Liest SDO_ORDINATES roh (dims-faches Interleaving, z.B. x,y,z,x,y,z,...). */
    private static double[] readOrdinates(Object ordinatesAttr) throws SQLException {
        if (!(ordinatesAttr instanceof Array)) {
            throw new SdoParseException("SDO_ORDINATES fehlt oder hat unerwarteten Typ.");
        }
        Object raw = ((Array) ordinatesAttr).getArray();
        int len = java.lang.reflect.Array.getLength(raw);
        double[] out = new double[len];
        for (int i = 0; i < len; i++) {
            out[i] = toDouble(java.lang.reflect.Array.get(raw, i));
        }
        return out;
    }

    /** Schneidet [startPoint, startPoint+pointCount) aus dem rohen Ordinaten-Array und liefert lon/lat-Paare. */
    private static double[] toLonLatPairs(double[] flat, int startPoint, int pointCount, int dims) {
        double[] out = new double[pointCount * 2];
        for (int i = 0; i < pointCount; i++) {
            int base = (startPoint + i) * dims;
            out[i * 2] = flat[base];
            out[i * 2 + 1] = flat[base + 1];
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
