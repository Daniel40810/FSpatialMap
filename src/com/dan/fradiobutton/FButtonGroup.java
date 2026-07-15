package com.dan.fradiobutton;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FButtonGroup — Erweiterung von {@link ButtonGroup} für {@link FRadioButton}.
 *
 * Zusätzliche Funktionen gegenüber ButtonGroup:
 *  - {@link #setColorScheme(Color, Color)} — Gradient-Farbschema für alle enthaltenen FRadioButtons
 *  - {@link #setTextColor(Color)} — Textfarbe für alle
 *  - {@link #setAnimated(boolean)} — Animation an/aus für alle
 *  - {@link #getFRadioButtons()} — typisierte Liste aller FRadioButtons
 *  - {@link #getSelectedFRadioButton()} — aktuell selektierter FRadioButton (oder null)
 *  - {@link #selectFirst()} — ersten Button selektieren
 *  - {@link #getSelectedIndex()} / {@link #setSelectedIndex(int)} — Index-basierter Zugriff
 *
 * Wichtig: clearSelection() wird NICHT überschrieben — ButtonGroup verwaltet
 * den Selektionszustand intern über ein spezielles ButtonModel; direktes
 * setSelected(false) auf den Buttons würde die Gruppe korrumpieren.
 */
public class FButtonGroup extends ButtonGroup {

    /**
     * Setzt das Gradient-Farbschema für alle enthaltenen {@link FRadioButton}s.
     *
     * @param top    Farbe des Gradient-Kerns oben  (Standard: {@code #00B5AD})
     * @param bottom Farbe des Gradient-Kerns unten (Standard: {@code #8B0024})
     */
    public void setColorScheme(Color top, Color bottom) {
        for (FRadioButton rb : getFRadioButtons()) {
            rb.setColorScheme(top, bottom);
        }
    }

    /**
     * Setzt die Textfarbe für alle enthaltenen {@link FRadioButton}s.
     */
    public void setTextColor(Color color) {
        for (FRadioButton rb : getFRadioButtons()) {
            rb.setTextColor(color);
        }
    }

    /**
     * Aktiviert oder deaktiviert die Hover/Ripple-Animation
     * für alle enthaltenen {@link FRadioButton}s.
     */
    public void setAnimated(boolean animated) {
        for (FRadioButton rb : getFRadioButtons()) {
            rb.setAnimated(animated);
        }
    }

    /**
     * Gibt den aktuell selektierten {@link FRadioButton} zurück,
     * oder {@code null} wenn keiner selektiert ist.
     */
    public FRadioButton getSelectedFRadioButton() {
        for (FRadioButton rb : getFRadioButtons()) {
            if (rb.isSelected()) return rb;
        }
        return null;
    }

    /**
     * Selektiert den ersten {@link FRadioButton} in der Gruppe,
     * sofern mindestens einer vorhanden ist.
     */
    public void selectFirst() {
        List<FRadioButton> list = getFRadioButtons();
        if (!list.isEmpty()) {
            list.get(0).setSelected(true);
        }
    }

    /**
     * Gibt den 0-basierten Index des aktuell selektierten Buttons zurück,
     * oder -1 wenn keiner selektiert ist.
     */
    public int getSelectedIndex() {
        List<FRadioButton> list = getFRadioButtons();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isSelected()) return i;
        }
        return -1;
    }

    /**
     * Selektiert den Button am gegebenen 0-basierten Index.
     *
     * @throws IndexOutOfBoundsException wenn der Index außerhalb des gültigen Bereichs liegt
     */
    public void setSelectedIndex(int index) {
        List<FRadioButton> list = getFRadioButtons();
        if (index < 0 || index >= list.size())
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of range [0, " + (list.size() - 1) + "]");
        list.get(index).setSelected(true);
    }

    /**
     * Gibt alle enthaltenen {@link FRadioButton}s als typisierte Liste zurück
     * (in der Reihenfolge, in der sie via {@link #add(AbstractButton)} hinzugefügt wurden).
     * Nicht-FRadioButton-Elemente werden ignoriert.
     */
    public List<FRadioButton> getFRadioButtons() {
        List<FRadioButton> result = new ArrayList<>();
        for (AbstractButton ab : Collections.list(getElements())) {
            if (ab instanceof FRadioButton) {
                result.add((FRadioButton) ab);
            }
        }
        return result;
    }
}
