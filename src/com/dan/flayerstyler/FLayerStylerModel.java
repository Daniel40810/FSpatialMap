package com.dan.flayerstyler;

import com.dan.fdal.color.ColorDTO;

import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.util.List;

/**
 * Datenvertrag fuer {@link FLayerStyler}. Kombiniert Geometrie (ueber
 * {@link FLayerFeatureProvider}) mit Farbzuordnungen aus COLOR_ADMIN.
 * <p>
 * PropertyChange-Events:
 * <ul>
 *   <li>{@code "layerNames"} — Layer-Liste neu geladen</li>
 *   <li>{@code "selectedLayer"} — Layer-Auswahl gewechselt</li>
 *   <li>{@code "featureRows"} — Feature-/Farbliste des aktiven Layers geaendert</li>
 * </ul>
 */
public interface FLayerStylerModel {

    List<String> getLayerNames();

    void reloadLayerNames();

    String getSelectedLayer();

    void setSelectedLayer(String layerName);

    List<FFeatureRow> getFeatureRows();

    /** Weist einem einzelnen Feature eine Farbe zu (unbegrenzt gueltig). */
    void assignColor(FFeatureRow row, ColorDTO color);

    /** Weist einem einzelnen Feature eine Farbe mit Gueltigkeitszeitraum zu. */
    void assignColor(FFeatureRow row, ColorDTO color, LocalDate validFrom, LocalDate validTo);

    /** Weist mehreren Features in einem Batch dieselbe Farbe zu. */
    void assignColorBatch(List<FFeatureRow> rows, ColorDTO color, LocalDate validFrom, LocalDate validTo);

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);
}
