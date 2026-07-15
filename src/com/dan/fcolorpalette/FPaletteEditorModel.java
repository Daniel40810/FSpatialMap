package com.dan.fcolorpalette;

import com.dan.fdal.color.ColorDTO;
import com.dan.fdal.color.PaletteDTO;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Datenvertrag fuer {@link FColorPaletteEditor}. Die Komponente rendert
 * ausschliesslich aus diesem Model; alle Datenzugriffe (JDBC etc.) sind hier
 * gekapselt.
 * <p>
 * Aenderungen werden ueber {@link PropertyChangeListener} propagiert:
 * <ul>
 *   <li>{@code "palettes"} — Palettenliste wurde neu geladen</li>
 *   <li>{@code "selectedPalette"} — Auswahl gewechselt</li>
 *   <li>{@code "paletteColors"} — Farbliste der aktiven Palette geaendert (Reihenfolge/Inhalt)</li>
 *   <li>{@code "allColors"} — globaler Farbbestand neu geladen</li>
 * </ul>
 */
public interface FPaletteEditorModel {

    List<PaletteDTO> getPalettes();

    /** Laedt die Palettenliste (und ggf. den globalen Farbbestand) neu vom Backend. */
    void reload();

    PaletteDTO getSelectedPalette();

    void setSelectedPalette(PaletteDTO palette);

    /** Geordnete Farbliste der aktuell selektierten Palette (leer, wenn keine Auswahl). */
    List<ColorDTO> getPaletteColors();

    /** Alle in COLOR_ADMIN bekannten Farben (Quelle fuer den Farb-Katalog). */
    List<ColorDTO> getAllColors();

    PaletteDTO createPalette(String name, String description);

    void deletePalette(PaletteDTO palette);

    void addColorToPalette(ColorDTO color);

    void removeColorFromPalette(ColorDTO color);

    /** Schreibt eine neue Reihenfolge fuer die Farben der aktiven Palette. */
    void reorderPaletteColors(List<ColorDTO> newOrder);

    /** Legt eine neue Farbe im COLOR_ADMIN-Bestand an. */
    ColorDTO createColor(String name, Color awt);

    /** Aktualisiert eine bestehende Farbe (RGB) und laedt sie neu. */
    void updateColor(ColorDTO color, Color awt);

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);
}
