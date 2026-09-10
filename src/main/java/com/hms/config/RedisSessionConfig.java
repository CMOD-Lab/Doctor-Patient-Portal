package com.hms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * RedisSessionConfig - Cloud Readiness Fix: cr-java-0065 (HTTP Session State Storage)
 *
 * Configures Spring Session to use Amazon ElastiCache for Redis as the centralized
 * session store. This replaces in-memory HTTP session storage with a distributed,
 * cloud-native session management solution that:
 *
 *   - Eliminates server affinity (sticky sessions) requirements
 *   - Enables horizontal scaling of application instances
 *   - Prevents session data loss when instances are terminated or restarted
 *   - Supports load balancing across multiple application servers
 *   - Follows 12-factor app stateless principles
 *
 * Spring Session transparently intercepts all HttpSession API calls in the
 * servlet layer (AdminLoginServlet, AdminLogoutServlet, DeleteDoctorServlet, etc.)
 * and delegates storage to the Redis cluster via the SpringSessionRepositoryFilter.
 *
 * Required environment variables:
 *   REDIS_HOST     - ElastiCache Redis primary endpoint (default: localhost)
 *   REDIS_PORT     - ElastiCache Redis port (default: 6379)
 *   REDIS_PASSWORD - ElastiCache Redis auth token (optional, leave unset if not configured)
 *
 * The @EnableRedisHttpSession annotation registers the SpringSessionRepositoryFilter
 * which replaces the default servlet container session with a Redis-backed session.
 * maxInactiveIntervalInSeconds controls the session TTL in Redis (default: 1800 = 30 min).
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class RedisSessionConfig {

    /**
     * Builds a Lettuce-based Redis connection factory pointing at the
     * Amazon ElastiCache cluster. Connection parameters are read from
     * environment variables to follow 12-factor app configuration principles.
     *
     * @return LettuceConnectionFactory connected to the ElastiCache Redis endpoint
     */
    @Bean
    public LettuceConnectionFactory connectionFactory() {
        String redisHost = System.getenv("REDIS_HOST") != null
                ? System.getenv("REDIS_HOST")
                : "localhost";

        int redisPort;
        try {
            redisPort = System.getenv("REDIS_PORT") != null
                    ? Integer.parseInt(System.getenv("REDIS_PORT"))
                    : 6379;
        } catch (NumberFormatException e) {
            redisPort = 6379;
        }

        RedisStandaloneConfiguration redisConfig =
                new RedisStandaloneConfiguration(redisHost, redisPort);

        // Set Redis AUTH token if provided (required for ElastiCache clusters
        // with in-transit encryption and AUTH token enabled).
        String redisPassword = System.getenv("REDIS_PASSWORD");
        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        return new LettuceConnectionFactory(redisConfig);
    }
}
