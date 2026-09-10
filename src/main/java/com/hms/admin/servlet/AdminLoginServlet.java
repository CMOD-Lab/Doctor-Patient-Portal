package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hms.entity.User;

/**
 * AdminLoginServlet - Cloud Readiness Fix: cr-java-0065 (HTTP Session State Storage)
 *
 * Session state is now managed via Spring Session backed by Amazon ElastiCache for Redis.
 * The HttpSession API is preserved; Spring Session transparently delegates all
 * session attribute storage to the centralized Redis cluster, enabling stateless
 * application instances and horizontal scaling without server affinity.
 *
 * The SpringSessionRepositoryFilter (registered in web.xml via DelegatingFilterProxy)
 * intercepts every request and wraps the HttpSession with a Redis-backed session.
 * All calls to req.getSession(), session.setAttribute(), and session.getAttribute()
 * are transparently routed to Amazon ElastiCache for Redis.
 *
 * Required infrastructure:
 *   - Amazon ElastiCache for Redis cluster
 *   - spring-session-data-redis dependency (see pom.xml)
 *   - RedisSessionConfig @Configuration class (com.hms.config.RedisSessionConfig)
 *   - SpringSessionRepositoryFilter registered in web.xml
 *   - Environment variables: REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
 */
@WebServlet("/adminLogin")
public class AdminLoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {

            // create one static Admin for this project
            String email = req.getParameter("email");
            String password = req.getParameter("password");

            // Spring Session intercepts getSession() and returns a Redis-backed session.
            // Session data is stored in Amazon ElastiCache for Redis rather than in local
            // JVM memory, enabling stateless instances and horizontal scaling.
            HttpSession session = req.getSession();

            // logic for a static Admin
            if ("admin@gmail.com".equals(email) && "admin".equals(password)) {

                // "adminObj" is stored in Redis via Spring Session; any application instance
                // can validate the session without requiring server affinity (sticky sessions).
                session.setAttribute("adminObj", new User());
                resp.sendRedirect("admin/index.jsp");
            } else {
                // Error message stored in Redis-backed session; survives load-balancer
                // routing to a different instance on the next request.
                session.setAttribute("errorMsg", "Invalid Username or Password.");
                resp.sendRedirect("admin_login.jsp");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
