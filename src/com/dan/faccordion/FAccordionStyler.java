package com.dan.faccordion;

import java.awt.Color;

/**
 * Wiederverwendbares Aussehens-Preset fuer {@link FAccordion}. Mutiert
 * kein Model, startet keine Timer — setzt nur Geometrie-/Farb-Properties.
 */
public final class FAccordionStyler {

    public int headerHeight = 40;
    public int sectionSpacing = 7;
    public int arc = 14;
    public boolean multipleOpen = true;
    public boolean collapsible = true;
    public double animationSpeed = 0.16;
    public float waveAmplitude = 6f;
    public FChevronPosition chevronPosition = FChevronPosition.RIGHT;
    public Color liquidColorTop;
    public Color liquidColorBottom;

    public FAccordionStyler() {
    }

    public void apply(FAccordion accordion) {
        accordion.setHeaderHeight(headerHeight);
        accordion.setSectionSpacing(sectionSpacing);
        accordion.setArc(arc);
        accordion.setMultipleOpen(multipleOpen);
        accordion.setCollapsible(collapsible);
        accordion.setAnimationSpeed(animationSpeed);
        accordion.setWaveAmplitude(waveAmplitude);
        accordion.setChevronPosition(chevronPosition);
        if (liquidColorTop != null) {
            accordion.setLiquidColorTop(liquidColorTop);
        }
        if (liquidColorBottom != null) {
            accordion.setLiquidColorBottom(liquidColorBottom);
        }
    }

    /** Kompakt fuer Sidebars/Tool-Panels: niedrigere Header, wenig Abstand. */
    public static FAccordionStyler compact() {
        FAccordionStyler s = new FAccordionStyler();
        s.headerHeight = 32;
        s.sectionSpacing = 4;
        s.arc = 10;
        s.animationSpeed = 0.22;
        s.waveAmplitude = 4f;
        return s;
    }

    /** Geraeumig fuer Dialoge/Einstellungsseiten: hohe Header, viel Abstand. */
    public static FAccordionStyler spacious() {
        FAccordionStyler s = new FAccordionStyler();
        s.headerHeight = 48;
        s.sectionSpacing = 12;
        s.arc = 18;
        s.animationSpeed = 0.12;
        s.waveAmplitude = 8f;
        return s;
    }
}
