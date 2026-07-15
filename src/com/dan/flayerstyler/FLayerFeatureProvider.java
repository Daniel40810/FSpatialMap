package com.dan.flayerstyler;

import com.dan.fgeochart.FGeoFeature;

import java.util.List;

/**
 * Liefert die Geometrie-Features eines Spatial-Layers. COLOR_ADMIN kennt nur
 * {@code layer_name + feature_id -> color_id} — die eigentliche Geometrie
 * (SDO_GEOMETRY, SRID 8307) liegt im jeweiligen Fach-Schema (z. B. SpatialApp).
 * <p>
 * Diese Schnittstelle entkoppelt {@link FLayerStyler} bewusst von einem
 * konkreten Spatial-Schema: der Host implementiert sie gegen seine eigene
 * DAL. {@link com.dan.fgeochart.FGeoFeature} wird als gemeinsames Geometrie-DTO
 * wiederverwendet (bereits vorhanden in {@code com.dan.fgeochart}).
 */
public interface FLayerFeatureProvider {

    /** Alle bekannten Layer-Namen (z. B. aus einer Metadaten-Tabelle oder USER_SDO_GEOM_METADATA). */
    List<String> findLayerNames();

    /** Alle Features eines Layers, unabhaengig von deren Farbzuordnung. */
    List<FGeoFeature> findFeatures(String layerName);
}
