package com.dan.fcheckbox;

import java.beans.PropertyChangeListener;

/**
 * Daten-/Zustandsvertrag einer {@link FCheckBox}.
 *
 * <p>Im F-Stil ist {@code setModel(...)} der primäre Daten-API-Vertrag: die
 * Komponente rendert ausschließlich aus dem Model und reagiert auf
 * {@link java.beans.PropertyChangeEvent}s mit Namen {@code "state"}.</p>
 *
 * <p>Das Model trägt den Tri-State-Zustand; ein einfacher boolescher
 * Auswahlzustand ist als Convenience darüber abgebildet.</p>
 */
public interface FCheckBoxModel {

    /** Property-Name, der bei Zustandsänderung gefeuert wird. */
    String STATE_PROPERTY = "state";

    /** Aktueller Zustand. Nie {@code null}. */
    FCheckState getState();

    /** Setzt den Zustand und feuert {@link #STATE_PROPERTY}, falls verändert. */
    void setState(FCheckState state);

    /** Erlaubt das Model den unbestimmten Zustand beim Durchklicken? */
    boolean isIndeterminateAllowed();

    /** Steuert, ob der unbestimmte Zustand per Klick erreichbar ist. */
    void setIndeterminateAllowed(boolean allowed);

    /** Convenience: {@code getState() == CHECKED}. */
    boolean isSelected();

    /** Convenience: setzt CHECKED bzw. UNCHECKED. */
    void setSelected(boolean selected);

    /** Schaltet auf den nächsten Zustand (siehe {@link FCheckState#next(boolean)}). */
    void toggle();

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);
}
