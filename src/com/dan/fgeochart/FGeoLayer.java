package com.dan.fgeochart;

import com.dan.fmaplegend.FLayerGeometryType;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ein benannter Layer im FGeoChart: haelt Features gleichen Typs plus
 * Darstellungs-Eigenschaften (Farbe, Sichtbarkeit, Strichstaerke, Punkgroesse).
 *
 * Farb-Logik: entweder feste {@link #setColor(Color) Farbe} fuer alle Features,
 * oder eine Farbrampe ({@link #setRampColors(Color, Color)}), die Feature-Werte
 * linear auf einen Verlauf abbildet (COLOR_ADMIN-Palette-Anbindung).
 */
public final class FGeoLayer {

    private final String name;
    private final FLayerGeometryType geometryType;
    private final List<FGeoFeature> features = new ArrayList<FGeoFeature>();

    private Color color = new Color(0x00B5AD);
    private Color rampLow;
    private Color rampHigh;
    private boolean visible = true;
    private float strokeWidth = 2.0f;
    private int pointRadius = 6;
    private float fillAlpha = 0.35f;

    private double minValue = 0.0;
    private double maxValue = 1.0;

    public FGeoLayer(String name, FLayerGeometryType geometryType) {
        this.name = name == null ? "Layer" : name;
        this.geometryType = geometryType == null ? FLayerGeometryType.POINT : geometryType;
    }

    public String getName()                     { return name; }
    public FLayerGeometryType getGeometryType() { return geometryType; }

    public void add(FGeoFeature f) {
        if (f != null) {
            features.add(f);
            if (f.getValue() < minValue) minValue = f.getValue();
            if (f.getValue() > maxValue) maxValue = f.getValue();
        }
    }

    public void clear()                    { features.clear(); }
    public int size()                      { return features.size(); }
    public FGeoFeature get(int index)      { return features.get(index); }
    public List<FGeoFeature> getFeatures() { return Collections.unmodifiableList(features); }

    public Color getColor()             { return color; }
    public void setColor(Color c)       { if (c != null) this.color = c; }

    /** Aktiviert Werte-basierte Farbrampe (low -> high). Null deaktiviert. */
    public void setRampColors(Color low, Color high) {
        this.rampLow = low;
        this.rampHigh = high;
    }

    public boolean hasRamp()            { return rampLow != null && rampHigh != null; }
    public Color getRampLow()           { return rampLow; }
    public Color getRampHigh()          { return rampHigh; }

    /** Wertebereich fuer die Rampe explizit setzen (sonst automatisch aus Features). */
    public void setValueRange(double min, double max) {
        this.minValue = min;
        this.maxValue = max;
    }

    public double getMinValue()         { return minValue; }
    public double getMaxValue()         { return maxValue; }

    /** Farbe eines Features unter Beruecksichtigung der Rampe. */
    public Color colorFor(FGeoFeature f) {
        if (!hasRamp() || maxValue <= minValue) {
            return color;
        }
        double t = (f.getValue() - minValue) / (maxValue - minValue);
        t = Math.max(0.0, Math.min(1.0, t));
        return com.dan.fstyle.FColors.blend(rampLow, rampHigh, (float) t);
    }

    public boolean isVisible()          { return visible; }
    public void setVisible(boolean v)   { this.visible = v; }

    public float getStrokeWidth()       { return strokeWidth; }
    public void setStrokeWidth(float w) { this.strokeWidth = Math.max(0.5f, w); }

    public int getPointRadius()         { return pointRadius; }
    public void setPointRadius(int r)   { this.pointRadius = Math.max(1, r); }

    public float getFillAlpha()         { return fillAlpha; }
    public void setFillAlpha(float a)   { this.fillAlpha = Math.max(0f, Math.min(1f, a)); }

    @Override
    public String toString() {
        return "FGeoLayer{" + name + ", " + geometryType + ", " + features.size() + " features}";
    }
}
