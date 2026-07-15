package com.dan.fdal.sdo;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Liefert JDBC-Verbindungen fuer {@link OracleSdoFeatureProvider}.
 * Gleiches Muster wie {@code com.dan.fdal.color.ConnectionProvider} /
 * {@code com.dan.fdal.spatial.ConnectionProvider} - bewusst pro Paket
 * eigenstaendig, damit jedes DAL-Paket ohne Querabhaengigkeiten nutzbar bleibt.
 */
@FunctionalInterface
public interface ConnectionProvider {
    Connection getConnection() throws SQLException;
}
