package com.dan.flayerstyler;

import com.dan.fdal.color.ColorDTO;
import com.dan.fgeochart.FGeoFeature;

import java.time.LocalDate;

/**
 * Verbindet ein {@link FGeoFeature} (Geometrie, aus {@link FLayerFeatureProvider})
 * mit seiner aktuellen Farbzuordnung (aus COLOR_ADMIN, {@code null} = ungefaerbt).
 * Unveraenderlich; der {@link FLayerStylerModel} erzeugt bei jeder Aenderung eine
 * neue Instanz statt bestehende zu mutieren.
 */
public final class FFeatureRow {

    private final FGeoFeature feature;
    private final ColorDTO color;
    private final LocalDate validFrom;
    private final LocalDate validTo;

    public FFeatureRow(FGeoFeature feature, ColorDTO color, LocalDate validFrom, LocalDate validTo) {
        this.feature = feature;
        this.color = color;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public FFeatureRow withColor(ColorDTO newColor, LocalDate newValidFrom, LocalDate newValidTo) {
        return new FFeatureRow(feature, newColor, newValidFrom, newValidTo);
    }

    public FGeoFeature getFeature() {
        return feature;
    }

    public String getFeatureId() {
        return String.valueOf(feature.getId());
    }

    public ColorDTO getColor() {
        return color;
    }

    public boolean isColored() {
        return color != null;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public boolean isUnlimited() {
        return validFrom == null && validTo == null;
    }
}
