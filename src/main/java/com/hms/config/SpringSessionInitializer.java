package com.hms.config;

import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

/**
 * Registers the Spring Session springSessionRepositoryFilter before any other
 * filter in the servlet container.
 *
 * Rule: cz-java-0069 - In-Memory Session Storage
 * Remediation: Externalize Session Storage to Amazon ElastiCache (Redis) on EKS.
 *
 * This initializer ensures that every HttpSession obtained via
 * HttpServletRequest.getSession() is transparently backed by Redis
 * (Amazon ElastiCache) instead of the in-memory container session store,
 * enabling horizontal scaling across multiple EKS pod instances without
 * session loss on container restart.
 */
public class SpringSessionInitializer extends AbstractHttpSessionApplicationInitializer {

    public SpringSessionInitializer() {
        super(RedisSessionConfig.class);
    }
}
