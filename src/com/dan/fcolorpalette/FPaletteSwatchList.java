package com.dan.fcolorpalette;

import com.dan.fdal.color.ColorDTO;
import com.dan.fstyle.FColors;
import com.dan.fstyle.FFontRole;
import com.dan.fstyle.FTheme;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.TransferHandler;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Zeigt die Farben der aktiven Palette in Reihenfolge; per Drag &amp; Drop
 * umsortierbar. Feuert {@link #setOnReorder(Consumer)} mit der neuen Reihenfolge
 * und {@link #setOnRemove(Consumer)} bei Klick auf das Entfernen-Kreuz.
 * <p>
 * Bewusst als schlanke {@link JList}-Auspraegung gebaut statt volles Custom-Paint
 * — DnD-Reorder-Semantik von {@code JList} ist ausgereift und robust.
 */
public class FPaletteSwatchList extends JList<ColorDTO> {

    private Consumer<List<ColorDTO>> onReorder;
    private Consumer<ColorDTO> onRemove;

    public FPaletteSwatchList() {
        setModel(new DefaultListModel<>());
        setOpaque(false);
        setCellRenderer(new SwatchCellRenderer());
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            // Drag & Drop erfordert eine echte Grafikumgebung — im Headless-Betrieb
            // (z. B. Bild-Export, Server-Rendering) bleibt die Liste sortierfest.
            setDragEnabled(true);
            setDropMode(DropMode.INSERT);
            setTransferHandler(new ReorderTransferHandler());
        }
        setFixedCellHeight(40);
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                if (index < 0) {
                    return;
                }
                java.awt.Rectangle cellBounds = getCellBounds(index, index);
                if (cellBounds == null) {
                    return;
                }
                int localX = e.getX() - cellBounds.x;
                if (localX > cellBounds.width - 28 && onRemove != null) {
                    onRemove.accept(getModel().getElementAt(index));
                }
            }
        });
    }

    public void setColors(List<ColorDTO> colors) {
        DefaultListModel<ColorDTO> m = new DefaultListModel<>();
        for (ColorDTO c : colors) {
            m.addElement(c);
        }
        setModel(m);
    }

    public List<ColorDTO> getColors() {
        List<ColorDTO> result = new ArrayList<>();
        for (int i = 0; i < getModel().getSize(); i++) {
            result.add(getModel().getElementAt(i));
        }
        return result;
    }

    public void setOnReorder(Consumer<List<ColorDTO>> onReorder) {
        this.onReorder = onReorder;
    }

    public void setOnRemove(Consumer<ColorDTO> onRemove) {
        this.onRemove = onRemove;
    }

    private void fireReorder() {
        if (onReorder != null) {
            onReorder.accept(getColors());
        }
    }

    // ------------------------------------------------------------------
    // Zellen-Rendering (Glas-Swatch + Name + Hex + Entfernen-Kreuz)
    // ------------------------------------------------------------------

    private static final class SwatchCellRenderer extends JComponent implements javax.swing.ListCellRenderer<ColorDTO> {
        private ColorDTO value;
        private boolean selected;

        SwatchCellRenderer() {
            setPreferredSize(new Dimension(220, 40));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ColorDTO> list, ColorDTO value,
                                                        int index, boolean isSelected, boolean cellHasFocus) {
            this.value = value;
            this.selected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (value == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FTheme theme = FTheme.getInstance();
            int w = getWidth(), h = getHeight();

            if (selected) {
                g2.setColor(FColors.withAlpha(theme.getPrimary(), 40));
                g2.fillRoundRect(0, 1, w, h - 2, 10, 10);
            }

            // Swatch
            RoundRectangle2D swatch = new RoundRectangle2D.Float(6, 6, 28, h - 12, 8, 8);
            g2.setColor(value.toColor());
            g2.fill(swatch);
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(theme.getGlassRim());
            g2.draw(swatch);

            // Name + Hex
            g2.setFont(theme.getFont(FFontRole.LABEL));
            g2.setColor(theme.getText());
            g2.drawString(value.getColorName(), 44, h / 2 - 2);
            g2.setFont(theme.getFont(FFontRole.CAPTION));
            g2.setColor(theme.getTextMuted());
            g2.drawString(value.getHexRgb(), 44, h / 2 + 13);

            // Entfernen-Kreuz rechts
            g2.setColor(theme.getTextMuted());
            g2.setStroke(new BasicStroke(1.6f));
            int cx = w - 16, cy = h / 2;
            g2.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
            g2.drawLine(cx - 5, cy + 5, cx + 5, cy - 5);

            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Drag & Drop Reorder
    // ------------------------------------------------------------------

    private final class ReorderTransferHandler extends TransferHandler {
        private final DataFlavor flavor = new DataFlavor(Integer.class, "index");

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            int index = getSelectedIndex();
            return new Transferable() {
                @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{flavor}; }
                @Override public boolean isDataFlavorSupported(DataFlavor f) { return f.equals(flavor); }
                @Override public Object getTransferData(DataFlavor f) { return index; }
            };
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(flavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                int from = (Integer) support.getTransferable().getTransferData(flavor);
                JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                int to = dl.getIndex();
                DefaultListModel<ColorDTO> m = (DefaultListModel<ColorDTO>) getModel();
                ColorDTO moved = m.remove(from);
                if (to > from) {
                    to--;
                }
                m.add(Math.max(0, Math.min(to, m.getSize())), moved);
                fireReorder();
                return true;
            } catch (UnsupportedFlavorException | IOException e) {
                return false;
            }
        }
    }
}
