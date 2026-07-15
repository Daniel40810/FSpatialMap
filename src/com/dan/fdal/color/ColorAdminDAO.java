package com.dan.fdal.color;

import java.awt.Color;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Datenzugriffsschicht (DAO) fuer das COLOR_ADMIN-Schema.
 * <p>
 * Liest Farben, Tags, Paletten und Geometrie-Farbzuordnungen und schreibt
 * Geometrie-Zuordnungen zurueck. Alle Verbindungen werden ueber einen
 * {@link ConnectionProvider} bezogen und per try-with-resources geschlossen,
 * sodass das DAO sowohl mit Connection-Pool als auch mit einer einfachen
 * DriverManager-Verbindung funktioniert.
 * <p>
 * Die SQL-Statements sprechen die PUBLIC SYNONYMS an (z.B. {@code CA_COLOR},
 * {@code CA_COLOR_TAG_V}), sind also unabhaengig vom aufrufenden Schema.
 * <p>
 * <b>Benoetigt:</b> Oracle JDBC-Treiber im Classpath (ojdbc8 oder ojdbc11).
 * Das DAO selbst nutzt nur {@code java.sql} und ist damit treiber-neutral.
 *
 * @author com.dan / Proj001
 */
public class ColorAdminDAO {

    private final ConnectionProvider connectionProvider;

    // --- Konstruktoren ----------------------------------------------------

    /**
     * Erzeugt ein DAO mit eigenem {@link ConnectionProvider} (empfohlen fuer
     * Pool-Betrieb).
     *
     * @param connectionProvider liefert JDBC-Verbindungen
     */
    public ColorAdminDAO(ConnectionProvider connectionProvider) {
        if (connectionProvider == null) {
            throw new IllegalArgumentException("connectionProvider darf nicht null sein");
        }
        this.connectionProvider = connectionProvider;
    }

    /**
     * Bequem-Konstruktor fuer eine einfache DriverManager-Verbindung, z.B. zur
     * lokalen Oracle-21c-Instanz. Fuer jede Operation wird eine neue Verbindung
     * geoeffnet und wieder geschlossen.
     *
     * @param jdbcUrl  z.B. {@code jdbc:oracle:thin:@localhost:1521/FREEPDB1}
     * @param user     DB-Benutzer (z.B. dein App-Schema)
     * @param password Passwort
     */
    public ColorAdminDAO(final String jdbcUrl, final String user, final String password) {
        this(new ConnectionProvider() {
            @Override
            public Connection getConnection() throws SQLException {
                return DriverManager.getConnection(jdbcUrl, user, password);
            }
        });
    }

    // --- Farben lesen -----------------------------------------------------

    private static final String COLOR_COLUMNS =
            "color_id, color_name, r, g, b, alpha, hex_rgb, hex_value, "
          + "hue, saturation, brightness";

