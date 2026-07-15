package com.dan.fdal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Zentraler Einstieg in die F-DAL: hält den {@link FConnectionPool}, führt SQL
 * im Auto-Commit aus und bietet einen sauberen Transaktions-Rahmen.
 *
 * <p>Implementiert {@link FSql}: jede Operation leiht eine Pool-Verbindung,
 * führt aus und gibt sie zuverlässig zurück (kein {@code close()}-Leck im
 * Aufrufer-Code).</p>
 *
 * <pre>{@code
 * FDatabaseManager db = new FDatabaseManager(cfg);
 * List<DashboardRow> rows = new DashboardRepository(db).findAll();
 * db.inTransaction(s -> {
 *     DashboardRepository repo = new DashboardRepository(s);
 *     repo.insert(a);
 *     repo.insert(b);   // a+b atomar
 * });
 * db.close();
 * }</pre>
 *
 * <p>Optional gibt es eine prozessweite Instanz über {@link #initShared(FDbConfig)}
 * / {@link #shared()} für einfache Single-DB-Anwendungen.</p>
 *
 * @author com.dan
 */
public final class FDatabaseManager implements FSql, AutoCloseable {

    private static volatile FDatabaseManager shared;

    private final FConnectionPool pool;
    private final FSqlDialect dialect;

    public FDatabaseManager(FDbConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("FDatabaseManager: config darf nicht null sein");
        }
        this.pool = new FConnectionPool(config);
        this.dialect = config.getDialect();
    }

    // -------------------------------------------------------- optionale shared-Instanz

    /** Initialisiert die prozessweite Instanz (idempotent pro JVM). */
    public static synchronized FDatabaseManager initShared(FDbConfig config) {
        if (shared == null) {
            shared = new FDatabaseManager(config);
        }
        return shared;
    }

    /** Liefert die prozessweite Instanz; wirft, falls nicht initialisiert. */
    public static FDatabaseManager shared() {
        FDatabaseManager s = shared;
        if (s == null) {
            throw new FDataAccessException(
                    "Shared-Instanz nicht initialisiert — zuerst initShared(config) aufrufen");
        }
        return s;
    }

    public FSqlDialect getDialect() {
        return dialect;
    }

    public FConnectionPool getPool() {
        return pool;
    }

    // -------------------------------------------------------- FSql (Auto-Commit)

    @Override
    public <T> List<T> queryList(String sql, FParams binder, FRowMapper<T> mapper) {
        Connection c = pool.borrow();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
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
        } finally {
            pool.release(c);
        }
    }

    @Override
    public <T> T queryOne(String sql, FParams binder, FRowMapper<T> mapper) {
        Connection c = pool.borrow();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapper.map(rs) : null;
            }
        } catch (SQLException e) {
            throw new FDataAccessException("queryOne fehlgeschlagen: " + sql, e);
        } finally {
            pool.release(c);
        }
    }

    @Override
    public int update(String sql, FParams binder) {
        Connection c = pool.borrow();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(ps);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new FDataAccessException("update fehlgeschlagen: " + sql, e);
        } finally {
            pool.release(c);
        }
    }

    @Override
    public Object updateAndReturnKey(String sql, FParams binder, String keyColumn) {
        Connection c = pool.borrow();
        try (PreparedStatement ps = c.prepareStatement(sql, new String[]{keyColumn})) {
            if (binder != null) binder.bind(ps);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getObject(1);
                throw new FDataAccessException("updateAndReturnKey: kein Schlüssel für " + keyColumn);
            }
        } catch (SQLException e) {
            throw new FDataAccessException("updateAndReturnKey fehlgeschlagen: " + sql, e);
        } finally {
            pool.release(c);
        }
    }

    @Override
    public int[] batch(String sql, List<FParams> rows) {
        if (rows == null || rows.isEmpty()) {
            return new int[0];
        }
        Connection c = pool.borrow();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (FParams row : rows) {
                row.bind(ps);
                ps.addBatch();
            }
            return ps.executeBatch();
        } catch (SQLException e) {
            throw new FDataAccessException("batch fehlgeschlagen: " + sql, e);
        } finally {
            pool.release(c);
        }
    }

    // -------------------------------------------------------- Transaktion

    /**
     * Führt {@code work} in einer Transaktion aus: Auto-Commit aus, bei Erfolg
     * Commit, bei Ausnahme Rollback. Die Verbindung wird danach sauber
     * (Auto-Commit wiederhergestellt) in den Pool zurückgegeben.
     */
    public void inTransaction(FUnitOfWork work) {
        Connection c = pool.borrow();
        try {
            c.setAutoCommit(false);
            FSession session = new FSession(c);
            work.run(session);
            c.commit();
        } catch (Exception e) {
            try {
                c.rollback();
            } catch (SQLException rb) {
                // Rollback-Fehler an die Hauptausnahme anhängen
                e.addSuppressed(rb);
            }
            if (e instanceof FDataAccessException) {
                throw (FDataAccessException) e;
            }
            throw new FDataAccessException("Transaktion zurückgerollt", e);
        } finally {
            // Auto-Commit zurücksetzen; release() validiert und entsorgt
            // eine Verbindung, deren Reset fehlschlägt.
            try {
                c.setAutoCommit(true);
            } catch (SQLException ignore) {
                // bewusst geschluckt — release prüft isValid()
            }
            pool.release(c);
        }
    }

    @Override
    public void close() {
        pool.close();
    }
}
