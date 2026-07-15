package com.dan.fdal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Schlanker, abhängigkeitsfreier Connection-Pool (reines Java SE / JDBC).
 *
 * <p>Bewusst kein HikariCP o. Ä. — die F-Style-Bibliothek bleibt selbsttragend
 * (nur der JDBC-Treiber des Wirt-Projekts wird zur Laufzeit gebraucht).</p>
 *
 * <p>Eigenschaften:</p>
 * <ul>
 *   <li>Beschränkt auf {@code poolSize} Verbindungen, lazy erzeugt.</li>
 *   <li>{@link #borrow()} wartet bis {@code borrowTimeoutMs}, dann Fehler.</li>
 *   <li>Validierung defekter Verbindungen via {@link Connection#isValid(int)};
 *       tote werden verworfen und nachgezogen.</li>
 *   <li>{@link #release(Connection)} rollt offene Transaktionen zurück und stellt
 *       Auto-Commit wieder her, bevor zurück in den Pool gelegt wird.</li>
 * </ul>
 *
 * <p>Die rohen {@link Connection}-Objekte werden <b>nie</b> direkt an Aufrufer
 * gegeben; {@link FDatabaseManager} leiht/​gibt intern zurück, sodass kein
 * {@code close()}-Leck entsteht.</p>
 *
 * @author com.dan
 */
public final class FConnectionPool implements AutoCloseable {

    private final FDbConfig config;
    private final LinkedBlockingQueue<Connection> idle = new LinkedBlockingQueue<Connection>();
    private final AtomicInteger total = new AtomicInteger(0);
    private volatile boolean closed = false;

    public FConnectionPool(FDbConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("FConnectionPool: config darf nicht null sein");
        }
        this.config = config;
        String driver = config.getDriverClass();
        if (driver != null && !driver.trim().isEmpty()) {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                throw new FDataAccessException("JDBC-Treiber nicht gefunden: " + driver, e);
            }
        }
    }

    /** Leiht eine (validierte) Verbindung; blockiert bis Timeout, falls Pool voll. */
    public Connection borrow() {
        if (closed) {
            throw new FDataAccessException("Pool ist geschlossen");
        }
        try {
            Connection c = idle.poll();
            // tote Idle-Verbindungen entsorgen
            while (c != null && !isUsable(c)) {
                silentClose(c);
                total.decrementAndGet();
                c = idle.poll();
            }
            if (c != null) {
                return c;
            }
            // Platz im Pool? -> neue Verbindung
            if (total.get() < config.getPoolSize()) {
                Connection fresh = open();
                total.incrementAndGet();
                return fresh;
            }
            // Pool ausgelastet -> warten
            c = idle.poll(config.getBorrowTimeoutMs(), TimeUnit.MILLISECONDS);
            if (c == null) {
                throw new FDataAccessException(
                        "Keine Verbindung verfügbar (Pool ausgeschöpft nach "
                                + config.getBorrowTimeoutMs() + " ms)");
            }
            if (!isUsable(c)) {
                silentClose(c);
                total.decrementAndGet();
                return borrow(); // einmal frisch nachziehen
            }
            return c;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FDataAccessException("Unterbrochen beim Warten auf Verbindung", e);
        } catch (SQLException e) {
            throw new FDataAccessException("Verbindung konnte nicht geöffnet werden", e);
        }
    }

    /** Gibt eine Verbindung zurück in den Pool (oder verwirft sie, wenn defekt). */
    public void release(Connection c) {
        if (c == null) {
            return;
        }
        if (closed) {
            silentClose(c);
            total.decrementAndGet();
            return;
        }
        try {
            if (!c.getAutoCommit()) {
                c.rollback();
                c.setAutoCommit(true);
            }
        } catch (SQLException ignore) {
            // defekt -> unten verworfen
        }
        if (isUsable(c)) {
            idle.offer(c);
        } else {
            silentClose(c);
            total.decrementAndGet();
        }
    }

    private Connection open() throws SQLException {
        Connection c;
        if (config.getUser() != null) {
            c = DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword());
        } else {
            c = DriverManager.getConnection(config.getUrl());
        }
        c.setAutoCommit(true);
        return c;
    }

    private boolean isUsable(Connection c) {
        try {
            return c != null && !c.isClosed() && c.isValid(config.getValidationTimeoutSec());
        } catch (SQLException e) {
            return false;
        }
    }

    private static void silentClose(Connection c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (SQLException ignore) {
            // bewusst geschluckt
        }
    }

    /** Anzahl aktuell erzeugter (geliehener + freier) Verbindungen. */
    public int size() {
        return total.get();
    }

    /** Anzahl gerade freier Verbindungen im Pool. */
    public int idleCount() {
        return idle.size();
    }

    @Override
    public void close() {
        closed = true;
        Connection c;
        while ((c = idle.poll()) != null) {
            silentClose(c);
            total.decrementAndGet();
        }
    }
}
