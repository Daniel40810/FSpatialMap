package com.dan.fspatialmap;

import java.awt.Color;

/**
 * Geometrie-Look fuer den ColorAdmin-Live-Layer (siehe
 * {@link FSpatialMapViewer#setColorAdminModel}). Bewusst als eigene, kleine
 * Konfigurationsklasse mit oeffentlichen Feldern - analog zu
 * {@code FAnimationConfig}/{@code FGeometryConfig} aus {@code com.dan.fstyle} -
 * statt in {@link FSpatialMapStyler}, weil sie ausschliesslich diesen einen
 * Layer betrifft und unabhaengig vom allgemeinen Chrome-Preset bleibt.
 */
public final class FColorAdminLayerStyle {

    public int pointRadius = 7;
    public float strokeWidth = 2.5f;
    public float fillAlpha = 0.40f;

    /** Farbe fuer Features ohne ColorAdmin-Zuweisung ({@code FFeatureRow.isColored() == false}). */
    public Color uncoloredColor = new Color(120, 130, 140, 160);

    /**
     * Wenn {@code true} (Default), werden nur Zeilen gemalt, deren Gueltigkeitszeitraum
     * {@link #referenceDate} einschliesst (bzw. die unbefristet sind). Passend zum
     * {@code valid_from}/{@code valid_to}-Konzept aus {@code CA_GEOM_COLOR}.
     */
    public boolean respectValidity = true;

    /** {@code null} = heute (Default, wird bei jedem Paint frisch ausgewertet). */
    public java.time.LocalDate referenceDate = null;

    public FColorAdminLayerStyle() {
    }

    public static FColorAdminLayerStyle defaults() {
        return new FColorAdminLayerStyle();
    }

    public FColorAdminLayerStyle copy() {
        FColorAdminLayerStyle c = new FColorAdminLayerStyle();
        c.pointRadius = pointRadius;
        c.strokeWidth = strokeWidth;
        c.fillAlpha = fillAlpha;
        c.uncoloredColor = uncoloredColor;
        c.respectValidity = respectValidity;
        c.referenceDate = referenceDate;
        return c;
    }
}
