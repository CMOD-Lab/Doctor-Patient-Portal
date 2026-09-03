package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.dao.SpecialistDAO;
import com.hms.db.DBConnection;
import com.hms.util.JwtUtil;

@WebServlet("/addSpecialist")
public class SpecialistServlet extends HttpServlet{

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		String specialistName = req.getParameter("specialistName");
		
		SpecialistDAO specialistDAO = new SpecialistDAO(DBConnection.getConn());
		boolean f = specialistDAO.addSpecialist(specialistName);
		
		// Use short-lived cookie for flash message instead of in-memory session attribute
		if (f==true) {
			JwtUtil.setMessageCookie(resp, "Specialist added Successfully.", "success");
			resp.sendRedirect("admin/index.jsp");
			
		} else {
			JwtUtil.setMessageCookie(resp, "Something went wrong on server", "error");
			resp.sendRedirect("admin/index.jsp");
		}
	}
	
	

}
