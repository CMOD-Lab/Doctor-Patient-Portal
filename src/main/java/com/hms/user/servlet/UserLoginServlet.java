package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.dao.UserDAO;
import com.hms.db.DBConnection;
import com.hms.entity.User;
import com.hms.util.JwtUtil;

@WebServlet("/userLogin")
public class UserLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		String email = req.getParameter("email");
		String password = req.getParameter("password");
		
		UserDAO userDAO = new UserDAO(DBConnection.getConn());
		User user = userDAO.loginUser(email, password);
		
		if (user!=null) {
			// Issue a JWT token for stateless authentication (replaces server-side session)
			// Eliminates HttpSession.setAttribute("userObj") to enable EKS horizontal scaling
			String token = JwtUtil.generateToken(email, "USER");
			JwtUtil.setJwtCookie(resp, token);
			resp.sendRedirect("index.jsp"); 
		}
		else {
			// Use short-lived cookie for flash message instead of in-memory session attribute
			JwtUtil.setMessageCookie(resp, "Invalid email or password", "error");
			resp.sendRedirect("user_login.jsp"); 
		}
	}
	
	
}
