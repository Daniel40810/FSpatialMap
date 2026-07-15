package com.dan.fcheckbox;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Mutable Default-Implementierung von {@link FCheckBoxModel}.
 *
 * <p>Hält den Tri-State-Zustand und propagiert Änderungen über
 * {@link PropertyChangeSupport} unter dem Namen {@link #STATE_PROPERTY}.</p>
 */
public class DefaultFCheckBoxModel implements FCheckBoxModel {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private FCheckState state = FCheckState.UNCHECKED;
    private boolean indeterminateAllowed = false;

    public DefaultFCheckBoxModel() {
    }

    public DefaultFCheckBoxModel(FCheckState initial) {
        if (initial != null) {
            this.state = initial;
        }
    }

    @Override
    public FCheckState getState() {
        return state;
    }

    @Override
    public void setState(FCheckState newState) {
        if (newState == null) {
            newState = FCheckState.UNCHECKED;
        }
        FCheckState old = this.state;
        if (old != newState) {
            this.state = newState;
            pcs.firePropertyChange(STATE_PROPERTY, old, newState);
        }
    }

    @Override
    public boolean isIndeterminateAllowed() {
        return indeterminateAllowed;
    }

    @Override
    public void setIndeterminateAllowed(boolean allowed) {
        this.indeterminateAllowed = allowed;
        // Falls der unbestimmte Zustand verboten wird, ihn auflösen.
        if (!allowed && state == FCheckState.INDETERMINATE) {
            setState(FCheckState.UNCHECKED);
        }
    }

    @Override
    public boolean isSelected() {
        return state == FCheckState.CHECKED;
    }

    @Override
    public void setSelected(boolean selected) {
        setState(selected ? FCheckState.CHECKED : FCheckState.UNCHECKED);
    }

    @Override
    public void toggle() {
        setState(state.next(indeterminateAllowed));
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
