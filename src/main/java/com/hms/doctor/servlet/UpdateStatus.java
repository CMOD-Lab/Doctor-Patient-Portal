package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hms.util.SessionUtil;

import com.hms.dao.AppointmentDAO;
import com.hms.db.DBConnection;

/**
 * Update appointment status servlet with distributed session management using Amazon ElastiCache for Redis.
 * Session data is stored in Redis instead of local memory, enabling:
 * - Stateless application instances
 * - Horizontal scalability across multiple servers
 * - Session persistence during instance restarts
 * - Load balancing without sticky sessions
 */
@WebServlet("/updateStatus")
public class UpdateStatus extends HttpServlet{

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		try {
			
		 int 	id = Integer.parseInt(req.getParameter("id"));
		 int 	doctorId = Integer.parseInt(req.getParameter("doctorId"));
		 String comment = req.getParameter("comment");
		 
		 AppointmentDAO appDAO = new AppointmentDAO(DBConnection.getConn());
		 boolean f = appDAO.updateDrAppointmentCommentStatus(id, doctorId, comment);
		 
		 
		 if(f == true) {
			 // Success message stored in Redis-backed session
			 SessionUtil.setAttribute(req, "successMsg", "Comment updated");
			 resp.sendRedirect("doctor/patient.jsp");
			 
		 }else {
			 // Error message stored in Redis-backed session
			 SessionUtil.setAttribute(req, "errorMsg", "Something went wrong on server!");
			 resp.sendRedirect("doctor/patient.jsp");
			 
		 }
		 
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
}
