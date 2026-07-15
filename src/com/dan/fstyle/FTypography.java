package com.dan.fstyle;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Zentrale Typografie-Konfiguration des F-Stils.
 *
 * <p>Hält eine <b>Familien-Fallback-Kette</b> sowie pro {@link FFontRole} eine
 * Größe (pt) und einen Stil ({@link Font#PLAIN}/{@link Font#BOLD}&hellip;).
 * {@link #getFont(FFontRole)} liefert das fertige, gecachte {@link Font}-Objekt.</p>
 *
 * <p>Die Familie wird gegen die real installierten Schriften aufgelöst: Ist die
 * bevorzugte Familie (z.&nbsp;B. {@code "Segoe UI"}) nicht vorhanden, greift die
 * Fallback-Kette bis hin zur logischen Schrift {@code SansSerif}. So sieht die
 * Bibliothek auf jedem System vernünftig aus, ohne Asset-Schriften mitzuliefern.</p>
 *
 * <p>Optionales <b>Tracking</b> (Laufweite) wird per {@link TextAttribute#TRACKING}
 * angewandt; nicht-ganzzahlige Größen über {@link TextAttribute#SIZE}.</p>
 *
 * @author com.dan
 */
public class FTypography {

    // ---- bevorzugte Familie + Fallback-Kette ----
    private String fontFamily = "Segoe UI";
    private String[] fallbackChain = {
            "Segoe UI", "Inter", "Roboto", "Open Sans", "Noto Sans",
            "DejaVu Sans", "Liberation Sans", Font.SANS_SERIF
    };

    // ---- Skala je Rolle ----
    private final EnumMap<FFontRole, Float> sizes  = new EnumMap<FFontRole, Float>(FFontRole.class);
    private final EnumMap<FFontRole, Integer> styles = new EnumMap<FFontRole, Integer>(FFontRole.class);

    /** Laufweite (Tracking); 0 = aus. Sinnvoll ca. -0.02 .. 0.12. */
    private float tracking = 0f;

    // ---- Cache (nicht persistent) ----
    private transient String resolvedFamily;
    private final transient EnumMap<FFontRole, Font> cache =
            new EnumMap<FFontRole, Font>(FFontRole.class);

    public FTypography() {
        // Standard-Typoskala (Laboratory Dark)
        sizes.put(FFontRole.CAPTION, 11f);
        sizes.put(FFontRole.BODY,    13f);
        sizes.put(FFontRole.LABEL,   13f);
        sizes.put(FFontRole.BUTTON,  13f);
        sizes.put(FFontRole.TITLE,   16f);
        sizes.put(FFontRole.HEADING, 20f);
        sizes.put(FFontRole.DISPLAY, 28f);

        styles.put(FFontRole.CAPTION, Font.PLAIN);
        styles.put(FFontRole.BODY,    Font.PLAIN);
        styles.put(FFontRole.LABEL,   Font.PLAIN);
        styles.put(FFontRole.BUTTON,  Font.BOLD);
        styles.put(FFontRole.TITLE,   Font.BOLD);
        styles.put(FFontRole.HEADING, Font.BOLD);
        styles.put(FFontRole.DISPLAY, Font.BOLD);
    }

    // ============================================================== Font-Zugriff

    /** Liefert (gecacht) die Schrift für die gegebene Rolle. */
    public Font getFont(FFontRole role) {
        Font f = cache.get(role);
        if (f != null) return f;

        float size = sizeFor(role);
        int style  = styleFor(role);
        Font base  = new Font(resolveFamily(), style, Math.round(size));

        boolean fractional = Math.abs(size - Math.round(size)) > 0.001f;
        if (fractional || tracking != 0f) {
            Map<TextAttribute, Object> attr = new HashMap<TextAttribute, Object>();
            if (fractional) attr.put(TextAttribute.SIZE, size);
            if (tracking != 0f) attr.put(TextAttribute.TRACKING, tracking);
            base = base.deriveFont(attr);
        }
        cache.put(role, base);
        return base;
    }

    /** Wie {@link #getFont(FFontRole)}, aber mit zusätzlichem Stil-Flag (ODER-verknüpft). */
    public Font getFont(FFontRole role, int extraStyle) {
        Font f = getFont(role);
        return f.deriveFont(f.getStyle() | extraStyle);
    }

    // ============================================================== Familie

    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String family) { this.fontFamily = family; invalidate(); }

    public String[] getFallbackChain() { return fallbackChain.clone(); }
    public void setFallbackChain(String[] chain) {
        this.fallbackChain = (chain == null) ? new String[0] : chain.clone();
        invalidate();
    }

    /** Die tatsächlich aufgelöste, auf diesem System verfügbare Familie. */
    public String getResolvedFamily() { return resolveFamily(); }

    // ============================================================== Skala

    public float getSize(FFontRole role) { return sizeFor(role); }
    public void setSize(FFontRole role, float size) {
        sizes.put(role, Math.max(1f, size));
        cache.remove(role);
    }

    public int getStyle(FFontRole role) { return styleFor(role); }
    public void setStyle(FFontRole role, int style) {
        styles.put(role, style);
        cache.remove(role);
    }

    public float getTracking() { return tracking; }
    public void setTracking(float tracking) { this.tracking = tracking; invalidate(); }

    /** Skaliert alle Rollengrößen mit {@code factor} (z.&nbsp;B. für HiDPI/Accessibility). */
    public void scaleAll(float factor) {
        if (factor <= 0f) return;
        for (FFontRole r : FFontRole.values()) {
            sizes.put(r, Math.max(1f, sizeFor(r) * factor));
        }
        cache.clear();
    }

    // ============================================================== intern

    private float sizeFor(FFontRole role) {
        Float s = sizes.get(role);
        return s != null ? s.floatValue() : 13f;
    }

    private int styleFor(FFontRole role) {
        Integer s = styles.get(role);
        return s != null ? s.intValue() : Font.PLAIN;
    }

    private void invalidate() {
        resolvedFamily = null;
        cache.clear();
    }

    private String resolveFamily() {
        if (resolvedFamily != null) return resolvedFamily;
        Set<String> available = availableFamilies();
        List<String> chain = new ArrayList<String>();
        if (fontFamily != null) chain.add(fontFamily);
        if (fallbackChain != null) {
            for (String s : fallbackChain) chain.add(s);
        }
        for (String name : chain) {
            if (name == null) continue;
            if (isLogical(name) || available.contains(name)) {
                resolvedFamily = name;
                return resolvedFamily;
            }
        }
        resolvedFamily = Font.SANS_SERIF;
        return resolvedFamily;
    }

    private static boolean isLogical(String name) {
        return Font.SANS_SERIF.equals(name) || Font.SERIF.equals(name)
            || Font.MONOSPACED.equals(name) || Font.DIALOG.equals(name)
            || Font.DIALOG_INPUT.equals(name);
    }

    private static Set<String> availableFamilies() {
        Set<String> set = new HashSet<String>();
        try {
            String[] fams = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames();
            for (String f : fams) set.add(f);
        } catch (Throwable t) {
            // ungewöhnliche/headless Umgebung — logischer Fallback genügt
        }
        return set;
    }
}
