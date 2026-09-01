package com.hms.entity;

import java.io.Serializable;

/**
 * Prescription entity representing a medical prescription in the Hospital Management System.
 * Implements Serializable to support Redis session storage for cloud-native distributed sessions.
 */
public class Prescription implements Serializable {

	/**
	 * Serial version UID for Redis session serialization compatibility
	 */
	private static final long serialVersionUID = 1L;

	private int id;
	private int appointmentId;
	private int doctorId;
	private int userId;
	private String medicineName;
	private String dosage;
	private String duration;
	private String instructions;
	private String prescriptionDate;

	public Prescription() {
		super();
	}

	public Prescription(int appointmentId, int doctorId, int userId, String medicineName, String dosage,
			String duration, String instructions, String prescriptionDate) {
		super();
		this.appointmentId = appointmentId;
		this.doctorId = doctorId;
		this.userId = userId;
		this.medicineName = medicineName;
		this.dosage = dosage;
		this.duration = duration;
		this.instructions = instructions;
		this.prescriptionDate = prescriptionDate;
	}

	public Prescription(int id, int appointmentId, int doctorId, int userId, String medicineName, String dosage,
			String duration, String instructions, String prescriptionDate) {
		super();
		this.id = id;
		this.appointmentId = appointmentId;
		this.doctorId = doctorId;
		this.userId = userId;
		this.medicineName = medicineName;
		this.dosage = dosage;
		this.duration = duration;
		this.instructions = instructions;
		this.prescriptionDate = prescriptionDate;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getAppointmentId() {
		return appointmentId;
	}

	public void setAppointmentId(int appointmentId) {
		this.appointmentId = appointmentId;
	}

	public int getDoctorId() {
		return doctorId;
	}

	public void setDoctorId(int doctorId) {
		this.doctorId = doctorId;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getMedicineName() {
		return medicineName;
	}

	public void setMedicineName(String medicineName) {
		this.medicineName = medicineName;
	}

	public String getDosage() {
		return dosage;
	}

	public void setDosage(String dosage) {
		this.dosage = dosage;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getInstructions() {
		return instructions;
	}

	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	public String getPrescriptionDate() {
		return prescriptionDate;
	}

	public void setPrescriptionDate(String prescriptionDate) {
		this.prescriptionDate = prescriptionDate;
	}
}
