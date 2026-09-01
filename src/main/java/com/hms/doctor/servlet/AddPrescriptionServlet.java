package com.hms.doctor.servlet;

import java.io.IOException;

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

@WebServlet("/addPrescription")
public class AddPrescriptionServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			// Check if doctor is logged in
			Doctor doctor = (Doctor) SessionUtil.getAttribute(req, "doctorObj");

			if (doctor == null) {
				SessionUtil.setAttribute(req, "errorMsg", "Please login to add prescriptions");
				resp.sendRedirect("doctor_login.jsp");
				return;
			}

			// Get form parameters
			int appointmentId = Integer.parseInt(req.getParameter("appointmentId"));
			int userId = Integer.parseInt(req.getParameter("userId"));
			String medicineName = req.getParameter("medicineName");
			String dosage = req.getParameter("dosage");
			String duration = req.getParameter("duration");
			String instructions = req.getParameter("instructions");
			String prescriptionDate = req.getParameter("prescriptionDate");

			// Create prescription object with doctorId from session
			Prescription prescription = new Prescription(appointmentId, doctor.getId(), userId, medicineName, dosage,
					duration, instructions, prescriptionDate);

			// Add prescription to database
			PrescriptionDAO prescriptionDAO = new PrescriptionDAO(DBConnection.getConn());
			boolean result = prescriptionDAO.addPrescription(prescription);

			if (result) {
				SessionUtil.setAttribute(req, "successMsg", "Prescription added successfully");
				resp.sendRedirect("doctor/add_prescription.jsp");
			} else {
				SessionUtil.setAttribute(req, "errorMsg", "Failed to add prescription");
				resp.sendRedirect("doctor/add_prescription.jsp");
			}

		} catch (Exception e) {
			e.printStackTrace();
			SessionUtil.setAttribute(req, "errorMsg", "Something went wrong");
			resp.sendRedirect("doctor/add_prescription.jsp");
		}
	}
}
