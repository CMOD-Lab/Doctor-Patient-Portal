package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;
import com.hms.util.JwtUtil;

@WebServlet("/deleteDoctor")
public class DeleteDoctorServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		//get id(which is coming as string value) and convert into int	
		int id = Integer.parseInt(req.getParameter("id"));
		
		DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());
		
		boolean f = docDAO.deleteDoctorById(id);
		
		// Use short-lived cookie for flash message instead of in-memory session attribute
		if(f==true) {
			JwtUtil.setMessageCookie(resp, "Doctor Deleted Successfully.", "success");
			resp.sendRedirect("admin/view_doctor.jsp");
		}
		else {
			JwtUtil.setMessageCookie(resp, "Something went wrong on server!", "error");
			resp.sendRedirect("admin/view_doctor.jsp");
		}
	}
	
	

}
