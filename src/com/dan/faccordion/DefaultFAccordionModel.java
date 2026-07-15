package com.dan.faccordion;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable Default-Implementierung von {@link FAccordionModel}. Haelt die
 * Sections als einfache Liste; Aenderungen feuern {@code "sections"}.
 */
public class DefaultFAccordionModel implements FAccordionModel {

    private static final class Entry {
        final String title;
        final Component content;
        final String tooltip;

        Entry(String title, Component content, String tooltip) {
            this.title = title;
            this.content = content;
            this.tooltip = tooltip;
        }
    }

    private final List<Entry> entries = new ArrayList<Entry>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public DefaultFAccordionModel() {
    }

    /** Fuegt eine Section ohne Tooltip an und liefert ihren Index. */
    public int addSection(String title, Component content) {
        return addSection(title, content, null);
    }

    /** Fuegt eine Section mit Tooltip an und liefert ihren Index. */
    public int addSection(String title, Component content, String tooltip) {
        if (title == null) {
            throw new IllegalArgumentException("title darf nicht null sein");
        }
        if (content == null) {
            throw new IllegalArgumentException("content darf nicht null sein");
        }
        entries.add(new Entry(title, content, tooltip));
        int index = entries.size() - 1;
        pcs.firePropertyChange("sections", null, Integer.valueOf(index));
        return index;
    }

    /** Entfernt die Section am gegebenen Index. */
    public void removeSection(int index) {
        entries.remove(index);
        pcs.firePropertyChange("sections", Integer.valueOf(index), null);
    }

    /** Entfernt alle Sections. */
    public void clear() {
        if (entries.isEmpty()) {
            return;
        }
        entries.clear();
        pcs.firePropertyChange("sections", null, null);
    }

    @Override
    public int getSectionCount() {
        return entries.size();
    }

    @Override
    public String getTitleAt(int index) {
        return entries.get(index).title;
    }

    @Override
    public Component getContentAt(int index) {
        return entries.get(index).content;
    }

    @Override
    public String getTooltipAt(int index) {
        return entries.get(index).tooltip;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}
