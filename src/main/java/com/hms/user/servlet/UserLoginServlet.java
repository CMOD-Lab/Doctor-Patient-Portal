package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hms.dao.UserDAO;
import com.hms.db.DBConnection;
import com.hms.entity.User;

/**
 * UserLoginServlet - Cloud Readiness Fix: cr-java-0065 (HTTP Session State Storage)
 *
 * Session state is now managed via Spring Session backed by Amazon ElastiCache for Redis.
 * The HttpSession API is preserved; Spring Session transparently delegates all
 * session attribute operations to the centralized Redis cluster, enabling stateless
 * application instances and horizontal scaling without server affinity.
 *
 * The SpringSessionRepositoryFilter (registered in web.xml via DelegatingFilterProxy)
 * intercepts every request and wraps the HttpSession with a Redis-backed session.
 * All calls to req.getSession() and session.setAttribute() are transparently
 * routed to Amazon ElastiCache for Redis.
 *
 * The authenticated User object and error messages are stored in the Redis-backed
 * session, ensuring they are available on redirect even if a different application
 * instance handles the subsequent request — eliminating server affinity requirements
 * and enabling true horizontal scaling.
 *
 * Required infrastructure:
 *   - Amazon ElastiCache for Redis cluster
 *   - spring-session-data-redis dependency (see pom.xml)
 *   - RedisSessionConfig @Configuration class (com.hms.config.RedisSessionConfig)
 *   - SpringSessionRepositoryFilter registered in web.xml
 *   - Environment variables: REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
 */
@WebServlet("/userLogin")
public class UserLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String email    = req.getParameter("email");
		String password = req.getParameter("password");

		// Spring Session intercepts getSession() and returns a Redis-backed session.
		// All subsequent setAttribute() calls operate on the centralized Redis store,
		// ensuring session data is available across all application instances.
		HttpSession session = req.getSession();

		UserDAO userDAO = new UserDAO(DBConnection.getConn());
		User user = userDAO.loginUser(email, password);

		if (user != null) {
			// Authenticated User object stored in Redis-backed session; available on
			// redirect even if a different application instance handles the next request.
			session.setAttribute("userObj", user);
			resp.sendRedirect("index.jsp");
		} else {
			// Login error message stored in Redis-backed session for cross-instance availability.
			session.setAttribute("errorMsg", "Invalid email or password");
			resp.sendRedirect("user_login.jsp");
		}
	}

}
