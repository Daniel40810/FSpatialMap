package com.dan.ficons;

/**
 * Standardgrößen für F-Style-Icons. Palette-freundlich: NetBeans bietet diese
 * Werte automatisch als Dropdown im Property-Sheet an.
 *
 * @author com.dan
 */
public enum FIconSize {

    SIZE_16(16),
    SIZE_24(24),
    SIZE_32(32),
    SIZE_48(48);

    /** Kantenlänge in Pixeln (quadratisch). */
    public final int px;

    FIconSize(int px) {
        this.px = px;
    }

    public int getPx() {
        return px;
    }

    /** Liefert die Größe, die {@code px} am nächsten kommt (für freie Größen). */
    public static FIconSize nearest(int px) {
        FIconSize best = SIZE_16;
        int bestDiff = Integer.MAX_VALUE;
        for (FIconSize s : values()) {
            int d = Math.abs(s.px - px);
            if (d < bestDiff) {
                bestDiff = d;
                best = s;
            }
        }
        return best;
    }
}
