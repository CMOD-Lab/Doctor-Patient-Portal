package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.entity.User;
import com.hms.util.JwtUtil;

@WebServlet("/adminLogin")
public class AdminLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		try {
			
			//create one static Admin for this project
			String email = req.getParameter("email");
			String password = req.getParameter("password");
			
			//logic for a static Admin
			if ("admin@gmail.com".equals(email) && "admin".equals(password)) {
				
				// Issue a JWT token for stateless authentication (replaces server-side session)
				// Eliminates HttpSession.setAttribute("adminObj") to enable EKS horizontal scaling
				String token = JwtUtil.generateToken(email, "ADMIN");
				JwtUtil.setJwtCookie(resp, token);
				resp.sendRedirect("admin/index.jsp");
			}
			else {
				// Use short-lived cookie for flash message instead of session attribute
				JwtUtil.setMessageCookie(resp, "Invalid Username or Password.", "error");
				resp.sendRedirect("admin_login.jsp");
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
}
