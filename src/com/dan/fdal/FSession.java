package com.dan.fdal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * An <i>eine</i> {@link Connection} gebundene SQL-Ausführung — für Transaktionen.
 *
 * <p>Wird von {@link FDatabaseManager#inTransaction(FUnitOfWork)} erzeugt und an
 * die {@link FUnitOfWork} übergeben. Alle Operationen laufen auf derselben
 * Verbindung; Commit/Rollback steuert der Manager. Die Session leiht/​gibt
 * selbst keine Pool-Verbindung zurück.</p>
 *
 * @author com.dan
 */
public final class FSession implements FSql {

    private final Connection conn;

    FSession(Connection conn) {
        this.conn = conn;
    }

    /** Die gebundene Verbindung (für dialektspezifische Sonderfälle). */
    public Connection connection() {
        return conn;
    }

    @Override
    public <T> List<T> queryList(String sql, FParams binder, FRowMapper<T> mapper) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<T>();
                while (rs.next()) {
                    out.add(mapper.map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new FDataAccessException("queryList fehlgeschlagen: " + sql, e);
        }
    }

    @Override
    public <T> T queryOne(String sql, FParams binder, FRowMapper<T> mapper) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapper.map(rs) : null;
            }
        } catch (SQLException e) {
            throw new FDataAccessException("queryOne fehlgeschlagen: " + sql, e);
        }
    }

    @Override
    public int update(String sql, FParams binder) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(ps);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new FDataAccessException("update fehlgeschlagen: " + sql, e);
        }
    }

    @Override
    public Object updateAndReturnKey(String sql, FParams binder, String keyColumn) {
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{keyColumn})) {
            if (binder != null) binder.bind(ps);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getObject(1);
                throw new FDataAccessException("updateAndReturnKey: kein Schlüssel für " + keyColumn);
            }
        } catch (SQLException e) {
            throw new FDataAccessException("updateAndReturnKey fehlgeschlagen: " + sql, e);
        }
    }

    @Override
    public int[] batch(String sql, List<FParams> rows) {
        if (rows == null || rows.isEmpty()) {
            return new int[0];
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (FParams row : rows) {
                row.bind(ps);
                ps.addBatch();
            }
            return ps.executeBatch();
        } catch (SQLException e) {
            throw new FDataAccessException("batch fehlgeschlagen: " + sql, e);
        }
    }
}
