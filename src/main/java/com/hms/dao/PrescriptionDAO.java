package com.hms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.hms.entity.Prescription;

public class PrescriptionDAO {

	private Connection conn;

	public PrescriptionDAO(Connection conn) {
		super();
		this.conn = conn;
	}

	public boolean addPrescription(Prescription prescription) {
		boolean f = false;

		try {
			String sql = "insert into prescription(appointmentId, doctorId, userId, medicineName, dosage, duration, instructions, prescriptionDate) values(?,?,?,?,?,?,?,?)";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setInt(1, prescription.getAppointmentId());
			pstmt.setInt(2, prescription.getDoctorId());
			pstmt.setInt(3, prescription.getUserId());
			pstmt.setString(4, prescription.getMedicineName());
			pstmt.setString(5, prescription.getDosage());
			pstmt.setString(6, prescription.getDuration());
			pstmt.setString(7, prescription.getInstructions());
			pstmt.setString(8, prescription.getPrescriptionDate());

			pstmt.executeUpdate();
			f = true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return f;
	}

	public Prescription getPrescriptionById(int id) {
		Prescription prescription = null;

		try {
			String sql = "select * from prescription where id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setInt(1, id);

			ResultSet resultSet = pstmt.executeQuery();

			if (resultSet.next()) {
				prescription = new Prescription();

				prescription.setId(resultSet.getInt("id"));
				prescription.setAppointmentId(resultSet.getInt("appointmentId"));
				prescription.setDoctorId(resultSet.getInt("doctorId"));
				prescription.setUserId(resultSet.getInt("userId"));
				prescription.setMedicineName(resultSet.getString("medicineName"));
				prescription.setDosage(resultSet.getString("dosage"));
				prescription.setDuration(resultSet.getString("duration"));
				prescription.setInstructions(resultSet.getString("instructions"));
				prescription.setPrescriptionDate(resultSet.getString("prescriptionDate"));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return prescription;
	}

	public List<Prescription> getAllPrescriptionsByDoctorId(int doctorId) {
		List<Prescription> prescriptionList = new ArrayList<Prescription>();
		Prescription prescription = null;

		try {
			String sql = "select * from prescription where doctorId=? order by id desc";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setInt(1, doctorId);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {
				prescription = new Prescription();

				prescription.setId(resultSet.getInt("id"));
				prescription.setAppointmentId(resultSet.getInt("appointmentId"));
				prescription.setDoctorId(resultSet.getInt("doctorId"));
				prescription.setUserId(resultSet.getInt("userId"));
				prescription.setMedicineName(resultSet.getString("medicineName"));
				prescription.setDosage(resultSet.getString("dosage"));
				prescription.setDuration(resultSet.getString("duration"));
				prescription.setInstructions(resultSet.getString("instructions"));
				prescription.setPrescriptionDate(resultSet.getString("prescriptionDate"));

				prescriptionList.add(prescription);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return prescriptionList;
	}

	public List<Prescription> getAllPrescriptionsByUserId(int userId) {
		List<Prescription> prescriptionList = new ArrayList<Prescription>();
		Prescription prescription = null;

		try {
			String sql = "select * from prescription where userId=? order by id desc";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setInt(1, userId);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {
				prescription = new Prescription();

				prescription.setId(resultSet.getInt("id"));
				prescription.setAppointmentId(resultSet.getInt("appointmentId"));
				prescription.setDoctorId(resultSet.getInt("doctorId"));
				prescription.setUserId(resultSet.getInt("userId"));
				prescription.setMedicineName(resultSet.getString("medicineName"));
				prescription.setDosage(resultSet.getString("dosage"));
				prescription.setDuration(resultSet.getString("duration"));
				prescription.setInstructions(resultSet.getString("instructions"));
				prescription.setPrescriptionDate(resultSet.getString("prescriptionDate"));

				prescriptionList.add(prescription);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return prescriptionList;
	}

	public List<Prescription> getAllPrescriptions() {
		List<Prescription> prescriptionList = new ArrayList<Prescription>();
		Prescription prescription = null;

		try {
			String sql = "select * from prescription order by id desc";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {
				prescription = new Prescription();

				prescription.setId(resultSet.getInt("id"));
				prescription.setAppointmentId(resultSet.getInt("appointmentId"));
				prescription.setDoctorId(resultSet.getInt("doctorId"));
				prescription.setUserId(resultSet.getInt("userId"));
				prescription.setMedicineName(resultSet.getString("medicineName"));
				prescription.setDosage(resultSet.getString("dosage"));
				prescription.setDuration(resultSet.getString("duration"));
				prescription.setInstructions(resultSet.getString("instructions"));
				prescription.setPrescriptionDate(resultSet.getString("prescriptionDate"));

				prescriptionList.add(prescription);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return prescriptionList;
	}

	public boolean updatePrescription(Prescription prescription) {
		boolean f = false;

		try {
			String sql = "update prescription set appointmentId=?, doctorId=?, userId=?, medicineName=?, dosage=?, duration=?, instructions=?, prescriptionDate=? where id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setInt(1, prescription.getAppointmentId());
			pstmt.setInt(2, prescription.getDoctorId());
			pstmt.setInt(3, prescription.getUserId());
			pstmt.setString(4, prescription.getMedicineName());
			pstmt.setString(5, prescription.getDosage());
			pstmt.setString(6, prescription.getDuration());
			pstmt.setString(7, prescription.getInstructions());
			pstmt.setString(8, prescription.getPrescriptionDate());
			pstmt.setInt(9, prescription.getId());

			pstmt.executeUpdate();
			f = true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return f;
	}

	public boolean deletePrescriptionById(int id) {
		boolean f = false;

		try {
			String sql = "delete from prescription where id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setInt(1, id);

			pstmt.executeUpdate();
			f = true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return f;
	}
}
