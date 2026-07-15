package com.dan.faccordion;

import java.util.EventListener;

/**
 * Benachrichtigungen ueber Auf-/Zuklapp-Vorgaenge einzelner Sections.
 * {@code *Opening}/{@code *Closing} feuern beim Start der Animation,
 * {@code *Opened}/{@code *Closed} beim Erreichen des Endzustands.
 */
public interface FAccordionSectionListener extends EventListener {

    void sectionOpening(int index);

    void sectionOpened(int index);

    void sectionClosing(int index);

    void sectionClosed(int index);
}
