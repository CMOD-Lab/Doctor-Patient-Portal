package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hms.util.SessionUtil;

import com.hms.dao.AppointmentDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Appointment;

/**
 * Appointment servlet with distributed session management using Amazon ElastiCache for Redis.
 * Session data is stored in Redis instead of local memory, enabling:
 * - Stateless application instances
 * - Horizontal scalability across multiple servers
 * - Session persistence during instance restarts
 * - Load balancing without sticky sessions
 */
@WebServlet("/addAppointment")
public class AppointmentServlet extends HttpServlet{

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
	int userId	= Integer.parseInt(req.getParameter("userId"));
	String fullName = req.getParameter("fullName");
	String gender = req.getParameter("gender");
	String age = req.getParameter("age");
	String appointmentDate = req.getParameter("appointmentDate");
	String email = req.getParameter("email");
	String phone = req.getParameter("phone");
	String diseases = req.getParameter("diseases");
	int doctorId = Integer.parseInt(req.getParameter("doctorNameSelect"));
	String address = req.getParameter("address");
	
	
	Appointment appointment = new Appointment(userId, fullName, gender, age, appointmentDate, email, phone, diseases, doctorId, address, "Pending");
	
	AppointmentDAO appointmentDAO = new AppointmentDAO(DBConnection.getConn());
	boolean f = appointmentDAO.addAppointment(appointment);
	
	if(f==true) {
		// Success message stored in Redis-backed session
		SessionUtil.setAttribute(req, "successMsg", "Appointment is recorded Successfully.");
		resp.sendRedirect("user_appointment.jsp");
		
		
	}
	else {
		// Error message stored in Redis-backed session
		SessionUtil.setAttribute(req, "errorMsg", "Something went wrong on server!");
		resp.sendRedirect("user_appointment.jsp");
		
	}
	
	
	
	
		
	}

	
}
