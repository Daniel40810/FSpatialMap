package com.dan.fcolorpalette;

import com.dan.fdal.color.ColorAdminDAO;
import com.dan.fdal.color.ColorAdminException;
import com.dan.fdal.color.ColorDTO;
import com.dan.fdal.color.FPaletteAdminDAO;
import com.dan.fdal.color.PaletteDTO;

import javax.swing.SwingWorker;
import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Default-Implementierung von {@link FPaletteEditorModel} auf Basis von
 * {@link ColorAdminDAO} (Lesen/insertColor) und {@link FPaletteAdminDAO}
 * (Paletten-CRUD, Farb-Update/Delete, Sortierung).
 * <p>
 * Ladeoperationen (reload, Palettenwechsel) laufen ueber {@link SwingWorker} im
 * Hintergrund; {@code firePropertyChange} passiert auf dem EDT. Schreiboperationen
 * (add/remove/reorder/create) sind bewusst synchron gehalten (kleine Payloads,
 * sofortiges optimistisches Nachladen).
 */
public class DefaultFPaletteEditorModel implements FPaletteEditorModel {

    private final ColorAdminDAO colorDao;
    private final FPaletteAdminDAO paletteDao;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private List<PaletteDTO> palettes = new ArrayList<>();
    private List<ColorDTO> allColors = new ArrayList<>();
    private PaletteDTO selectedPalette;
    private List<ColorDTO> paletteColors = new ArrayList<>();

    public DefaultFPaletteEditorModel(ColorAdminDAO colorDao, FPaletteAdminDAO paletteDao) {
        this.colorDao = colorDao;
        this.paletteDao = paletteDao;
    }

    // ------------------------------------------------------------------
    // Laden
    // ------------------------------------------------------------------

    @Override
    public void reload() {
        runAsync(() -> {
            List<PaletteDTO> p = paletteDao.findAllPalettes();
            List<ColorDTO> c = colorDao.findAllColors();
            Object[] combined = new Object[]{p, c};
            return combined;
        }, result -> {
            @SuppressWarnings("unchecked")
            List<PaletteDTO> p = (List<PaletteDTO>) result[0];
            @SuppressWarnings("unchecked")
            List<ColorDTO> c = (List<ColorDTO>) result[1];
            List<PaletteDTO> oldP = this.palettes;
            List<ColorDTO> oldC = this.allColors;
            this.palettes = p;
            this.allColors = c;
            pcs.firePropertyChange("palettes", oldP, p);
            pcs.firePropertyChange("allColors", oldC, c);
            if (selectedPalette != null) {
                // Auswahl anhand Name in neu geladener Liste wiederfinden
                for (PaletteDTO candidate : p) {
                    if (candidate.getPaletteName().equals(selectedPalette.getPaletteName())) {
                        setSelectedPalette(candidate);
                        return;
                    }
                }
                setSelectedPalette(null);
            }
        });
    }

    @Override
    public List<PaletteDTO> getPalettes() {
        return Collections.unmodifiableList(palettes);
    }

    @Override
    public List<ColorDTO> getAllColors() {
        return Collections.unmodifiableList(allColors);
    }

    @Override
    public PaletteDTO getSelectedPalette() {
        return selectedPalette;
    }

    @Override
    public void setSelectedPalette(PaletteDTO palette) {
        PaletteDTO old = this.selectedPalette;
        this.selectedPalette = palette;
        pcs.firePropertyChange("selectedPalette", old, palette);
        if (palette == null) {
            List<ColorDTO> oldColors = this.paletteColors;
            this.paletteColors = new ArrayList<>();
            pcs.firePropertyChange("paletteColors", oldColors, this.paletteColors);
            return;
        }
        runAsync(() -> colorDao.findPaletteByName(palette.getPaletteName()),
                full -> {
                    List<ColorDTO> oldColors = this.paletteColors;
                    this.paletteColors = full == null ? new ArrayList<>() : new ArrayList<>(full.getColors());
                    pcs.firePropertyChange("paletteColors", oldColors, this.paletteColors);
                });
    }

