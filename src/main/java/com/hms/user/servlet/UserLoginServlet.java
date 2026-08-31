package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hms.util.SessionUtil;

import com.hms.dao.UserDAO;
import com.hms.db.DBConnection;
import com.hms.entity.User;

/**
 * User login servlet with distributed session management using Amazon ElastiCache for Redis.
 * Session data is stored in Redis instead of local memory, enabling:
 * - Stateless application instances
 * - Horizontal scalability across multiple servers
 * - Session persistence during instance restarts
 * - Load balancing without sticky sessions
 */
@WebServlet("/userLogin")
public class UserLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		String email = req.getParameter("email");
		String password = req.getParameter("password");
		
		UserDAO userDAO = new UserDAO(DBConnection.getConn());
		User user = userDAO.loginUser(email, password);
		
		if (user!=null) {
			// User object stored in Redis-backed session
			SessionUtil.setAttribute(req, "userObj", user);
			resp.sendRedirect("index.jsp"); 
		}
		else {
			// Error message stored in Redis-backed session
			SessionUtil.setAttribute(req, "errorMsg", "Invalid email or password");
			resp.sendRedirect("user_login.jsp"); 
		}
	}
	
	
}
