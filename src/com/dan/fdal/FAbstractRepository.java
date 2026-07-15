package com.dan.fdal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialektgesteuerte Repository-Basis. Erzeugt CRUD-SQL aus Tabellenname und
 * {@link FColumn}-Metadaten — Unterklassen liefern nur Zeilen-Mapping und
 * Wert-Extraktion.
 *
 * <p>Unterklassen implementieren:</p>
 * <ul>
 *   <li>{@link #mapRow(ResultSet)} — eine Zeile → Entität (Lesen).</li>
 *   <li>{@link #value(Object, String)} — Spaltenwert einer Entität (Schreiben).</li>
 * </ul>
 *
 * <p>Alle Werte werden über {@link PreparedStatement} gebunden (kein
 * String-Einbau → keine SQL-Injection). {@code java.util.Date} und
 * {@code java.time}-Typen werden auf {@code java.sql}-Typen normalisiert.</p>
 *
 * @param <T>  Entitätstyp
 * @param <ID> Schlüsseltyp
 * @author com.dan
 */
public abstract class FAbstractRepository<T, ID> implements FRepository<T, ID> {

    protected final FSql sql;
    protected final FSqlDialect dialect;
    protected final String table;
    protected final FColumn[] columns;
    protected final FColumn pk;

    /** Zeilen pro {@code INSERT ALL}-Statement (Oracle), um Riesen-SQL zu vermeiden. */
    protected int insertChunkSize = 100;

    /**
     * @param exec     SQL-Ausführer ({@link FDatabaseManager} oder {@link FSession})
     * @param dialect  SQL-Dialekt
     * @param table    Tabellenname
     * @param columns  Spalten in Reihenfolge; genau eine sollte PK sein
     */
    protected FAbstractRepository(FSql exec, FSqlDialect dialect, String table, FColumn... columns) {
        if (exec == null)    throw new IllegalArgumentException("exec darf nicht null sein");
        if (dialect == null) throw new IllegalArgumentException("dialect darf nicht null sein");
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("table darf nicht leer sein");
        }
        if (columns == null || columns.length == 0) {
            throw new IllegalArgumentException("columns darf nicht leer sein");
        }
        this.sql = exec;
        this.dialect = dialect;
        this.table = table;
        this.columns = columns.clone();
        FColumn found = null;
        for (FColumn c : this.columns) {
            if (c.isPrimaryKey()) {
                found = c;
                break;
            }
        }
        this.pk = found;
    }

    // -------------------------------------------------------- Unterklassen-Vertrag

    /** Bildet die aktuelle ResultSet-Zeile auf eine Entität ab. */
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    /** Liefert den Wert einer Entität für die angegebene Spalte. */
    protected abstract Object value(T entity, String column);

    // -------------------------------------------------------- CRUD

    @Override
    public List<T> findAll() {
        return sql.queryList("SELECT * FROM " + table, FParams.NONE, rowMapper());
    }

    /** Seitenweise lesen (dialektabhängiges Paging). */
    public List<T> findPage(long offset, int limit) {
        String s;
        switch (dialect) {
            case ORACLE:
                s = "SELECT * FROM " + table + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
                break;
            case GENERIC:
            default:
                s = "SELECT * FROM " + table + " LIMIT ? OFFSET ?";
                break;
        }
        final long off = Math.max(0L, offset);
        final int lim = Math.max(0, limit);
        if (dialect == FSqlDialect.ORACLE) {
            return sql.queryList(s, ps -> { ps.setLong(1, off); ps.setInt(2, lim); }, rowMapper());
        }
        return sql.queryList(s, ps -> { ps.setInt(1, lim); ps.setLong(2, off); }, rowMapper());
    }

    @Override
    public T findById(ID id) {
        requirePk();
        return sql.queryOne(
                "SELECT * FROM " + table + " WHERE " + pk.getName() + " = ?",
                ps -> ps.setObject(1, normalize(id)),
                rowMapper());
    }

    @Override
    public int insert(T entity) {
        List<FColumn> ins = insertable();
        String s = "INSERT INTO " + table + " (" + names(ins) + ") VALUES (" + qmarks(ins.size()) + ")";
        return sql.update(s, ps -> bindValues(ps, 1, ins, entity));
    }

    /**
     * INSERT mit automatischem Schlüssel-Rückgabe (Trigger / Sequence).
     * Liefert den rohen JDBC-Schlüsselwert (meistens {@link java.math.BigDecimal}
     * bei Oracle-NUMBER oder {@link Long}).
     */
    public Object insertAndReturnKey(T entity) {
        requirePk();
        List<FColumn> ins = insertable();
        String s = "INSERT INTO " + table + " (" + names(ins) + ") VALUES (" + qmarks(ins.size()) + ")";
        return sql.updateAndReturnKey(s, ps -> bindValues(ps, 1, ins, entity), pk.getName());
    }

    @Override
    public int update(T entity) {
        requirePk();
        List<FColumn> upd = updatable();
        if (upd.isEmpty()) {
            throw new FDataAccessException("update: keine aktualisierbaren Spalten in " + table);
        }
        StringBuilder set = new StringBuilder();
        for (int i = 0; i < upd.size(); i++) {
            if (i > 0) set.append(", ");
            set.append(upd.get(i).getName()).append(" = ?");
        }
        String s = "UPDATE " + table + " SET " + set + " WHERE " + pk.getName() + " = ?";
        return sql.update(s, ps -> {
            int idx = bindValues(ps, 1, upd, entity);
            ps.setObject(idx, normalize(value(entity, pk.getName())));
        });
    }

    @Override
    public int deleteById(ID id) {
        requirePk();
        return sql.update(
                "DELETE FROM " + table + " WHERE " + pk.getName() + " = ?",
                ps -> ps.setObject(1, normalize(id)));
    }

    @Override
    public int insertAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        switch (dialect) {
            case ORACLE:
                return insertAllOracle(entities);
            case GENERIC:
            default:
                return insertAllGeneric(entities);
        }
    }

    @Override
    public long count() {
        Long n = sql.queryOne("SELECT COUNT(*) FROM " + table, FParams.NONE,
                rs -> rs.getLong(1));
        return n == null ? 0L : n;
    }

    // -------------------------------------------------------- Convenience für Unterklassen

    /** SELECT * FROM table {whereClause}, z. B. whereClause = "WHERE METRIC = ?". */
    protected List<T> query(String whereClause, FParams binder) {
        String s = "SELECT * FROM " + table + (whereClause == null ? "" : " " + whereClause);
        return sql.queryList(s, binder, rowMapper());
    }

    protected T queryFirst(String whereClause, FParams binder) {
        String s = "SELECT * FROM " + table + (whereClause == null ? "" : " " + whereClause);
        return sql.queryOne(s, binder, rowMapper());
    }

    // -------------------------------------------------------- Oracle / generischer Batch

    private int insertAllOracle(List<T> entities) {
        List<FColumn> ins = insertable();
        String cols = names(ins);
        String marks = qmarks(ins.size());
        int affected = 0;
        int n = entities.size();
        for (int start = 0; start < n; start += insertChunkSize) {
            int end = Math.min(n, start + insertChunkSize);
            final List<T> chunk = entities.subList(start, end);
            StringBuilder sb = new StringBuilder("INSERT ALL ");
            for (int i = 0; i < chunk.size(); i++) {
                sb.append("INTO ").append(table).append(" (").append(cols)
                  .append(") VALUES (").append(marks).append(") ");
            }
            sb.append("SELECT * FROM DUAL");
            final List<FColumn> insF = ins;
            affected += sql.update(sb.toString(), ps -> {
                int idx = 1;
                for (T e : chunk) {
                    idx = bindValues(ps, idx, insF, e);
                }
            });
        }
        return affected;
    }

    private int insertAllGeneric(List<T> entities) {
        final List<FColumn> ins = insertable();
        String s = "INSERT INTO " + table + " (" + names(ins) + ") VALUES (" + qmarks(ins.size()) + ")";
        List<FParams> rows = new ArrayList<FParams>(entities.size());
        for (final T e : entities) {
            rows.add(ps -> bindValues(ps, 1, ins, e));
        }
        int[] r = sql.batch(s, rows);
        int sum = 0;
        for (int v : r) {
            // Treiber liefern teils SUCCESS_NO_INFO (-2) — als 1 zählen
            sum += (v >= 0) ? v : 1;
        }
        return sum;
    }

    // -------------------------------------------------------- intern

    private FRowMapper<T> rowMapper() {
        return this::mapRow;
    }

    /** Bindet die Werte der Spalten ab {@code startIndex}; liefert nächsten freien Index. */
    private int bindValues(PreparedStatement ps, int startIndex, List<FColumn> cols, T entity)
            throws SQLException {
        int idx = startIndex;
        for (FColumn c : cols) {
            ps.setObject(idx++, normalize(value(entity, c.getName())));
        }
        return idx;
    }

    /** Spalten, die in ein INSERT gehen: alles außer generierte. */
    private List<FColumn> insertable() {
        List<FColumn> out = new ArrayList<FColumn>();
        for (FColumn c : columns) {
            if (!c.isGenerated()) {
                out.add(c);
            }
        }
        return out;
    }

    /** Spalten, die ein UPDATE setzt: nicht generiert und nicht PK. */
    private List<FColumn> updatable() {
        List<FColumn> out = new ArrayList<FColumn>();
        for (FColumn c : columns) {
            if (!c.isGenerated() && !c.isPrimaryKey()) {
                out.add(c);
            }
        }
        return out;
    }

    private void requirePk() {
        if (pk == null) {
            throw new FDataAccessException("Kein Primärschlüssel in " + table + " definiert");
        }
    }

    private static String names(List<FColumn> cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(cols.get(i).getName());
        }
        return sb.toString();
    }

    private static String qmarks(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(", ");
            sb.append('?');
        }
        return sb.toString();
    }

    /** Normalisiert java.util.Date und java.time-Typen auf java.sql-Typen. */
    protected static Object normalize(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof java.sql.Date
                || v instanceof java.sql.Timestamp
                || v instanceof java.sql.Time) {
            return v; // schon java.sql
        }
        if (v instanceof java.util.Date) {
            return new java.sql.Timestamp(((java.util.Date) v).getTime());
        }
        if (v instanceof java.time.LocalDate) {
            return java.sql.Date.valueOf((java.time.LocalDate) v);
        }
        if (v instanceof java.time.LocalDateTime) {
            return java.sql.Timestamp.valueOf((java.time.LocalDateTime) v);
        }
        return v;
    }
}
