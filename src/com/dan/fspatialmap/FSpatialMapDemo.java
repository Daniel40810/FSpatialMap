package com.dan.fspatialmap;

import com.dan.fframe.FFrame;
import com.dan.fdal.color.ColorDTO;
import com.dan.fdal.color.PaletteDTO;
import com.dan.fgeochart.DefaultFGeoChartModel;
import com.dan.fgeochart.FGeoFeature;
import com.dan.fgeochart.FGeoLayer;
import com.dan.flabel.FLabel;
import com.dan.flayerstyler.FFeatureRow;
import com.dan.flayerstyler.FLayerStylerModel;
import com.dan.fmaplegend.FLayerGeometryType;
import com.dan.fscrollpane.FScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Gesamt-Demo der FSpatialMap-Familie (Phasen 1-6 in einem Fenster):
 * WMS-Basiskarte + Vektor-Overlay (Polygon/Punkt) + Heatmap-Palette auf einem
 * Layer + Layer-Panel (Sichtbarkeit/Transparenz) + Toolbar (Pan/Identify/Measure)
 * + ColorAdmin-Live-Kopplung (mit einem simplen Stub statt echter Oracle-Verbindung,
 * damit die Demo ohne Datenbank laeuft - im echten Projekt {@code DefaultFLayerStylerModel}
 * mit {@code ColorAdminDAO}/{@code FPaletteAdminDAO} verwenden).
 * <p>
 * Nicht Teil des JARs (siehe f-style-swing-Skill, Abschnitt 9) - nur Demo/Referenz.
 */
