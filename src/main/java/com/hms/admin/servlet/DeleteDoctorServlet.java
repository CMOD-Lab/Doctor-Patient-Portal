package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;

/**
 * DeleteDoctorServlet - Cloud Readiness Fix: cr-java-0065 (HTTP Session State Storage)
 *
 * Session state is now managed via Spring Session backed by Amazon ElastiCache for Redis.
 * The HttpSession API is preserved; Spring Session transparently delegates all
 * session attribute storage to the centralized Redis cluster, enabling stateless
 * application instances and horizontal scaling without server affinity.
 *
 * The SpringSessionRepositoryFilter (registered in web.xml via DelegatingFilterProxy)
 * intercepts every request and wraps the HttpSession with a Redis-backed session.
 * All calls to req.getSession() and session.setAttribute() are transparently
 * routed to Amazon ElastiCache for Redis.
 *
 * Required infrastructure:
 *   - Amazon ElastiCache for Redis cluster
 *   - spring-session-data-redis dependency (see pom.xml)
 *   - RedisSessionConfig @Configuration class (com.hms.config.RedisSessionConfig)
 *   - SpringSessionRepositoryFilter registered in web.xml
 *   - Environment variables: REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
 */
@WebServlet("/deleteDoctor")
public class DeleteDoctorServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // get id (which is coming as string value) and convert into int
        int id = Integer.parseInt(req.getParameter("id"));

        DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());

        // Spring Session intercepts getSession() and returns a Redis-backed session.
        // All setAttribute calls store data in Amazon ElastiCache for Redis, ensuring
        // flash messages are available regardless of which instance handles the redirect.
        HttpSession session = req.getSession();

        boolean f = docDAO.deleteDoctorById(id);

        if (f == true) {
            // Success message stored in Redis-backed session; available on redirect
            // even if a different application instance handles the subsequent request.
            session.setAttribute("successMsg", "Doctor Deleted Successfully.");
            resp.sendRedirect("admin/view_doctor.jsp");
        } else {
            // Error message stored in Redis-backed session; survives load-balancer
            // routing to a different instance on the next request.
            session.setAttribute("errorMsg", "Something went wrong on server!");
            resp.sendRedirect("admin/view_doctor.jsp");
        }
    }

}
