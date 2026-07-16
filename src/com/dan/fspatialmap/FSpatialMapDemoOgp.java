package com.dan.fspatialmap;

import com.dan.fdal.sdo.ConnectionProvider;
import com.dan.fdal.sdo.OracleSdoFeatureProvider;
import com.dan.fdal.sdo.SdoParseException;
import com.dan.fframe.FFrame;
import com.dan.fgeochart.DefaultFGeoChartModel;
import com.dan.fgeochart.FGeoFeature;
import com.dan.fgeochart.FGeoLayer;
import com.dan.flabel.FLabel;
import com.dan.flayerstyler.FFeatureRow;
import com.dan.fmaplegend.FLayerGeometryType;
import com.dan.fscrollpane.FScrollPane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Live-Variante von {@link FSpatialMapDemo}: laedt echte Schleswig-Holstein-
 * Geometrien (aus OSM/Overpass importiert) aus dem Oracle-21c-Schema
 * {@code OGP} statt der Beispiel-Quadrate.
 * <p>
 * <b>Update 16.07.2026:</b> Der urspruengliche Plan, MULTI*-Geometrien per
 * SQL-Filter zu ueberspringen, reichte nicht - die weitaus meisten Polygone
 * hier haben Loecher (Enklaven/Inseln), was schon bei GTYPE=2003 (einfaches
 * POLYGON) zur SdoParseException fuehrte, nicht erst bei MULTI*. Deshalb
 * wurde {@link com.dan.fdal.sdo.SdoGeometryParser} erweitert: Aussenringe
 * werden jetzt beliebig oft unterstuetzt (echte Multipolygone -> mehrere
 * FGeoFeature pro Zeile), Innenringe/Loecher werden bewusst verworfen
 * (Flaeche erscheint dann ohne Aussparung statt gar nicht). Ein SQL-Filter
 * ist daher nicht mehr noetig; einzelne, wirklich nicht unterstuetzte
 * Geometrien (z.B. Kreisbogen-Ringe) werden jetzt pro Zeile uebersprungen
 * und geloggt statt den ganzen Layer abzubrechen.
 * <p>
 * Gemeinden, deren Relation beim Overpass-Export nicht zu einem geschlossenen
 * Ring zusammengesetzt werden konnte, liegen als einzelner Punkt vor
 * (GTYPE 2001, ca. 629 von 1892) - werden bewusst als Punkt-Marker mit
 * gerendert statt gefiltert.
 * <p>
 * Nicht Teil des JARs - nur Demo/Referenz, analog {@link FSpatialMapDemo}.
 */
public final class FSpatialMapDemoOgp {