public final class FSpatialMapDemo {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FSpatialMapDemo::buildAndShow);
    }

    private static void buildAndShow() {
        FFrame frame = new FFrame("FSpatialMap Demo \u2014 Schleswig-Holstein");
        frame.setPreferredFrameSize(new Dimension(1180, 780));
        frame.setTaskbarColors(new Color(0x01, 0x67, 0x6A), new Color(0x8B, 0x00, 0x24));

        FSpatialMapViewer viewer = new FSpatialMapViewer();
        FSpatialMapStyler.standard().apply(viewer);
        viewer.setView(10.1228, 54.3233, 11.0); // Kiel

        DefaultFGeoChartModel vectorModel = buildSampleVectorModel();
        viewer.setVectorModel(vectorModel);

        // Heatmap (Phase 4): "Gemeinden" nach simuliertem Einwohnerwert einfaerben
        FGeoLayer gemeinden = vectorModel.findLayer("Gemeinden");
        FHeatmapApplier.autoNormalize(gemeinden);
        viewer.setLayerPalette(gemeinden, viridisPalette());

        // ColorAdmin-Live-Kopplung (Phase 3) - Demo-Stub statt echter DB-Verbindung
        DemoLayerStylerModel colorAdminModel = new DemoLayerStylerModel(vectorModel.findLayer("Schutzgebiete"));
        viewer.setColorAdminModel(colorAdminModel);

        FLabel status = new FLabel("Bereit \u2014 Werkzeug: Verschieben");
        viewer.addSpatialMapListener(new FSpatialMapListener() {
            @Override
            public void mapClicked(double lon, double lat) {
                status.setText(String.format(java.util.Locale.GERMANY,
                        "Klick: %.5f\u00B0, %.5f\u00B0", lon, lat));
            }

            @Override
            public void featureClicked(FGeoLayer layer, FGeoFeature feature) {
                status.setText("Feature: " + feature.getName() + " (Layer: " + layer.getName() + ")");
            }

            @Override
            public void featureRowClicked(FFeatureRow row) {
                status.setText("ColorAdmin-Feature: " + row.getFeature().getName()
                        + (row.isColored() ? " \u2014 " + row.getColor().getColorName() : " \u2014 ungef\u00E4rbt"));
            }
        });

        FSpatialMapToolbar toolbar = new FSpatialMapToolbar(viewer);

        FLayerPanel layerPanel = new FLayerPanel(viewer);
        FScrollPane layerScroll = new FScrollPane(layerPanel);
        layerScroll.setBorder(BorderFactory.createEmptyBorder());
        layerScroll.setPreferredSize(new Dimension(300, 100));
        layerScroll.getViewport().setOpaque(false);
        layerScroll.setOpaque(false);

        JPanel root = frame.getComponentPane();
        root.setBackground(new Color(0x12, 0x14, 0x1E));
        root.add(toolbar, BorderLayout.NORTH);
        root.add(viewer, BorderLayout.CENTER);
        root.add(layerScroll, BorderLayout.WEST);
        root.add(status, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    static DefaultFGeoChartModel buildSampleVectorModel() {
        DefaultFGeoChartModel model = new DefaultFGeoChartModel();

        // Polygon-Layer "Gemeinden" - wird per Heatmap-Palette eingefaerbt (Phase 4)
        FGeoLayer gemeinden = model.addLayer("Gemeinden", FLayerGeometryType.POLYGON, new Color(0x00, 0xB5, 0xAD));
        gemeinden.setFillAlpha(0.45f);
        gemeinden.setStrokeWidth(1.5f);
        gemeinden.add(square(1, "Altstadt", 10.125, 54.320, 0.012, 82.0));
        gemeinden.add(square(2, "Ravensberg", 10.145, 54.315, 0.012, 45.0));
        gemeinden.add(square(3, "Gaarden", 10.140, 54.310, 0.012, 63.0));
        gemeinden.add(square(4, "Wik", 10.115, 54.335, 0.012, 28.0));
        gemeinden.add(square(5, "Elmschenhagen", 10.155, 54.300, 0.012, 15.0));

        // Polygon-Layer "Schutzgebiete" - wird per ColorAdmin-Stub gefaerbt (Phase 3)
        FGeoLayer schutz = model.addLayer("Schutzgebiete", FLayerGeometryType.POLYGON, new Color(0x8B, 0x00, 0x24));
        schutz.setFillAlpha(0.35f);
        schutz.add(square(10, "Vogelschutzgebiet Nord", 10.08, 54.34, 0.02, 0));
        schutz.add(square(11, "Wattenmeer-Puffer", 10.20, 54.28, 0.02, 0));

        // Punkt-Layer "Messstationen"
        FGeoLayer stationen = model.addLayer("Messstationen", FLayerGeometryType.POINT, new Color(0xFF, 0xD0, 0x40));
        stationen.setPointRadius(7);
        stationen.add(FGeoFeature.point(20, "Pegel Kiel-F\u00F6rde", 10.135, 54.325));
        stationen.add(FGeoFeature.point(21, "Wetterstation Holtenau", 10.150, 54.370));

        return model;
    }

    private static FGeoFeature square(long id, String name, double centerLon, double centerLat, double half, double value) {
        double[] ring = {
            centerLon - half, centerLat - half,
            centerLon + half, centerLat - half,
            centerLon + half, centerLat + half,
            centerLon - half, centerLat + half,
            centerLon - half, centerLat - half
        };
        Map<String, String> attrs = new HashMap<>();
        attrs.put("Einwohnerdichte", String.valueOf(value));
        return new FGeoFeature(id, name, FLayerGeometryType.POLYGON, ring, value, attrs);
    }

    static PaletteDTO viridisPalette() {
        return new PaletteDTO(1, "Viridis-artig", "Demo-Palette", Arrays.asList(
                color(1, "0", 0x44, 0x01, 0x54),
                color(2, "1", 0x3B, 0x52, 0x8B),
                color(3, "2", 0x21, 0x91, 0x8D),
                color(4, "3", 0x5D, 0xC9, 0x63),
                color(5, "4", 0xFD, 0xE7, 0x25)
        ));
    }

    private static ColorDTO color(int id, String name, int r, int g, int b) {
        return new ColorDTO(id, name, r, g, b, 255, "#", "#", 0f, 0f, 0f, Collections.emptyList());
    }

    /**
     * Minimaler Demo-Stub statt {@code DefaultFLayerStylerModel} + echter
     * {@code ColorAdminDAO}/{@code FPaletteAdminDAO}-Verbindung. Faerbt die
     * Features des uebergebenen Layers reihum mit drei Demo-Farben ein, damit
     * die Live-Kopplung (Phase 3) ohne Datenbank sichtbar wird.
     */
    private static final class DemoLayerStylerModel implements FLayerStylerModel {
        private final FGeoLayer layer;
        private final List<FFeatureRow> rows;
        private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

        DemoLayerStylerModel(FGeoLayer layer) {
            this.layer = layer;
            ColorDTO[] palette = {
                color(101, "Schutzstufe A", 0x2E, 0xCC, 0x71),
                color(102, "Schutzstufe B", 0xF3, 0x9C, 0x12)
            };
            List<FFeatureRow> built = new java.util.ArrayList<>();
            int i = 0;
            for (FGeoFeature f : layer.getFeatures()) {
                built.add(new FFeatureRow(f, palette[i % palette.length], null, null));
                i++;
            }
            this.rows = built;
        }

        @Override
        public List<String> getLayerNames() {
            return Collections.singletonList(layer.getName());
        }

        @Override
        public void reloadLayerNames() {
        }

        @Override
        public String getSelectedLayer() {
            return layer.getName();
        }

        @Override
        public void setSelectedLayer(String l) {
        }

        @Override
        public List<FFeatureRow> getFeatureRows() {
            return rows;
        }

        @Override
        public void assignColor(FFeatureRow row, ColorDTO color) {
        }

        @Override
        public void assignColor(FFeatureRow row, ColorDTO color, java.time.LocalDate from, java.time.LocalDate to) {
        }

        @Override
        public void assignColorBatch(List<FFeatureRow> rows, ColorDTO color, java.time.LocalDate from, java.time.LocalDate to) {
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

    private FSpatialMapDemo() {
    }
}
