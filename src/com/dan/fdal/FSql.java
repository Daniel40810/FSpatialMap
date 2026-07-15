package com.dan.fdal;

import java.util.List;

/**
 * Zentrale SQL-Ausführungs-Abstraktion.
 *
 * <p>Wird sowohl vom {@link FDatabaseManager} (jede Operation leiht/​gibt eine
 * Pool-Verbindung zurück, Auto-Commit) als auch von {@link FSession}
 * (eine gebundene Verbindung, transaktional) implementiert. Repositories
 * hängen nur an {@code FSql} und funktionieren dadurch innerhalb wie außerhalb
 * einer Transaktion.</p>
 *
 * @author com.dan
 */
public interface FSql {

    /** Führt ein SELECT aus und mappt alle Zeilen. */
    <T> List<T> queryList(String sql, FParams binder, FRowMapper<T> mapper);

    /** Führt ein SELECT aus und liefert die erste Zeile oder {@code null}. */
    <T> T queryOne(String sql, FParams binder, FRowMapper<T> mapper);

    /** Führt INSERT/UPDATE/DELETE aus und liefert die betroffene Zeilenzahl. */
    int update(String sql, FParams binder);

    /**
     * Führt dasselbe SQL für viele Parametersätze über JDBC-Batch aus
     * ({@code addBatch()/executeBatch()}). Liefert die betroffenen Zeilen je Satz.
     */
    int[] batch(String sql, List<FParams> rows);

    /**
     * INSERT mit {@code RETURN_GENERATED_KEYS}; liefert den vom Trigger oder der
     * Sequence erzeugten Schlüsselwert der angegebenen Spalte.
     */
    Object updateAndReturnKey(String sql, FParams binder, String keyColumn);
}