    @Override
    public List<ColorDTO> getPaletteColors() {
        return Collections.unmodifiableList(paletteColors);
    }

    // ------------------------------------------------------------------
    // Paletten schreiben
    // ------------------------------------------------------------------

    @Override
    public PaletteDTO createPalette(String name, String description) {
        int id = paletteDao.insertPalette(name, description);
        PaletteDTO created = new PaletteDTO(id, name, description, Collections.emptyList());
        reload();
        return created;
    }

    @Override
    public void deletePalette(PaletteDTO palette) {
        if (palette == null) {
            return;
        }
        paletteDao.deletePalette(palette.getPaletteId());
        if (selectedPalette != null && selectedPalette.getPaletteId() == palette.getPaletteId()) {
            setSelectedPalette(null);
        }
        reload();
    }

    @Override
    public void addColorToPalette(ColorDTO color) {
        if (selectedPalette == null || color == null) {
            return;
        }
        int nextOrder = paletteColors.size();
        paletteDao.addColorToPalette(selectedPalette.getPaletteId(), color.getColorId(), nextOrder);
        List<ColorDTO> oldColors = this.paletteColors;
        List<ColorDTO> next = new ArrayList<>(oldColors);
        next.add(color);
        this.paletteColors = next;
        pcs.firePropertyChange("paletteColors", oldColors, next);
    }

    @Override
    public void removeColorFromPalette(ColorDTO color) {
        if (selectedPalette == null || color == null) {
            return;
        }
        paletteDao.removeColorFromPalette(selectedPalette.getPaletteId(), color.getColorId());
        List<ColorDTO> oldColors = this.paletteColors;
        List<ColorDTO> next = new ArrayList<>(oldColors);
        next.removeIf(c -> c.getColorId() == color.getColorId());
        this.paletteColors = next;
        pcs.firePropertyChange("paletteColors", oldColors, next);
    }

    @Override
    public void reorderPaletteColors(List<ColorDTO> newOrder) {
        if (selectedPalette == null) {
            return;
        }
        List<Integer> ids = new ArrayList<>(newOrder.size());
        for (ColorDTO c : newOrder) {
            ids.add(c.getColorId());
        }
        paletteDao.reorderPaletteColors(selectedPalette.getPaletteId(), ids);
        List<ColorDTO> oldColors = this.paletteColors;
        this.paletteColors = new ArrayList<>(newOrder);
        pcs.firePropertyChange("paletteColors", oldColors, this.paletteColors);
    }

    // ------------------------------------------------------------------
    // Farben schreiben
    // ------------------------------------------------------------------

    @Override
    public ColorDTO createColor(String name, Color awt) {
        int id = colorDao.insertColor(name, awt);
        ColorDTO created = colorDao.findColorById(id);
        List<ColorDTO> oldColors = this.allColors;
        List<ColorDTO> next = new ArrayList<>(oldColors);
        next.add(created);
        this.allColors = next;
        pcs.firePropertyChange("allColors", oldColors, next);
        return created;
    }

    @Override
    public void updateColor(ColorDTO color, Color awt) {
        if (color == null) {
            return;
        }
        paletteDao.updateColor(color.getColorId(), awt);
        reload();
    }

    // ------------------------------------------------------------------
    // Listener
    // ------------------------------------------------------------------

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    // ------------------------------------------------------------------
    // Hilfsmethoden
    // ------------------------------------------------------------------

    private <T> void runAsync(Supplier<T> backgroundWork, Consumer<T> onDoneEdt) {
        new SwingWorker<T, Void>() {
            private ColorAdminException failure;

            @Override
            protected T doInBackground() {
                try {
                    return backgroundWork.get();
                } catch (ColorAdminException e) {
                    failure = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (failure != null) {
                    pcs.firePropertyChange("loadError", null, failure);
                    return;
                }
                try {
                    onDoneEdt.accept(get());
                } catch (Exception e) {
                    pcs.firePropertyChange("loadError", null, e);
                }
            }
        }.execute();
    }
}
