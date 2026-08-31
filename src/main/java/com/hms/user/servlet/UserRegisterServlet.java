package com.hms.user.servlet;

import java.io.IOException;
import java.io.PrintWriter;

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
 * User registration servlet with distributed session management using Amazon ElastiCache for Redis.
 * Session data is stored in Redis instead of local memory, enabling:
 * - Stateless application instances
 * - Horizontal scalability across multiple servers
 * - Session persistence during instance restarts
 * - Load balancing without sticky sessions
 */
@WebServlet("/user_register")
public class UserRegisterServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			// PrintWriter out = resp.getWriter();

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

			if (f == true) {
				// Success message stored in Redis-backed session
				SessionUtil.setAttribute(req, "successMsg", "Register Successfully");
				resp.sendRedirect("signup.jsp");//which page you want to show this msg
				//System.out.println("register successfull");
				// out.println("success");

			} else {
				// Error message stored in Redis-backed session
				SessionUtil.setAttribute(req, "errorMsg", "Something went wrong!");
				resp.sendRedirect("signup.jsp");//which page you want to show this msg
				
				//System.out.println("Error! Something went wrong");
				// out.println("error");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
