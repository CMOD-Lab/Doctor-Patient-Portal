package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * UserLogoutServlet - Cloud Readiness Fix: cr-java-0065 (HTTP Session State Storage)
 *
 * Session state is now managed via Spring Session backed by Amazon ElastiCache for Redis.
 * The HttpSession API is preserved; Spring Session transparently delegates all
 * session attribute operations to the centralized Redis cluster, enabling stateless
 * application instances and horizontal scaling without server affinity.
 *
 * The SpringSessionRepositoryFilter (registered in web.xml via DelegatingFilterProxy)
 * intercepts every request and wraps the HttpSession with a Redis-backed session.
 * All calls to req.getSession(), session.removeAttribute(), and session.setAttribute()
 * are transparently routed to Amazon ElastiCache for Redis.
 *
 * On logout, the user object is removed from the Redis-backed session and a success
 * flash message is stored. Because the session is centralized in Redis, the logout
 * operation is effective across all application instances immediately — no server
 * affinity is required and no stale in-memory session state can persist.
 *
 * Required infrastructure:
 *   - Amazon ElastiCache for Redis cluster
 *   - spring-session-data-redis dependency (see pom.xml)
 *   - RedisSessionConfig @Configuration class (com.hms.config.RedisSessionConfig)
 *   - SpringSessionRepositoryFilter registered in web.xml
 *   - Environment variables: REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
 */
@WebServlet("/userLogout")
public class UserLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// Spring Session intercepts getSession() and returns a Redis-backed session.
		// removeAttribute() and setAttribute() operate on the centralized Redis store,
		// ensuring the logout is effective across all application instances.
		HttpSession session = req.getSession();

		// Remove the authenticated user object from the Redis-backed session.
		// This invalidates the user's login state across all application instances.
		session.removeAttribute("userObj");

		// Logout success message stored in Redis-backed session; available on redirect
		// even if a different application instance handles the subsequent request.
		session.setAttribute("successMsg", "User Logout Successfully.");
		resp.sendRedirect("user_login.jsp");
	}

}
