package com.dan.fdal;

/**
 * SQL-Dialekt — steuert dialektabhängige Strategien (Batch-Insert, Paging).
 *
 * <p>Nach F-Style-Regel "feste Auswahlmenge → Enum". Wird in {@link FDbConfig}
 * gesetzt und von {@link FAbstractRepository} ausgewertet, damit dieselbe
 * Repository-Logik auf Oracle <i>und</i> generischen JDBC-Backends läuft.</p>
 *
 * @author com.dan
 */
public enum FSqlDialect {

    /**
     * Oracle (getestet gegen 21c).
     * <ul>
     *   <li>Batch-Insert über {@code INSERT ALL ... SELECT * FROM DUAL}.</li>
     *   <li>Paging über {@code OFFSET ? ROWS FETCH NEXT ? ROWS ONLY} (ab 12c).</li>
     * </ul>
     */
    ORACLE,

    /**
     * Generischer JDBC-Dialekt (H2, PostgreSQL, MySQL, …).
     * <ul>
     *   <li>Batch-Insert über JDBC {@code addBatch()/executeBatch()}.</li>
     *   <li>Paging über {@code LIMIT ? OFFSET ?}.</li>
     * </ul>
     */
    GENERIC
}
