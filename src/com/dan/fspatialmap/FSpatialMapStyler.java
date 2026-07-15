package com.dan.fspatialmap;

import java.awt.Color;

/**
 * Preset-Styler fuer {@link FSpatialMapViewer} (Model/Enum/Styler-Trennung,
 * siehe f-style-swing-Skill). Setzt nur Farb-/Flag-Properties, startet keine
 * Timer und mutiert kein Model.
 */
public final class FSpatialMapStyler {

    public Color chromeBackground = new Color(0x12, 0x14, 0x1E);
    public Color glassRim = new Color(140, 200, 255, 150);
    public Color attributionColor = new Color(0xEA, 0xFB, 0xFF, 160);
    public Color crosshairColor = new Color(0x00, 0xB5, 0xAD);
    public boolean showAttribution = true;
    public boolean showZoomControls = true;
    public boolean showCoordinateReadout = true;

    public FSpatialMapStyler() {
    }

    public void apply(FSpatialMapViewer v) {
        v.setGlassRim(glassRim);
        v.setAttributionColor(attributionColor);
        v.setCrosshairColor(crosshairColor);
        v.setShowAttribution(showAttribution);
        v.setShowZoomControls(showZoomControls);
        v.setShowCoordinateReadout(showCoordinateReadout);
        v.setBackground(chromeBackground);
    }

    /** Kompakter Preset ohne Zoom-Buttons/Attribution - fuer eingebettete Vorschaukarten. */
    public static FSpatialMapStyler compact() {
        FSpatialMapStyler s = new FSpatialMapStyler();
        s.showAttribution = false;
        s.showZoomControls = false;
        s.showCoordinateReadout = false;
        return s;
    }

    /** Standard-Preset - volles Chrome, wie fuer ein Haupt-Kartenfenster. */
    public static FSpatialMapStyler standard() {
        return new FSpatialMapStyler();
    }
}
