package com.dan.fdal;

import java.util.List;

/**
 * Generischer CRUD-Vertrag für eine Entität {@code T} mit Schlüssel {@code ID}.
 *
 * <p>Implementierungen leiten i. d. R. von {@link FAbstractRepository} ab und
 * müssen nur Tabelle, Spalten, Zeilen-Mapping und Parameter-Extraktion liefern.</p>
 *
 * @param <T>  Entitätstyp
 * @param <ID> Schlüsseltyp
 * @author com.dan
 */
public interface FRepository<T, ID> {

    List<T> findAll();

    /** Liefert die Entität zum Schlüssel oder {@code null}. */
    T findById(ID id);

    /** INSERT einer Entität. Liefert die Zahl betroffener Zeilen. */
    int insert(T entity);

    /** UPDATE einer Entität anhand des Primärschlüssels. */
    int update(T entity);

    /** DELETE anhand des Schlüssels. */
    int deleteById(ID id);

    /** Massen-INSERT (dialektabhängige Strategie). */
    int insertAll(List<T> entities);

    long count();
}
