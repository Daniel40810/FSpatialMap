package com.dan.fcolorpalette;

/** Darstellungsart der Palette-Vorschau in {@link FPalettePreviewPanel}. */
public enum FPreviewMode {
    GRADIENT("Verlauf"),
    HEATMAP("Heatmap"),
    BARS("Balken");

    private final String label;

    FPreviewMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
