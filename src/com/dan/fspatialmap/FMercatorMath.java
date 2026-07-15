package com.dan.fspatialmap;

/**
 * Reine Rechenhilfen fuer sphaerische Web-Mercator-Projektion (EPSG:3857),
 * Slippy-Map-Kachelschema (wie OSM/WMTS: 256px Kacheln, z=0..18).
 * Keine Seiteneffekte, keine Abhaengigkeiten.
 */
final class FMercatorMath {

    static final int TILE_SIZE = 256;
    private static final double ORIGIN_SHIFT = 20037508.342789244; // Halbumfang Erde in Metern (EPSG:3857)
    private static final double EARTH_RADIUS = 6378137.0;

    private FMercatorMath() {
    }

    /** Weltgroesse in Pixeln bei gegebenem (fraktionalem) Zoom. */
    static double worldSizePx(double zoom) {
        return TILE_SIZE * Math.pow(2.0, zoom);
    }

    /** WGS84 -> Pixel-X in der Weltkarte bei gegebenem Zoom. */
    static double lonToWorldX(double lon, double zoom) {
        return (lon + 180.0) / 360.0 * worldSizePx(zoom);
    }

    /** WGS84 -> Pixel-Y in der Weltkarte bei gegebenem Zoom. */
    static double latToWorldY(double lat, double zoom) {
        double latRad = Math.toRadians(lat);
        double y = 0.5 - Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0)) / (2.0 * Math.PI);
        return y * worldSizePx(zoom);
    }

    static double worldXToLon(double worldX, double zoom) {
        return worldX / worldSizePx(zoom) * 360.0 - 180.0;
    }

    static double worldYToLat(double worldY, double zoom) {
        double y = worldY / worldSizePx(zoom);
        double n = Math.PI - 2.0 * Math.PI * y;
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    /** EPSG:3857-BBOX (Meter) einer Kachel z/x/y, Reihenfolge minX,minY,maxX,maxY. */
    static double[] tileBoundsMeters(int z, int x, int y) {
        double tiles = Math.pow(2.0, z);
        double tileSizeMeters = (2.0 * ORIGIN_SHIFT) / tiles;
        double minX = -ORIGIN_SHIFT + x * tileSizeMeters;
        double maxX = minX + tileSizeMeters;
        double maxY = ORIGIN_SHIFT - y * tileSizeMeters;
        double minY = maxY - tileSizeMeters;
        return new double[]{minX, minY, maxX, maxY};
    }

    static double lonToMetersX(double lon) {
        return lon * ORIGIN_SHIFT / 180.0;
    }

    static double latToMetersY(double lat) {
        double y = Math.log(Math.tan((90.0 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
        return y * ORIGIN_SHIFT / 180.0;
    }

    /** Grosskreisdistanz in Metern zwischen zwei WGS84-Punkten (lon, lat), Haversine-Formel. */
    static double haversineMeters(double lon1, double lat1, double lon2, double lat2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double sinLat = Math.sin(dLat / 2.0);
        double sinLon = Math.sin(dLon / 2.0);
        double h = sinLat * sinLat + Math.cos(lat1Rad) * Math.cos(lat2Rad) * sinLon * sinLon;
        return 2.0 * EARTH_RADIUS * Math.asin(Math.sqrt(h));
    }
}
