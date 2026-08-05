package com.hms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.hms.entity.Appointment;

/**
 * Data Access Object for Appointment entity.
 * Updated for PostgreSQL 16 compatibility:
 * - Replaced quoted camelCase column names with snake_case column names
 * - PostgreSQL best practice: use snake_case identifiers (no quoting needed)
 * - All JDBC operations use standard java.sql API
 */
public class AppointmentDAO {

	private Connection conn;

	public AppointmentDAO(Connection conn) {
		super();
		this.conn = conn;
	}

	// for create appointment
	public boolean addAppointment(Appointment appointment) {

		boolean f = false;

		try {

			String sql = "insert into appointment(user_id, full_name, gender, age, appointment_date, email, phone, diseases, doctor_id, address, status) values(?,?,?,?,?,?,?,?,?,?,?)";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setInt(1, appointment.getUserId());
			pstmt.setString(2, appointment.getFullName());
			pstmt.setString(3, appointment.getGender());
			pstmt.setString(4, appointment.getAge());
			pstmt.setString(5, appointment.getAppointmentDate());
			pstmt.setString(6, appointment.getEmail());
			pstmt.setString(7, appointment.getPhone());
			pstmt.setString(8, appointment.getDiseases());
			pstmt.setInt(9, appointment.getDoctorId());
			pstmt.setString(10, appointment.getAddress());
			pstmt.setString(11, appointment.getStatus());

			pstmt.executeUpdate();

			f = true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return f;
	}

	// get list of appointment for logged in specific user
	// show appointment list for specific user panel
	public List<Appointment> getAllAppointmentByLoginUser(int userId) {
		List<Appointment> appList = new ArrayList<Appointment>();

		Appointment appointment = null;

		try {

			String sql = "select * from appointment where user_id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setInt(1, userId);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {

				appointment = new Appointment();

				appointment.setId(resultSet.getInt("id"));
				appointment.setUserId(resultSet.getInt("user_id"));
				appointment.setFullName(resultSet.getString("full_name"));
				appointment.setGender(resultSet.getString("gender"));
				appointment.setAge(resultSet.getString("age"));
				appointment.setAppointmentDate(resultSet.getString("appointment_date"));
				appointment.setEmail(resultSet.getString("email"));
				appointment.setPhone(resultSet.getString("phone"));
				appointment.setDiseases(resultSet.getString("diseases"));
				appointment.setDoctorId(resultSet.getInt("doctor_id"));
				appointment.setAddress(resultSet.getString("address"));
				appointment.setStatus(resultSet.getString("status"));
				appList.add(appointment);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return appList;

	}

	// get appointment list of patient for specific doctor
	// show list of appointment in specific doctor panel
	public List<Appointment> getAllAppointmentByLoginDoctor(int doctorId) {
		List<Appointment> appList = new ArrayList<Appointment>();

		Appointment appointment = null;

		try {

			String sql = "select * from appointment where doctor_id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setInt(1, doctorId);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {

				appointment = new Appointment();

				appointment.setId(resultSet.getInt("id"));
				appointment.setUserId(resultSet.getInt("user_id"));
				appointment.setFullName(resultSet.getString("full_name"));
				appointment.setGender(resultSet.getString("gender"));
				appointment.setAge(resultSet.getString("age"));
				appointment.setAppointmentDate(resultSet.getString("appointment_date"));
				appointment.setEmail(resultSet.getString("email"));
				appointment.setPhone(resultSet.getString("phone"));
				appointment.setDiseases(resultSet.getString("diseases"));
				appointment.setDoctorId(resultSet.getInt("doctor_id"));
				appointment.setAddress(resultSet.getString("address"));
				appointment.setStatus(resultSet.getString("status"));
				appList.add(appointment);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return appList;

	}

	// for doctor comment need specific appointment id
	public Appointment getAppointmentById(int id) {

		Appointment appointment = null;

		try {

			String sql = "select * from appointment where id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setInt(1, id);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {

				appointment = new Appointment();

				appointment.setId(resultSet.getInt("id"));
				appointment.setUserId(resultSet.getInt("user_id"));
				appointment.setFullName(resultSet.getString("full_name"));
				appointment.setGender(resultSet.getString("gender"));
				appointment.setAge(resultSet.getString("age"));
				appointment.setAppointmentDate(resultSet.getString("appointment_date"));
				appointment.setEmail(resultSet.getString("email"));
				appointment.setPhone(resultSet.getString("phone"));
				appointment.setDiseases(resultSet.getString("diseases"));
				appointment.setDoctorId(resultSet.getInt("doctor_id"));
				appointment.setAddress(resultSet.getString("address"));
				appointment.setStatus(resultSet.getString("status"));

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return appointment;

	}

	// for update comment status
	public boolean updateDrAppointmentCommentStatus(int apptId, int docId, String comment) {

		boolean f = false;

		try {

			String sql = "update appointment set status=? where id=? and doctor_id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setString(1, comment);
			pstmt.setInt(2, apptId);
			pstmt.setInt(3, docId);

			pstmt.executeUpdate();

			f = true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return f;
	}

	// get all appointment in admin panel
	public List<Appointment> getAllAppointment() {
		List<Appointment> appList = new ArrayList<Appointment>();
		Appointment appointment = null;

		try {

			String sql = "select * from appointment order by id desc";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {

				appointment = new Appointment();

				appointment.setId(resultSet.getInt("id"));
				appointment.setUserId(resultSet.getInt("user_id"));
				appointment.setFullName(resultSet.getString("full_name"));
				appointment.setGender(resultSet.getString("gender"));
				appointment.setAge(resultSet.getString("age"));
				appointment.setAppointmentDate(resultSet.getString("appointment_date"));
				appointment.setEmail(resultSet.getString("email"));
				appointment.setPhone(resultSet.getString("phone"));
				appointment.setDiseases(resultSet.getString("diseases"));
				appointment.setDoctorId(resultSet.getInt("doctor_id"));
				appointment.setAddress(resultSet.getString("address"));
				appointment.setStatus(resultSet.getString("status"));
				appList.add(appointment);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return appList;
	}

}
