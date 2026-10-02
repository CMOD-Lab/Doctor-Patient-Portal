package com.hms.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Health Check Endpoint - Mandatory containerization requirement.
 *
 * Exposes GET /health returning a JSON status response so that Kubernetes
 * liveness and readiness probes can verify the application is running.
 *
 * Example response:
 *   {"status":"UP","application":"Doctor-Patient-Portal"}
 */
@WebServlet("/health")
public class HealthCheckServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = resp.getWriter();
        out.print("{\"status\":\"UP\",\"application\":\"Doctor-Patient-Portal\"}");
        out.flush();
    }
}
