package com.dan.fcolorpalette;

/**
 * Preset-Styler fuer {@link FColorPaletteEditor}. Setzt nur Geometrie-/Anzeige-
 * Properties in einem Aufruf; mutiert kein Model, startet keine Timer.
 */
public final class FColorPaletteEditorStyler {

    public int paletteListWidth = 170;
    public int swatchListWidth = 240;
    public int pickerWidth = 220;
    public FPreviewMode initialPreviewMode = FPreviewMode.GRADIENT;

    public FColorPaletteEditorStyler() {
    }

    public void apply(FColorPaletteEditor editor) {
        editor.setPaletteListWidth(paletteListWidth);
        editor.setSwatchListWidth(swatchListWidth);
        editor.setPickerWidth(pickerWidth);
        editor.setPreviewMode(initialPreviewMode);
    }

    /** Kompakt: schmalere Spalten, fuer eingebettete Nutzung (z. B. spaeter im FLayerStyler). */
    public static FColorPaletteEditorStyler compact() {
        FColorPaletteEditorStyler s = new FColorPaletteEditorStyler();
        s.paletteListWidth = 130;
        s.swatchListWidth = 190;
        s.pickerWidth = 180;
        return s;
    }

    /** Studio: breite Spalten fuer den eigenstaendigen Editor-Dialog. */
    public static FColorPaletteEditorStyler studio() {
        return new FColorPaletteEditorStyler();
    }
}
