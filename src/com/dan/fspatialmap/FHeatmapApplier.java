package com.dan.fspatialmap;

import com.dan.fdal.color.ColorDTO;
import com.dan.fdal.color.FPaletteAdminDAO;
import com.dan.fdal.color.PaletteDTO;
import com.dan.fgeochart.FGeoFeature;
import com.dan.fgeochart.FGeoLayer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalisierung und ColorAdmin-Anbindung fuer die Heatmap-Faerbung von
 * {@link FGeoLayer}s (Phase 4). Reine Berechnungsklasse ohne Swing-Abhaengigkeit -
 * {@link FSpatialMapViewer#setLayerPalette} nutzt {@link #normalize} fuer die
 * Live-Anzeige, {@link #persist} schreibt das Ergebnis optional dauerhaft nach
 * {@code CA_GEOM_COLOR} (mit Gueltigkeitszeitraum, wie im uebrigen ColorAdmin-Schema).
 */
public final class FHeatmapApplier {

    private FHeatmapApplier() {
    }

    /** Minimum/Maximum von {@code FGeoFeature.getValue()} ueber alle Features des Layers. */
    public static double[] computeValueRange(FGeoLayer layer) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (FGeoFeature f : layer.getFeatures()) {
            double v = f.getValue();
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
        }
        if (!(min <= max)) {
            min = 0.0;
            max = 1.0;
        }
        return new double[]{min, max};
    }

    /** Setzt {@code layer.setValueRange(...)} auf das tatsaechliche Min/Max der Feature-Werte. */
    public static void autoNormalize(FGeoLayer layer) {
        double[] r = computeValueRange(layer);
        layer.setValueRange(r[0], r[1]);
    }

    /** {@code value} linear auf [0..1] normiert, geclampt; {@code max <= min} ergibt 0.5 (neutral). */
    static double normalize(double value, double min, double max) {
        if (max <= min) {
            return 0.5;
        }
        double t = (value - min) / (max - min);
        return Math.max(0.0, Math.min(1.0, t));
    }

    /** Feature-ID (als String, wie von {@code FPaletteAdminDAO} erwartet) -&gt; zugeordnete Palettenfarbe. */
    public static Map<String, ColorDTO> computeColorAssignments(FGeoLayer layer, PaletteDTO palette) {
        Map<String, ColorDTO> result = new LinkedHashMap<>();
        for (FGeoFeature f : layer.getFeatures()) {
            double t = normalize(f.getValue(), layer.getMinValue(), layer.getMaxValue());
            result.put(String.valueOf(f.getId()), palette.colorAt(t));
        }
        return result;
    }

    /**
     * Schreibt die berechnete Heatmap-Faerbung dauerhaft nach {@code CA_GEOM_COLOR}
     * (gruppiert nach Ziel-Farb-ID fuer effiziente Batch-Aufrufe von
     * {@code FPaletteAdminDAO.assignColorToGeometries}). {@code validFrom}/{@code validTo}
     * duerfen {@code null} sein (unbefristet).
     */
    public static void persist(FPaletteAdminDAO dao, String layerName, FGeoLayer layer, PaletteDTO palette,
            LocalDate validFrom, LocalDate validTo) {
        Map<Integer, List<String>> featureIdsByColorId = new LinkedHashMap<>();
        for (FGeoFeature f : layer.getFeatures()) {
            double t = normalize(f.getValue(), layer.getMinValue(), layer.getMaxValue());
            ColorDTO c = palette.colorAt(t);
            featureIdsByColorId.computeIfAbsent(c.getColorId(), k -> new ArrayList<>())
                    .add(String.valueOf(f.getId()));
        }
        for (Map.Entry<Integer, List<String>> entry : featureIdsByColorId.entrySet()) {
            dao.assignColorToGeometries(layerName, entry.getValue(), entry.getKey(), validFrom, validTo);
        }
    }
}
