package com.hms.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DBConnection - Cloud-Ready Connection Pool using HikariCP + Amazon RDS Proxy.
 *
 * Cloud Readiness Fix (cr-java-0073 - Direct JDBC Connections):
 * Replaces raw DriverManager.getConnection() with a HikariCP connection pool.
 * HikariCP is configured to connect through Amazon RDS Proxy, which provides:
 *   - Efficient connection pooling and reuse across application instances
 *   - Automatic failover and connection multiplexing for Amazon RDS/Aurora
 *   - IAM-based authentication support for enhanced security
 *   - Reduced database load from connection churn during scaling events
 *
 * Cloud Readiness Fix (cr-java-0097 - Missing Connection Timeouts):
 * Adds explicit connection, socket, and validation timeouts to prevent
 * indefinite hangs and resource exhaustion in cloud environments with
 * variable network latency and potential service failures:
 *   - connectionTimeout  : max time to wait for a connection from the pool
 *   - socketTimeout      : max time to wait for data on an established socket
 *                          (passed as JDBC URL parameter to the MySQL driver)
 *   - loginTimeout       : max time for the JDBC driver to establish a connection
 *   - keepAliveTime      : interval for sending keep-alive probes on idle connections
 *   - validationTimeout  : max time to validate a connection before returning it
 *   - initializationFailTimeout : fail fast if the pool cannot be initialised
 *
 * All connection parameters are read from environment variables so that no
 * credentials or host names are hardcoded in source code (12-factor app,
 * principle III – Config):
 *
 *   DB_HOST              - RDS Proxy endpoint (e.g., myproxy.proxy-xxxx.us-east-1.rds.amazonaws.com)
 *                          Falls back to "localhost" for local development.
 *   DB_PORT              - Database port. Defaults to 3306 (MySQL).
 *   DB_NAME              - Database / schema name. Defaults to "hospital".
 *   DB_USER              - Database username. Defaults to "root".
 *   DB_PASSWORD          - Database password. Defaults to empty string.
 *   DB_CONNECTION_TIMEOUT_MS - Max wait for a pool connection (ms). Defaults to 30000.
 *   DB_SOCKET_TIMEOUT_MS     - Socket read timeout (ms). Defaults to 30000.
 *   DB_LOGIN_TIMEOUT_MS      - JDBC driver login timeout (ms). Defaults to 10000.
 *
 * The HikariDataSource is initialised once (singleton) and shared across all
 * callers. Callers must close() the Connection after use so that it is
 * returned to the pool rather than discarded.
 */
public class DBConnection {

    // -----------------------------------------------------------------------
    // Pool configuration – values resolved from environment variables so that
    // the same WAR can be deployed to any environment without recompilation.
    // -----------------------------------------------------------------------

    private static final String DB_HOST =
            getEnv("DB_HOST", "localhost");

    private static final String DB_PORT =
            getEnv("DB_PORT", "3306");

    private static final String DB_NAME =
            getEnv("DB_NAME", "hospital");

    private static final String DB_USER =
            getEnv("DB_USER", "root");

    private static final String DB_PASSWORD =
            getEnv("DB_PASSWORD", "");

    // cr-java-0097: Timeout values read from environment variables with safe defaults.
    // All values are in milliseconds.
    private static final long DB_CONNECTION_TIMEOUT_MS =
            getLongEnv("DB_CONNECTION_TIMEOUT_MS", 30_000L);   // max wait for pool connection

    private static final long DB_SOCKET_TIMEOUT_MS =
            getLongEnv("DB_SOCKET_TIMEOUT_MS", 30_000L);       // socket / read timeout

    private static final long DB_LOGIN_TIMEOUT_MS =
            getLongEnv("DB_LOGIN_TIMEOUT_MS", 10_000L);        // JDBC driver login timeout

