<%@page import="com.hms.entity.Prescription"%>
<%@page import="com.hms.dao.PrescriptionDAO"%>
<%@page import="com.hms.db.DBConnection"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<!-- for jstl tag -->
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!-- end of jstl tag -->

<%@page isELIgnored="false"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Edit Prescription</title>

<%@include file="../component/allcss.jsp"%>

<!-- customs css for this page -->
<style type="text/css">
.my-card {
	box-shadow: 0px 0px 10px 1px maroon;
}
</style>
<!-- end of customs css for this page -->

</head>
<body>
	<%@include file="navbar.jsp"%>

	<!-- if "doctorObj" is empty means no one is login. -->
	<c:if test="${empty doctorObj }">
		<c:redirect url="../doctor_login.jsp"></c:redirect>
	</c:if>
	<!-- check is doctor is login or not -->

	<%
	int prescriptionId = Integer.parseInt(request.getParameter("id"));
	PrescriptionDAO prescriptionDAO = new PrescriptionDAO(DBConnection.getConn());
	Prescription prescription = prescriptionDAO.getPrescriptionById(prescriptionId);
	%>

	<div class="container p-3">
		<div class="row">
			<div class="col-md-8 offset-md-2">
				<div class="card my-card">
					<div class="card-body">
						<p class="text-center text-success fs-3">Edit Prescription</p>

						<!-- message print -->
						<!-- for success msg -->
						<c:if test="${not empty successMsg }">
							<p class="text-center text-success fs-5">${successMsg}</p>
							<c:remove var="successMsg" scope="session" />
						</c:if>

						<!-- for error msg -->
						<c:if test="${not empty errorMsg }">
							<p class="text-center text-danger fs-5">${errorMsg}</p>
							<c:remove var="errorMsg" scope="session" />
						</c:if>
						<!-- End of message print -->

						<!-- Edit Prescription Form -->
						<form action="../updatePrescription" method="post">

							<input type="hidden" name="id" value="<%=prescription.getId()%>">

							<div class="mb-3">
								<label class="form-label">Appointment ID</label>
								<input type="number" name="appointmentId" class="form-control"
									value="<%=prescription.getAppointmentId()%>" required>
							</div>

							<div class="mb-3">
								<label class="form-label">Patient ID</label>
								<input type="number" name="userId" class="form-control"
									value="<%=prescription.getUserId()%>" required>
							</div>

							<div class="mb-3">
								<label class="form-label">Medicine Name</label>
								<input type="text" name="medicineName" class="form-control"
									value="<%=prescription.getMedicineName()%>" required>
							</div>

							<div class="mb-3">
								<label class="form-label">Dosage</label>
								<input type="text" name="dosage" class="form-control"
									value="<%=prescription.getDosage()%>" required>
							</div>

							<div class="mb-3">
								<label class="form-label">Duration</label>
								<input type="text" name="duration" class="form-control"
									value="<%=prescription.getDuration()%>" required>
							</div>

							<div class="mb-3">
								<label class="form-label">Instructions</label>
								<textarea name="instructions" class="form-control" rows="3"><%=prescription.getInstructions()%></textarea>
							</div>

							<div class="mb-3">
								<label class="form-label">Prescription Date</label>
								<input type="text" name="prescriptionDate" class="form-control"
									value="<%=prescription.getPrescriptionDate()%>" required>
							</div>

							<button type="submit" class="btn btn-success col-md-12">Update Prescription</button>
						</form>
						<!-- End of Edit Prescription Form -->

					</div>
				</div>
			</div>
		</div>
	</div>

</body>
</html>
