package com.dan.fslider;

import java.util.Locale;

/**
 * Formatiert die Tick-Beschriftung der {@link FSliderVariant#TICKS}-Variante sowie
 * die Wert-Bubble beim Ziehen. Der {@link FSlider} arbeitet intern immer mit
 * {@code int}-Werten (Vertrag von {@link javax.swing.JSlider}); der Formatter
 * bestimmt nur die Anzeige, z. B. "75%", "120 px" oder "4,5" (Zehntel-Skalierung).
 *
 * <p>Java-8-Functional-Interface &ndash; per Lambda direkt nutzbar:
 * <pre>{@code
 * slider.setTickLabelFormatter(v -> v + " px");
 * }</pre>
 *
 * Ist kein Formatter gesetzt ({@code null}, Default), wird der rohe Integer-Wert
 * angezeigt (bisheriges Verhalten).
 */
@FunctionalInterface
public interface FSliderLabelFormatter {

    /** Formatiert einen einzelnen Slider-Wert fuer die Anzeige. */
    String format(int value);

    // ---- Vorgefertigte Presets --------------------------------------------

    /** Haengt ein Prozentzeichen an: {@code 75 -> "75%"}. */
    static FSliderLabelFormatter percent() {
        return v -> v + "%";
    }

    /** Haengt eine feste Einheit mit Leerzeichen an, z. B. {@code suffix("px") -> "120 px"}. */
    static FSliderLabelFormatter suffix(String unit) {
        return v -> v + " " + unit;
    }

    /**
     * Interpretiert den internen Integer-Wert skaliert als Dezimalzahl, z. B.
     * bei {@code divisor=10, fractionDigits=1}: Wert {@code 42 -> "4,2"}.
     * Nuetzlich, weil {@link javax.swing.JSlider} selbst nur Ganzzahlen kennt.
     * Format folgt deutscher Locale (Komma als Dezimaltrenner).
     */
    static FSliderLabelFormatter decimal(int divisor, int fractionDigits) {
        final int div = Math.max(1, divisor);
        return v -> String.format(Locale.GERMANY, "%." + fractionDigits + "f", v / (double) div);
    }
}
