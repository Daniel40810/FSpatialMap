package com.dan.fdal;

/**
 * Unveränderliche Verbindungs- und Pool-Konfiguration.
 *
 * <p>Über {@link Builder} fluent zu erstellen. Wird an {@link FDatabaseManager}
 * bzw. {@link FConnectionPool} übergeben. Hält bewusst keine offenen Ressourcen
 * — reines Wertobjekt.</p>
 *
 * <pre>{@code
 * FDbConfig cfg = FDbConfig.builder()
 *         .url("jdbc:oracle:thin:@//host:1521/ORCLPDB1")
 *         .user("dashboard")
 *         .password("secret")
 *         .driverClass("oracle.jdbc.OracleDriver")
 *         .dialect(FSqlDialect.ORACLE)
 *         .poolSize(8)
 *         .build();
 * }</pre>
 *
 * @author com.dan
 */
public final class FDbConfig {

    private final String url;
    private final String user;
    private final String password;
    private final String driverClass;     // optional (JDBC 4 lädt Treiber per SPI)
    private final FSqlDialect dialect;
    private final int poolSize;
    private final long borrowTimeoutMs;
    private final int validationTimeoutSec;

    private FDbConfig(Builder b) {
        this.url = b.url;
        this.user = b.user;
        this.password = b.password;
        this.driverClass = b.driverClass;
        this.dialect = b.dialect;
        this.poolSize = b.poolSize;
        this.borrowTimeoutMs = b.borrowTimeoutMs;
        this.validationTimeoutSec = b.validationTimeoutSec;
    }

    public String getUrl()                { return url; }
    public String getUser()               { return user; }
    public String getPassword()           { return password; }
    public String getDriverClass()        { return driverClass; }
    public FSqlDialect getDialect()       { return dialect; }
    public int getPoolSize()              { return poolSize; }
    public long getBorrowTimeoutMs()      { return borrowTimeoutMs; }
    public int getValidationTimeoutSec()  { return validationTimeoutSec; }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent-Builder mit sinnvollen Defaults (Oracle, Pool 8). */
    public static final class Builder {
        private String url;
        private String user;
        private String password;
        private String driverClass;                  // null = SPI-Autoload
        private FSqlDialect dialect = FSqlDialect.ORACLE;
        private int poolSize = 8;
        private long borrowTimeoutMs = 5000L;
        private int validationTimeoutSec = 2;

        public Builder url(String v)                { this.url = v; return this; }
        public Builder user(String v)               { this.user = v; return this; }
        public Builder password(String v)           { this.password = v; return this; }
        public Builder driverClass(String v)        { this.driverClass = v; return this; }
        public Builder dialect(FSqlDialect v)        { this.dialect = v; return this; }
        public Builder poolSize(int v)              { this.poolSize = Math.max(1, v); return this; }
        public Builder borrowTimeoutMs(long v)      { this.borrowTimeoutMs = Math.max(0L, v); return this; }
        public Builder validationTimeoutSec(int v)  { this.validationTimeoutSec = Math.max(0, v); return this; }

        public FDbConfig build() {
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("FDbConfig: url darf nicht leer sein");
            }
            if (dialect == null) {
                throw new IllegalArgumentException("FDbConfig: dialect darf nicht null sein");
            }
            return new FDbConfig(this);
        }
    }
}
