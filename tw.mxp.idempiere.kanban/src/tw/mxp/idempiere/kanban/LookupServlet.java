package tw.mxp.idempiere.kanban;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.compiere.util.DB;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * GET /lookup?table=C_BPartner&search=xxx&limit=20
 * Generic lookup endpoint for ERP entity search.
 */
public class LookupServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// Allowed tables and their display column
	private static final Map<String, String[]> TABLES = new HashMap<>();
	static {
		// table → [idColumn, nameColumn, extraFilter]
		TABLES.put("C_BPartner",  new String[]{"C_BPartner_ID",  "Name", ""});
		TABLES.put("M_Product",   new String[]{"M_Product_ID",   "Name", ""});
		TABLES.put("C_Order",     new String[]{"C_Order_ID",     "DocumentNo", ""});
		TABLES.put("C_Invoice",   new String[]{"C_Invoice_ID",   "DocumentNo", ""});
		TABLES.put("C_Payment",   new String[]{"C_Payment_ID",   "DocumentNo", ""});
		TABLES.put("C_Project",   new String[]{"C_Project_ID",   "Name", ""});
		TABLES.put("C_Campaign",  new String[]{"C_Campaign_ID",  "Name", ""});
		TABLES.put("A_Asset",     new String[]{"A_Asset_ID",     "Name", ""});
		TABLES.put("C_Activity",  new String[]{"C_Activity_ID",  "Name", ""});
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");

		String table = req.getParameter("table");
		String search = req.getParameter("search");
		int limit = 20;
		try { limit = Integer.parseInt(req.getParameter("limit")); } catch (Exception ignored) {}

		String[] meta = TABLES.get(table);
		if (meta == null) {
			resp.setStatus(400);
			resp.getWriter().print("{\"error\":\"Unsupported table\"}");
			return;
		}

		int clientId = AuthContext.getClientId(req);
		String idCol = meta[0], nameCol = meta[1];

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(idCol).append(", ").append(nameCol);
		// For documents, also show description/name for context
		if ("C_Order".equals(table) || "C_Invoice".equals(table) || "C_Payment".equals(table)) {
			sql.append(", COALESCE(Description,'') AS Description");
		}
		sql.append(" FROM ").append(table);
		sql.append(" WHERE AD_Client_ID IN (0, ?) AND IsActive='Y'");

		if (search != null && !search.trim().isEmpty()) {
			sql.append(" AND LOWER(").append(nameCol).append(") LIKE ?");
		}
		sql.append(" ORDER BY ").append(nameCol);
		sql.append(" FETCH FIRST ").append(limit).append(" ROWS ONLY");

		JsonArray results = new JsonArray();
		try (PreparedStatement pstmt = DB.prepareStatement(sql.toString(), null)) {
			int idx = 1;
			pstmt.setInt(idx++, clientId);
			if (search != null && !search.trim().isEmpty()) {
				pstmt.setString(idx++, "%" + search.trim().toLowerCase() + "%");
			}
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					JsonObject row = new JsonObject();
					row.addProperty("id", rs.getInt(idCol));
					String name = rs.getString(nameCol);
					try {
						String desc = rs.getString("Description");
						if (desc != null && !desc.isEmpty()) name = name + " — " + desc;
					} catch (Exception ignored) {}
					row.addProperty("name", name);
					results.add(row);
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
		result.add("results", results);
		resp.getWriter().print(result.toString());
	}
}
