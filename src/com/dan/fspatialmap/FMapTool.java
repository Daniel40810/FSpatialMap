package com.dan.fspatialmap;

/**
 * Aktives Werkzeug fuer Mausklicks auf {@link FSpatialMapViewer}
 * (siehe {@link FSpatialMapViewer#setTool}, gesteuert z.B. von {@link FSpatialMapToolbar}).
 */
public enum FMapTool {

    /** Standard: Klick feuert {@code mapClicked}/{@code featureClicked}, Drag pannt. */
    PAN("Verschieben"),

    /** Klick zeigt ein Attribut-Popup fuer das getroffene Feature (Vektor- oder ColorAdmin-Layer). */
    IDENTIFY("Abfragen"),

    /** Klick fuegt einen Messpunkt hinzu; Strecke wird kumulativ eingeblendet. Rechtsklick setzt zurueck. */
    MEASURE("Messen");

    private final String label;

    FMapTool(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
