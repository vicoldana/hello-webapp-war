package com.vicoldana.hello;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<h2>✅ Hello from Jenkins + Tomcat in Kubernetes!</h2>");
        out.println("<p>Deployed automatically using your pipeline.</p>");
    }
}
