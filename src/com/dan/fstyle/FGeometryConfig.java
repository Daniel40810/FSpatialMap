package com.dan.fstyle;

/**
 * Geometrie-Parameter des F-Stils (Innenabstände, Eckenradius, Strichbreiten).
 *
 * <p>Bietet Dichte-Presets ({@link #compact()}, {@link #spacious()}), damit sich
 * dieselbe Komponentenfamilie kompakt (Tabellen, Toolbars) oder großzügig
 * (Karten, Dialoge) ausprägen lässt.</p>
 *
 * @author com.dan
 */
public class FGeometryConfig {

    /** Innenabstand horizontal (px). */
    public int   padX           = 12;
    /** Innenabstand vertikal (px). */
    public int   padY           = 7;
    /** Eckenradius der Glasflächen (px). */
    public int   arc            = 16;
    /** Strichbreite des Glasrands. */
    public float rimStrokeWidth = 1.6f;
    /** Höhe des Akzent-Balkens (px). */
    public int   accentHeight   = 3;
    /** Versatz/Tiefe des Schattens (px). */
    public int   shadowOffset   = 4;

    public FGeometryConfig() { }

    /** Standard-Dichte. */
    public static FGeometryConfig defaults() {
        return new FGeometryConfig();
    }

    /** Kompakt: weniger Luft, dünnere Strokes, kleinere Radien. */
    public static FGeometryConfig compact() {
        FGeometryConfig c = new FGeometryConfig();
        c.padX = 8;
        c.padY = 4;
        c.arc = 12;
        c.rimStrokeWidth = 1.2f;
        c.accentHeight = 2;
        c.shadowOffset = 3;
        return c;
    }

    /** Großzügig: mehr Luft, kräftigere Strokes, größere Radien. */
    public static FGeometryConfig spacious() {
        FGeometryConfig c = new FGeometryConfig();
        c.padX = 16;
        c.padY = 10;
        c.arc = 20;
        c.rimStrokeWidth = 2.0f;
        c.accentHeight = 4;
        c.shadowOffset = 5;
        return c;
    }

    /** Tiefe Kopie. */
    public FGeometryConfig copy() {
        FGeometryConfig c = new FGeometryConfig();
        c.padX = padX;
        c.padY = padY;
        c.arc = arc;
        c.rimStrokeWidth = rimStrokeWidth;
        c.accentHeight = accentHeight;
        c.shadowOffset = shadowOffset;
        return c;
    }
}
