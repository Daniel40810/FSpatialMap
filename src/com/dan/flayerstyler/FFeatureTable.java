package com.dan.flayerstyler;

import com.dan.fstyle.FColors;
import com.dan.fstyle.FFontRole;
import com.dan.fstyle.FTheme;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabelle der Features eines Layers: Swatch | Feature-ID | Farbname | gueltig von..bis.
 * Mehrfachauswahl fuer Batch-Operationen (siehe {@link FLayerStyler}).
 */
public class FFeatureTable extends JTable {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final RowModel tableModel = new RowModel();

    public FFeatureTable() {
        setModel(tableModel);
        setRowHeight(28);
        setOpaque(false);
        setShowGrid(false);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        getColumnModel().getColumn(0).setMaxWidth(36);
        getColumnModel().getColumn(0).setCellRenderer(new SwatchRenderer());
        setDefaultRenderer(Object.class, new TextRenderer());
    }

    public void setRows(List<FFeatureRow> rows) {
        tableModel.setRows(rows);
    }

    public List<FFeatureRow> getSelectedRows2() {
        List<FFeatureRow> result = new ArrayList<>();
        for (int viewRow : getSelectedRows()) {
            int modelRow = convertRowIndexToModel(viewRow);
            result.add(tableModel.rowAt(modelRow));
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Table-Model
    // ------------------------------------------------------------------

    private static final class RowModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"", "Feature-ID", "Farbe", "Gueltig von", "Gueltig bis"};
        private List<FFeatureRow> rows = new ArrayList<>();

        void setRows(List<FFeatureRow> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        FFeatureRow rowAt(int i) {
            return rows.get(i);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            FFeatureRow row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return row;
                case 1: return row.getFeatureId();
                case 2: return row.isColored() ? row.getColor().getColorName() : "—";
                case 3: return row.getValidFrom() == null ? "unbegrenzt" : row.getValidFrom().format(DATE_FMT);
                case 4: return row.getValidTo() == null ? "unbegrenzt" : row.getValidTo().format(DATE_FMT);
                default: return null;
            }
        }
    }

    // ------------------------------------------------------------------
    // Renderer
    // ------------------------------------------------------------------

    private static final class SwatchRenderer extends javax.swing.JComponent
            implements javax.swing.table.TableCellRenderer {
        private Color color;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            FFeatureRow r = (FFeatureRow) value;
            this.color = r != null && r.isColored() ? r.getColor().toColor() : null;
            setOpaque(false);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 10;
            int x = (getWidth() - size) / 2, y = (getHeight() - size) / 2;
            if (color != null) {
                g2.setColor(color);
                g2.fillRoundRect(x, y, size, size, 6, 6);
                g2.setColor(FTheme.getInstance().getGlassRim());
                g2.drawRoundRect(x, y, size, size, 6, 6);
            } else {
                g2.setColor(FColors.withAlpha(FTheme.getInstance().getTextMuted(), 90));
                g2.setStroke(new java.awt.BasicStroke(1.2f,
                        java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 1f,
                        new float[]{3f, 2f}, 0f));
                g2.drawRoundRect(x, y, size, size, 6, 6);
            }
            g2.dispose();
        }
    }

    private static final class TextRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            FTheme theme = FTheme.getInstance();
            c.setForeground(theme.getText());
            setBackground(isSelected ? FColors.withAlpha(theme.getPrimary(), 45) : new Color(0, 0, 0, 0));
            setFont(theme.getFont(FFontRole.BODY));
            setOpaque(isSelected);
            return c;
        }
    }
}
