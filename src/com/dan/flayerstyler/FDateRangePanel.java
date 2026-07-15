package com.dan.flayerstyler;

import com.dan.flabel.FLabel;
import com.dan.fstyle.FFontRole;
import com.dan.fstyle.FTheme;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import java.awt.FlowLayout;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * Eingabe fuer den Gueltigkeitszeitraum einer Farbzuordnung (z. B. Saisonfarben).
 * Bei aktivierter "unbegrenzt"-Checkbox liefern {@link #getValidFrom()}/
 * {@link #getValidTo()} {@code null} (= dauerhaft gueltig, Projekt-Konvention).
 */
public class FDateRangePanel extends JPanel {

    private final JCheckBox unlimited = new JCheckBox("Unbegrenzt gueltig", true);
    private final JSpinner fromSpinner = dateSpinner();
    private final JSpinner toSpinner = dateSpinner();

    public FDateRangePanel() {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));

        FLabel fromLabel = new FLabel("Von:");
        FLabel toLabel = new FLabel("Bis:");
        fromLabel.setFont(FTheme.getInstance().getFont(FFontRole.CAPTION));
        toLabel.setFont(FTheme.getInstance().getFont(FFontRole.CAPTION));

        add(unlimited);
        add(fromLabel);
        add(fromSpinner);
        add(toLabel);
        add(toSpinner);

        unlimited.addActionListener(e -> updateEnabled());
        updateEnabled();
    }

    private void updateEnabled() {
        boolean limited = !unlimited.isSelected();
        fromSpinner.setEnabled(limited);
        toSpinner.setEnabled(limited);
    }

    private static JSpinner dateSpinner() {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "dd.MM.yyyy"));
        return s;
    }

    public LocalDate getValidFrom() {
        if (unlimited.isSelected()) {
            return null;
        }
        return toLocalDate((Date) fromSpinner.getValue());
    }

    public LocalDate getValidTo() {
        if (unlimited.isSelected()) {
            return null;
        }
        return toLocalDate((Date) toSpinner.getValue());
    }

    public boolean isUnlimited() {
        return unlimited.isSelected();
    }

    private static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
