package com.dan.fcheckbox;

/**
 * Zustand einer {@link FCheckBox}. Unterstützt Tri-State.
 *
 * <ul>
 *   <li>{@link #UNCHECKED} – nicht angehakt</li>
 *   <li>{@link #CHECKED} – angehakt</li>
 *   <li>{@link #INDETERMINATE} – unbestimmt (Strich), z. B. teilweise ausgewählte Gruppe</li>
 * </ul>
 */
public enum FCheckState {
    UNCHECKED,
    CHECKED,
    INDETERMINATE;

    /** {@code true}, wenn der Zustand als "ausgewählt" zählt (für {@code isSelected()}). */
    public boolean isSelected() {
        return this == CHECKED;
    }

    /**
     * Nächster Zustand beim Klick.
     *
     * @param allowIndeterminate wenn {@code true}, durchläuft der Klick
     *                           UNCHECKED → CHECKED → INDETERMINATE → UNCHECKED,
     *                           sonst nur UNCHECKED ↔ CHECKED.
     */
    public FCheckState next(boolean allowIndeterminate) {
        switch (this) {
            case UNCHECKED:
                return CHECKED;
            case CHECKED:
                return allowIndeterminate ? INDETERMINATE : UNCHECKED;
            case INDETERMINATE:
            default:
                return UNCHECKED;
        }
    }
}
