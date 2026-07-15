package com.dan.fspatialmap;

import com.dan.faccordion.DefaultFAccordionModel;
import com.dan.faccordion.FAccordion;
import com.dan.fcheckbox.FCheckBox;
import com.dan.fgeochart.FGeoChartModel;
import com.dan.fgeochart.FGeoLayer;
import com.dan.fslider.FSlider;
import com.dan.fstyle.FColors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.beans.PropertyChangeListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Layer-Panel fuer {@link FSpatialMapViewer}: eine {@link FAccordion}-Sektion je
 * {@link FGeoLayer} des angehaengten {@link FGeoChartModel}, mit Sichtbarkeits-
 * {@link FCheckBox} und Transparenz-{@link FSlider} - beides live an den
 * Viewer gekoppelt (kein Extra-"Anwenden"-Knopf, jede Aenderung wirkt sofort).
 * <p>
 * Anders als die ColorAdmin-Live-Kopplung aus Phase 3 (die genau den einen im
 * {@code FLayerStyler} <em>ausgewaehlten</em> Layer zeigt) steuert dieses Panel
 * <b>alle</b> Layer des Vektor-Overlays gleichzeitig - das ist der Sinn von Phase 5.
 */
public class FLayerPanel extends JPanel {

    private final FAccordion accordion = new FAccordion();
    private FSpatialMapViewer viewer;
    private FGeoChartModel model;
    private final PropertyChangeListener modelListener = evt -> rebuild();

    public FLayerPanel() {
        super(new BorderLayout());
        setOpaque(false);
        add(accordion, BorderLayout.CENTER);
    }

    public FLayerPanel(FSpatialMapViewer viewer) {
        this();
        setViewer(viewer);
    }

    /** Der Viewer, dessen Layer-Sichtbarkeit/-Transparenz dieses Panel steuert. */
    public FSpatialMapViewer getViewer() {
        return viewer;
    }

    public void setViewer(FSpatialMapViewer viewer) {
        this.viewer = viewer;
        setModel(viewer != null ? viewer.getVectorModel() : null);
    }

    /**
     * Normalerweise nicht direkt gesetzt - {@link #setViewer} liest das Model
     * vom Viewer. Explizit nutzbar, wenn das Panel unabhaengig vom Viewer ein
     * Vektor-Overlay-Model anzeigen soll (z.B. Vorschau vor {@code setVectorModel}).
     */
    public void setModel(FGeoChartModel model) {
        if (this.model != null) {
            this.model.removePropertyChangeListener(modelListener);
        }
        this.model = model;
        if (model != null) {
            model.addPropertyChangeListener(modelListener);
        }
        rebuild();
    }

    public FGeoChartModel getModel() {
        return model;
    }

    public FAccordion getAccordion() {
        return accordion;
    }

    private void rebuild() {
        DefaultFAccordionModel am = new DefaultFAccordionModel();
        if (model != null) {
            for (FGeoLayer layer : model.getLayers()) {
                am.addSection(layer.getName(), buildLayerRow(layer),
                        layer.size() + " Feature" + (layer.size() == 1 ? "" : "s"));
            }
        }
        accordion.setModel(am);
    }

    private JPanel buildLayerRow(FGeoLayer layer) {
        JPanel row = new JPanel(new BorderLayout(8, 4));
        row.setOpaque(false);

        FCheckBox visible = new FCheckBox("Sichtbar");
        visible.setSelected(layer.isVisible());
        visible.setLiquidColorTop(layer.getColor() != null ? layer.getColor() : FColors.hex("#00B5AD"));
        visible.addActionListener(e -> {
            layer.setVisible(visible.isSelected());
            if (viewer != null) {
                viewer.repaint();
            }
        });

        JLabel opacityLabel = new JLabel("Transparenz");
        opacityLabel.setHorizontalAlignment(SwingConstants.LEFT);

        int initialPercent = viewer != null ? Math.round(viewer.getLayerOpacity(layer) * 100) : 100;
        FSlider opacity = new FSlider(0, 100, initialPercent);
        opacity.setShowValueBubble(true);
        opacity.addChangeListener(e -> {
            if (viewer != null) {
                viewer.setLayerOpacity(layer, opacity.getValue() / 100f);
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        top.setOpaque(false);
        top.add(visible);

        JPanel bottom = new JPanel(new BorderLayout(6, 0));
        bottom.setOpaque(false);
        bottom.add(opacityLabel, BorderLayout.WEST);
        bottom.add(opacity, BorderLayout.CENTER);

        row.add(top, BorderLayout.NORTH);
        row.add(bottom, BorderLayout.SOUTH);
        return row;
    }
}
