package com.hms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Spring Session configuration backed by Amazon ElastiCache for Redis.
 *
 * Rule: cz-java-0069 - In-Memory Session Storage
 * Remediation: Externalize Session Storage to Amazon ElastiCache (Redis) on EKS.
 *
 * Replaces in-memory HttpSession with Spring Session backed by Amazon ElastiCache
 * for Redis, deployed as a Kubernetes-managed workload on EKS with IRSA for
 * secure access. Sessions survive container restarts and work correctly when
 * scaling horizontally across multiple EKS pod instances.
 *
 * Required environment variables:
 *   REDIS_HOST  - ElastiCache Redis primary endpoint (default: localhost)
 *   REDIS_PORT  - Redis port (default: 6379)
 */
@Configuration
@EnableRedisHttpSession
public class RedisSessionConfig {

    @Value("${REDIS_HOST:localhost}")
    private String redisHost;

    @Value("${REDIS_PORT:6379}")
    private int redisPort;

    @Bean
    public LettuceConnectionFactory connectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(config);
    }
}
