package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;
import com.hms.util.JwtUtil;

@WebServlet("/doctorChangePassword")
public class DoctorChangePassword extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		int doctorId = Integer.parseInt(req.getParameter("doctorId"));
		String newPassword = req.getParameter("newPassword");
		String oldPassword = req.getParameter("oldPassword");

		DoctorDAO doctorDAO = new DoctorDAO(DBConnection.getConn());

		// Use short-lived cookie for flash message instead of in-memory session attribute
		if (doctorDAO.checkOldPassword(doctorId, oldPassword)) {

			if (doctorDAO.changePassword(doctorId, newPassword)) {
				
				JwtUtil.setMessageCookie(resp, "Password change successfully.", "success");
				resp.sendRedirect("doctor/edit_profile.jsp");

			} else {
				
				JwtUtil.setMessageCookie(resp, "Something went wrong on server!", "error");
				resp.sendRedirect("doctor/edit_profile.jsp");

			}

		} else {
			JwtUtil.setMessageCookie(resp, "Old Password not match", "error");
			resp.sendRedirect("doctor/edit_profile.jsp");

		}
	}

}
