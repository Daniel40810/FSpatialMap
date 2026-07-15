package com.dan.fdal;

/**
 * Eine transaktionale Arbeitseinheit über einer {@link FSession}.
 *
 * <p>An {@link FDatabaseManager#inTransaction(FUnitOfWork)} übergeben. Läuft die
 * Methode normal durch, committet der Manager; wirft sie, wird zurückgerollt und
 * die Ausnahme propagiert.</p>
 *
 * @author com.dan
 */
@FunctionalInterface
public interface FUnitOfWork {

    void run(FSession session) throws Exception;
}
