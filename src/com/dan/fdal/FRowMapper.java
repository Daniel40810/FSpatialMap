package com.dan.fdal;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Bildet die <i>aktuelle</i> Zeile eines {@link ResultSet} auf ein Objekt ab.
 *
 * <p>Funktionales Interface. Der Aufrufer (z. B. {@link FDatabaseManager})
 * steuert den Cursor via {@code rs.next()}; {@code map} liest nur Spalten der
 * aktuellen Zeile — kein eigenes {@code next()} im Mapper.</p>
 *
 * @param <T> Zieltyp
 * @author com.dan
 */
@FunctionalInterface
public interface FRowMapper<T> {

    T map(ResultSet rs) throws SQLException;
}
