package com.dan.fdal;

/**
 * Beschreibt eine Tabellenspalte für die generische SQL-Erzeugung in
 * {@link FAbstractRepository}.
 *
 * <p>{@code generated} markiert Spalten, die die DB selbst füllt (Oracle-Sequenz,
 * IDENTITY, Trigger) — sie werden aus INSERTs ausgeschlossen. {@code primaryKey}
 * markiert die ID-Spalte für {@code findById}/{@code update}/{@code delete}.</p>
 *
 * @author com.dan
 */
public final class FColumn {

    private final String name;
    private final boolean primaryKey;
    private final boolean generated;

    private FColumn(String name, boolean primaryKey, boolean generated) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("FColumn: name darf nicht leer sein");
        }
        this.name = name;
        this.primaryKey = primaryKey;
        this.generated = generated;
    }

    /** Normale Datenspalte. */
    public static FColumn of(String name) {
        return new FColumn(name, false, false);
    }

    /** Primärschlüssel-Spalte, vom Aufrufer befüllt (nicht generiert). */
    public static FColumn pk(String name) {
        return new FColumn(name, true, false);
    }

    /** Generierter Primärschlüssel (Sequenz/IDENTITY) — aus INSERT ausgeschlossen. */
    public static FColumn generatedPk(String name) {
        return new FColumn(name, true, true);
    }

    public String getName()       { return name; }
    public boolean isPrimaryKey() { return primaryKey; }
    public boolean isGenerated()  { return generated; }

    @Override
    public String toString() {
        return name + (primaryKey ? " [PK]" : "") + (generated ? " [gen]" : "");
    }
}
