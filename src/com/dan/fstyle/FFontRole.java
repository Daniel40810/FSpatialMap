package com.dan.fstyle;

/**
 * Semantische Schrift-Rollen der F-Stil-Typoskala.
 *
 * <p>Komponenten fragen nicht nach Pixelgrößen, sondern nach einer Rolle
 * ({@code theme.getFont(FFontRole.LABEL)}). Dadurch bleibt die Typografie
 * zentral steuerbar und konsistent über die gesamte Bibliothek.</p>
 *
 * <ul>
 *   <li>{@link #CAPTION} &mdash; kleinste Schrift (Hinweise, Achsenbeschriftung)</li>
 *   <li>{@link #BODY} &mdash; Fließtext / Zellinhalt</li>
 *   <li>{@link #LABEL} &mdash; FLabel, Formularbeschriftung</li>
 *   <li>{@link #BUTTON} &mdash; Schaltflächentext (etwas fetter)</li>
 *   <li>{@link #TITLE} &mdash; Panel-/Karten-Titel</li>
 *   <li>{@link #HEADING} &mdash; Abschnittsüberschrift</li>
 *   <li>{@link #DISPLAY} &mdash; große Kennzahl / Hero-Text</li>
 * </ul>
 *
 * @author com.dan
 */
public enum FFontRole {
    CAPTION,
    BODY,
    LABEL,
    BUTTON,
    TITLE,
    HEADING,
    DISPLAY
}
