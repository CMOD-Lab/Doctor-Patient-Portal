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

/**
 * User change password servlet with distributed session management using Amazon ElastiCache for Redis.
 * Session data is stored in Redis instead of local memory, enabling:
 * - Stateless application instances
 * - Horizontal scalability across multiple servers
 * - Session persistence during instance restarts
 * - Load balancing without sticky sessions
 */
@WebServlet("/userChangePassword")
public class ChangePasswordServlet extends HttpServlet{

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		int userId = Integer.parseInt(req.getParameter("userId"));
		String oldPassword = req.getParameter("oldPassword");
		String newPassword = req.getParameter("newPassword");
		
		UserDAO uDAO = new UserDAO(DBConnection.getConn());
		//boolean f = uDAO.checkOldPassword(userId, oldPassword);
		
		
		if(uDAO.checkOldPassword(userId, oldPassword)) {
			
			if(uDAO.changePassword(userId, newPassword)) {
				// Success message stored in Redis-backed session
				SessionUtil.setAttribute(req, "successMsg", "Password Change Successfully.");
				resp.sendRedirect("change_password.jsp");
				
			}else {
				// Error message stored in Redis-backed session
				SessionUtil.setAttribute(req, "errorMsg", "Something wrong on server!");
				resp.sendRedirect("change_password.jsp");
				
			}
			
		}else {
			// Error message stored in Redis-backed session
			SessionUtil.setAttribute(req, "errorMsg", "Old password incorrect");
			resp.sendRedirect("change_password.jsp");
		}
		
		
		
	}
	
	

}
