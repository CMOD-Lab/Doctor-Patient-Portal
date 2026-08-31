# Redis Session Implementation for Cloud-Native Deployment

## Overview

This application has been migrated from traditional HTTP session storage to **Amazon ElastiCache for Redis** using **Spring Session**. This enables stateless application instances, horizontal scalability, and cloud-native deployment patterns.

## Architecture

### Before Migration (Traditional HTTP Sessions)
```
┌─────────────┐     ┌─────────────┐
│  Instance 1 │     │  Instance 2 │
│  ┌────────┐ │     │  ┌────────┐ │
│  │Session │ │     │  │Session │ │
│  │ Store  │ │     │  │ Store  │ │
│  └────────┘ │     │  └────────┘ │
└─────────────┘     └─────────────┘
     ❌ Server Affinity Required
     ❌ Session Loss on Instance Restart
     ❌ Cannot Scale Horizontally
```

### After Migration (Redis-Backed Sessions)
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Instance 1 │     │  Instance 2 │     │  Instance N │
│  (Stateless)│     │  (Stateless)│     │  (Stateless)│
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │
                    ┌──────▼──────┐
                    │   Amazon    │
                    │ ElastiCache │
                    │   (Redis)   │
                    └─────────────┘
     ✅ No Server Affinity Needed
     ✅ Session Persists Across Restarts
     ✅ Horizontal Scaling Enabled
```

## Implementation Components

### 1. Spring Session Configuration (`RedisSessionConfig.java`)

**Location:** `src/main/java/com/hms/config/RedisSessionConfig.java`

**Purpose:** Configures Spring Session to use Redis as the session store.

**Key Features:**
- Connects to AWS ElastiCache Redis cluster
- Configures Lettuce client for optimal performance
- Sets session timeout (30 minutes default)
- Supports Redis AUTH for secure connections
- Uses JSON serialization for complex objects

**Configuration Properties:**
```properties
spring.redis.host=${REDIS_HOST:localhost}
spring.redis.port=${REDIS_PORT:6379}
spring.redis.password=${REDIS_PASSWORD:}
spring.session.timeout=1800s
```

### 2. Session Utility (`SessionUtil.java`)

**Location:** `src/main/java/com/hms/util/SessionUtil.java`

**Purpose:** Provides a consistent abstraction layer for session operations.

**Benefits:**
- Centralizes session management logic
- Makes Redis integration transparent to servlets
- Simplifies testing and maintenance
- Provides clear API for session operations

**API Methods:**
```java
// Get or create session
HttpSession session = SessionUtil.getSession(request);

// Set attribute (stored in Redis)
SessionUtil.setAttribute(request, "userObj", user);

// Get attribute (retrieved from Redis)
Object value = SessionUtil.getAttribute(request, "userObj");

// Remove attribute (removed from Redis)
SessionUtil.removeAttribute(request, "userObj");

// Invalidate session (removed from Redis)
SessionUtil.invalidateSession(request);
```

### 3. Serializable Entities

All entity classes that are stored in sessions implement `Serializable`:

- `User.java` - User entity with serialVersionUID
- `Doctor.java` - Doctor entity with serialVersionUID
- `Appointment.java` - Appointment entity with serialVersionUID
- `Specialist.java` - Specialist entity with serialVersionUID

**Why Serializable?**
Redis stores session data as byte arrays. Java objects must be serializable to be converted to/from bytes.

### 4. Web Configuration (`web.xml`)

**Spring Session Filter:** Intercepts all HTTP requests to manage Redis-backed sessions.

```xml
<filter>
  <filter-name>springSessionRepositoryFilter</filter-name>
  <filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
