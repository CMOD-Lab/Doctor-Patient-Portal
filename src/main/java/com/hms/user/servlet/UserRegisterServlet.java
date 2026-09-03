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

@WebServlet("/user_register")
public class UserRegisterServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			// get all data/value which is coming from signup.jsp page for new User
			// registration
			String fullName = req.getParameter("fullName");
			String email = req.getParameter("email");
			String password = req.getParameter("password");

			// Set all data to User Entity
			User user = new User(fullName, email, password);

			// Create Connection with DB
			UserDAO userDAO = new UserDAO(DBConnection.getConn());

			// call userRegister() and pass user object to insert or save user into DB.
			boolean f = userDAO.userRegister(user); // userRegister() method return boolean type value

			// Use short-lived cookie for flash message instead of in-memory session attribute
			if (f == true) {

				JwtUtil.setMessageCookie(resp, "Register Successfully", "success");
				resp.sendRedirect("signup.jsp");//which page you want to show this msg

			} else {
				
				JwtUtil.setMessageCookie(resp, "Something went wrong!", "error");
				resp.sendRedirect("signup.jsp");//which page you want to show this msg
				
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