    /**
     * Liest eine Farbe anhand ihrer ID (inkl. Tags).
     *
     * @param colorId Primaerschluessel
     * @return {@link ColorDTO} oder {@code null}, wenn nicht vorhanden
     */
    public ColorDTO findColorById(int colorId) {
        String sql = "SELECT color_id, color_name, r, g, b, alpha, hex_rgb, "
                   + "hex_value, hue, saturation, brightness, tags "
                   + "FROM ca_color_tag_v WHERE color_id = ?";
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, colorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ColorDTO.fromResultSet(rs, "tags");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new ColorAdminException("Fehler beim Lesen der Farbe id=" + colorId, e);
        }
    }

    /**
     * Liest eine Farbe anhand ihres Namens (inkl. Tags).
     *
     * @param colorName Farbname, z.B. "ForestGreen"
     * @return {@link ColorDTO} oder {@code null}, wenn nicht vorhanden
     */
    public ColorDTO findColorByName(String colorName) {
        String sql = "SELECT color_id, color_name, r, g, b, alpha, hex_rgb, "
                   + "hex_value, hue, saturation, brightness, tags "
                   + "FROM ca_color_tag_v WHERE color_name = ?";
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, colorName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ColorDTO.fromResultSet(rs, "tags");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new ColorAdminException("Fehler beim Lesen der Farbe name=" + colorName, e);
        }
    }

    /**
     * Liest alle Farben (inkl. Tags), sortiert nach Name.
     *
     * @return Liste aller {@link ColorDTO} (nie {@code null})
     */
    public List<ColorDTO> findAllColors() {
        String sql = "SELECT color_id, color_name, r, g, b, alpha, hex_rgb, "
                   + "hex_value, hue, saturation, brightness, tags "
                   + "FROM ca_color_tag_v ORDER BY color_name";
        List<ColorDTO> out = new ArrayList<ColorDTO>();
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(ColorDTO.fromResultSet(rs, "tags"));
            }
        } catch (SQLException e) {
            throw new ColorAdminException("Fehler beim Lesen aller Farben", e);
        }
        return out;
    }

    /**
     * Liest alle Farben, die ein bestimmtes Tag tragen.
     *
     * @param tagName Tag, z.B. "terrain"
     * @return Liste passender {@link ColorDTO} (nie {@code null})
     */
    public List<ColorDTO> findColorsByTag(String tagName) {
        String sql = "SELECT c.color_id, c.color_name, c.r, c.g, c.b, c.alpha, "
                   + "c.hex_rgb, c.hex_value, c.hue, c.saturation, c.brightness "
                   + "FROM ca_color c "
                   + "JOIN ca_color_tag ct ON ct.color_id = c.color_id "
                   + "JOIN ca_tag t ON t.tag_id = ct.tag_id "
                   + "WHERE t.tag_name = ? ORDER BY c.color_name";
        List<ColorDTO> out = new ArrayList<ColorDTO>();
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tagName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(ColorDTO.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new ColorAdminException("Fehler beim Lesen der Farben fuer Tag=" + tagName, e);
        }
        return out;
    }

    // --- Paletten ---------------------------------------------------------

    /**
     * Liest eine Palette inkl. ihrer geordneten Farben aus {@code CA_PALETTE_V}.
     *
     * @param paletteName Palettenname, z.B. "Heatmap"
     * @return {@link PaletteDTO} oder {@code null}, wenn nicht vorhanden
     */
    public PaletteDTO findPaletteByName(String paletteName) {
        String sql = "SELECT palette_id, palette_name, description, "
                   + "color_id, color_name, hex_value, r, g, b, alpha, sort_order "
                   + "FROM ca_palette_v WHERE palette_name = ? ORDER BY sort_order";
        int palId = -1;
        String palName = null;
        String palDesc = null;
        List<ColorDTO> colors = new ArrayList<ColorDTO>();
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, paletteName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (palId == -1) {
                        palId = rs.getInt("palette_id");
                        palName = rs.getString("palette_name");
                        palDesc = rs.getString("description");
                    }
                    // CA_PALETTE_V liefert kein hex_rgb / HSB -> aus RGB ableiten
                    int r = rs.getInt("r");
                    int g = rs.getInt("g");
                    int b = rs.getInt("b");
                    int a = rs.getInt("alpha");
                    float[] hsb = Color.RGBtoHSB(r, g, b, null);
                    ColorDTO c = new ColorDTO(
                            rs.getInt("color_id"),
                            rs.getString("color_name"),
                            r, g, b, a,
                            toHexRgb(r, g, b),
                            rs.getString("hex_value"),
                            hsb[0], hsb[1], hsb[2],
                            null);
                    colors.add(c);
                }
            }
        } catch (SQLException e) {
            throw new ColorAdminException("Fehler beim Lesen der Palette=" + paletteName, e);
        }
        if (palId == -1) {
            return null;
        }
        return new PaletteDTO(palId, palName, palDesc, colors);
    }

    // --- Geometrie-Zuordnung ----------------------------------------------

    /**
     * Liefert die aktuell gueltige Farbe fuer ein Spatial-Feature.
     * "Gueltig" heisst: {@code valid_from <= heute} und
     * ({@code valid_to IS NULL} oder {@code valid_to >= heute}).
     *
     * @param layerName  Layer/Tabellenbezeichner
     * @param featureId  Feature-Kennung (als String)
     * @return {@link ColorDTO} oder {@code null}, wenn keine gueltige Zuordnung
     */
    public ColorDTO findColorForGeometry(String layerName, String featureId) {
        String sql = "SELECT c.color_id, c.color_name, c.r, c.g, c.b, c.alpha, "
                   + "c.hex_rgb, c.hex_value, c.hue, c.saturation, c.brightness "
                   + "FROM ca_geom_color gc "
                   + "JOIN ca_color c ON c.color_id = gc.color_id "
                   + "WHERE gc.layer_name = ? AND gc.feature_id = ? "
                   + "AND gc.valid_from <= SYSDATE "
                   + "AND (gc.valid_to IS NULL OR gc.valid_to >= SYSDATE) "
                   + "ORDER BY gc.valid_from DESC "
                   + "FETCH FIRST 1 ROWS ONLY";
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, layerName);
            ps.setString(2, featureId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ColorDTO.fromResultSet(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new ColorAdminException("Fehler beim Lesen der Geometrie-Farbe fuer "
                    + layerName + "/" + featureId, e);
        }
    }

    /**
     * Liest alle gueltigen Farbzuordnungen eines Layers als Map
     * {@code featureId -> Color}. Ideal, um vor dem Rendern eines ganzen Layers
     * in einem Rutsch alle Farben zu laden.
     *
     * @param layerName Layer/Tabellenbezeichner
     * @return Map von Feature-ID auf {@link java.awt.Color} (nie {@code null})
     */
    public Map<String, Color> loadLayerColorMap(String layerName) {
        String sql = "SELECT gc.feature_id, c.r, c.g, c.b, c.alpha "
                   + "FROM ca_geom_color gc "
                   + "JOIN ca_color c ON c.color_id = gc.color_id "
                   + "WHERE gc.layer_name = ? "
                   + "AND gc.valid_from <= SYSDATE "
                   + "AND (gc.valid_to IS NULL OR gc.valid_to >= SYSDATE)";
        Map<String, Color> map = new HashMap<String, Color>();
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, layerName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Color col = new Color(rs.getInt("r"), rs.getInt("g"),
                            rs.getInt("b"), rs.getInt("alpha"));
                    map.put(rs.getString("feature_id"), col);
                }
            }
        } catch (SQLException e) {
            throw new ColorAdminException("Fehler beim Laden der Layer-Farbmap fuer "
                    + layerName, e);
        }
        return map;
    }

    /**
     * Weist einem Spatial-Feature eine Farbe zu (unbegrenzt gueltig ab heute).
     * Voraussetzung: das aufrufende Schema besitzt INSERT-Recht auf
     * {@code CA_GEOM_COLOR}.
     *
     * @param layerName Layer/Tabellenbezeichner
     * @param featureId Feature-Kennung
     * @param colorId   Ziel-Farbe
     * @return generierte {@code geom_color_id}
     */
    public int assignColorToGeometry(String layerName, String featureId, int colorId) {
        return assignColorToGeometry(layerName, featureId, colorId, null, null);
    }

    /**
     * Weist einem Spatial-Feature eine zeitlich gueltige Farbe zu.
     *
     * @param layerName Layer/Tabellenbezeichner
     * @param featureId Feature-Kennung
     * @param colorId   Ziel-Farbe
     * @param validFrom Gueltig ab (bei {@code null} setzt die DB SYSDATE)
     * @param validTo   Gueltig bis ({@code null} = unbegrenzt)
     * @return generierte {@code geom_color_id}
     */
    public int assignColorToGeometry(String layerName, String featureId, int colorId,
                                     LocalDate validFrom, LocalDate validTo) {
        String sql = "INSERT INTO ca_geom_color "
                   + "(layer_name, feature_id, color_id, valid_from, valid_to) "
                   + "VALUES (?, ?, ?, NVL(?, SYSDATE), ?)";
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, new String[]{"geom_color_id"})) {
            ps.setString(1, layerName);
            ps.setString(2, featureId);
            ps.setInt(3, colorId);
            ps.setDate(4, (validFrom != null) ? java.sql.Date.valueOf(validFrom) : null);
            ps.setDate(5, (validTo != null) ? java.sql.Date.valueOf(validTo) : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new ColorAdminException("Fehler beim Zuweisen der Farbe zu "
                    + layerName + "/" + featureId, e);
        }
    }

    // --- Nearest-Color (reines SQL, ohne PL/SQL-Package) ------------------

    /**
     * Findet die aehnlichste vorhandene Farbe zu einem RGB-Tripel ueber den
     * euklidischen Abstand im RGB-Raum. Optional auf eine Palette eingeschraenkt.
     *
     * @param r           Rot 0..255
     * @param g           Gruen 0..255
     * @param b           Blau 0..255
     * @param paletteName Palette einschraenken oder {@code null} fuer alle Farben
     * @return naechstgelegenes {@link ColorDTO} oder {@code null}, wenn keine Farbe existiert
     */
    public ColorDTO nearestColor(int r, int g, int b, String paletteName) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.color_id, c.color_name, c.r, c.g, c.b, c.alpha, ");
        sql.append("c.hex_rgb, c.hex_value, c.hue, c.saturation, c.brightness ");
        sql.append("FROM ca_color c ");
        if (paletteName != null) {
            sql.append("JOIN ca_palette_color pc ON pc.color_id = c.color_id ");
            sql.append("JOIN ca_palette p ON p.palette_id = pc.palette_id ");
            sql.append("WHERE p.palette_name = ? ");
        }
        sql.append("ORDER BY POWER(c.r - ?, 2) + POWER(c.g - ?, 2) + POWER(c.b - ?, 2) ");
        sql.append("FETCH FIRST 1 ROWS ONLY");

        try (Connection con = connectionProvider.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int idx = 1;
            if (paletteName != null) {
                ps.setString(idx++, paletteName);
            }
            ps.setInt(idx++, r);
            ps.setInt(idx++, g);
            ps.setInt(idx++, b);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ColorDTO.fromResultSet(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new ColorAdminException("Fehler bei nearestColor(" + r + "," + g + "," + b + ")", e);
        }
    }

    // --- Farbe anlegen ----------------------------------------------------

    /**
     * Legt eine neue Farbe an (aus einem AWT-Color). HSB wird aus RGB berechnet.
     * Voraussetzung: INSERT-Recht auf {@code CA_COLOR}.
     *
     * @param colorName Anzeigename (darf {@code null} sein)
     * @param color     AWT-Farbe inkl. Alpha
     * @return generierte {@code color_id}
     */
    public int insertColor(String colorName, Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();
        float[] hsb = Color.RGBtoHSB(r, g, b, null);

        String sql = "INSERT INTO ca_color "
                   + "(color_name, r, g, b, alpha, hue, saturation, brightness) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, new String[]{"color_id"})) {
            ps.setString(1, colorName);
            ps.setInt(2, r);
            ps.setInt(3, g);
            ps.setInt(4, b);
            ps.setInt(5, a);
            ps.setFloat(6, hsb[0]);
            ps.setFloat(7, hsb[1]);
            ps.setFloat(8, hsb[2]);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new ColorAdminException("Fehler beim Anlegen der Farbe name=" + colorName, e);
        }
    }

    // --- Hilfsmethoden ----------------------------------------------------

    private static String toHexRgb(int r, int g, int b) {
        return String.format("#%02X%02X%02X", Integer.valueOf(r),
                Integer.valueOf(g), Integer.valueOf(b));
    }
}
