package com.dan.fdal;

/**
 * Bequeme Oracle-Basis: {@link FAbstractRepository} mit fest verdrahtetem
 * {@link FSqlDialect#ORACLE}. Für Dashboard- und sonstige Oracle-21c-Repositories.
 *
 * <pre>{@code
 * public final class DashboardRepository extends FOracleRepository<DashboardRow, Integer> {
 *     public DashboardRepository(FSql exec) {
 *         super(exec, "DASHBOARD_DATA",
 *               FColumn.pk("ID"),
 *               FColumn.of("METRIC"),
 *               FColumn.of("VALUE_NUM"),
 *               FColumn.of("RECORDED_AT"));
 *     }
 *     protected DashboardRow mapRow(ResultSet rs) { ... }
 *     protected Object value(DashboardRow r, String col) { ... }
 * }
 * }</pre>
 *
 * @param <T>  Entitätstyp
 * @param <ID> Schlüsseltyp
 * @author com.dan
 */
public abstract class FOracleRepository<T, ID> extends FAbstractRepository<T, ID> {

    protected FOracleRepository(FSql exec, String table, FColumn... columns) {
        super(exec, FSqlDialect.ORACLE, table, columns);
    }
}
