package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hms.dao.AppointmentDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Appointment;

/**
 * AppointmentServlet - Cloud Readiness Fix: cr-java-0065 (HTTP Session State Storage)
 *
 * Session state is now managed via Spring Session backed by Amazon ElastiCache for Redis.
 * The HttpSession API is preserved; Spring Session transparently delegates all
 * session attribute operations to the centralized Redis cluster, enabling stateless
 * application instances and horizontal scaling without server affinity.
 *
 * The SpringSessionRepositoryFilter (registered in web.xml via DelegatingFilterProxy)
 * intercepts every request and wraps the HttpSession with a Redis-backed session.
 * All calls to req.getSession() and session.setAttribute() are transparently routed
 * to Amazon ElastiCache for Redis.
 *
 * Success and error flash messages are stored in the Redis-backed session, ensuring
 * they are available on redirect even if a different application instance handles the
 * subsequent request — eliminating server affinity requirements and enabling true
 * horizontal scaling.
 *
 * Required infrastructure:
 *   - Amazon ElastiCache for Redis cluster
 *   - spring-session-data-redis dependency (see pom.xml)
 *   - RedisSessionConfig @Configuration class (com.hms.config.RedisSessionConfig)
 *   - SpringSessionRepositoryFilter registered in web.xml
 *   - Environment variables: REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
 */
@WebServlet("/addAppointment")
public class AppointmentServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		int    userId          = Integer.parseInt(req.getParameter("userId"));
		String fullName        = req.getParameter("fullName");
		String gender          = req.getParameter("gender");
		String age             = req.getParameter("age");
		String appointmentDate = req.getParameter("appointmentDate");
		String email           = req.getParameter("email");
		String phone           = req.getParameter("phone");
		String diseases        = req.getParameter("diseases");
		int    doctorId        = Integer.parseInt(req.getParameter("doctorNameSelect"));
		String address         = req.getParameter("address");

		Appointment appointment = new Appointment(userId, fullName, gender, age, appointmentDate,
				email, phone, diseases, doctorId, address, "Pending");

		AppointmentDAO appointmentDAO = new AppointmentDAO(DBConnection.getConn());
		boolean f = appointmentDAO.addAppointment(appointment);

		// Spring Session intercepts getSession() and returns a Redis-backed session.
		// setAttribute() operates on the centralized Redis store, ensuring flash
		// messages are available across all application instances on redirect.
		HttpSession session = req.getSession();

		if (f == true) {
			// Success message stored in Redis-backed session; available on redirect
			// even if a different application instance handles the subsequent request.
			session.setAttribute("successMsg", "Appointment is recorded Successfully.");
			resp.sendRedirect("user_appointment.jsp");

		} else {
			// Error message stored in Redis-backed session for cross-instance availability.
			session.setAttribute("errorMsg", "Something went wrong on server!");
			resp.sendRedirect("user_appointment.jsp");
		}

	}

}
