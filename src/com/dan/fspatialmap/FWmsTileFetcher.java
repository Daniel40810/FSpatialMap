package com.dan.fspatialmap;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Default-{@link FTileFetcher}: fragt fuer jede Slippy-Map-Kachel eine
 * WMS-1.3.0-GetMap-Anfrage (EPSG:3857, 256x256, PNG) beim jeweiligen
 * {@link FBasemapType#getWmsBaseUrl()} an. Der Zieldienst liefert keine
 * vorgekachelten Bilder (kein WMTS) - die Kachelung passiert also client-seitig,
 * ein Kachel-Cache ist damit umso wichtiger (siehe {@link FTileCache}).
 */
public class FWmsTileFetcher implements FTileFetcher {

    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public FWmsTileFetcher() {
        this(8000, 8000);
    }

    public FWmsTileFetcher(int connectTimeoutMs, int readTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    @Override
    public BufferedImage fetchTile(FBasemapType basemap, int z, int x, int y) throws IOException {
        String url = buildGetMapUrl(basemap, z, x, y);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setRequestProperty("User-Agent", "FStyle-FSpatialMapViewer/1.0");
        try (InputStream in = conn.getInputStream()) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                throw new IOException("WMS-Antwort konnte nicht als Bild dekodiert werden: " + url);
            }
            return img;
        } finally {
            conn.disconnect();
        }
    }

    /** Package-sichtbar fuer Tests/Debug - baut die GetMap-URL ohne HTTP-Aufruf. */
    static String buildGetMapUrl(FBasemapType basemap, int z, int x, int y) {
        double[] b = FMercatorMath.tileBoundsMeters(z, x, y);
        return String.format(Locale.ROOT,
                "%s?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&LAYERS=%s&STYLES=&"
                + "CRS=EPSG:3857&BBOX=%f,%f,%f,%f&WIDTH=%d&HEIGHT=%d&FORMAT=image/png&TRANSPARENT=FALSE",
                basemap.getWmsBaseUrl(), basemap.getLayerName(),
                b[0], b[1], b[2], b[3], FMercatorMath.TILE_SIZE, FMercatorMath.TILE_SIZE);
    }
}