    private static final String JDBC_URL = "jdbc:oracle:thin:@//localhost:1521/PDBORCL";
    private static final String DB_USER = "ogp";
    private static final String DB_PASSWORD = "ogp";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FSpatialMapDemoOgp::buildAndShow);
    }

    private static void buildAndShow() {
        FFrame frame = new FFrame("FSpatialMap \u2014 Schleswig-Holstein (Live: OGP/PDBORCL)");
        frame.setPreferredFrameSize(new Dimension(1180, 780));
        frame.setTaskbarColors(new Color(0x01, 0x67, 0x6A), new Color(0x8B, 0x00, 0x24));

        FSpatialMapViewer viewer = new FSpatialMapViewer();
        FSpatialMapStyler.standard().apply(viewer);
        // Grober Mittelpunkt von Schleswig-Holstein, weit herausgezoomt
        viewer.setView(9.85, 54.30, 8.7);

        FLabel status = new FLabel("Lade Daten aus OGP/PDBORCL \u2026");

        ConnectionProvider connectionProvider = () ->
                DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);

        DefaultFGeoChartModel vectorModel;
        try {
            vectorModel = loadVectorModel(connectionProvider, status);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null,
                    "Laden aus OGP/PDBORCL fehlgeschlagen:\n" + ex.getMessage(),
                    "Datenbankfehler", JOptionPane.ERROR_MESSAGE);
            vectorModel = new DefaultFGeoChartModel();
        }
        viewer.setVectorModel(vectorModel);

        FGeoLayer gemeinden = vectorModel.findLayer("Gemeinden");
        if (gemeinden != null) {
            FHeatmapApplier.autoNormalize(gemeinden);
        }

        viewer.addSpatialMapListener(new FSpatialMapListener() {
            @Override
            public void mapClicked(double lon, double lat) {
                status.setText(String.format(Locale.GERMANY,
                        "Klick: %.5f\u00B0, %.5f\u00B0", lon, lat));
            }

            @Override
            public void featureClicked(FGeoLayer layer, FGeoFeature feature) {
                status.setText("Feature: " + feature.getName() + " (Layer: " + layer.getName() + ")");
            }

            @Override
            public void featureRowClicked(FFeatureRow row) {
                status.setText("Feature: " + row.getFeature().getName());
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

    /**
     * Baut das Vektor-Modell aus den vier OGP-Tabellen. Jede Layer-SQL liefert
     * exakt drei Spalten in der von {@link OracleSdoFeatureProvider} erwarteten
     * Reihenfolge: ID (NUMBER, aus VARCHAR2 "relation/NNNNN" per REGEXP
     * herausgeloest), NAME, GEOM.
     */
    private static DefaultFGeoChartModel loadVectorModel(ConnectionProvider connectionProvider, FLabel status) {
        Map<String, String> layerQueries = new LinkedHashMap<>();
        layerQueries.put("Gemeinden", idNameGeomQuery("GEMEINDEN_SH"));
        layerQueries.put("Schutzgebiete", idNameGeomQuery("SCHUTZGEBIETE"));
        layerQueries.put("Seen", idNameGeomQuery("SEEN"));
        layerQueries.put("Wald", idNameGeomQuery("WALD"));

        OracleSdoFeatureProvider provider = new OracleSdoFeatureProvider(connectionProvider, layerQueries);

        DefaultFGeoChartModel model = new DefaultFGeoChartModel();

        addLayer(model, provider, "Gemeinden", FLayerGeometryType.POLYGON,
                new Color(0x00, 0xB5, 0xAD), 0.35f, status);
        addLayer(model, provider, "Schutzgebiete", FLayerGeometryType.POLYGON,
                new Color(0x8B, 0x00, 0x24), 0.35f, status);
        addLayer(model, provider, "Seen", FLayerGeometryType.POLYGON,
                new Color(0x3B, 0x8A, 0xD4), 0.55f, status);
        addLayer(model, provider, "Wald", FLayerGeometryType.POLYGON,
                new Color(0x2E, 0x7D, 0x32), 0.45f, status);

        status.setText("Bereit \u2014 Werkzeug: Verschieben");
        return model;
    }

    private static String idNameGeomQuery(String table) {
        return "SELECT TO_NUMBER(REGEXP_REPLACE(g.ID, '[^0-9]', '')) AS ID, "
                + "g.NAME AS NAME, g.GEOM AS GEOM "
                + "FROM " + table + " g";
    }

    private static void addLayer(DefaultFGeoChartModel model, OracleSdoFeatureProvider provider,
                                  String layerName, FLayerGeometryType type, Color color,
                                  float fillAlpha, FLabel status) {
        status.setText("Lade Layer \"" + layerName + "\" \u2026");
        FGeoLayer layer = model.addLayer(layerName, type, color);
        layer.setFillAlpha(fillAlpha);
        layer.setStrokeWidth(1.2f);
        try {
            List<FGeoFeature> features = provider.findFeatures(layerName);
            for (FGeoFeature f : features) {
                layer.add(f);
            }
            System.out.println(layerName + ": " + features.size() + " Features geladen.");
        } catch (SdoParseException ex) {
            // Trotz GTYPE-Filter kann im Einzelfall eine "einfache" Geometrie
            // zusammengesetzt sein (z.B. compound curve) - Layer bleibt dann
            // teilweise leer statt die ganze App abzubrechen.
            System.err.println("Layer \"" + layerName + "\" konnte nicht vollstaendig geladen werden: "
                    + ex.getMessage());
        }
    }

    private FSpatialMapDemoOgp() {
    }
}
