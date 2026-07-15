package com.dan.fstyle;

import com.dan.flabel.FLabel;

import javax.swing.JComponent;
import java.awt.Color;
import java.lang.ref.WeakReference;

/**
 * Wendet {@link FTheme} auf F-Komponenten an &mdash; extern, ohne die
 * Komponenten selbst zu verändern (Styler-Prinzip des f-style-swing-Skills).
 *
 * <p>Zwei Modi:</p>
 * <ul>
 *   <li><b>Einmalig</b>: {@link #styleLabel(FLabel)} überträgt den aktuellen
 *       Theme-Zustand auf eine Instanz.</li>
 *   <li><b>Live</b>: {@link #bindLabel(FLabel)} registriert einen
 *       <i>schwachen</i> Listener, der die Komponente bei jeder Theme-Änderung
 *       neu stylt und sich automatisch abmeldet, sobald die Komponente vom GC
 *       eingesammelt wurde (kein Leak durch den Singleton).</li>
 * </ul>
 *
 * @author com.dan
 */
public final class FComponentStyler {

    private FComponentStyler() { }

    // ============================================================== FLabel

    /** Überträgt den aktuellen Theme-Zustand einmalig auf {@code label}. */
    public static void styleLabel(FLabel label) {
        if (label == null) return;
        FTheme t = FTheme.getInstance();

        // Farben
        label.setLiquidColorTop(t.getPrimary());
        label.setLiquidColorBottom(t.getAccent());
        label.setForeground(t.getText());
        label.setHoverTextColor(FColors.lighten(t.getText(), 0.18f));
        label.setShadowColor(t.getShadow());
        label.setGlassRim(t.getGlassRim());
        label.setGlassBody(t.getGlassBody());
        label.setGlassHighlight(t.getGlassHighlight());
        label.setRippleColor(t.getRipple());
        label.setBgAlpha(t.getGlassFillAlpha());

        // Schrift
        label.setFont(t.getFont(FFontRole.LABEL));

        // Animation
        FAnimationConfig a = t.getAnimationConfig();
        label.setHoverEaseIn(a.hoverEaseIn);
        label.setHoverEaseOut(a.hoverEaseOut);
        label.setRippleSpeed(a.rippleSpeed);
        label.setRippleFade(a.rippleFade);

        // Geometrie
        FGeometryConfig g = t.getGeometryConfig();
        label.setPadX(g.padX);
        label.setPadY(g.padY);
        label.setArc(g.arc);
        label.setRimStrokeWidth(g.rimStrokeWidth);
        label.setAccentHeight(g.accentHeight);
    }

    /**
     * Wie {@link #styleLabel(FLabel)}, registriert die Komponente aber zusätzlich
     * für Live-Updates. Der zurückgegebene Listener kann via
     * {@link FTheme#removeThemeListener} manuell wieder entfernt werden.
     */
    public static FTheme.ThemeListener bindLabel(final FLabel label) {
        styleLabel(label);
        final WeakReference<FLabel> ref = new WeakReference<FLabel>(label);
        final FTheme.ThemeListener[] holder = new FTheme.ThemeListener[1];
        FTheme.ThemeListener l = new FTheme.ThemeListener() {
            @Override public void themeChanged(FTheme theme) {
                FLabel lb = ref.get();
                if (lb == null) {
                    theme.removeThemeListener(holder[0]); // selbst abmelden
                    return;
                }
                styleLabel(lb);
            }
        };
        holder[0] = l;
        FTheme.getInstance().addThemeListener(l);
        return l;
    }

    // ============================================================== generisch

    /** Setzt Schrift (per Rolle) und Vordergrundfarbe aus dem Theme. */
    public static void applyText(JComponent c, FFontRole role) {
        if (c == null) return;
        FTheme t = FTheme.getInstance();
        c.setFont(t.getFont(role));
        c.setForeground(t.getText());
    }

    /** Färbt {@code c} flächig als Theme-Hintergrund (Background) ein. */
    public static void applyBackground(JComponent c) {
        if (c == null) return;
        c.setOpaque(true);
        c.setBackground(FTheme.getInstance().getBackground());
    }

    /** Bequemer Zugriff auf eine getönte Akzentfarbe (z.&nbsp;B. Hover-Tint). */
    public static Color accentTint(float alpha01) {
        return FColors.withAlpha(FTheme.getInstance().getPrimary(), alpha01);
    }
}
