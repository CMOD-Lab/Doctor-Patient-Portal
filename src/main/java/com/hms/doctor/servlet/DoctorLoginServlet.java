package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hms.dao.DoctorDAO;
import com.hms.dao.UserDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Doctor;

/**
 * DoctorLoginServlet - Cloud Readiness Fix: cr-java-0065 (HTTP Session State Storage)
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
@WebServlet("/doctorLogin")
public class DoctorLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		//get email and password which is coming from doctor_login.jsp page
		String email = req.getParameter("email");
		String password = req.getParameter("password");

		// Spring Session intercepts getSession() and returns a Redis-backed session.
		// The doctorObj and errorMsg attributes are stored in Amazon ElastiCache for
		// Redis, ensuring session data is available across all application instances
		// regardless of which server handles subsequent requests.
		HttpSession session = req.getSession();

		//create DB connection
		DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());
		
		//call loginDoctor() method for doctor login which method declared in DoctorDAO 
		Doctor doctor = docDAO.loginDoctor(email, password);

		if (doctor != null) {
			//means doctor is valid or exist
			//then store particular logged in doctor object in Redis-backed session.
			// Spring Session serializes the Doctor object and stores it in ElastiCache
			// for Redis, making it available to any application instance on redirect.
			session.setAttribute("doctorObj", doctor);
			//and redirect the particular doctor index page which is reside doctor folder
			resp.sendRedirect("doctor/index.jsp");//doctor index means dashboard of doctors
		} else {
			// Error message stored in Redis-backed session; survives load-balancer
			// routing to a different instance on the next request.
			session.setAttribute("errorMsg", "Invalid email or password");
			resp.sendRedirect("doctor_login.jsp");
		}

	}

}
