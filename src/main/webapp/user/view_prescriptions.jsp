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
<title>My Prescriptions</title>

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
	<%@include file="../component/navbar.jsp"%>

	<!-- if "userObj" is empty means no one is login. -->
	<c:if test="${empty userObj }">
		<c:redirect url="../user_login.jsp"></c:redirect>
	</c:if>
	<!-- check is user is login or not -->

	<div class="container p-3">
		<div class="row">
			<div class="col-md-12">
				<div class="card my-card">
					<div class="card-body">
						<p class="text-center text-success fs-3">My Prescriptions</p>

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
									<th scope="col">Medicine Name</th>
									<th scope="col">Dosage</th>
									<th scope="col">Duration</th>
									<th scope="col">Instructions</th>
									<th scope="col">Prescription Date</th>
									<th scope="col">Doctor ID</th>
									<th scope="col">Appointment ID</th>
								</tr>
							</thead>
							<tbody>

								<%
								List<Prescription> prescriptionList = (List<Prescription>) request.getAttribute("prescriptionList");
								if (prescriptionList != null) {
									for (Prescription prescription : prescriptionList) {
								%>

								<tr>
									<td><%=prescription.getMedicineName()%></td>
									<td><%=prescription.getDosage()%></td>
									<td><%=prescription.getDuration()%></td>
									<td><%=prescription.getInstructions()%></td>
									<td><%=prescription.getPrescriptionDate()%></td>
									<td><%=prescription.getDoctorId()%></td>
									<td><%=prescription.getAppointmentId()%></td>
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
