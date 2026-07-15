package com.dan.fspatialmap;

import com.dan.fbutton.FButton;
import com.dan.fradiobutton.FButtonGroup;
import com.dan.fradiobutton.FRadioButton;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.JPanel;

/**
 * Werkzeug-Leiste fuer {@link FSpatialMapViewer}: exklusive Auswahl zwischen
 * {@link FMapTool#PAN}, {@link FMapTool#IDENTIFY} und {@link FMapTool#MEASURE}
 * (per {@link FButtonGroup}/{@link FRadioButton}), plus ein Button, um eine
 * laufende Messung zu verwerfen. Direkt an einen Viewer gebunden - kein
 * getrenntes Model noetig, {@code FMapTool} selbst ist der Zustand.
 */
public class FSpatialMapToolbar extends JPanel {

    private final FButtonGroup group = new FButtonGroup();
    private final FRadioButton panButton = new FRadioButton(FMapTool.PAN.getLabel());
    private final FRadioButton identifyButton = new FRadioButton(FMapTool.IDENTIFY.getLabel());
    private final FRadioButton measureButton = new FRadioButton(FMapTool.MEASURE.getLabel());
    private final FButton clearMeasureButton = new FButton("Messung l\u00F6schen");

    private FSpatialMapViewer viewer;

    public FSpatialMapToolbar() {
        super(new FlowLayout(FlowLayout.LEFT, 10, 4));
        setOpaque(false);

        group.add(panButton);
        group.add(identifyButton);
        group.add(measureButton);
        panButton.setSelected(true);

        panButton.addActionListener(e -> setToolOnViewer(FMapTool.PAN));
        identifyButton.addActionListener(e -> setToolOnViewer(FMapTool.IDENTIFY));
        measureButton.addActionListener(e -> setToolOnViewer(FMapTool.MEASURE));
        clearMeasureButton.addActionListener(e -> {
            if (viewer != null) {
                viewer.clearMeasurement();
            }
        });

        add(panButton);
        add(identifyButton);
        add(measureButton);
        add(clearMeasureButton);
    }

    public FSpatialMapToolbar(FSpatialMapViewer viewer) {
        this();
        setViewer(viewer);
    }

    public FSpatialMapViewer getViewer() {
        return viewer;
    }

    public void setViewer(FSpatialMapViewer viewer) {
        this.viewer = viewer;
        if (viewer != null) {
            selectButtonFor(viewer.getTool());
        }
    }

    private void setToolOnViewer(FMapTool tool) {
        if (viewer != null) {
            viewer.setTool(tool);
        }
    }

    private void selectButtonFor(FMapTool tool) {
        switch (tool) {
            case IDENTIFY:
                identifyButton.setSelected(true);
                break;
            case MEASURE:
                measureButton.setSelected(true);
                break;
            default:
                panButton.setSelected(true);
        }
    }

    /** Akzentfarbe der Radiobuttons/des Loeschen-Buttons - Default Tuerkis (LABORATORY_DARK-Primaerfarbe). */
    public void setAccentColor(Color color) {
        group.setColorScheme(color, color.darker());
    }
}
