package com.hms.user.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.dao.PrescriptionDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Prescription;
import com.hms.entity.User;
import com.hms.util.SessionUtil;

@WebServlet("/viewUserPrescriptions")
public class ViewUserPrescriptionsServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			// Check if user is logged in
			User user = (User) SessionUtil.getAttribute(req, "userObj");

			if (user == null) {
				SessionUtil.setAttribute(req, "errorMsg", "Please login to view prescriptions");
				resp.sendRedirect("user_login.jsp");
				return;
			}

			// Get all prescriptions for this user
			PrescriptionDAO prescriptionDAO = new PrescriptionDAO(DBConnection.getConn());
			List<Prescription> prescriptionList = prescriptionDAO.getAllPrescriptionsByUserId(user.getId());

			// Set prescription list in request scope
			req.setAttribute("prescriptionList", prescriptionList);

			// Forward to JSP
			req.getRequestDispatcher("user/view_prescriptions.jsp").forward(req, resp);

		} catch (Exception e) {
			e.printStackTrace();
			SessionUtil.setAttribute(req, "errorMsg", "Something went wrong");
			resp.sendRedirect("index.jsp");
		}
	}
}
