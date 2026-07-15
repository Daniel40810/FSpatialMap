package com.dan.fdal.sdo;

import com.dan.fgeochart.FGeoFeature;
import com.dan.flayerstyler.FLayerFeatureProvider;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link FLayerFeatureProvider}-Implementierung fuer Oracle-Tabellen mit
 * einer {@code SDO_GEOMETRY}-Spalte (SRID 8307 / WGS84, wie im uebrigen
 * {@code com.dan}-GIS-Stack ueblich). Bindet sich damit direkt an
 * {@code DefaultFLayerStylerModel} und darueber an {@code ColorAdminDAO} -
 * die Kette "Oracle SDO -&gt; FGeoFeature -&gt; ColorAdmin-Faerbung -&gt;
 * FSpatialMapViewer-Overlay" ist damit geschlossen.
 * <p>
 * Jede Layer-SQL muss genau drei Spalten in dieser Reihenfolge liefern:
 * {@code ID (NUMBER), NAME (VARCHAR2), GEOM (SDO_GEOMETRY)}. Bewusst kein
 * automatisches Spalten-Mapping - der Aufrufer kennt sein Schema am besten
 * (Filter, Joins, WHERE-Klauseln lassen sich so frei in die SQL packen).
 *
 * <pre>{@code
 * Map<String,String> layers = new LinkedHashMap<>();
 * layers.put("Gemeinden", "SELECT gemeinde_id AS ID, gemeinde_name AS NAME, "
 *         + "geom AS GEOM FROM sh_gemeinden");
 * layers.put("Schutzgebiete", "SELECT sg_id AS ID, sg_name AS NAME, geom AS GEOM "
 *         + "FROM sh_schutzgebiete WHERE status = 'AKTIV'");
 *
 * OracleSdoFeatureProvider provider = new OracleSdoFeatureProvider(
 *         () -> DriverManager.getConnection(url, user, pwd), layers);
 * }</pre>
 */
public class OracleSdoFeatureProvider implements FLayerFeatureProvider {

    private final ConnectionProvider connectionProvider;
    private final Map<String, String> layerQueries;

    public OracleSdoFeatureProvider(ConnectionProvider connectionProvider, Map<String, String> layerQueries) {
        this.connectionProvider = connectionProvider;
        this.layerQueries = new LinkedHashMap<>(layerQueries);
    }

    /** Fuegt eine weitere Layer-Definition hinzu bzw. ueberschreibt eine bestehende. */
    public void putLayer(String layerName, String sql) {
        layerQueries.put(layerName, sql);
    }

    @Override
    public List<String> findLayerNames() {
        return new ArrayList<>(layerQueries.keySet());
    }

    @Override
    public List<FGeoFeature> findFeatures(String layerName) {
        String sql = layerQueries.get(layerName);
        if (sql == null) {
            throw new SdoParseException("Unbekannter Layer: " + layerName);
        }
        List<FGeoFeature> result = new ArrayList<>();
        try (Connection conn = connectionProvider.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong(1);
                String name = rs.getString(2);
                Object geomObj = rs.getObject(3);
                if (geomObj == null) {
                    continue;
                }
                if (!(geomObj instanceof Struct)) {
                    throw new SdoParseException("Spalte 3 (\"GEOM\") in Layer \"" + layerName
                            + "\" ist keine SDO_GEOMETRY (Typ: " + geomObj.getClass() + ").");
                }
                FGeoFeature feature = SdoGeometryParser.toFeature(id, name, (Struct) geomObj);
                if (feature != null) {
                    result.add(feature);
                }
            }
        } catch (SQLException ex) {
            throw new SdoParseException("Layer \"" + layerName + "\" konnte nicht geladen werden.", ex);
        }
        return result;
    }
}
