package com.hms.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Session management utility for distributed session handling with Redis.
 * This utility provides a consistent interface for session operations that work
 * seamlessly with Spring Session and Amazon ElastiCache for Redis.
 * 
 * All session operations are automatically synchronized with Redis, enabling:
 * - Stateless application instances
 * - Horizontal scalability
 * - Session persistence across instance restarts
 * - Load balancing without session affinity
 */
public class SessionUtil {

    /**
     * Get or create a session from the request.
     * With Spring Session, this session is backed by Redis.
     */
    public static HttpSession getSession(HttpServletRequest request) {
        return request.getSession();
    }

    /**
     * Get or create a session with explicit creation flag.
     */
    public static HttpSession getSession(HttpServletRequest request, boolean create) {
        return request.getSession(create);
    }

    /**
     * Set an attribute in the session.
     * The attribute is automatically persisted to Redis.
     */
    public static void setAttribute(HttpServletRequest request, String name, Object value) {
        HttpSession session = request.getSession();
        session.setAttribute(name, value);
    }

    /**
     * Get an attribute from the session.
     * The attribute is retrieved from Redis if not in local cache.
     */
    public static Object getAttribute(HttpServletRequest request, String name) {
        HttpSession session = request.getSession(false);
        return session != null ? session.getAttribute(name) : null;
    }

    /**
     * Remove an attribute from the session.
     * The attribute is removed from Redis.
     */
    public static void removeAttribute(HttpServletRequest request, String name) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(name);
        }
    }

    /**
     * Invalidate the session.
     * The session is removed from Redis.
     */
    public static void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * Check if a session exists.
     */
    public static boolean hasSession(HttpServletRequest request) {
        return request.getSession(false) != null;
    }

    /**
     * Get session ID.
     * With Spring Session, this is a Redis-backed session ID.
     */
    public static String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null ? session.getId() : null;
    }
}
