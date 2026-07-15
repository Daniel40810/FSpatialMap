package com.dan.fmaplegend;

/**
 * Aussehens-Preset fuer FMapLegend. Setzt nur Properties.
 */
public final class FMapLegendStyler {

    public String title = "Layer";
    public boolean showTitle = true;
    public boolean showDetailsPopup = true;
    public boolean paintOwnFrame = true;
    public int rowHeight = 36;
    public int iconSize = 22;
    public int colorBarWidth = 54;

    public FMapLegendStyler() {
    }

    public void apply(FMapLegend legend) {
        legend.setTitle(title);
        legend.setShowTitle(showTitle);
        legend.setShowDetailsPopup(showDetailsPopup);
        legend.setPaintOwnFrame(paintOwnFrame);
        legend.setRowHeight(rowHeight);
        legend.setIconSize(iconSize);
        legend.setColorBarWidth(colorBarWidth);
    }

    public FMapLegend build() {
        FMapLegend l = new FMapLegend();
        apply(l);
        return l;
    }

    /** Seitenleiste: Standard mit Titel und Rahmen. */
    public static FMapLegendStyler sidebar() {
        return new FMapLegendStyler();
    }

    /** Karten-Overlay: kompakt, halbtransparent wirkend, ohne Titel. */
    public static FMapLegendStyler overlay() {
        FMapLegendStyler s = new FMapLegendStyler();
        s.showTitle = false;
        s.rowHeight = 30;
        s.iconSize = 18;
        s.colorBarWidth = 44;
        return s;
    }

    /** Eingebettet (z. B. in FCard): ohne eigenen Rahmen. */
    public static FMapLegendStyler embedded() {
        FMapLegendStyler s = new FMapLegendStyler();
        s.paintOwnFrame = false;
        s.showTitle = false;
        return s;
    }
}
