package com.dan.ftooltip;

import com.dan.ficons.FIcon;

/**
 * Eine Zeile innerhalb eines {@link FTooltipContent}: Text mit optionalem
 * fuehrenden Icon (aus dem F-Icon-System). Immutable.
 */
public final class FTooltipLine {

    private final String text;
    private final FIcon icon;

    public FTooltipLine(String text) {
        this(text, null);
    }

    public FTooltipLine(String text, FIcon icon) {
        this.text = text == null ? "" : text;
        this.icon = icon;
    }

    public String getText() { return text; }
    public FIcon getIcon()  { return icon; }
    public boolean hasIcon() { return icon != null; }
}
