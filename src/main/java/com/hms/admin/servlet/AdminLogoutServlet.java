package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * AdminLogoutServlet - Cloud Readiness Fix: cr-java-0065 (HTTP Session State Storage)
 *
 * Session state is now managed via Spring Session backed by Amazon ElastiCache for Redis.
 * The HttpSession API is preserved; Spring Session transparently delegates all
 * session attribute storage and invalidation to the centralized Redis cluster,
 * enabling stateless application instances and horizontal scaling without server affinity.
 *
 * The SpringSessionRepositoryFilter (registered in web.xml via DelegatingFilterProxy)
 * intercepts every request and wraps the HttpSession with a Redis-backed session.
 * All calls to req.getSession(), session.removeAttribute(), and session.setAttribute()
 * are transparently routed to Amazon ElastiCache for Redis.
 *
 * Required infrastructure:
 *   - Amazon ElastiCache for Redis cluster
 *   - spring-session-data-redis dependency (see pom.xml)
 *   - RedisSessionConfig @Configuration class (com.hms.config.RedisSessionConfig)
 *   - SpringSessionRepositoryFilter registered in web.xml
 *   - Environment variables: REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
 */
@WebServlet("/adminLogout")
public class AdminLogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Spring Session intercepts getSession() and returns a Redis-backed session.
        // removeAttribute("adminObj") removes the attribute from the Redis store,
        // ensuring the logout is effective across all application instances.
        HttpSession session = req.getSession();
        session.removeAttribute("adminObj");

        // Success message stored in Redis-backed session; survives load-balancer
        // routing to a different instance on the redirect request.
        session.setAttribute("successMsg", "Admin Logout Successfully");
        resp.sendRedirect("admin_login.jsp");

    }

}
