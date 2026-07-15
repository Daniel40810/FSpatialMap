package com.dan.fdal;

/**
 * Ungeprüfte Hülle für Datenzugriffsfehler.
 *
 * <p>Die F-DAL kapselt geprüfte {@link java.sql.SQLException}s in diese
 * {@code RuntimeException}, damit UI- und Service-Code nicht überall
 * {@code throws SQLException} schleppen muss. Die Original-{@code SQLException}
 * bleibt als {@code cause} erhalten.</p>
 *
 * @author com.dan
 */
public class FDataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FDataAccessException(String message) {
        super(message);
    }

    public FDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
