package com.hms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.hms.entity.Doctor;

/**
 * Data Access Object for Doctor entity.
 * Updated for PostgreSQL 16 compatibility:
 * - Replaced quoted camelCase column names with snake_case column names
 * - PostgreSQL best practice: use snake_case identifiers (no quoting needed)
 * - All JDBC operations use standard java.sql API
 */
public class DoctorDAO {

	private Connection conn;

	public DoctorDAO(Connection conn) {
		super();
		this.conn = conn;
	}

	public boolean registerDoctor(Doctor doctor) {

		boolean f = false;

		try {

			String sql = "insert into doctor(full_name, date_of_birth, qualification, specialist, email, phone, password) values(?,?,?,?,?,?,?)";

			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setString(1, doctor.getFullName());
			pstmt.setString(2, doctor.getDateOfBirth());
			pstmt.setString(3, doctor.getQualification());
			pstmt.setString(4, doctor.getSpecialist());
			pstmt.setString(5, doctor.getEmail());
			pstmt.setString(6, doctor.getPhone());
			pstmt.setString(7, doctor.getPassword());

			pstmt.executeUpdate();
			f = true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return f;
	}

	// getAllDoctors list
	public List<Doctor> getAllDoctor() {

		Doctor doctor = null;
		List<Doctor> docList = new ArrayList<Doctor>();

		try {

			String sql = "select * from doctor order by id desc";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {
				doctor = new Doctor();

				doctor.setId(resultSet.getInt("id"));
				doctor.setFullName(resultSet.getString("full_name"));
				doctor.setDateOfBirth(resultSet.getString("date_of_birth"));
				doctor.setQualification(resultSet.getString("qualification"));
				doctor.setSpecialist(resultSet.getString("specialist"));
				doctor.setEmail(resultSet.getString("email"));
				doctor.setPhone(resultSet.getString("phone"));
				doctor.setPassword(resultSet.getString("password"));
				docList.add(doctor);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return docList;
	}

	// get doctor by id
	public Doctor getDoctorById(int id) {

		Doctor doctor = null;

		try {

			String sql = "select * from doctor where id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setInt(1, id);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {
				doctor = new Doctor();

				doctor.setId(resultSet.getInt("id"));
				doctor.setFullName(resultSet.getString("full_name"));
				doctor.setDateOfBirth(resultSet.getString("date_of_birth"));
				doctor.setQualification(resultSet.getString("qualification"));
				doctor.setSpecialist(resultSet.getString("specialist"));
				doctor.setEmail(resultSet.getString("email"));
				doctor.setPhone(resultSet.getString("phone"));
				doctor.setPassword(resultSet.getString("password"));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return doctor;
	}

	// update doctors by id
	public boolean updateDoctor(Doctor doctor) {

		boolean f = false;

		try {

			String sql = "update doctor set full_name=?, date_of_birth=?, qualification=?, specialist=?, email=?, phone=?, password=? where id=?";

			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setString(1, doctor.getFullName());
			pstmt.setString(2, doctor.getDateOfBirth());
			pstmt.setString(3, doctor.getQualification());
			pstmt.setString(4, doctor.getSpecialist());
			pstmt.setString(5, doctor.getEmail());
			pstmt.setString(6, doctor.getPhone());
			pstmt.setString(7, doctor.getPassword());
			pstmt.setInt(8, doctor.getId());

			pstmt.executeUpdate();
			f = true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return f;
	}

	// delete doctors by id
	public boolean deleteDoctorById(int id) {

		boolean f = false;

		try {

			String sql = "delete from doctor where id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setInt(1, id);

			pstmt.executeUpdate();

			f = true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return f;
	}

	// doctor login
	public Doctor loginDoctor(String email, String password) {

		Doctor doctor = null;

		try {

			String sql = "select * from doctor where email=? and password=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setString(1, email);
			pstmt.setString(2, password);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {
				doctor = new Doctor();

				doctor.setId(resultSet.getInt("id"));
				doctor.setFullName(resultSet.getString("full_name"));
				doctor.setDateOfBirth(resultSet.getString("date_of_birth"));
				doctor.setQualification(resultSet.getString("qualification"));
				doctor.setSpecialist(resultSet.getString("specialist"));
				doctor.setEmail(resultSet.getString("email"));
				doctor.setPhone(resultSet.getString("phone"));
				doctor.setPassword(resultSet.getString("password"));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return doctor;
	}

	// Count total Doctor Number
	public int countTotalDoctor() {

		int i = 0;

		try {

			String sql = "select count(*) from doctor";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			ResultSet resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				i = resultSet.getInt(1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return i;
	}

	// Count total Appointment Number
	public int countTotalAppointment() {

		int i = 0;

		try {

			String sql = "select count(*) from appointment";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			ResultSet resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				i = resultSet.getInt(1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return i;
	}

	// Count total number of Appointment for a specific doctor
	public int countTotalAppointmentByDoctorId(int doctorId) {

		int i = 0;

		try {

			String sql = "select count(*) from appointment where doctor_id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setInt(1, doctorId);

			ResultSet resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				i = resultSet.getInt(1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return i;
	}

	// Count total User Number
	public int countTotalUser() {

		int i = 0;

		try {

			String sql = "select count(*) from user_details";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			ResultSet resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				i = resultSet.getInt(1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return i;
	}

	// Count total Specialist Number
	public int countTotalSpecialist() {

		int i = 0;

		try {

			String sql = "select count(*) from specialist";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			ResultSet resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				i = resultSet.getInt(1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return i;
	}

	// check old password
	public boolean checkOldPassword(int doctorId, String oldPassword) {

		boolean f = false;

		try {

			String sql = "select * from doctor where id=? and password=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setInt(1, doctorId);
			pstmt.setString(2, oldPassword);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {
				f = true;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return f;
	}

	// change password
	public boolean changePassword(int doctorId, String newPassword) {

		boolean f = false;

		try {

			String sql = "update doctor set password=? where id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setString(1, newPassword);
			pstmt.setInt(2, doctorId);

			pstmt.executeUpdate();

			f = true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return f;
	}

	// edit doctor profile in doctor panel edit profile
	public boolean editDoctorProfile(Doctor doctor) {

		boolean f = false;

		try {

			String sql = "update doctor set full_name=?, date_of_birth=?, qualification=?, specialist=?, email=?, phone=? where id=?";

			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setString(1, doctor.getFullName());
			pstmt.setString(2, doctor.getDateOfBirth());
			pstmt.setString(3, doctor.getQualification());
			pstmt.setString(4, doctor.getSpecialist());
			pstmt.setString(5, doctor.getEmail());
			pstmt.setString(6, doctor.getPhone());
			pstmt.setInt(7, doctor.getId());

			pstmt.executeUpdate();
			f = true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return f;
	}
}
