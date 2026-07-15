package com.dan.fdal.color;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Datenobjekt fuer eine Farbpalette aus {@code CA_PALETTE} inklusive ihrer
 * geordneten Farben (aus {@code CA_PALETTE_V}).
 * <p>
 * Praktisch fuer Heatmaps, Terrain-Skalen oder diskrete Klassifizierungen im
 * Spatial-Rendering: {@link #colorAt(double)} liefert die passende Farbe zu
 * einem normierten Wert 0..1.
 *
 * @author com.dan / Proj001
 */
public final class PaletteDTO {

    private final int paletteId;
    private final String paletteName;
    private final String description;
    private final List<ColorDTO> colors;

    public PaletteDTO(int paletteId, String paletteName, String description,
                      List<ColorDTO> colors) {
        this.paletteId = paletteId;
        this.paletteName = paletteName;
        this.description = description;
        this.colors = (colors == null)
                ? Collections.<ColorDTO>emptyList()
                : Collections.unmodifiableList(new ArrayList<ColorDTO>(colors));
    }

    /**
     * Liefert die Farbe an einem normierten Position 0..1 (diskret).
     * Nuetzlich zum Mappen numerischer Attribute auf eine Farbskala.
     *
     * @param t Wert im Bereich 0.0 .. 1.0 (wird geklemmt)
     * @return {@link ColorDTO} oder {@code null} bei leerer Palette
     */
    public ColorDTO colorAt(double t) {
        if (colors.isEmpty()) {
            return null;
        }
        double clamped = Math.max(0.0, Math.min(1.0, t));
        int idx = (int) Math.round(clamped * (colors.size() - 1));
        return colors.get(idx);
    }

    /**
     * Liefert alle AWT-Farben der Palette in Reihenfolge.
     *
     * @return neue Liste von {@link java.awt.Color}
     */
    public List<Color> toAwtColors() {
        ArrayList<Color> out = new ArrayList<Color>(colors.size());
        for (int i = 0; i < colors.size(); i++) {
            out.add(colors.get(i).toColor());
        }
        return out;
    }

    // --- Getter -----------------------------------------------------------

    public int getPaletteId()          { return paletteId; }
    public String getPaletteName()     { return paletteName; }
    public String getDescription()     { return description; }
    public List<ColorDTO> getColors()  { return colors; }
    public int size()                  { return colors.size(); }

    @Override
    public String toString() {
        return "PaletteDTO{id=" + paletteId + ", name=" + paletteName
                + ", colors=" + colors.size() + "}";
    }
}
