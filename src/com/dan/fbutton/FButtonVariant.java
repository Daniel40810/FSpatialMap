package com.dan.fbutton;

import java.awt.Color;

/**
 * Erscheinungsbild-Variante eines {@link FButton}s im "ReagenzglasBar"-Stil.
 *
 * <ul>
 *   <li>{@link #PRIMARY}  — Türkis→Weinrot  (Standardaktion).</li>
 *   <li>{@link #GHOST}    — fast transparent, nur Rim sichtbar (sekundäre Aktion).</li>
 *   <li>{@link #DANGER}   — Weinrot→Dunkelrot (destruktive Aktion).</li>
 *   <li>{@link #SUCCESS}  — Grün→Türkis (Bestätigung / Speichern).</li>
 * </ul>
 *
 * <p>Als Enum ist die Variante NetBeans-Palette-freundlich: sie erscheint
 * automatisch als Dropdown im Property-Sheet.</p>
 *
 * @author com.dan
 */
public enum FButtonVariant {

    PRIMARY(new Color(0x00, 0xB5, 0xAD), new Color(0x8B, 0x00, 0x24)),
    GHOST  (new Color(0x00, 0xB5, 0xAD), new Color(0x8B, 0x00, 0x24)),
    DANGER (new Color(0xB3, 0x00, 0x1B), new Color(0x4D, 0x00, 0x10)),
    SUCCESS(new Color(0x00, 0xCC, 0x66), new Color(0x00, 0xB5, 0xAD));

    /** Verlaufsfarbe oben (Türkis-Pol). */
    public final Color top;
    /** Verlaufsfarbe unten (Weinrot-Pol). */
    public final Color bottom;

    FButtonVariant(Color top, Color bottom) {
        this.top    = top;
        this.bottom = bottom;
    }
}
