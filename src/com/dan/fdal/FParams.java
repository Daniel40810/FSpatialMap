package com.dan.fdal;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Bindet Parameter an ein {@link PreparedStatement} (Index 1-basiert).
 *
 * <p>Funktionales Interface — als Lambda nutzbar. Hält Werte aus Roh-SQL heraus
 * und verhindert damit SQL-Injection.</p>
 *
 * <pre>{@code
 * sql.queryList("SELECT * FROM DASHBOARD_DATA WHERE METRIC = ?",
 *         ps -> ps.setString(1, "revenue"),
 *         mapper);
 * }</pre>
 *
 * @author com.dan
 */
@FunctionalInterface
public interface FParams {

    /** Leerer Binder (keine Parameter). */
    FParams NONE = ps -> { /* nichts zu binden */ };

    void bind(PreparedStatement ps) throws SQLException;
}
