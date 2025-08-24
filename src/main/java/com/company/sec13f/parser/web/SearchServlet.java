package com.company.sec13f.parser.web;

import com.company.sec13f.parser.database.FilingDAO;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SearchServlet extends HttpServlet {
    private FilingDAO filingDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        super.init();
        this.filingDAO = new FilingDAO();
        this.gson = new Gson();
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        try {
            String cik = req.getParameter("cik");
            if (cik != null && !cik.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("cik", cik);
                result.put("holdings", filingDAO.getAllHoldingsByCik(cik));
                resp.getWriter().write(gson.toJson(result));
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"CIK parameter is required\"}");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}