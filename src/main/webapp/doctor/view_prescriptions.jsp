<%@page import="com.hms.entity.Prescription"%>
<%@page import="java.util.List"%>
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
<title>View Prescriptions</title>

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

	<div class="container p-3">
		<div class="row">
			<div class="col-md-12">
				<div class="card my-card">
					<div class="card-body">
						<p class="text-center text-success fs-3">Prescription List</p>

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

						<!-- table for prescription list -->
						<table class="table table-striped">
							<thead>
								<tr>
									<th scope="col">ID</th>
									<th scope="col">Appointment ID</th>
									<th scope="col">Patient ID</th>
									<th scope="col">Medicine</th>
									<th scope="col">Dosage</th>
									<th scope="col">Duration</th>
									<th scope="col">Instructions</th>
									<th scope="col">Date</th>
									<th scope="col">Actions</th>
								</tr>
							</thead>
							<tbody>

								<%
								List<Prescription> prescriptionList = (List<Prescription>) request.getAttribute("prescriptionList");
								if (prescriptionList != null) {
									for (Prescription prescription : prescriptionList) {
								%>

								<tr>
									<td><%=prescription.getId()%></td>
									<td><%=prescription.getAppointmentId()%></td>
									<td><%=prescription.getUserId()%></td>
									<td><%=prescription.getMedicineName()%></td>
									<td><%=prescription.getDosage()%></td>
									<td><%=prescription.getDuration()%></td>
									<td><%=prescription.getInstructions()%></td>
									<td><%=prescription.getPrescriptionDate()%></td>
									<td>
										<a href="edit_prescription.jsp?id=<%=prescription.getId()%>" class="btn btn-sm btn-primary">Edit</a>
										<form action="../deletePrescription" method="post" style="display:inline;">
											<input type="hidden" name="id" value="<%=prescription.getId()%>">
											<button type="submit" class="btn btn-sm btn-danger" onclick="return confirm('Are you sure you want to delete this prescription?');">Delete</button>
										</form>
									</td>
								</tr>

								<%
									}
								}
								%>

							</tbody>
						</table>
						<!-- end of table for prescription list -->

					</div>
				</div>
			</div>
		</div>
	</div>

</body>
</html>
