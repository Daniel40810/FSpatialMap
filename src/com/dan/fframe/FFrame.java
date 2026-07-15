package com.dan.fframe;

import com.dan.ficons.FIconButtonStyler;
import com.dan.ficons.FIconComponent;
import com.dan.ficons.FIconType;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowEvent;

/**
 * FFrame — frameloser Fensterrahmen im ReagenzglasBar-Stil mit animierten
 * {@link FIconComponent}-Schaltflächen und gemaltem Katzen-Logo.
 *
 * <p>Nachfolger von {@code DFrame}. Statt statischer Icon-Buttons aus
 * {@code com.dan.icon} kommen vollständig gemalte F-Icons zum Einsatz, die bei
 * Hover sanft nach vorne treten (Skalierung) und leicht aufglühen. Das Logo ist
 * ein {@link FIconType#CAT}-Glyph — über {@link #setLogoType(FIconType)}
 * austauschbar.</p>
 *
 * <ul>
 *   <li>Minimieren / Maximieren / Schließen als animierte F-Icons.</li>
 *   <li>Maximieren-Button wechselt im maximierten Zustand auf
 *       {@link FIconType#RESTORE}.</li>
 *   <li>Schließen-Button glüht in Weinrot, die übrigen in Türkis.</li>
 *   <li>Verschieben über die Taskbar, Größenänderung an allen Kanten/Ecken.</li>
 * </ul>
 *
 * <p>Demos hängen Inhalt über {@link #getComponentPane()} ein.</p>
 *
 * @author com.dan
 */
public class FFrame extends JFrame {

    private Dimension preferredFrameSize = new Dimension(560, 360);
    private Color componentPaneColor = new Color(0x0C, 0x10, 0x15);
    private Color taskbarColor1 = new Color(0x01, 0x4B, 0x4E);
    private Color taskbarColor2 = new Color(0x5C, 0x00, 0x2A);
    private String frameTitle = "FFrame";

    // Icon-Styling (öffentlich für Palette/JAR)
    public Color iconGlowColor  = new Color(0x00, 0xB5, 0xAD);  // Türkis
    public Color closeGlowColor = new Color(0x8B, 0x00, 0x24);  // Weinrot
    public float iconHoverLift  = 0.16f;                        // wie weit Icons nach vorne treten
    public float iconGlowAlpha  = 90f;

    private FIconType logoType = FIconType.CAT;

    private static final int ARC = 20;
    private static final int TASKBAR_HEIGHT = 38;
    private static final int RESIZE_MARGIN = 6;

    private JPanel componentPane;
    private FTaskbar taskbar;
    private JPanel outerPanel;

    // ============================================================ Konstruktoren

    public FFrame() {
        this("FFrame");
    }

    public FFrame(String title) {
        this.frameTitle = title;
        initUI();
    }

    // ============================================================ UI-Aufbau

