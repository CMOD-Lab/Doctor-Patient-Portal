package com.hms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.hms.entity.User;

/**
 * Data Access Object for User entity.
 * Updated for PostgreSQL 16 compatibility:
 * - Uses snake_case column names (full_name) for PostgreSQL best practice
 * - PostgreSQL treats unquoted identifiers as lowercase; snake_case columns work without quoting
 * - All JDBC operations use standard java.sql API
 */
public class UserDAO {

	private Connection conn;

	public UserDAO(Connection conn) {
		super();
		this.conn = conn;
	}

	public boolean userRegister(User user) {

		boolean f = false;

		try {
			// insert user in db
			String sql = "insert into user_details(full_name, email, password) values(?,?,?)";

			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, user.getFullName());
			pstmt.setString(2, user.getEmail());
			pstmt.setString(3, user.getPassword());

			pstmt.executeUpdate();

			f = true; // if query execute successfully then f becomes true otherwise false...

		} catch (Exception e) {
			e.printStackTrace();

		}

		return f;
	}

	// when call loginUser() method, it checks that particular user available or
	// not?
	// if not available then return null user object.
	// and if particular user available then, create User object(i.e user) and fetch
	// all the data of that user from db
	// and return that specific users object.
	public User loginUser(String email, String password) {

		User user = null;

		try {
			String sql = "select * from user_details where email=? and password=?";

			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setString(1, email);
			pstmt.setString(2, password);

			ResultSet resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				// if any row available, then fetch the data of that row...

				// create new user object
				user = new User();

				// fetch data one by one from db table and set it/bind it to user's objects.
				user.setId(resultSet.getInt("id"));
				user.setFullName(resultSet.getString("full_name"));
				user.setEmail(resultSet.getString("email"));
				user.setPassword(resultSet.getString("password"));

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return user;

	}

	// check old password
	public boolean checkOldPassword(int userId, String oldPassword) {

		boolean f = false;

		try {

			String sql = "select * from user_details where id=? and password=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setInt(1, userId);
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
	public boolean changePassword(int userId, String newPassword) {

		boolean f = false;

		try {

			String sql = "update user_details set password=? where id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setString(1, newPassword);
			pstmt.setInt(2, userId);

			pstmt.executeUpdate();

			f = true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return f;
	}

}
