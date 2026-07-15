package com.dan.fdal.color;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Liefert JDBC-Verbindungen fuer das {@link ColorAdminDAO}.
 * <p>
 * Das DAO ruft pro Operation {@link #getConnection()} auf und schliesst die
 * Verbindung anschliessend via try-with-resources. Damit funktioniert das DAO
 * sowohl mit einem Connection-Pool (z.B. UCP / HikariCP) als auch mit einer
 * einfachen {@code DriverManager}-Verbindung.
 * <p>
 * <b>Hinweis bei einer einzelnen, dauerhaften Verbindung:</b> Soll eine bereits
 * geoeffnete Verbindung wiederverwendet werden, ohne dass das DAO sie schliesst,
 * muss der Provider eine Connection zurueckliefern, deren {@code close()} keinen
 * Effekt hat (Wrapper). Beim Pool-Betrieb gibt {@code close()} die Verbindung
 * korrekt an den Pool zurueck.
 *
 * @author com.dan / Proj001
 */
@FunctionalInterface
public interface ConnectionProvider {

    /**
     * Liefert eine einsatzbereite JDBC-Verbindung.
     *
     * @return offene {@link Connection}
     * @throws SQLException wenn keine Verbindung aufgebaut werden kann
     */
    Connection getConnection() throws SQLException;
}
