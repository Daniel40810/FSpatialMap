package com.dan.fcolorpalette;

import com.dan.fbutton.FButton;
import com.dan.fbutton.FButtonVariant;
import com.dan.fdal.color.ColorDTO;
import com.dan.fdal.color.PaletteDTO;
import com.dan.ficons.FIconButtonStyler;
import com.dan.ficons.FIconComponent;
import com.dan.ficons.FIconType;
import com.dan.flabel.FLabel;
import com.dan.fstyle.FColors;
import com.dan.fstyle.FFontRole;
import com.dan.fstyle.FTheme;
import com.dan.ftextfield.FTextfield;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Vollstaendiger Editor fuer COLOR_ADMIN-Paletten: Paletten anlegen/loeschen,
 * Farben aus dem Katalog zuordnen oder neu anlegen, per Drag &amp; Drop
 * umsortieren, live als Verlauf/Heatmap/Balken pruefen.
 * <p>
 * Rendert ausschliesslich aus {@link FPaletteEditorModel}; alle DB-Operationen
 * laufen durchs Model. Kompakt einbettbar (z. B. spaeter im FLayerStyler) via
 * {@link FColorPaletteEditorStyler#compact()}.
 */
public class FColorPaletteEditor extends JPanel {

    private FPaletteEditorModel model;

    private final JList<PaletteDTO> paletteList = new JList<>(new DefaultListModel<>());
    private final JList<ColorDTO> catalogList = new JList<>(new DefaultListModel<>());
    private final FTextfield catalogSearch = new FTextfield();
    private final FPaletteSwatchList swatchList = new FPaletteSwatchList();
    private final FHSVColorPicker picker = new FHSVColorPicker();
    private final FTextfield newColorName = new FTextfield();
    private final FPalettePreviewPanel preview = new FPalettePreviewPanel();

    private final JSplitPane westSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    public FColorPaletteEditor() {
        this(new DefaultFPaletteEditorModel(null, null));
    }

    public FColorPaletteEditor(FPaletteEditorModel model) {
        setOpaque(false);
        setLayout(new BorderLayout(10, 10));
        setModel(model);
        buildUi();
    }

    private void buildUi() {
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildWest(), BorderLayout.WEST);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildEast(), BorderLayout.EAST);
    }

    // ------------------------------------------------------------------
    // Toolbar
    // ------------------------------------------------------------------

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);

        FIconComponent addPalette = FIconButtonStyler.toolbar().build(FIconType.ADD);
        addPalette.addActionListener(e -> onCreatePalette());

        FIconComponent deletePalette = FIconButtonStyler.toolbar().build(FIconType.DELETE);
        deletePalette.addActionListener(e -> onDeletePalette());

        FIconComponent refresh = FIconButtonStyler.toolbar().build(FIconType.REFRESH);
        refresh.addActionListener(e -> model.reload());

        bar.add(title("Paletten-Editor", FFontRole.TITLE));
        bar.add(addPalette);
        bar.add(deletePalette);
        bar.add(refresh);
        return bar;
    }

    // ------------------------------------------------------------------
    // West: Paletten + Katalog
    // ------------------------------------------------------------------

    private JPanel buildWest() {
        JPanel west = new JPanel(new BorderLayout());
        west.setOpaque(false);
        west.setPreferredSize(new Dimension(190, 10));

        JPanel palettePanel = new JPanel(new BorderLayout(0, 4));
        palettePanel.setOpaque(false);
        palettePanel.add(title("Paletten", FFontRole.LABEL), BorderLayout.NORTH);
        paletteList.setOpaque(false);
        paletteList.setCellRenderer(new PaletteCellRenderer());
        paletteList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                model.setSelectedPalette(paletteList.getSelectedValue());
            }
        });
        palettePanel.add(new JScrollPane(paletteList), BorderLayout.CENTER);

        JPanel catalogPanel = new JPanel(new BorderLayout(0, 4));
        catalogPanel.setOpaque(false);
        catalogPanel.add(title("Katalog (Doppelklick fuegt hinzu)", FFontRole.CAPTION), BorderLayout.NORTH);
        catalogSearch.setLabelText("Suche");
        catalogSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshCatalog(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshCatalog(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshCatalog(); }
        });
        catalogList.setOpaque(false);
        catalogList.setCellRenderer(new CatalogCellRenderer());
        catalogList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ColorDTO c = catalogList.getSelectedValue();
                    if (c != null) {
                        model.addColorToPalette(c);
                    }
                }
            }
        });
        JPanel catalogTop = new JPanel(new BorderLayout());
        catalogTop.setOpaque(false);
        catalogTop.add(title("Katalog (Doppelklick fuegt hinzu)", FFontRole.CAPTION), BorderLayout.NORTH);
        catalogTop.add(catalogSearch, BorderLayout.SOUTH);
        catalogPanel.removeAll();
        catalogPanel.add(catalogTop, BorderLayout.NORTH);
        catalogPanel.add(new JScrollPane(catalogList), BorderLayout.CENTER);

        westSplit.setTopComponent(palettePanel);
        westSplit.setBottomComponent(catalogPanel);
        westSplit.setResizeWeight(0.45);
        westSplit.setOpaque(false);
        westSplit.setBorder(null);
        west.add(westSplit, BorderLayout.CENTER);
        return west;
    }

    // ------------------------------------------------------------------
    // Center: Farben der aktiven Palette
    // ------------------------------------------------------------------

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.setOpaque(false);
        center.add(title("Farben in Palette (Drag = umsortieren, Klick × = entfernen)", FFontRole.LABEL),
                BorderLayout.NORTH);
        swatchList.setOnReorder(model::reorderPaletteColors);
        swatchList.setOnRemove(model::removeColorFromPalette);
        center.add(new JScrollPane(swatchList), BorderLayout.CENTER);
        return center;
    }

    // ------------------------------------------------------------------
    // East: Picker + Vorschau
    // ------------------------------------------------------------------

    private JPanel buildEast() {
        JPanel east = new JPanel(new BorderLayout(0, 10));
        east.setOpaque(false);
        east.setPreferredSize(new Dimension(240, 10));

        JPanel pickerPanel = new JPanel(new BorderLayout(0, 6));
        pickerPanel.setOpaque(false);
        pickerPanel.add(title("Neue Farbe", FFontRole.LABEL), BorderLayout.NORTH);
        pickerPanel.add(picker, BorderLayout.CENTER);

        newColorName.setLabelText("Name");
        FButton createBtn = new FButton("Anlegen + hinzufuegen");
        createBtn.addActionListener(e -> onCreateAndAddColor());

        JPanel createRow = new JPanel(new BorderLayout(0, 4));
        createRow.setOpaque(false);
        createRow.add(newColorName, BorderLayout.NORTH);
        createRow.add(createBtn, BorderLayout.SOUTH);
        pickerPanel.add(createRow, BorderLayout.SOUTH);

        JPanel previewPanel = new JPanel(new BorderLayout(0, 6));
        previewPanel.setOpaque(false);
        previewPanel.add(title("Vorschau", FFontRole.LABEL), BorderLayout.NORTH);
        previewPanel.add(preview, BorderLayout.CENTER);
        previewPanel.add(buildModeToggle(), BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pickerPanel, previewPanel);
        split.setResizeWeight(0.55);
        split.setOpaque(false);
        split.setBorder(null);
        east.add(split, BorderLayout.CENTER);
        return east;
    }

    private JPanel buildModeToggle() {
        JPanel row = new JPanel(new GridLayout(1, 3, 4, 0));
        row.setOpaque(false);
        for (FPreviewMode m : FPreviewMode.values()) {
            FButton b = new FButton(m.getLabel());
            b.setVariant(m == preview.getMode() ? FButtonVariant.PRIMARY : FButtonVariant.GHOST);
            b.addActionListener(e -> preview.setMode(m));
            row.add(b);
        }
        return row;
    }

    private FLabel title(String text, FFontRole role) {
        FLabel label = new FLabel(text);
        label.setFont(FTheme.getInstance().getFont(role));
        return label;
    }

    // ------------------------------------------------------------------
    // Aktionen
    // ------------------------------------------------------------------

    private void onCreatePalette() {
        String name = JOptionPane.showInputDialog(this, "Name der neuen Palette:");
        if (name != null && !name.trim().isEmpty()) {
            model.createPalette(name.trim(), null);
        }
    }

    private void onDeletePalette() {
        PaletteDTO selected = model.getSelectedPalette();
        if (selected == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Palette \"" + selected.getPaletteName() + "\" wirklich loeschen?",
                "Palette loeschen", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            model.deletePalette(selected);
        }
    }

    private void onCreateAndAddColor() {
        String name = newColorName.getText();
        if (name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte einen Namen fuer die Farbe eingeben.");
            return;
        }
        ColorDTO created = model.createColor(name.trim(), picker.getColor());
        model.addColorToPalette(created);
        newColorName.setText("");
    }

    private void refreshCatalog() {
        String query = catalogSearch.getText();
        String lower = query == null ? "" : query.trim().toLowerCase();
        DefaultListModel<ColorDTO> m = new DefaultListModel<>();
        for (ColorDTO c : model.getAllColors()) {
            if (lower.isEmpty() || c.getColorName().toLowerCase().contains(lower)) {
                m.addElement(c);
            }
        }
        catalogList.setModel(m);
    }

    // ------------------------------------------------------------------
    // Model-Bindung
    // ------------------------------------------------------------------

    public FPaletteEditorModel getModel() {
        return model;
    }

    public void setModel(FPaletteEditorModel model) {
        if (this.model != null) {
            this.model.removePropertyChangeListener(this::onModelChanged);
        }
        this.model = model;
        model.addPropertyChangeListener(this::onModelChanged);
        refreshPaletteList();
        refreshCatalog();
        swatchList.setColors(model.getPaletteColors());
        preview.setColors(model.getPaletteColors());
    }

    private void onModelChanged(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "palettes":
                refreshPaletteList();
                break;
            case "allColors":
                refreshCatalog();
                break;
            case "selectedPalette":
                paletteList.setSelectedValue(model.getSelectedPalette(), true);
                break;
            case "paletteColors":
                swatchList.setColors(model.getPaletteColors());
                preview.setColors(model.getPaletteColors());
                break;
            default:
                break;
        }
    }

    private void refreshPaletteList() {
        DefaultListModel<PaletteDTO> m = new DefaultListModel<>();
        for (PaletteDTO p : model.getPalettes()) {
            m.addElement(p);
        }
        paletteList.setModel(m);
    }

    // ------------------------------------------------------------------
    // Bean-Properties (fuer Styler)
    // ------------------------------------------------------------------

    public void setPaletteListWidth(int w) {
        westSplit.getTopComponent().setPreferredSize(new Dimension(w, 10));
    }

    public void setSwatchListWidth(int w) {
        swatchList.setPreferredSize(new Dimension(w, swatchList.getPreferredSize().height));
    }

    public void setPickerWidth(int w) {
        picker.setPreferredSize(new Dimension(w, picker.getPreferredSize().height));
    }

    public void setPreviewMode(FPreviewMode mode) {
        preview.setMode(mode);
    }

    // ------------------------------------------------------------------
    // Renderer
    // ------------------------------------------------------------------

    private static final class PaletteCellRenderer extends javax.swing.JComponent
            implements javax.swing.ListCellRenderer<PaletteDTO> {
        private PaletteDTO value;
        private boolean selected;

        PaletteCellRenderer() {
            setPreferredSize(new Dimension(150, 26));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends PaletteDTO> list, PaletteDTO value,
                                                        int index, boolean isSelected, boolean cellHasFocus) {
            this.value = value;
            this.selected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            FTheme theme = FTheme.getInstance();
            if (selected) {
                g2.setColor(FColors.withAlpha(theme.getPrimary(), 45));
                g2.fillRoundRect(0, 1, getWidth(), getHeight() - 2, 8, 8);
            }
            g2.setFont(theme.getFont(FFontRole.LABEL));
            g2.setColor(theme.getText());
            g2.drawString(value == null ? "" : value.getPaletteName(), 8, getHeight() / 2 + 5);
            g2.dispose();
        }
    }

    private static final class CatalogCellRenderer extends javax.swing.JComponent
            implements javax.swing.ListCellRenderer<ColorDTO> {
        private ColorDTO value;
        private boolean selected;

        CatalogCellRenderer() {
            setPreferredSize(new Dimension(150, 24));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ColorDTO> list, ColorDTO value,
                                                        int index, boolean isSelected, boolean cellHasFocus) {
            this.value = value;
            this.selected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            FTheme theme = FTheme.getInstance();
            if (selected) {
                g2.setColor(FColors.withAlpha(theme.getPrimary(), 45));
                g2.fillRoundRect(0, 1, getWidth(), getHeight() - 2, 6, 6);
            }
            if (value != null) {
                g2.setColor(value.toColor());
                g2.fillRoundRect(4, 4, 16, getHeight() - 8, 5, 5);
                g2.setColor(theme.getGlassRim());
                g2.drawRoundRect(4, 4, 16, getHeight() - 8, 5, 5);
                g2.setFont(theme.getFont(FFontRole.CAPTION));
                g2.setColor(theme.getText());
                g2.drawString(value.getColorName(), 26, getHeight() / 2 + 4);
            }
            g2.dispose();
        }
    }
}
