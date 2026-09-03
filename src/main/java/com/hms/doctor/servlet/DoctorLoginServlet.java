package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Doctor;
import com.hms.util.JwtUtil;


@WebServlet("/doctorLogin")
public class DoctorLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		//get email and password which is coming from doctor_login.jsp page
		String email = req.getParameter("email");
		String password = req.getParameter("password");

		//create DB connection
		DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());
		
		//call loginDoctor() method for doctor login which method declared in DoctorDAO 
		Doctor doctor = docDAO.loginDoctor(email, password);

		if (doctor != null) {
			// Issue a JWT token for stateless authentication (replaces server-side session)
			// Eliminates HttpSession.setAttribute("doctorObj") to enable EKS horizontal scaling
			String token = JwtUtil.generateToken(email, "DOCTOR");
			JwtUtil.setJwtCookie(resp, token);
			//and redirect the particular doctor index page which is reside doctor folder
			resp.sendRedirect("doctor/index.jsp");//doctor index means dashboard of doctors
		} else {
			// Use short-lived cookie for flash message instead of in-memory session attribute
			JwtUtil.setMessageCookie(resp, "Invalid email or password", "error");
			resp.sendRedirect("doctor_login.jsp");
		}

	}

}
