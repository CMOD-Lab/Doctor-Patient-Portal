package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.util.SessionUtil;

/**
 * Admin logout servlet with distributed session management using Amazon ElastiCache for Redis.
 * Session cleanup is performed in Redis, ensuring logout is effective across all application instances.
 */
@WebServlet("/adminLogout")
public class AdminLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		// Check if session exists
		if (SessionUtil.hasSession(req)) {
			// Remove admin object from Redis-backed session
			SessionUtil.removeAttribute(req, "adminObj");
			
			// Show message after logout - stored in Redis
			SessionUtil.setAttribute(req, "successMsg", "Admin Logout Successfully");
		}
		
		resp.sendRedirect("admin_login.jsp");
		
	}

	
}
