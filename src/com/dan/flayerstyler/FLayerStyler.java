package com.dan.flayerstyler;

import com.dan.fbutton.FButton;
import com.dan.fcolorpalette.FColorPaletteEditor;
import com.dan.fcolorpalette.FColorPaletteEditorStyler;
import com.dan.fcolorpalette.FPaletteEditorModel;
import com.dan.fcombobox.FComboBox;
import com.dan.fdal.color.ColorDTO;
import com.dan.fdal.color.PaletteDTO;
import com.dan.ficons.FIconButtonStyler;
import com.dan.ficons.FIconComponent;
import com.dan.ficons.FIconType;
import com.dan.flabel.FLabel;
import com.dan.fstyle.FFontRole;
import com.dan.fstyle.FTheme;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.util.List;

/**
 * Interaktive raeumliche Stil-Verwaltung: Feature-Auswahl + Farbzuweisung mit
 * optionalem Gueltigkeitszeitraum, live in der {@link FLayerMapPreview}
 * sichtbar. Bettet {@link FColorPaletteEditor} (kompakt) als Farbquelle ein —
 * Farben werden aus einer Palette per Klick zugewiesen.
 * <p>
 * Rendert ausschliesslich aus {@link FLayerStylerModel}; die Geometrie-Quelle
 * ist ueber {@link FLayerFeatureProvider} vom konkreten Spatial-Schema entkoppelt.
 */
public class FLayerStyler extends JPanel {

    private FLayerStylerModel model;

    private final FComboBox layerCombo = new FComboBox();
    private final FLabel statsLabel = new FLabel("");
    private final FFeatureTable featureTable = new FFeatureTable();
    private final FLayerMapPreview mapPreview = new FLayerMapPreview();
    private final FDateRangePanel dateRangePanel = new FDateRangePanel();
    private final FColorPaletteEditor paletteEditor;
    private ColorDTO pendingSelectionColor;

    public FLayerStyler(FLayerStylerModel model, FPaletteEditorModel paletteModel) {
        setOpaque(false);
        setLayout(new BorderLayout(10, 10));
        this.paletteEditor = new FColorPaletteEditor(paletteModel);
        FColorPaletteEditorStyler.compact().apply(this.paletteEditor);
        setModel(model);
        buildUi();
    }

    private void buildUi() {
        add(buildNorth(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildEast(), BorderLayout.EAST);
    }

    // ------------------------------------------------------------------
    // Nord: Layer-Auswahl + Statistik + Toolbar
    // ------------------------------------------------------------------

    private JPanel buildNorth() {
        JPanel north = new JPanel(new BorderLayout(10, 4));
        north.setOpaque(false);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);

        FLabel title = new FLabel("Layer-Styler");
        title.setFont(FTheme.getInstance().getFont(FFontRole.TITLE));
        row.add(title);

        FLabel layerLabel = new FLabel("Layer:");
        row.add(layerLabel);
        row.add(layerCombo);
        layerCombo.addActionListener(e -> {
            Object selected = layerCombo.getSelectedItem();
            model.setSelectedLayer(selected == null ? null : selected.toString());
        });

        FIconComponent refresh = FIconButtonStyler.toolbar().build(FIconType.REFRESH);
        refresh.addActionListener(e -> model.reloadLayerNames());
        row.add(refresh);

        row.add(statsLabel);

        north.add(row, BorderLayout.NORTH);
        north.add(dateRangePanel, BorderLayout.SOUTH);
        return north;
    }

