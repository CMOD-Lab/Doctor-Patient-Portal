package com.hms.entity;

import java.io.Serializable;

/**
 * Specialist entity representing a medical specialist in the Hospital Management System.
 * Implements Serializable to support Redis session storage for cloud-native distributed sessions.
 */
public class Specialist implements Serializable {
	
	/**
	 * Serial version UID for Redis session serialization compatibility
	 */
	private static final long serialVersionUID = 1L;
	
	private int id;
	private String specialistName;
	
	public Specialist() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Specialist(int id, String specialistName) {
		super();
		this.id = id;
		this.specialistName = specialistName;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getSpecialistName() {
		return specialistName;
	}

	public void setSpecialistName(String specialistName) {
		this.specialistName = specialistName;
	}
	
	
	
	
	

}
