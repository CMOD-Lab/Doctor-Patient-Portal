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

@WebServlet("/updatePrescription")
public class UpdatePrescriptionServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			// Check if doctor is logged in
			Doctor doctor = (Doctor) SessionUtil.getAttribute(req, "doctorObj");

			if (doctor == null) {
				SessionUtil.setAttribute(req, "errorMsg", "Please login to update prescriptions");
				resp.sendRedirect("doctor_login.jsp");
				return;
			}

			// Get form parameters
			int id = Integer.parseInt(req.getParameter("id"));
			int appointmentId = Integer.parseInt(req.getParameter("appointmentId"));
			int userId = Integer.parseInt(req.getParameter("userId"));
			String medicineName = req.getParameter("medicineName");
			String dosage = req.getParameter("dosage");
			String duration = req.getParameter("duration");
			String instructions = req.getParameter("instructions");
			String prescriptionDate = req.getParameter("prescriptionDate");

			// Verify doctor owns this prescription
			PrescriptionDAO prescriptionDAO = new PrescriptionDAO(DBConnection.getConn());
			Prescription existingPrescription = prescriptionDAO.getPrescriptionById(id);

			if (existingPrescription != null && existingPrescription.getDoctorId() == doctor.getId()) {
				// Create updated prescription object
				Prescription prescription = new Prescription(id, appointmentId, doctor.getId(), userId, medicineName,
						dosage, duration, instructions, prescriptionDate);

				// Update prescription in database
				boolean result = prescriptionDAO.updatePrescription(prescription);

				if (result) {
					SessionUtil.setAttribute(req, "successMsg", "Prescription updated successfully");
					resp.sendRedirect("viewDoctorPrescriptions");
				} else {
					SessionUtil.setAttribute(req, "errorMsg", "Failed to update prescription");
					resp.sendRedirect("viewDoctorPrescriptions");
				}
			} else {
				SessionUtil.setAttribute(req, "errorMsg", "Unauthorized: You can only update your own prescriptions");
				resp.sendRedirect("viewDoctorPrescriptions");
			}

		} catch (Exception e) {
			e.printStackTrace();
			SessionUtil.setAttribute(req, "errorMsg", "Something went wrong");
			resp.sendRedirect("viewDoctorPrescriptions");
		}
	}
}
