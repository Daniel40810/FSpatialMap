package com.dan.ficons;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Vertrag für ein gemaltes F-Style-Glyph (Symbol ohne Hintergrund/Glow).
 *
 * <p>Ein {@code FIcon} malt ausschließlich das Symbol in ein quadratisches
 * Feld der Kantenlänge {@code dim}, beginnend bei (0,0). Hover-Glow, Skalierung
 * und Aktivierungs-Puls werden bewusst <b>nicht</b> hier, sondern in
 * {@link FIconComponent} erzeugt — so bleiben die Glyphs sauber wiederverwendbar
 * (u. a. für BeanInfo-Palette-Icons, die statisch gerendert werden).</p>
 *
 * <p>Die Standard-Bibliothek liefert {@link FIconType} (ein Enum, das dieses
 * Interface implementiert). Eigene Symbole entstehen durch Implementieren von
 * {@link #paintGlyph(Graphics2D, int)}.</p>
 *
 * @author com.dan
 */
public interface FIcon {

    /**
     * Malt das Symbol in das Feld [0,0 .. dim,dim].
     * Der übergebene Kontext ist bereits mit Antialiasing vorbereitet; die
     * Implementierung darf Paint/Stroke/Composite frei setzen (eine Kopie wird
     * von {@link FIconPaint#render} verwaltet).
     *
     * @param g2  Zielkontext (Ursprung = linke obere Ecke des Icon-Felds)
     * @param dim Kantenlänge des quadratischen Felds in Pixeln
     */
    void paintGlyph(Graphics2D g2, int dim);

    /** Rendert dieses Glyph in ein frisches transparentes {@link BufferedImage}. */
    default BufferedImage toImage(int dim) {
        return FIconPaint.render(this, dim);
    }

    /** Rendert dieses Glyph in die durch {@link FIconSize} bestimmte Größe. */
    default BufferedImage toImage(FIconSize size) {
        return FIconPaint.render(this, size.px);
    }
}
