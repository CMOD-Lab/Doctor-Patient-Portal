package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.dao.UserDAO;
import com.hms.db.DBConnection;
import com.hms.util.JwtUtil;

@WebServlet("/userChangePassword")
public class ChangePasswordServlet extends HttpServlet{

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		int userId = Integer.parseInt(req.getParameter("userId"));
		String oldPassword = req.getParameter("oldPassword");
		String newPassword = req.getParameter("newPassword");
		
		UserDAO uDAO = new UserDAO(DBConnection.getConn());
		
		// Use short-lived cookie for flash message instead of in-memory session attribute
		if(uDAO.checkOldPassword(userId, oldPassword)) {
			
			if(uDAO.changePassword(userId, newPassword)) {
				
				JwtUtil.setMessageCookie(resp, "Password Change Successfully.", "success");
				resp.sendRedirect("change_password.jsp");
				
			}else {
				
				JwtUtil.setMessageCookie(resp, "Something wrong on server!", "error");
				resp.sendRedirect("change_password.jsp");
				
			}
			
		}else {
			JwtUtil.setMessageCookie(resp, "Old password incorrect", "error");
			resp.sendRedirect("change_password.jsp");
		}
		
		
		
	}
	
	

}
