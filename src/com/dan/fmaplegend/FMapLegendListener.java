package com.dan.fmaplegend;

import java.util.EventListener;

/**
 * Benachrichtigung, wenn der Nutzer die Sichtbarkeit eines Layers in der
 * Legende umschaltet — z. B. um den Layer im Karten-Rendering ein-/auszublenden.
 */
public interface FMapLegendListener extends EventListener {

    void layerVisibilityChanged(FLayerEntry entry);
}
