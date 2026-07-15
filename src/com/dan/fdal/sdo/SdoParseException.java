package com.dan.fdal.sdo;

/**
 * Unchecked Exception fuer alles, was beim Lesen/Parsen von SDO_GEOMETRY-Spalten
 * schiefgeht - kapselt SQLException analog zu {@code ColorAdminException} /
 * {@code FDataAccessException} aus den Schwester-Paketen.
 */
public class SdoParseException extends RuntimeException {

    public SdoParseException(String message) {
        super(message);
    }

    public SdoParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
