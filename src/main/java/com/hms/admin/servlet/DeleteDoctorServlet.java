package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;
import com.hms.util.SessionUtil;

/**
 * Delete doctor servlet with distributed session management using Amazon ElastiCache for Redis.
 * Status messages are stored in Redis-backed session, accessible from any application instance.
 */
@WebServlet("/deleteDoctor")
public class DeleteDoctorServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		//get id(which is coming as string value) and convert into int	
		int id = Integer.parseInt(req.getParameter("id"));
		
		DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());
		
		// Session operations handled through SessionUtil with Redis-backed storage
		
		boolean f = docDAO.deleteDoctorById(id);
		
		if(f==true) {
			// Success message stored in Redis, accessible from any instance
			SessionUtil.setAttribute(req, "successMsg", "Doctor Deleted Successfully.");
			resp.sendRedirect("admin/view_doctor.jsp");
		}
		else {
			// Error message stored in Redis, accessible from any instance
			SessionUtil.setAttribute(req, "errorMsg", "Something went wrong on server!");
			resp.sendRedirect("admin/view_doctor.jsp");
		}
	}
	
	

}
