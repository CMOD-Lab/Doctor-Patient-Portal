package com.hms.admin.servlet;

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

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;

@WebServlet("/deleteDoctor")
public class DeleteDoctorServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		//get id(which is coming as string value) and convert into int	
		int id = Integer.parseInt(req.getParameter("id"));
		
		DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());
		// cz-java-0069: Session is Redis-backed via Spring Session + ElastiCache (not in-memory)
		HttpSession session = req.getSession();
		
		boolean f = docDAO.deleteDoctorById(id);
		
		if(f==true) {
			session.setAttribute("successMsg", "Doctor Deleted Successfully.");
			resp.sendRedirect("admin/view_doctor.jsp");
		}
		else {
			session.setAttribute("errorMsg", "Something went wrong on server!");
			resp.sendRedirect("admin/view_doctor.jsp");
		}
	}
	
	

}
