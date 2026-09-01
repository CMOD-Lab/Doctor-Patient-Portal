package com.hms.doctor.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hms.dao.PrescriptionDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Doctor;
import com.hms.entity.Prescription;
import com.hms.util.SessionUtil;

@WebServlet("/viewDoctorPrescriptions")
public class ViewDoctorPrescriptionsServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			// Check if doctor is logged in
			Doctor doctor = (Doctor) SessionUtil.getAttribute(req, "doctorObj");

			if (doctor == null) {
				SessionUtil.setAttribute(req, "errorMsg", "Please login to view prescriptions");
				resp.sendRedirect("doctor_login.jsp");
				return;
			}

			// Get all prescriptions for this doctor
			PrescriptionDAO prescriptionDAO = new PrescriptionDAO(DBConnection.getConn());
			List<Prescription> prescriptionList = prescriptionDAO.getAllPrescriptionsByDoctorId(doctor.getId());

			// Set prescription list in request scope
			req.setAttribute("prescriptionList", prescriptionList);

			// Forward to JSP
			req.getRequestDispatcher("doctor/view_prescriptions.jsp").forward(req, resp);

		} catch (Exception e) {
			e.printStackTrace();
			SessionUtil.setAttribute(req, "errorMsg", "Something went wrong");
			resp.sendRedirect("doctor/index.jsp");
		}
	}
}
