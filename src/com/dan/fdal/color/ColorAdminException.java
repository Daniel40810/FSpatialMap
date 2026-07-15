package com.dan.fdal.color;

/**
 * Unchecked-Wrapper fuer Fehler in der COLOR_ADMIN-Datenzugriffsschicht.
 * <p>
 * Kapselt {@link java.sql.SQLException} und aehnliche Low-Level-Fehler, damit
 * aufrufender UI-/Service-Code nicht mit checked SQLExceptions durchsetzt wird.
 * Die Original-Ursache bleibt ueber {@link #getCause()} erhalten.
 *
 * @author com.dan / Proj001
 */
public class ColorAdminException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ColorAdminException(String message) {
        super(message);
    }

    public ColorAdminException(String message, Throwable cause) {
        super(message, cause);
    }
}
