package com.dan.fspatialmap;

/**
 * Verfuegbare WMS-Basiskarten des Geoservers der GDI-SH
 * (Landesamt fuer Vermessung und Geoinformation Schleswig-Holstein).
 * <p>
 * Alle hier hinterlegten Dienste sind zur privaten Nutzung kostenfrei
 * (Creative Commons CC BY 4.0, Quellenvermerk siehe {@link #getAttribution()}).
 * Quelle: https://service.gdi-sh.de/WMS_SH_DTK25_OpenGBD (GetCapabilities).
 */
public enum FBasemapType {

    DTK25_FARBE(
            "SH DTK25 (farbig)",
            "https://service.gdi-sh.de/WMS_SH_DTK25_OpenGBD",
            "sh_dtk25_col"),

    DTK25_GRAU(
            "SH DTK25 (Graustufen)",
            "https://service.gdi-sh.de/WMS_SH_DTK25_OpenGBD",
            "sh_dtk25_ein");

    private final String label;
    private final String wmsBaseUrl;
    private final String layerName;

    FBasemapType(String label, String wmsBaseUrl, String layerName) {
        this.label = label;
        this.wmsBaseUrl = wmsBaseUrl;
        this.layerName = layerName;
    }

    public String getLabel() {
        return label;
    }

    public String getWmsBaseUrl() {
        return wmsBaseUrl;
    }

    public String getLayerName() {
        return layerName;
    }

    public String getAttribution() {
        return "\u00A9 GeoBasis-DE/LVermGeo SH/CC BY 4.0";
    }

    @Override
    public String toString() {
        return label;
    }
}
