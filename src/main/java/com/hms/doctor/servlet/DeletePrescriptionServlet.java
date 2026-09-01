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

@WebServlet("/deletePrescription")
public class DeletePrescriptionServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			// Check if doctor is logged in
			Doctor doctor = (Doctor) SessionUtil.getAttribute(req, "doctorObj");

			if (doctor == null) {
				SessionUtil.setAttribute(req, "errorMsg", "Please login to delete prescriptions");
				resp.sendRedirect("doctor_login.jsp");
				return;
			}

			// Get prescription ID
			int id = Integer.parseInt(req.getParameter("id"));

			// Verify doctor owns this prescription
			PrescriptionDAO prescriptionDAO = new PrescriptionDAO(DBConnection.getConn());
			Prescription existingPrescription = prescriptionDAO.getPrescriptionById(id);

			if (existingPrescription != null && existingPrescription.getDoctorId() == doctor.getId()) {
				// Delete prescription
				boolean result = prescriptionDAO.deletePrescriptionById(id);

				if (result) {
					SessionUtil.setAttribute(req, "successMsg", "Prescription deleted successfully");
					resp.sendRedirect("viewDoctorPrescriptions");
				} else {
					SessionUtil.setAttribute(req, "errorMsg", "Failed to delete prescription");
					resp.sendRedirect("viewDoctorPrescriptions");
				}
			} else {
				SessionUtil.setAttribute(req, "errorMsg", "Unauthorized: You can only delete your own prescriptions");
				resp.sendRedirect("viewDoctorPrescriptions");
			}

		} catch (Exception e) {
			e.printStackTrace();
			SessionUtil.setAttribute(req, "errorMsg", "Something went wrong");
			resp.sendRedirect("viewDoctorPrescriptions");
		}
	}
}