    // ------------------------------------------------------------------
    // Center: Feature-Tabelle + Batch-Buttons + Kartenvorschau
    // ------------------------------------------------------------------

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.62);
        split.setOpaque(false);
        split.setBorder(null);

        JPanel tablePanel = new JPanel(new BorderLayout(0, 4));
        tablePanel.setOpaque(false);
        tablePanel.add(new FLabel("Features"), BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(featureTable), BorderLayout.CENTER);
        tablePanel.add(buildBatchToolbar(), BorderLayout.SOUTH);

        JPanel mapPanel = new JPanel(new BorderLayout(0, 4));
        mapPanel.setOpaque(false);
        JPanel mapHeader = new JPanel(new BorderLayout());
        mapHeader.setOpaque(false);
        mapHeader.add(new FLabel("Kartenvorschau"), BorderLayout.WEST);
        FButton zoomFit = new FButton("Zoom to Fit");
        zoomFit.addActionListener(e -> mapPreview.zoomToFit());
        JPanel zoomWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        zoomWrap.setOpaque(false);
        zoomWrap.add(zoomFit);
        mapHeader.add(zoomWrap, BorderLayout.EAST);
        mapPanel.add(mapHeader, BorderLayout.NORTH);
        mapPanel.add(mapPreview, BorderLayout.CENTER);

        split.setTopComponent(tablePanel);
        split.setBottomComponent(mapPanel);
        center.add(split, BorderLayout.CENTER);
        return center;
    }

    private JPanel buildBatchToolbar() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);

        FButton assignSelected = new FButton("Ausgewaehlte einfaerben");
        assignSelected.addActionListener(e -> onAssignToSelection());
        row.add(assignSelected);

        FButton assignAll = new FButton("Alle sichtbaren einfaerben");
        assignAll.addActionListener(e -> onAssignToAll());
        row.add(assignAll);

        FLabel hint = new FLabel("Farbe rechts in der Palette anklicken, dann hier zuweisen");
        hint.setFont(FTheme.getInstance().getFont(FFontRole.CAPTION));
        row.add(hint);
        return row;
    }

    // ------------------------------------------------------------------
    // Ost: eingebetteter FColorPaletteEditor als Farbquelle
    // ------------------------------------------------------------------

    private JPanel buildEast() {
        JPanel east = new JPanel(new BorderLayout());
        east.setOpaque(false);
        east.setPreferredSize(new Dimension(560, 10));
        east.add(paletteEditor, BorderLayout.CENTER);
        return east;
    }

    // ------------------------------------------------------------------
    // Aktionen
    // ------------------------------------------------------------------

    private ColorDTO resolveSourceColor() {
        // Die zuletzt in der eingebetteten Palette markierte Farbe der aktiven
        // Palette dient als Zuweisungs-Quelle (einfachste, vorhersagbare Regel).
        FPaletteEditorModel pm = paletteEditor.getModel();
        List<ColorDTO> colors = pm.getPaletteColors();
        if (colors.isEmpty()) {
            return null;
        }
        return colors.get(colors.size() - 1);
    }

    private void onAssignToSelection() {
        List<FFeatureRow> selected = featureTable.getSelectedRows2();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte zuerst Features in der Tabelle markieren.");
            return;
        }
        assignBatch(selected);
    }

    private void onAssignToAll() {
        List<FFeatureRow> all = model.getFeatureRows();
        if (all.isEmpty()) {
            return;
        }
        assignBatch(all);
    }

    private void assignBatch(List<FFeatureRow> rows) {
        ColorDTO color = resolveSourceColor();
        if (color == null) {
            JOptionPane.showMessageDialog(this,
                    "Bitte rechts eine Palette waehlen, die mindestens eine Farbe enthaelt.");
            return;
        }
        model.assignColorBatch(rows, color, dateRangePanel.getValidFrom(), dateRangePanel.getValidTo());
    }

    // ------------------------------------------------------------------
    // Model-Bindung
    // ------------------------------------------------------------------

    public FLayerStylerModel getModel() {
        return model;
    }

    public void setModel(FLayerStylerModel model) {
        if (this.model != null) {
            this.model.removePropertyChangeListener(this::onModelChanged);
        }
        this.model = model;
        model.addPropertyChangeListener(this::onModelChanged);
        refreshLayerCombo();
        featureTable.setRows(model.getFeatureRows());
        mapPreview.setRows(model.getFeatureRows());
        updateStats();
    }

    private void onModelChanged(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "layerNames":
                refreshLayerCombo();
                break;
            case "selectedLayer":
                Object sel = model.getSelectedLayer();
                if (sel != null && !sel.equals(layerCombo.getSelectedItem())) {
                    layerCombo.setSelectedItem(sel);
                }
                break;
            case "featureRows":
                featureTable.setRows(model.getFeatureRows());
                mapPreview.setRows(model.getFeatureRows());
                updateStats();
                break;
            default:
                break;
        }
    }

    private void refreshLayerCombo() {
        layerCombo.setModel(new DefaultComboBoxModel(model.getLayerNames().toArray()));
    }

    private void updateStats() {
        int total = model.getFeatureRows().size();
        long colored = model.getFeatureRows().stream().filter(FFeatureRow::isColored).count();
        statsLabel.setText(total == 0 ? "" : colored + " / " + total + " gefaerbt");
    }
}
