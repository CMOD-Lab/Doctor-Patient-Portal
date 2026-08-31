package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hms.util.SessionUtil;

/**
 * User logout servlet with distributed session management using Amazon ElastiCache for Redis.
 * Session cleanup is performed in Redis, ensuring logout is effective across all application instances.
 */
@WebServlet("/userLogout")
public class UserLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		// Remove user object from Redis-backed session
		SessionUtil.removeAttribute(req, "userObj");
		// Success message stored in Redis-backed session
		SessionUtil.setAttribute(req, "successMsg", "User Logout Successfully.");
		resp.sendRedirect("user_login.jsp");
		
	}

	
	
}