    private void initUI() {
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(preferredFrameSize);
        setMinimumSize(new Dimension(260, 150));

        outerPanel = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) { /* transparent */ }
        };
        outerPanel.setOpaque(false);

        taskbar = new FTaskbar();
        outerPanel.add(taskbar, BorderLayout.NORTH);

        componentPane = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(componentPaneColor);
                int w = getWidth(), h = getHeight();
                g2.fillRoundRect(0, -ARC, w, h + ARC, ARC * 2, ARC * 2);
                g2.dispose();
            }

            @Override protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new RoundRectangle2D.Float(
                        0, -ARC, getWidth(), getHeight() + ARC,
                        ARC * 2, ARC * 2));
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        componentPane.setOpaque(false);
        componentPane.setLayout(new BorderLayout());
        outerPanel.add(componentPane, BorderLayout.CENTER);

        DResizer resizer = new DResizer(this);
        outerPanel.addMouseListener(resizer);
        outerPanel.addMouseMotionListener(resizer);

        setContentPane(outerPanel);
        pack();
        setLocationRelativeTo(null);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            pack();
            setLocationRelativeTo(null);
        }
        super.setVisible(visible);
    }

    // ============================================================ Public API

    public JPanel getComponentPane() {
        return componentPane;
    }

    public void setPreferredFrameSize(Dimension d) {
        this.preferredFrameSize = d;
        setPreferredSize(d);
        pack();
    }

    public void setComponentPaneColor(Color c) {
        this.componentPaneColor = c;
        componentPane.repaint();
    }

    public Color getComponentPaneColor() { return componentPaneColor; }

    public void setTaskbarColors(Color c1, Color c2) {
        this.taskbarColor1 = c1;
        this.taskbarColor2 = c2;
        taskbar.repaint();
    }

    public Color getTaskbarColor1() { return taskbarColor1; }
    public void setTaskbarColor1(Color c) { this.taskbarColor1 = c; taskbar.repaint(); }

    public Color getTaskbarColor2() { return taskbarColor2; }
    public void setTaskbarColor2(Color c) { this.taskbarColor2 = c; taskbar.repaint(); }

    @Override
    public void setTitle(String title) {
        this.frameTitle = title;
        if (taskbar != null) taskbar.repaint();
    }

    @Override
    public String getTitle() { return frameTitle; }

    /** Logo-Symbol austauschen (Standard: Katze). */
    public FIconType getLogoType() { return logoType; }
    public void setLogoType(FIconType type) {
        this.logoType = (type != null) ? type : FIconType.CAT;
        if (taskbar != null) taskbar.logo.setType(this.logoType);
    }

    public Color getIconGlowColor() { return iconGlowColor; }
    public void setIconGlowColor(Color c) {
        this.iconGlowColor = c;
        if (taskbar != null) {
            taskbar.btnMinimize.setGlowColor(c);
            taskbar.btnMaximize.setGlowColor(c);
        }
    }

    public Color getCloseGlowColor() { return closeGlowColor; }
    public void setCloseGlowColor(Color c) {
        this.closeGlowColor = c;
        if (taskbar != null) taskbar.btnClose.setGlowColor(c);
    }

    public float getIconHoverLift() { return iconHoverLift; }
    public void setIconHoverLift(float v) {
        this.iconHoverLift = v;
        if (taskbar != null) {
            taskbar.btnMinimize.setHoverLift(v);
            taskbar.btnMaximize.setHoverLift(v);
            taskbar.btnClose.setHoverLift(v);
            taskbar.logo.setHoverLift(v);
        }
    }

    // ============================================================ Taskbar

    private class FTaskbar extends JPanel {

        private final FIconComponent logo;
        private final FIconComponent btnMinimize;
        private final FIconComponent btnMaximize;
        private final FIconComponent btnClose;

        private Point dragStart;
        private boolean maximized = false;
        private Rectangle restoreBounds;

        FTaskbar() {
            setPreferredSize(new Dimension(0, TASKBAR_HEIGHT));
            setOpaque(false);
            setLayout(null);

            // Logo (Katze) — ruhig, kein Press, dezenter Hover-Lift
            logo = new FIconComponent(logoType);
            logo.setHoverGlowEnabled(false);
            logo.setPressEnabled(false);
            logo.setHoverLift(0.10f);
            logo.setIconGap(3);

            // Fenster-Buttons — treten bei Hover hervor, dezenter Glow
            FIconButtonStyler style = new FIconButtonStyler();
            style.iconSize = 18;
            style.iconGap = 6;
            style.maxGlowAlpha = iconGlowAlpha;

            btnMinimize = style.build(FIconType.MINIMIZE);
            btnMaximize = style.build(FIconType.MAXIMIZE);
            btnClose    = style.build(FIconType.CLOSE);

            for (FIconComponent b : new FIconComponent[]{btnMinimize, btnMaximize, btnClose}) {
                b.setGlowColor(iconGlowColor);
                b.setHoverLift(iconHoverLift);
            }
            btnClose.setGlowColor(closeGlowColor);

            btnMinimize.setToolTipText("Minimieren");
            btnMaximize.setToolTipText("Maximieren");
            btnClose.setToolTipText("Schließen");

            btnMinimize.addActionListener(e -> FFrame.this.setState(JFrame.ICONIFIED));
            btnMaximize.addActionListener(e -> toggleMaximize());
            btnClose.addActionListener(e -> {
                java.awt.Window w = SwingUtilities.getWindowAncestor(btnClose);
                if (w != null) w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
            });

            add(logo);
            add(btnMinimize);
            add(btnMaximize);
            add(btnClose);

            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { dragStart = e.getPoint(); }
                @Override public void mouseReleased(MouseEvent e) { dragStart = null; }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) {
                    if (dragStart != null) {
                        Point loc = FFrame.this.getLocation();
                        FFrame.this.setLocation(
                                loc.x + e.getX() - dragStart.x,
                                loc.y + e.getY() - dragStart.y);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            if (w <= 1) w = FFrame.this.getWidth();
            int h = getHeight();

            g2.setPaint(new GradientPaint(0, 0, taskbarColor1, w, 0, taskbarColor2));
            g2.fillRoundRect(0, 0, w, h + ARC, ARC * 2, ARC * 2);

            g2.setColor(new Color(255, 255, 255, 55));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, w - 2, h + ARC, ARC * 2, ARC * 2);

            // Titel rechts neben dem Logo
            int textX = logo.getX() + logo.getWidth() + 4;
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(frameTitle, textX, textY);

            g2.dispose();
        }

        @Override
        public void doLayout() {
            super.doLayout();
            int h = getHeight();
            int logoSz = h - 8;
            logo.setBounds(8, (h - logoSz) / 2, logoSz, logoSz);

            int btnW = 30, btnH = 26, gap = 2;
            int y = (h - btnH) / 2;
            int x = getWidth() - (btnW + gap) * 3 - 6;
            btnMinimize.setBounds(x, y, btnW, btnH); x += btnW + gap;
            btnMaximize.setBounds(x, y, btnW, btnH); x += btnW + gap;
            btnClose.setBounds(x, y, btnW, btnH);
        }

        private void toggleMaximize() {
            if (!maximized) {
                restoreBounds = FFrame.this.getBounds();
                GraphicsConfiguration gc = FFrame.this.getGraphicsConfiguration();
                Rectangle screen = gc.getBounds();
                Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
                FFrame.this.setBounds(
                        screen.x + insets.left, screen.y + insets.top,
                        screen.width - insets.left - insets.right,
                        screen.height - insets.top - insets.bottom);
                maximized = true;
                btnMaximize.setType(FIconType.RESTORE);
                btnMaximize.setToolTipText("Wiederherstellen");
            } else {
                FFrame.this.setBounds(restoreBounds);
                maximized = false;
                btnMaximize.setType(FIconType.MAXIMIZE);
                btnMaximize.setToolTipText("Maximieren");
            }
        }
    }

    // ============================================================ DResizer

    private static class DResizer extends MouseAdapter {

        private final JFrame frame;
        private int direction = 0;
        private Point dragStart = null;
        private Rectangle startBounds = null;

        private static final int N = 1, S = 2, W = 4, E = 8;

        DResizer(JFrame frame) { this.frame = frame; }

        @Override public void mouseMoved(MouseEvent e) {
            direction = getDirection(e.getPoint(), frame);
            frame.setCursor(getCursor(direction));
        }

        @Override public void mousePressed(MouseEvent e) {
            direction = getDirection(e.getPoint(), frame);
            if (direction != 0) {
                dragStart = e.getLocationOnScreen();
                startBounds = frame.getBounds();
            }
        }

        @Override public void mouseDragged(MouseEvent e) {
            if (direction == 0 || dragStart == null) return;
            Point cur = e.getLocationOnScreen();
            int dx = cur.x - dragStart.x, dy = cur.y - dragStart.y;
            int x = startBounds.x, y = startBounds.y, w = startBounds.width, h = startBounds.height;
            int minW = frame.getMinimumSize().width, minH = frame.getMinimumSize().height;
            if ((direction & E) != 0) w = Math.max(minW, w + dx);
            if ((direction & S) != 0) h = Math.max(minH, h + dy);
            if ((direction & W) != 0) { int nw = Math.max(minW, w - dx); x = x + w - nw; w = nw; }
            if ((direction & N) != 0) { int nh = Math.max(minH, h - dy); y = y + h - nh; h = nh; }
            frame.setBounds(x, y, w, h);
        }

        @Override public void mouseReleased(MouseEvent e) {
            dragStart = null; startBounds = null; direction = 0;
            frame.setCursor(java.awt.Cursor.getDefaultCursor());
        }

        private static int getDirection(Point p, JFrame f) {
            int m = RESIZE_MARGIN, w = f.getWidth(), h = f.getHeight(), dir = 0;
            if (p.y < m) dir |= N;
            if (p.y > h - m) dir |= S;
            if (p.x < m) dir |= W;
            if (p.x > w - m) dir |= E;
            return dir;
        }

        private static java.awt.Cursor getCursor(int dir) {
            switch (dir) {
                case N: return java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.N_RESIZE_CURSOR);
                case S: return java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.S_RESIZE_CURSOR);
                case W: return java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.W_RESIZE_CURSOR);
                case E: return java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR);
                case N | W: return java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.NW_RESIZE_CURSOR);
                case N | E: return java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.NE_RESIZE_CURSOR);
                case S | W: return java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.SW_RESIZE_CURSOR);
                case S | E: return java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.SE_RESIZE_CURSOR);
                default: return java.awt.Cursor.getDefaultCursor();
            }
        }
    }
}
