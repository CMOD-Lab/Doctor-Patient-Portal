package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * DoctorLogoutServlet - Cloud Readiness Fix: cr-java-0065 (HTTP Session State Storage)
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
 * On logout, the doctorObj attribute is removed from the Redis-backed session and
 * a success message is stored. The session entry in Redis is updated atomically,
 * ensuring consistent state across all application instances.
 *
 * Required infrastructure:
 *   - Amazon ElastiCache for Redis cluster
 *   - spring-session-data-redis dependency (see pom.xml)
 *   - RedisSessionConfig @Configuration class (com.hms.config.RedisSessionConfig)
 *   - SpringSessionRepositoryFilter registered in web.xml
 *   - Environment variables: REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
 */
@WebServlet("/doctorLogout")
public class DoctorLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// Spring Session intercepts getSession() and returns a Redis-backed session.
		// removeAttribute() and setAttribute() operate on the centralized Redis store,
		// ensuring the logout is reflected across all application instances immediately.
		HttpSession session = req.getSession();
		// Remove the doctor principal from the Redis-backed session; this invalidates
		// the doctor's authenticated state on all application instances simultaneously.
		session.removeAttribute("doctorObj");
		// Success message stored in Redis-backed session; available on redirect
		// even if a different application instance handles the subsequent request.
		session.setAttribute("successMsg", "Doctor Logout Successfully.");
		resp.sendRedirect("doctor_login.jsp");
	}

}
