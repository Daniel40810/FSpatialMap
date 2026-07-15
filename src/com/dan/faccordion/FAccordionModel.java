package com.dan.faccordion;

import java.awt.Component;
import java.beans.PropertyChangeListener;

/**
 * Datenvertrag fuer {@link FAccordion}. Liefert ausschliesslich statische
 * Section-Daten (Titel, Inhalt, Tooltip) — der Auf/Zu-Zustand ist reiner
 * Live-Zustand der Komponente und lebt bewusst nicht im Model.
 *
 * <p>Aenderungen an der Sectionliste (hinzufuegen/entfernen) werden ueber
 * {@link PropertyChangeListener} mit Property-Name {@code "sections"}
 * propagiert; {@link FAccordion} baut sich dann intern neu auf.</p>
 */
public interface FAccordionModel {

    /** Anzahl der Sections. */
    int getSectionCount();

    /** Ueberschrift der Section im Header. */
    String getTitleAt(int index);

    /** Beliebige Inhaltskomponente, wird als Kind-Komponente eingehaengt. */
    Component getContentAt(int index);

    /** Optionaler Tooltip fuer den Header; darf {@code null} sein. */
    String getTooltipAt(int index);

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);
}
