package tw.mxp.idempiere.kanban;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.compiere.util.DB;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * GET /gantt?scope=All&requestTypeId=N — cards with StartDate/EndTime for Gantt view.
 */
public class GanttServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");

		int clientId = AuthContext.getClientId(req);
		int userId = AuthContext.getUserId(req);
		String scope = req.getParameter("scope");
		if (scope == null) scope = "All";
		String requestTypeIdParam = req.getParameter("requestTypeId");

		StringBuilder sql = new StringBuilder();
		List<Object> params = new ArrayList<>();

		sql.append("SELECT r.R_Request_ID, r.DocumentNo, r.Summary, r.StartDate, r.EndTime, ");
		sql.append("r.Priority, r.SalesRep_ID, r.R_Status_ID, ");
		sql.append("COALESCE(u.Name,'') AS SalesRepName, s.Name AS StatusName, s.IsClosed ");
		sql.append("FROM R_Request r ");
		sql.append("JOIN R_Status s ON r.R_Status_ID=s.R_Status_ID ");
		sql.append("LEFT JOIN AD_User u ON r.SalesRep_ID=u.AD_User_ID ");
		sql.append("WHERE r.AD_Client_ID=? AND r.StartDate IS NOT NULL ");
		params.add(clientId);

		if ("Private".equals(scope)) {
			sql.append("AND r.SalesRep_ID=? ");
			params.add(userId);
		} else if ("Subordinates".equals(scope)) {
			sql.append("AND (r.SalesRep_ID=? OR r.SalesRep_ID IN (");
			sql.append("  WITH RECURSIVE subordinates AS (");
			sql.append("    SELECT AD_User_ID FROM AD_User WHERE Supervisor_ID=? AND IsActive='Y'");
			sql.append("    UNION ALL");
			sql.append("    SELECT u2.AD_User_ID FROM AD_User u2 JOIN subordinates sub ON u2.Supervisor_ID=sub.AD_User_ID WHERE u2.IsActive='Y'");
			sql.append("  ) SELECT AD_User_ID FROM subordinates");
			sql.append(")) ");
			params.add(userId);
			params.add(userId);
		}

		if (requestTypeIdParam != null && !requestTypeIdParam.isEmpty()) {
			try {
				sql.append("AND r.R_RequestType_ID=? ");
				params.add(Integer.parseInt(requestTypeIdParam));
			} catch (NumberFormatException ignored) {}
		}

		sql.append("ORDER BY r.StartDate, r.EndTime NULLS LAST");

		JsonArray tasks = new JsonArray();
		try (PreparedStatement pstmt = DB.prepareStatement(sql.toString(), null)) {
			for (int i = 0; i < params.size(); i++) {
				Object p = params.get(i);
				if (p instanceof Integer) pstmt.setInt(i + 1, (Integer) p);
				else pstmt.setString(i + 1, (String) p);
			}
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					JsonObject t = new JsonObject();
					t.addProperty("id", rs.getInt("R_Request_ID"));
					t.addProperty("documentNo", rs.getString("DocumentNo"));
					t.addProperty("summary", rs.getString("Summary") != null ? rs.getString("Summary") : "");
					Timestamp start = rs.getTimestamp("StartDate");
					Timestamp end = rs.getTimestamp("EndTime");
					if (start != null) t.addProperty("startDate", start.getTime());
					if (end != null) t.addProperty("endDate", end.getTime());
					t.addProperty("priority", rs.getString("Priority") != null ? rs.getString("Priority") : "5");
					t.addProperty("salesRepName", rs.getString("SalesRepName"));
					t.addProperty("statusName", rs.getString("StatusName"));
					t.addProperty("isClosed", "Y".equals(rs.getString("IsClosed")));
					tasks.add(t);
				}
			}
		} catch (Exception e) {
			resp.setStatus(500);
			String msg = e.getMessage();
			if (msg != null) msg = msg.replace("\"", "'").replace("\n", " ");
			resp.getWriter().print("{\"error\":\"" + msg + "\"}");
			return;
		}

		JsonObject result = new JsonObject();
		result.add("tasks", tasks);
		resp.getWriter().print(result.toString());
	}
}
