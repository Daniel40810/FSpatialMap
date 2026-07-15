package com.dan.ftooltip;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Globaler Koordinator fuer {@link FTooltip}: verwaltet Show-/Hide-Delays,
 * Positionierung relativ zum Anker (mit automatischer Kanten-Auswahl) und
 * haengt eine einzige, wiederverwendete {@link FTooltip}-Instanz pro Root-Fenster
 * in dessen {@code JLayeredPane} (Popup-Layer) ein — kein JWindow, kein
 * Fokus-/Z-Order-Flackern.
 *
 * Nutzung:
 * <pre>
 * FTooltipManager.getInstance().install(button,
 *     FTooltipContent.of("Speichern").addLine("\u2318S"));
 * </pre>
 */
public final class FTooltipManager {

    private static final FTooltipManager INSTANCE = new FTooltipManager();

    public static FTooltipManager getInstance() {
        return INSTANCE;
    }

    private int showDelayMs = 450;
    private int hideDelayMs = 150;

    private final Map<JComponent, Installation> installations = new HashMap<JComponent, Installation>();
    private final Map<JRootPane, FTooltip> tooltipsByRoot = new HashMap<JRootPane, FTooltip>();

    private Timer showTimer;
    private Timer hideTimer;
    private JComponent pendingTarget;
    private Point pendingScreenPoint;
    private JComponent activeTarget;

    private FTooltipManager() {
    }

    // ------------------------------------------------------------------
    // Globale Einstellungen
    // ------------------------------------------------------------------
    public int getShowDelayMs()           { return showDelayMs; }
    public void setShowDelayMs(int ms)    { showDelayMs = Math.max(0, ms); }

    public int getHideDelayMs()           { return hideDelayMs; }
    public void setHideDelayMs(int ms)    { hideDelayMs = Math.max(0, ms); }

    // ------------------------------------------------------------------
    // Registrierung
    // ------------------------------------------------------------------
    /** Registriert eine Komponente mit statischem Tooltip-Inhalt. */
    public void install(JComponent target, FTooltipContent content) {
        install(target, new StaticSupplier(content));
    }

    /** Registriert eine Komponente mit dynamisch berechnetem Inhalt (z. B. je nach Hover-Feature). */
    public void install(JComponent target, ContentSupplier supplier) {
        if (target == null || supplier == null) {
            return;
        }
        uninstall(target);
        Installation inst = new Installation(supplier);
        installations.put(target, inst);
        target.addMouseListener(inst.mouseListener);
        target.addMouseMotionListener(inst.mouseListener);
    }

    public void uninstall(JComponent target) {
        Installation inst = installations.remove(target);
        if (inst != null) {
            target.removeMouseListener(inst.mouseListener);
            target.removeMouseMotionListener(inst.mouseListener);
            if (target == activeTarget) {
                hideImmediately();
            }
        }
    }

    /** Liefert den Tooltip-Inhalt fuer eine Komponente. */
    public interface ContentSupplier {
        FTooltipContent getContent(MouseEvent e);
    }

    private static final class StaticSupplier implements ContentSupplier {
        private final FTooltipContent content;
        StaticSupplier(FTooltipContent c) { this.content = c; }
        @Override
        public FTooltipContent getContent(MouseEvent e) { return content; }
    }

    private final class Installation {
        final ContentSupplier supplier;
        final MouseAdapter mouseListener;

