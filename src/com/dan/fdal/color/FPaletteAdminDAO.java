package com.dan.fdal.color;

import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Erweitert {@link ColorAdminDAO} um Schreib-/Verwaltungsoperationen, die der
 * FColorPaletteEditor benoetigt: Paletten anlegen/loeschen, Farben einer Palette
 * zuordnen/entfernen/umsortieren, sowie Farben aktualisieren/loeschen.
 * <p>
 * Bewusst als eigenstaendige Klasse statt Subklasse von {@link ColorAdminDAO}
 * angelegt, da diese ihren {@code ConnectionProvider} nicht exponiert. Beide
 * Klassen teilen sich denselben Verbindungsaufbau und werden vom Aufrufer mit
 * demselben {@link ConnectionProvider} instanziiert.
 * <p>
 * Alle Methoden werfen {@link ColorAdminException} (unchecked), analog zu
 * {@link ColorAdminDAO}. Schreiboperationen nutzen wo sinnvoll {@code MERGE}
 * fuer Idempotenz (Projekt-Konvention).
 */
public class FPaletteAdminDAO {

    private final ConnectionProvider connectionProvider;

    public FPaletteAdminDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public FPaletteAdminDAO(String url, String user, String password) {
        this.connectionProvider = () -> java.sql.DriverManager.getConnection(url, user, password);
    }

    // ------------------------------------------------------------------
    // Paletten lesen (leichtgewichtig, ohne Farben)
    // ------------------------------------------------------------------

