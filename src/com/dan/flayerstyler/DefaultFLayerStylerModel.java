package com.dan.flayerstyler;

import com.dan.fdal.color.ColorAdminDAO;
import com.dan.fdal.color.ColorAdminException;
import com.dan.fdal.color.ColorDTO;
import com.dan.fdal.color.FPaletteAdminDAO;
import com.dan.fdal.color.GeomColorDTO;
import com.dan.fgeochart.FGeoFeature;

import javax.swing.SwingWorker;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Default-Implementierung von {@link FLayerStylerModel}. Laedt Geometrie ueber
 * den injizierten {@link FLayerFeatureProvider} und Farbzuordnungen ueber
 * {@link ColorAdminDAO}/{@link FPaletteAdminDAO}, und fuehrt beides zu
 * {@link FFeatureRow}-Zeilen zusammen.
 * <p>
 * Ladeoperationen laufen ueber {@link SwingWorker}; Schreiboperationen sind
 * synchron mit optimistischem lokalem Update.
 */
public class DefaultFLayerStylerModel implements FLayerStylerModel {

    private final FLayerFeatureProvider featureProvider;
    private final ColorAdminDAO colorDao;
    private final FPaletteAdminDAO paletteDao;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private List<String> layerNames = new ArrayList<>();
    private String selectedLayer;
    private List<FFeatureRow> featureRows = new ArrayList<>();

    public DefaultFLayerStylerModel(FLayerFeatureProvider featureProvider,
                                     ColorAdminDAO colorDao, FPaletteAdminDAO paletteDao) {
        this.featureProvider = featureProvider;
        this.colorDao = colorDao;
        this.paletteDao = paletteDao;
    }

    @Override
    public void reloadLayerNames() {
        runAsync(featureProvider::findLayerNames, names -> {
            List<String> old = this.layerNames;
            this.layerNames = names;
            pcs.firePropertyChange("layerNames", old, names);
        });
    }

    @Override
    public List<String> getLayerNames() {
        return Collections.unmodifiableList(layerNames);
    }

    @Override
    public String getSelectedLayer() {
        return selectedLayer;
    }

    @Override
    public void setSelectedLayer(String layerName) {
        String old = this.selectedLayer;
        this.selectedLayer = layerName;
        pcs.firePropertyChange("selectedLayer", old, layerName);
        if (layerName == null) {
            setFeatureRows(new ArrayList<>());
            return;
        }
        runAsync(() -> buildRows(layerName), this::setFeatureRows);
    }

    private List<FFeatureRow> buildRows(String layerName) {
        List<FGeoFeature> features = featureProvider.findFeatures(layerName);
        List<GeomColorDTO> assignments = paletteDao.findGeomColorsForLayer(layerName);

        Map<String, GeomColorDTO> currentByFeature = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (GeomColorDTO g : assignments) {
            boolean validNow = (g.getValidFrom() == null || !g.getValidFrom().isAfter(today))
                    && (g.getValidTo() == null || !g.getValidTo().isBefore(today));
            if (validNow) {
                // Bei mehreren gleichzeitig gueltigen Eintraegen gewinnt der zuletzt gelesene
                // (CA_GEOM_COLOR ist nach feature_id sortiert, nicht nach Aktualitaet — fuer
                // ueberlappende Gueltigkeitszeitraeume muesste hier zusaetzlich nach valid_from
                // sortiert werden; im Normalfall nicht ueberlappend).
                currentByFeature.put(g.getFeatureId(), g);
            }
        }

        Map<Integer, ColorDTO> colorById = new HashMap<>();
        List<FFeatureRow> rows = new ArrayList<>(features.size());
        for (FGeoFeature f : features) {
            String featureId = String.valueOf(f.getId());
            GeomColorDTO assignment = currentByFeature.get(featureId);
            ColorDTO color = null;
            LocalDate from = null;
            LocalDate to = null;
            if (assignment != null) {
                color = colorById.computeIfAbsent(assignment.getColorId(), colorDao::findColorById);
                from = assignment.getValidFrom();
                to = assignment.getValidTo();
            }
            rows.add(new FFeatureRow(f, color, from, to));
        }
        return rows;
    }

    private void setFeatureRows(List<FFeatureRow> rows) {
        List<FFeatureRow> old = this.featureRows;
        this.featureRows = rows;
        pcs.firePropertyChange("featureRows", old, rows);
    }

    @Override
    public List<FFeatureRow> getFeatureRows() {
        return Collections.unmodifiableList(featureRows);
    }

    @Override
    public void assignColor(FFeatureRow row, ColorDTO color) {
        assignColor(row, color, null, null);
    }

    @Override
    public void assignColor(FFeatureRow row, ColorDTO color, LocalDate validFrom, LocalDate validTo) {
        assignColorBatch(Collections.singletonList(row), color, validFrom, validTo);
    }

    @Override
    public void assignColorBatch(List<FFeatureRow> rows, ColorDTO color, LocalDate validFrom, LocalDate validTo) {
        if (selectedLayer == null || rows.isEmpty() || color == null) {
            return;
        }
        List<String> ids = new ArrayList<>(rows.size());
        for (FFeatureRow r : rows) {
            ids.add(r.getFeatureId());
        }
        paletteDao.assignColorToGeometries(selectedLayer, ids, color.getColorId(), validFrom, validTo);

        // Optimistisches lokales Update statt vollem Reload
        List<FFeatureRow> old = this.featureRows;
        List<FFeatureRow> next = new ArrayList<>(old);
        for (int i = 0; i < next.size(); i++) {
            FFeatureRow row = next.get(i);
            if (ids.contains(row.getFeatureId())) {
                next.set(i, row.withColor(color, validFrom, validTo));
            }
        }
        this.featureRows = next;
        pcs.firePropertyChange("featureRows", old, next);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

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
