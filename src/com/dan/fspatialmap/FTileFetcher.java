package com.dan.fspatialmap;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Beschafft eine einzelne 256x256-Kachel fuer die gegebene Slippy-Map-Adresse.
 * Austauschbar (z. B. Stub in Headless-Tests, damit kein echter Netzwerkzugriff
 * noetig ist) analog zum {@code StubSql}-Pattern aus {@code com.dan.fdal}.
 */
@FunctionalInterface
public interface FTileFetcher {

    /**
     * @param basemap aktive Basiskarte (liefert WMS-Endpunkt + Layername)
     * @param z Zoomstufe (Slippy-Map-Schema)
     * @param x Kachel-Spalte
     * @param y Kachel-Zeile
     * @return dekodiertes 256x256-Bild, nie {@code null}
     */
    BufferedImage fetchTile(FBasemapType basemap, int z, int x, int y) throws IOException;
}
