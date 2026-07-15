package com.dan.fspatialmap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Zweistufiger Cache fuer WMS-Kacheln:
 * <ol>
 *   <li>In-Memory-LRU (schnell, begrenzte Groesse, {@code maxMemoryTiles})</li>
 *   <li>Disk-Cache unter {@code ~/.fstyle/tiles/&lt;basemap&gt;/&lt;z&gt;/&lt;x&gt;/&lt;y&gt;.png}
 *       (ueberlebt Neustarts, spart wiederholte WMS-Anfragen)</li>
 * </ol>
 * Nicht Timer-getrieben, keine Animation - reine Datenhaltung, threadsicher
 * ueber synchronisierten Zugriff auf die LRU-Map.
 */
public class FTileCache {

    private final int maxMemoryTiles;
    private final File diskCacheDir;
    private final Map<String, BufferedImage> memory;

    public FTileCache() {
        this(400, new File(System.getProperty("user.home"), ".fstyle/tiles"));
    }

    public FTileCache(int maxMemoryTiles, File diskCacheDir) {
        this.maxMemoryTiles = maxMemoryTiles;
        this.diskCacheDir = diskCacheDir;
        this.memory = new LinkedHashMap<String, BufferedImage>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
                return size() > FTileCache.this.maxMemoryTiles;
            }
        };
    }

    static String key(FBasemapType basemap, int z, int x, int y) {
        return basemap.name() + '/' + z + '/' + x + '/' + y;
    }

    /** Nur In-Memory-Lookup (kein Disk-I/O, EDT-sicher). */
    public synchronized BufferedImage getFromMemory(FBasemapType basemap, int z, int x, int y) {
        return memory.get(key(basemap, z, x, y));
    }

    public synchronized void putMemory(FBasemapType basemap, int z, int x, int y, BufferedImage img) {
        memory.put(key(basemap, z, x, y), img);
    }

    /** Disk-Lookup - blockierend, nur aus Hintergrund-Threads aufrufen. */
    public BufferedImage readFromDisk(FBasemapType basemap, int z, int x, int y) {
        File f = tileFile(basemap, z, x, y);
        if (!f.isFile()) {
            return null;
        }
        try {
            return ImageIO.read(f);
        } catch (IOException ex) {
            return null;
        }
    }

    /** Disk-Write - blockierend, nur aus Hintergrund-Threads aufrufen. */
    public void writeToDisk(FBasemapType basemap, int z, int x, int y, BufferedImage img) {
        File f = tileFile(basemap, z, x, y);
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            return;
        }
        try {
            ImageIO.write(img, "png", f);
        } catch (IOException ignored) {
            // Disk-Cache ist Best-Effort - kein Grund, den Ladevorgang scheitern zu lassen
        }
    }

    private File tileFile(FBasemapType basemap, int z, int x, int y) {
        return new File(diskCacheDir, basemap.name() + File.separator + z + File.separator
                + x + File.separator + y + ".png");
    }

    public synchronized int memorySize() {
        return memory.size();
    }

    public synchronized void clearMemory() {
        memory.clear();
    }
}