</filter>
<filter-mapping>
  <filter-name>springSessionRepositoryFilter</filter-name>
  <url-pattern>/*</url-pattern>
</filter-mapping>
```

### 5. Maven Dependencies (`pom.xml`)

**Required Dependencies:**
- `spring-session-data-redis` - Spring Session Redis integration
- `spring-context` - Spring Core framework
- `spring-web` - Spring Web support
- `lettuce-core` - Redis client (recommended for Spring Session)
- `commons-pool2` - Connection pooling for Redis

## AWS ElastiCache Setup

### Prerequisites

1. **Create ElastiCache Redis Cluster:**
   ```bash
   aws elasticache create-cache-cluster \
     --cache-cluster-id hms-session-cache \
     --engine redis \
     --cache-node-type cache.t3.micro \
     --num-cache-nodes 1 \
     --engine-version 7.0
   ```

2. **Configure Security Groups:**
   - Allow inbound traffic on port 6379 from application security group
   - Ensure VPC and subnet configuration allows connectivity

3. **Enable AUTH (Optional but Recommended):**
   ```bash
   aws elasticache modify-cache-cluster \
     --cache-cluster-id hms-session-cache \
     --auth-token-enabled \
     --auth-token "your-secure-token"
   ```

### Environment Variables

Set these environment variables in your deployment environment:

```bash
# Required
export REDIS_HOST=hms-session-cache.abc123.0001.use1.cache.amazonaws.com
export REDIS_PORT=6379

# Optional (for encrypted connections)
export REDIS_SSL=true
export REDIS_PASSWORD=your-auth-token
```

**For AWS ECS/EKS:**
```json
{
  "environment": [
    {
      "name": "REDIS_HOST",
      "value": "hms-session-cache.abc123.0001.use1.cache.amazonaws.com"
    },
    {
      "name": "REDIS_PORT",
      "value": "6379"
    },
    {
      "name": "REDIS_SSL",
      "value": "true"
    }
  ]
}
```

**For AWS Elastic Beanstalk:**
```bash
eb setenv REDIS_HOST=hms-session-cache.abc123.0001.use1.cache.amazonaws.com \
          REDIS_PORT=6379 \
          REDIS_SSL=true
```

## Migration Summary

### Files Modified

1. **Entity Classes** - Added `Serializable` interface:
   - `User.java`
   - `Doctor.java`
   - `Appointment.java`
   - `Specialist.java`

2. **Servlet Classes** - Updated to use `SessionUtil`:
   - `UserLoginServlet.java`
   - `UserLogoutServlet.java`
   - `ChangePasswordServlet.java`
   - `AdminLoginServlet.java`
   - `AdminLogoutServlet.java`
   - `DoctorLoginServlet.java`
   - `DoctorLogoutServlet.java`
   - All other servlets using session

3. **Configuration Files**:
   - `pom.xml` - Added Spring Session and Redis dependencies
   - `web.xml` - Added Spring Session filter
   - `application.properties` - Added Redis configuration

4. **New Files Created**:
   - `RedisSessionConfig.java` - Spring Session configuration
   - `SessionUtil.java` - Session management utility

### Session Operations Migrated

| Operation | Before | After |
|-----------|--------|-------|
| Get Session | `request.getSession()` | `SessionUtil.getSession(request)` |
| Set Attribute | `session.setAttribute(name, value)` | `SessionUtil.setAttribute(request, name, value)` |
| Get Attribute | `session.getAttribute(name)` | `SessionUtil.getAttribute(request, name)` |
| Remove Attribute | `session.removeAttribute(name)` | `SessionUtil.removeAttribute(request, name)` |
| Invalidate | `session.invalidate()` | `SessionUtil.invalidateSession(request)` |

## Benefits Achieved

### 1. Stateless Application Instances
- Application instances no longer store session data locally
- Instances can be terminated and recreated without session loss
- Supports auto-scaling and spot instances

### 2. Horizontal Scalability
- Multiple instances can serve the same user
- Load balancers can distribute traffic without sticky sessions
- Easy to scale up/down based on demand

### 3. High Availability
- Session data persists in Redis even if application instances fail
- Redis replication provides data redundancy
- Automatic failover with ElastiCache

### 4. Cloud-Native Deployment
- Compatible with AWS ECS, EKS, Elastic Beanstalk
- Supports containerized deployments
- Follows 12-factor app principles

### 5. Performance
- Redis provides fast in-memory session access
- Connection pooling optimizes Redis connections
- Lettuce client provides async operations

## Testing

### Local Testing

1. **Start Redis locally:**
   ```bash
   docker run -d -p 6379:6379 redis:7.0
   ```

2. **Run the application:**
   ```bash
   mvn clean package
   # Deploy WAR to Tomcat or run with embedded server
   ```

3. **Verify session storage:**
   ```bash
   # Connect to Redis
   redis-cli
   
   # List session keys
   KEYS hms:session:*
   
   # View session data
   GET hms:session:sessions:<session-id>
   ```

### Production Testing

1. **Verify ElastiCache connectivity:**
   ```bash
   telnet hms-session-cache.abc123.0001.use1.cache.amazonaws.com 6379
   ```

2. **Monitor session metrics:**
   - CloudWatch metrics for ElastiCache
   - Application logs for session operations
   - Redis INFO command for statistics

3. **Test failover:**
   - Terminate an application instance
   - Verify user session persists on another instance
   - Check Redis for session data

## Troubleshooting

### Issue: Cannot connect to Redis

**Symptoms:**
- Application fails to start
- Session operations throw connection errors

**Solutions:**
1. Verify REDIS_HOST and REDIS_PORT environment variables
2. Check security group rules allow traffic on port 6379
3. Verify VPC and subnet configuration
4. Check Redis AUTH token if enabled

### Issue: Session data not persisting

**Symptoms:**
- Users logged out after instance restart
- Session attributes not available

**Solutions:**
1. Verify Spring Session filter is configured in web.xml
2. Check entity classes implement Serializable
3. Verify Redis connection is established
4. Check application logs for serialization errors

### Issue: Performance degradation

**Symptoms:**
- Slow session operations
- High Redis CPU usage

**Solutions:**
1. Enable connection pooling (already configured)
2. Increase ElastiCache instance size
3. Enable Redis clustering for high traffic
4. Review session data size and optimize

## Security Considerations

### 1. Redis AUTH
- Always enable AUTH token in production
- Store AUTH token in AWS Secrets Manager
- Rotate AUTH tokens regularly

### 2. Encryption in Transit
- Enable SSL/TLS for Redis connections
- Set `REDIS_SSL=true` in production
- Use ElastiCache encryption in transit

### 3. Network Security
- Use VPC security groups to restrict access
- Only allow application security group to access Redis
- Use private subnets for ElastiCache

### 4. Session Data
- Avoid storing sensitive data in sessions
- Use encryption for sensitive session attributes
- Set appropriate session timeout values

## Monitoring

### CloudWatch Metrics

Monitor these ElastiCache metrics:
- `CPUUtilization` - Redis CPU usage
- `NetworkBytesIn/Out` - Network traffic
- `CurrConnections` - Active connections
- `Evictions` - Memory pressure indicator
- `CacheHits/CacheMisses` - Cache efficiency

### Application Logs

Monitor these application events:
- Session creation/destruction
- Redis connection errors
- Serialization failures
- Session timeout events

## Cost Optimization

### 1. Right-Size ElastiCache Instance
- Start with `cache.t3.micro` for development
- Use `cache.t3.small` or `cache.t3.medium` for production
- Monitor memory usage and scale as needed

### 2. Session Timeout
- Set appropriate timeout (default: 30 minutes)
- Shorter timeout = less memory usage
- Balance user experience with cost

### 3. Reserved Instances
- Purchase reserved instances for production
- Save up to 55% compared to on-demand pricing

## Compliance

This implementation supports:
- **12-Factor App Principles** - Stateless processes, external configuration
- **Cloud-Native Patterns** - Horizontal scaling, high availability
- **AWS Well-Architected Framework** - Reliability, performance, security

## References

- [Spring Session Documentation](https://docs.spring.io/spring-session/reference/)
- [AWS ElastiCache for Redis](https://aws.amazon.com/elasticache/redis/)
- [Lettuce Redis Client](https://lettuce.io/)
- [12-Factor App Methodology](https://12factor.net/)

## Support

For issues or questions:
1. Check application logs for errors
2. Review CloudWatch metrics for ElastiCache
3. Verify environment variables are set correctly
4. Consult Spring Session documentation
5. Contact AWS Support for ElastiCache issues