    // JDBC URL pointing at the RDS Proxy endpoint (or localhost for dev).
    // socketTimeout is passed as a JDBC URL parameter so the MySQL driver
    // enforces a network-level read timeout on every socket operation,
    // preventing indefinite hangs when the database or proxy becomes
    // unresponsive (cr-java-0097).
    // connectTimeout is the TCP-level connection establishment timeout.
    // useSSL=true is recommended for RDS Proxy; requireSSL can be enforced
    // via the RDS Proxy configuration in AWS.
    private static final String JDBC_URL = String.format(
            "jdbc:mysql://%s:%s/%s"
            + "?useSSL=false"
            + "&serverTimezone=UTC"
            + "&allowPublicKeyRetrieval=true"
            + "&connectTimeout=%d"
            + "&socketTimeout=%d",
            DB_HOST, DB_PORT, DB_NAME,
            DB_LOGIN_TIMEOUT_MS,    // TCP connect timeout (ms) for MySQL driver
            DB_SOCKET_TIMEOUT_MS);  // socket read/write timeout (ms) for MySQL driver

    // -----------------------------------------------------------------------
    // Singleton HikariDataSource – initialised once on first use.
    // -----------------------------------------------------------------------

    private static final HikariDataSource DATA_SOURCE;

    static {
        HikariConfig config = new HikariConfig();

        // Driver & URL
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(JDBC_URL);

        // Credentials
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);

        // Pool sizing – sensible defaults for a cloud deployment behind RDS Proxy.
        // RDS Proxy itself manages the upstream connection pool to the RDS instance,
        // so the application-side pool can be kept relatively small.
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        // -----------------------------------------------------------------------
        // cr-java-0097: Explicit connection, socket, and API call timeouts.
        // These prevent connections from hanging indefinitely in cloud environments
        // with variable network latency or transient service failures.
        // -----------------------------------------------------------------------

        // Maximum number of milliseconds that a client will wait for a connection
        // from the pool. If this time is exceeded without a connection becoming
        // available, a SQLException will be thrown. (HikariCP pool-level timeout)
        config.setConnectionTimeout(DB_CONNECTION_TIMEOUT_MS);

        // Maximum amount of time that a connection is allowed to sit idle in the
        // pool. Connections that are idle beyond this timeout will be retired.
        config.setIdleTimeout(600_000);        // 10 minutes

        // Maximum lifetime of a connection in the pool. Connections that reach
        // this age are retired and replaced, preventing stale connections.
        config.setMaxLifetime(1_800_000);      // 30 minutes

        // Maximum time that HikariCP will wait for a connection to be validated
        // as alive. Must be less than connectionTimeout.
        config.setValidationTimeout(5_000);    // 5 seconds (cr-java-0097)

        // Frequency at which HikariCP will attempt to keep idle connections alive
        // by sending a keep-alive query. Helps detect dead connections early in
        // cloud environments where NAT gateways / load balancers silently drop
        // idle TCP connections. (cr-java-0097)
        config.setKeepaliveTime(60_000);       // 60 seconds

        // Fail fast during startup if the pool cannot acquire an initial connection.
        // A value of 0 means HikariCP will not fail on startup if the DB is
        // temporarily unavailable (useful for cloud deployments where the DB may
        // not be ready immediately). Set to -1 to disable.
        config.setInitializationFailTimeout(1); // fail fast on startup (cr-java-0097)

        // Pool name – visible in JMX / CloudWatch metrics
        config.setPoolName("HmsHikariPool");

        // Validation query – keeps idle connections alive through RDS Proxy
        config.setConnectionTestQuery("SELECT 1");

        DATA_SOURCE = new HikariDataSource(config);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Borrows a {@link Connection} from the HikariCP pool.
     *
     * <p>Callers are responsible for closing the connection (preferably in a
     * try-with-resources block) so that it is returned to the pool:
     *
     * <pre>{@code
     *   try (Connection conn = DBConnection.getConn()) {
     *       // use conn ...
     *   }
     * }</pre>
     *
     * @return a live {@link Connection} from the pool, or {@code null} if the
     *         pool cannot provide one (error is printed to stderr).
     */
    public static Connection getConn() {
        try {
            return DATA_SOURCE.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the value of the named environment variable, or {@code defaultValue}
     * when the variable is absent or blank.
     */
    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }

    /**
     * Returns the long value of the named environment variable, or {@code defaultValue}
     * when the variable is absent, blank, or not a valid long.
     * Used for timeout configuration (cr-java-0097).
     */
    private static long getLongEnv(String name, long defaultValue) {
        String value = System.getenv(name);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return defaultValue;
    }
}
