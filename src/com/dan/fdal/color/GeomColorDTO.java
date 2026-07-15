package com.dan.fdal.color;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Datenobjekt fuer eine Geometrie-Farbzuordnung aus {@code CA_GEOM_COLOR}.
 * <p>
 * Verbindet ueber {@code layerName} + {@code featureId} ein Spatial-Feature mit
 * einer Farbe, ohne die eigentlichen SDO_GEOMETRY-Tabellen zu veraendern.
 * Ueber {@code validFrom}/{@code validTo} lassen sich zeitlich gueltige
 * Einfaerbungen abbilden ({@code validTo == null} = unbegrenzt gueltig).
 *
 * @author com.dan / Proj001
 */
public final class GeomColorDTO {

    private final int geomColorId;
    private final String layerName;
    private final String featureId;
    private final int colorId;
    private final LocalDate validFrom;
    private final LocalDate validTo;

    public GeomColorDTO(int geomColorId, String layerName, String featureId,
                        int colorId, LocalDate validFrom, LocalDate validTo) {
        this.geomColorId = geomColorId;
        this.layerName = layerName;
        this.featureId = featureId;
        this.colorId = colorId;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    /**
     * Baut ein DTO aus einer ResultSet-Zeile von {@code CA_GEOM_COLOR}.
     *
     * @param rs positioniertes ResultSet
     * @return neues GeomColorDTO
     * @throws SQLException bei Zugriffsfehlern
     */
    public static GeomColorDTO fromResultSet(ResultSet rs) throws SQLException {
        Date from = rs.getDate("valid_from");
        Date to = rs.getDate("valid_to");
        return new GeomColorDTO(
                rs.getInt("geom_color_id"),
                rs.getString("layer_name"),
                rs.getString("feature_id"),
                rs.getInt("color_id"),
                (from != null) ? from.toLocalDate() : null,
                (to != null) ? to.toLocalDate() : null
        );
    }

    // --- Getter -----------------------------------------------------------

    public int getGeomColorId()     { return geomColorId; }
    public String getLayerName()    { return layerName; }
    public String getFeatureId()    { return featureId; }
    public int getColorId()         { return colorId; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo()   { return validTo; }

    @Override
    public String toString() {
        return "GeomColorDTO{layer=" + layerName + ", feature=" + featureId
                + ", colorId=" + colorId + "}";
    }
}