    /**
     * Alle Paletten, sortiert nach Name. Farben werden NICHT mitgeladen
     * (siehe {@link ColorAdminDAO#findPaletteByName(String)} fuer die volle
     * Palette inkl. geordneter Farbliste).
     */
    public List<PaletteDTO> findAllPalettes() {
        String sql = "SELECT palette_id, palette_name, description FROM ca_palette ORDER BY palette_name";
        try (Connection c = connectionProvider.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<PaletteDTO> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new PaletteDTO(
                        rs.getInt("palette_id"),
                        rs.getString("palette_name"),
                        rs.getString("description"),
                        Collections.emptyList()));
            }
            return result;
        } catch (SQLException e) {
            throw new ColorAdminException("findAllPalettes fehlgeschlagen", e);
        }
    }

    // ------------------------------------------------------------------
    // Paletten schreiben
    // ------------------------------------------------------------------

    /** Legt eine neue, leere Palette an und liefert die generierte {@code palette_id}. */
    public int insertPalette(String name, String description) {
        String insert = "INSERT INTO ca_palette (palette_name, description) VALUES (?, ?)";
        try (Connection c = connectionProvider.getConnection();
             PreparedStatement ps = c.prepareStatement(insert)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ColorAdminException("insertPalette fehlgeschlagen: " + name, e);
        }
        PaletteDTO created = findPaletteIdByName(name);
        if (created == null) {
            throw new ColorAdminException("Palette nach Insert nicht auffindbar: " + name);
        }
        return created.getPaletteId();
    }

    private PaletteDTO findPaletteIdByName(String name) {
        String sql = "SELECT palette_id, palette_name, description FROM ca_palette WHERE palette_name = ?";
        try (Connection c = connectionProvider.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PaletteDTO(rs.getInt("palette_id"), rs.getString("palette_name"),
                            rs.getString("description"), Collections.emptyList());
                }
                return null;
            }
        } catch (SQLException e) {
            throw new ColorAdminException("Palettensuche nach Name fehlgeschlagen: " + name, e);
        }
    }

    /** Loescht eine Palette samt ihrer Farb-Zuordnungen (nicht die Farben selbst). */
    public void deletePalette(int paletteId) {
        try (Connection c = connectionProvider.getConnection()) {
            boolean prevAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                try (PreparedStatement del1 = c.prepareStatement(
                        "DELETE FROM ca_palette_color WHERE palette_id = ?")) {
                    del1.setInt(1, paletteId);
                    del1.executeUpdate();
                }
                try (PreparedStatement del2 = c.prepareStatement(
                        "DELETE FROM ca_palette WHERE palette_id = ?")) {
                    del2.setInt(1, paletteId);
                    del2.executeUpdate();
                }
                c.commit();
            } catch (SQLException inner) {
                c.rollback();
                throw inner;
            } finally {
                c.setAutoCommit(prevAutoCommit);
            }
        } catch (SQLException e) {
            throw new ColorAdminException("deletePalette fehlgeschlagen: " + paletteId, e);
        }
    }

    /**
     * Ordnet eine Farbe einer Palette zu (oder aktualisiert ihre Sortierposition,
     * falls bereits zugeordnet). Nutzt MERGE fuer Idempotenz.
     */
    public void addColorToPalette(int paletteId, int colorId, int sortOrder) {
        String merge = "MERGE INTO ca_palette_color tgt " +
                "USING (SELECT ? palette_id, ? color_id, ? sort_order FROM dual) src " +
                "ON (tgt.palette_id = src.palette_id AND tgt.color_id = src.color_id) " +
                "WHEN MATCHED THEN UPDATE SET tgt.sort_order = src.sort_order " +
                "WHEN NOT MATCHED THEN INSERT (palette_id, color_id, sort_order) " +
                "VALUES (src.palette_id, src.color_id, src.sort_order)";
        try (Connection c = connectionProvider.getConnection();
             PreparedStatement ps = c.prepareStatement(merge)) {
            ps.setInt(1, paletteId);
            ps.setInt(2, colorId);
            ps.setInt(3, sortOrder);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ColorAdminException("addColorToPalette fehlgeschlagen: palette=" + paletteId
                    + " color=" + colorId, e);
        }
    }

    /** Entfernt eine Farbe aus einer Palette (die Farbe selbst bleibt in CA_COLOR erhalten). */
    public void removeColorFromPalette(int paletteId, int colorId) {
        String sql = "DELETE FROM ca_palette_color WHERE palette_id = ? AND color_id = ?";
        try (Connection c = connectionProvider.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, paletteId);
            ps.setInt(2, colorId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ColorAdminException("removeColorFromPalette fehlgeschlagen: palette=" + paletteId
                    + " color=" + colorId, e);
        }
    }

    /**
     * Schreibt eine neue Sortierreihenfolge fuer alle Farben einer Palette in einem Batch.
     * {@code colorIdsInOrder} muss die gewuenschte Endreihenfolge (0-basiert) enthalten.
     */
    public void reorderPaletteColors(int paletteId, List<Integer> colorIdsInOrder) {
        String sql = "UPDATE ca_palette_color SET sort_order = ? WHERE palette_id = ? AND color_id = ?";
        try (Connection c = connectionProvider.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < colorIdsInOrder.size(); i++) {
                ps.setInt(1, i);
                ps.setInt(2, paletteId);
                ps.setInt(3, colorIdsInOrder.get(i));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new ColorAdminException("reorderPaletteColors fehlgeschlagen: palette=" + paletteId, e);
        }
    }

    // ------------------------------------------------------------------
    // Geometrie-Farbzuordnungen lesen/schreiben (fuer FLayerStyler)
    // ------------------------------------------------------------------

    /**
     * Alle Farbzuordnungen eines Layers (aktuelle und historische, inkl.
     * Gueltigkeitszeitraum). {@link ColorAdminDAO#findColorForGeometry} liefert
     * nur die aktuell gueltige Zuordnung je Feature — diese Methode liefert den
     * vollen Bestand fuer die Tabellenansicht im FLayerStyler.
     */
    public List<GeomColorDTO> findGeomColorsForLayer(String layerName) {
        String sql = "SELECT geom_color_id, layer_name, feature_id, color_id, valid_from, valid_to " +
                "FROM ca_geom_color WHERE layer_name = ? ORDER BY feature_id";
        try (Connection c = connectionProvider.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, layerName);
            try (ResultSet rs = ps.executeQuery()) {
                List<GeomColorDTO> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(GeomColorDTO.fromResultSet(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new ColorAdminException("findGeomColorsForLayer fehlgeschlagen: " + layerName, e);
        }
    }

    /**
     * Weist mehreren Features eines Layers in einem Batch dieselbe Farbe zu
     * (z. B. "alle markierten Features einfaerben"). {@code validFrom}/{@code validTo}
     * duerfen {@code null} sein (unbegrenzt gueltig).
     */
    public void assignColorToGeometries(String layerName, List<String> featureIds, int colorId,
                                         java.time.LocalDate validFrom, java.time.LocalDate validTo) {
        String merge = "MERGE INTO ca_geom_color tgt " +
                "USING (SELECT ? layer_name, ? feature_id FROM dual) src " +
                "ON (tgt.layer_name = src.layer_name AND tgt.feature_id = src.feature_id) " +
                "WHEN MATCHED THEN UPDATE SET tgt.color_id = ?, tgt.valid_from = ?, tgt.valid_to = ? " +
                "WHEN NOT MATCHED THEN INSERT (layer_name, feature_id, color_id, valid_from, valid_to) " +
                "VALUES (src.layer_name, src.feature_id, ?, ?, ?)";
        try (Connection c = connectionProvider.getConnection();
             PreparedStatement ps = c.prepareStatement(merge)) {
            java.sql.Date from = validFrom == null ? null : java.sql.Date.valueOf(validFrom);
            java.sql.Date to = validTo == null ? null : java.sql.Date.valueOf(validTo);
            for (String featureId : featureIds) {
                ps.setString(1, layerName);
                ps.setString(2, featureId);
                ps.setInt(3, colorId);
                ps.setDate(4, from);
                ps.setDate(5, to);
                ps.setInt(6, colorId);
                ps.setDate(7, from);
                ps.setDate(8, to);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new ColorAdminException("assignColorToGeometries fehlgeschlagen: layer=" + layerName, e);
        }
    }

    // ------------------------------------------------------------------
    // Farben schreiben (Ergaenzung zu ColorAdminDAO.insertColor)
    // ------------------------------------------------------------------

    /** Aktualisiert RGB(A) einer bestehenden Farbe; HSB wird aus RGB neu berechnet (wie insertColor). */
    public void updateColor(int colorId, Color awt) {
        float[] hsb = Color.RGBtoHSB(awt.getRed(), awt.getGreen(), awt.getBlue(), null);
        String sql = "UPDATE ca_color SET r = ?, g = ?, b = ?, alpha = ?, " +
                "hue = ?, saturation = ?, brightness = ? WHERE color_id = ?";
        try (Connection c = connectionProvider.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, awt.getRed());
            ps.setInt(2, awt.getGreen());
            ps.setInt(3, awt.getBlue());
            ps.setInt(4, awt.getAlpha());
            ps.setFloat(5, hsb[0]);
            ps.setFloat(6, hsb[1]);
            ps.setFloat(7, hsb[2]);
            ps.setInt(8, colorId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ColorAdminException("updateColor fehlgeschlagen: " + colorId, e);
        }
    }

    /** Loescht eine Farbe vollstaendig inkl. aller Tag-/Paletten-/Geometrie-Zuordnungen. */
    public void deleteColor(int colorId) {
        String[] cascade = {
                "DELETE FROM ca_color_tag WHERE color_id = ?",
                "DELETE FROM ca_palette_color WHERE color_id = ?",
                "DELETE FROM ca_geom_color WHERE color_id = ?",
                "DELETE FROM ca_color WHERE color_id = ?"
        };
        try (Connection c = connectionProvider.getConnection()) {
            boolean prevAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                for (String sql : cascade) {
                    try (PreparedStatement ps = c.prepareStatement(sql)) {
                        ps.setInt(1, colorId);
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException inner) {
                c.rollback();
                throw inner;
            } finally {
                c.setAutoCommit(prevAutoCommit);
            }
        } catch (SQLException e) {
            throw new ColorAdminException("deleteColor fehlgeschlagen: " + colorId, e);
        }
    }
}