        Installation(ContentSupplier supplier) {
            this.supplier = supplier;
            this.mouseListener = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    scheduleShow((JComponent) e.getSource(), e);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    scheduleHide();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    hideImmediately();
                }
            };
        }
    }

    // ------------------------------------------------------------------
    // Show / Hide Scheduling
    // ------------------------------------------------------------------
    private void scheduleShow(final JComponent target, MouseEvent e) {
        cancelHide();
        if (target == activeTarget) {
            return;
        }
        pendingTarget = target;
        pendingScreenPoint = e.getLocationOnScreen();
        if (showTimer != null) {
            showTimer.stop();
        }
        showTimer = new Timer(showDelayMs, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                if (target == pendingTarget) {
                    showFor(target, pendingScreenPoint);
                }
            }
        });
        showTimer.setRepeats(false);
        showTimer.start();
    }

    private void scheduleHide() {
        cancelShow();
        if (hideTimer != null) {
            hideTimer.stop();
        }
        hideTimer = new Timer(hideDelayMs, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                hideActive();
            }
        });
        hideTimer.setRepeats(false);
        hideTimer.start();
    }

    private void cancelShow() {
        pendingTarget = null;
        if (showTimer != null) {
            showTimer.stop();
        }
    }

    private void cancelHide() {
        if (hideTimer != null) {
            hideTimer.stop();
        }
    }

    private void hideActive() {
        if (activeTarget == null) {
            return;
        }
        JRootPane root = SwingUtilities.getRootPane(activeTarget);
        activeTarget = null;
        if (root == null) {
            return;
        }
        final FTooltip tip = tooltipsByRoot.get(root);
        if (tip != null) {
            tip.fadeOut(new Runnable() {
                @Override
                public void run() {
                    java.awt.Container parent = tip.getParent();
                    if (parent != null) {
                        parent.remove(tip);
                        parent.repaint();
                    }
                }
            });
        }
    }

    private void hideImmediately() {
        activeTarget = null;
        cancelShow();
        cancelHide();
        for (Map.Entry<JRootPane, FTooltip> entry : tooltipsByRoot.entrySet()) {
            FTooltip tip = entry.getValue();
            tip.resetHidden();
            java.awt.Container parent = tip.getParent();
            if (parent != null) {
                parent.remove(tip);
                parent.repaint();
            }
        }
    }

    // ------------------------------------------------------------------
    // Anzeige & Positionierung
    // ------------------------------------------------------------------
    private void showFor(JComponent target, Point screenPoint) {
        Installation inst = installations.get(target);
        if (inst == null || !target.isShowing()) {
            return;
        }
        JRootPane rootPane = SwingUtilities.getRootPane(target);
        if (rootPane == null) {
            return;
        }
        FTooltipContent content = inst.supplier.getContent(null);
        if (content == null || content.isEmpty()) {
            return;
        }

        FTooltip tip = tooltipsByRoot.get(rootPane);
        if (tip == null) {
            tip = new FTooltip();
            tooltipsByRoot.put(rootPane, tip);
        }
        tip.resetHidden();
        tip.setContent(content);

        JLayeredPane layered = rootPane.getLayeredPane();
        java.awt.Point anchorInLayer = SwingUtilities.convertPoint(target,
                new Point(target.getWidth() / 2, 0), layered);
        Rectangle targetInLayer = SwingUtilities.convertRectangle(
                target, new Rectangle(0, 0, target.getWidth(), target.getHeight()), layered);

        placeAndShow(tip, layered, targetInLayer, anchorInLayer);
        activeTarget = target;
    }

    private void placeAndShow(FTooltip tip, JLayeredPane layered, Rectangle targetRect, Point anchor) {
        java.awt.Dimension size = tip.computeBubbleSize();
        Placement p = computePlacement(targetRect, anchor, size, layered.getWidth(), layered.getHeight());

        tip.setArrow(p.edge, p.arrowOffset);
        tip.setBounds(p.x, p.y, size.width, size.height);

        layered.add(tip, JLayeredPane.POPUP_LAYER);
        layered.moveToFront(tip);
        tip.fadeIn();
    }

    /**
     * Reine Platzierungslogik (kein Swing-Peer noetig): waehlt die Kante mit
     * mehr verfuegbarem Platz und klemmt die X-Position an die Layer-Grenzen.
     * Als eigene Methode ausgelagert, damit sie ohne echtes Fenster testbar ist.
     */
    public static Placement computePlacement(Rectangle targetRect, Point anchor, java.awt.Dimension size,
                                       int layerWidth, int layerHeight) {
        int gap = 6;
        int spaceAbove = targetRect.y;
        int spaceBelow = layerHeight - (targetRect.y + targetRect.height);

        FTooltip.Edge edge;
        int x, y;
        if (spaceAbove >= size.height + gap || spaceAbove >= spaceBelow) {
            edge = FTooltip.Edge.BOTTOM;   // Bubble oberhalb, Pfeil zeigt nach unten
            x = anchor.x - size.width / 2;
            y = targetRect.y - size.height - gap;
        } else {
            edge = FTooltip.Edge.TOP;      // Bubble unterhalb, Pfeil zeigt nach oben
            x = anchor.x - size.width / 2;
            y = targetRect.y + targetRect.height + gap;
        }

        int clampedX = Math.max(4, Math.min(layerWidth - size.width - 4, x));
        int arrowOffsetX = anchor.x - clampedX;
        return new Placement(edge, clampedX, Math.max(0, y), arrowOffsetX);
    }

    /** Ergebnis der Platzierungsberechnung. */
    public static final class Placement {
        public final FTooltip.Edge edge;
        public final int x, y, arrowOffset;
        Placement(FTooltip.Edge edge, int x, int y, int arrowOffset) {
            this.edge = edge; this.x = x; this.y = y; this.arrowOffset = arrowOffset;
        }
    }

    /** Testbare, synchronisierte Variante ohne Timer-Delays (fuer Smoke-Tests). */
    public void showImmediatelyForTest(JComponent target) {
        Installation inst = installations.get(target);
        if (inst == null) {
            return;
        }
        showFor(target, new Point(0, 0));
    }
}
