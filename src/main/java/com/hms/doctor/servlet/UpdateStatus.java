package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// cz-java-0069: In-Memory Session Storage remediation.
// HttpSession is now backed by Amazon ElastiCache (Redis) via Spring Session.
// The springSessionRepositoryFilter (registered in SpringSessionInitializer) transparently
// replaces the in-memory container session with a Redis-backed session, enabling horizontal
// scaling across multiple EKS pod instances without session loss on container restart.
// Environment variables required: REDIS_HOST, REDIS_PORT
import javax.servlet.http.HttpSession;

import com.hms.dao.AppointmentDAO;
import com.hms.db.DBConnection;

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
		 
		 // cz-java-0069: Session is Redis-backed via Spring Session + ElastiCache (not in-memory)
		 HttpSession session = req.getSession();
		 
		 
		 if(f == true) {
			 session.setAttribute("successMsg", "Comment updated");
			 resp.sendRedirect("doctor/patient.jsp");
			 
		 }else {
			 
			 session.setAttribute("errorMsg", "Something went wrong on server!");
			 resp.sendRedirect("doctor/patient.jsp");
			 
		 }
		 
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
}
