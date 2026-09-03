package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.util.JwtUtil;

@WebServlet("/adminLogout")
public class AdminLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		// Clear the JWT cookie to invalidate the stateless session (replaces session.removeAttribute)
		JwtUtil.clearJwtCookie(resp);
		// Use short-lived cookie for flash message instead of in-memory session attribute
		JwtUtil.setMessageCookie(resp, "Admin Logout Successfully", "success");
		resp.sendRedirect("admin_login.jsp");
		
		
		
	}

	
}
