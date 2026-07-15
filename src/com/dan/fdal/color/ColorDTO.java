package com.dan.fdal.color;

import java.awt.Color;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Unveraenderliches Datenobjekt fuer eine Farbe aus dem COLOR_ADMIN-Schema.
 * <p>
 * Bildet eine Zeile aus {@code CA_COLOR} bzw. der View {@code CA_COLOR_TAG_V}
 * ab und liefert direkt ein einsatzbereites {@link java.awt.Color}-Objekt fuer
 * Swing-/Spatial-Rendering. HSB-Werte liegen normiert im Bereich 0..1 vor und
 * sind damit kompatibel zu {@link Color#RGBtoHSB(int, int, int, float[])}.
 *
 * @author com.dan / Proj001
 */
public final class ColorDTO {

    private final int colorId;
    private final String colorName;
    private final int r;
    private final int g;
    private final int b;
    private final int alpha;
    private final String hexRgb;    // #RRGGBB     -> Color.decode(...)
    private final String hexRgba;   // #RRGGBBAA   -> fuer CSS/SVG-Export
    private final float hue;        // 0.0 .. 1.0
    private final float saturation; // 0.0 .. 1.0
    private final float brightness; // 0.0 .. 1.0
    private final List<String> tags;

    /** Zwischengespeichertes AWT-Color-Objekt (lazy). */
    private transient Color awtColor;

    public ColorDTO(int colorId, String colorName,
                    int r, int g, int b, int alpha,
                    String hexRgb, String hexRgba,
                    float hue, float saturation, float brightness,
                    List<String> tags) {
        this.colorId = colorId;
        this.colorName = colorName;
        this.r = r;
        this.g = g;
        this.b = b;
        this.alpha = alpha;
        this.hexRgb = hexRgb;
        this.hexRgba = hexRgba;
        this.hue = hue;
        this.saturation = saturation;
        this.brightness = brightness;
        this.tags = (tags == null)
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(tags);
    }

    /**
     * Baut ein DTO aus einer ResultSet-Zeile OHNE Tag-Spalte (z.B. aus
     * {@code CA_COLORS_V} oder direkt aus {@code CA_COLOR}).
     *
     * @param rs positioniertes ResultSet
     * @return neues ColorDTO mit leerer Tag-Liste
     * @throws SQLException bei Zugriffsfehlern auf das ResultSet
     */
    public static ColorDTO fromResultSet(ResultSet rs) throws SQLException {
        return fromResultSet(rs, null);
    }

    /**
     * Baut ein DTO aus einer ResultSet-Zeile. Ist {@code tagColumn} gesetzt und
     * die Spalte vorhanden, wird der LISTAGG-String ("terrain,warm,cold") in
     * eine Tag-Liste aufgeteilt.
     *
     * @param rs        positioniertes ResultSet
     * @param tagColumn Name der Tag-Aggregat-Spalte oder {@code null}
     * @return neues ColorDTO
     * @throws SQLException bei Zugriffsfehlern auf das ResultSet
     */
    public static ColorDTO fromResultSet(ResultSet rs, String tagColumn) throws SQLException {
        List<String> tagList = null;
        if (tagColumn != null) {
            String raw = rs.getString(tagColumn);
            if (raw != null && !raw.trim().isEmpty()) {
                String[] parts = raw.split(",");
                java.util.ArrayList<String> tmp = new java.util.ArrayList<String>(parts.length);
                for (int i = 0; i < parts.length; i++) {
                    String t = parts[i].trim();
                    if (!t.isEmpty()) {
                        tmp.add(t);
                    }
                }
                tagList = tmp;
            }
        }
        return new ColorDTO(
                rs.getInt("color_id"),
                rs.getString("color_name"),
                rs.getInt("r"),
                rs.getInt("g"),
                rs.getInt("b"),
                rs.getInt("alpha"),
                rs.getString("hex_rgb"),
                rs.getString("hex_value"),
                rs.getFloat("hue"),
                rs.getFloat("saturation"),
                rs.getFloat("brightness"),
                tagList
        );
    }

    /**
     * Liefert das AWT-Color-Objekt inkl. Alpha. Wird beim ersten Aufruf
     * erzeugt und danach zwischengespeichert.
     *
     * @return {@link java.awt.Color} mit R, G, B, Alpha
     */
    public Color toColor() {
        if (awtColor == null) {
            awtColor = new Color(r, g, b, alpha);
        }
        return awtColor;
    }

    /**
     * Liefert das AWT-Color-Objekt OHNE Transparenz (Alpha = 255).
     *
     * @return deckendes {@link java.awt.Color}
     */
    public Color toOpaqueColor() {
        return new Color(r, g, b);
    }

    /**
     * Prueft, ob diese Farbe ein bestimmtes Tag traegt (case-insensitive).
     *
     * @param tag zu pruefendes Tag
     * @return {@code true} wenn vorhanden
     */
    public boolean hasTag(String tag) {
        if (tag == null) {
            return false;
        }
        for (int i = 0; i < tags.size(); i++) {
            if (tags.get(i).equalsIgnoreCase(tag)) {
                return true;
            }
        }
        return false;
    }

    // --- Getter -----------------------------------------------------------

    public int getColorId()        { return colorId; }
    public String getColorName()   { return colorName; }
    public int getR()              { return r; }
    public int getG()              { return g; }
    public int getB()              { return b; }
    public int getAlpha()          { return alpha; }
    public String getHexRgb()      { return hexRgb; }
    public String getHexRgba()     { return hexRgba; }
    public float getHue()          { return hue; }
    public float getSaturation()   { return saturation; }
    public float getBrightness()   { return brightness; }
    public List<String> getTags()  { return tags; }

    @Override
    public String toString() {
        return "ColorDTO{id=" + colorId + ", name=" + colorName
                + ", hex=" + hexRgb + ", alpha=" + alpha
                + ", tags=" + tags + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ColorDTO)) {
            return false;
        }
        return colorId == ((ColorDTO) o).colorId;
    }

    @Override
    public int hashCode() {
        return colorId;
    }
}
