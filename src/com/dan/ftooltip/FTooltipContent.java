package com.dan.ftooltip;

import com.dan.ficons.FIcon;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inhalt eines {@link FTooltip}: optionaler Titel, optionale Akzentfarbe
 * (linker Balken / Header-Tint) und eine geordnete Liste von Zeilen.
 *
 * Builder-Stil: Methoden geben {@code this} zurueck, damit der Aufruf
 * verkettet werden kann. Immutable nach aussen (Kopie der Zeilenliste
 * beim Zugriff nicht noetig, da nur der Builder selbst mutiert).
 */
public final class FTooltipContent {

    /** Vordefinierte Akzentfarben, passend zu den F-Style-Statusfarben. */
    public static final Color ACCENT_INFO    = new Color(0x00, 0xB5, 0xAD);
    public static final Color ACCENT_DANGER  = new Color(0xE2, 0x4B, 0x4A);
    public static final Color ACCENT_WARNING = new Color(0xF2, 0xA6, 0x23);
    public static final Color ACCENT_SUCCESS = new Color(0x39, 0xC6, 0x7A);

    private String header;
    private Color accentColor;
    private final List<FTooltipLine> lines = new ArrayList<FTooltipLine>();

    public FTooltipContent() {
    }

    /** Convenience: einzeiliger Tooltip ohne Header/Akzent. */
    public static FTooltipContent of(String text) {
        return new FTooltipContent().addLine(text);
    }

    public static FTooltipContent info(String text) {
        return new FTooltipContent().withAccent(ACCENT_INFO).addLine(text);
    }

    public static FTooltipContent danger(String text) {
        return new FTooltipContent().withAccent(ACCENT_DANGER).addLine(text);
    }

    public static FTooltipContent warning(String text) {
        return new FTooltipContent().withAccent(ACCENT_WARNING).addLine(text);
    }

    public static FTooltipContent success(String text) {
        return new FTooltipContent().withAccent(ACCENT_SUCCESS).addLine(text);
    }

    public FTooltipContent withHeader(String header) {
        this.header = header;
        return this;
    }

    public FTooltipContent withAccent(Color color) {
        this.accentColor = color;
        return this;
    }

    public FTooltipContent addLine(String text) {
        lines.add(new FTooltipLine(text));
        return this;
    }

    public FTooltipContent addLine(String text, FIcon icon) {
        lines.add(new FTooltipLine(text, icon));
        return this;
    }

    public String getHeader()          { return header; }
    public boolean hasHeader()         { return header != null && !header.isEmpty(); }
    public Color getAccentColor()      { return accentColor; }
    public boolean hasAccent()         { return accentColor != null; }
    public List<FTooltipLine> getLines() { return Collections.unmodifiableList(lines); }
    public boolean isEmpty()           { return !hasHeader() && lines.isEmpty(); }
}
