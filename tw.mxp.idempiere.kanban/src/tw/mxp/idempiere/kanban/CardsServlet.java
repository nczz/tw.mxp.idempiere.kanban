package tw.mxp.idempiere.kanban;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.adempiere.base.event.EventManager;
import org.compiere.model.MRequest;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.TrxRunnable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Handles all /cards/* routes:
 *   GET  /cards           — list cards by scope
 *   POST /cards/{id}/move — move card to new status
 */
public class CardsServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		String pathInfo = req.getPathInfo();

		if (pathInfo == null || pathInfo.equals("/")) {
			listCards(req, resp);
		} else {
			// Phase 2: GET /cards/{id} for single card detail
			resp.setStatus(404);
			resp.getWriter().print("{\"error\":\"Not implemented\"}");
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		String pathInfo = req.getPathInfo();

		if (pathInfo != null && pathInfo.endsWith("/move")) {
			moveCard(req, resp, pathInfo);
		} else {
			// Phase 2: POST /cards for new request
			resp.setStatus(404);
			resp.getWriter().print("{\"error\":\"Not implemented\"}");
		}
	}

	/**
	 * GET /cards?scope=Private|Subordinates|Team|All&requestTypeId=N&closed=false
	 */
	private void listCards(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		int clientId = AuthContext.getClientId(req);
		int orgId = AuthContext.getOrgId(req);
		int userId = AuthContext.getUserId(req);
		String scope = req.getParameter("scope");
		if (scope == null) scope = "Private";
		String requestTypeIdParam = req.getParameter("requestTypeId");
		boolean closed = "true".equals(req.getParameter("closed"));

		// Build SQL
		StringBuilder sql = new StringBuilder();
		List<Object> params = new ArrayList<>();

		sql.append("SELECT r.R_Request_ID, r.DocumentNo, r.Summary, r.R_Status_ID, ");
		sql.append("r.Priority, r.DueType, r.DateNextAction, r.SalesRep_ID, ");
		sql.append("r.R_RequestType_ID, r.C_BPartner_ID, ");
		sql.append("s.Name AS StatusName, s.SeqNo AS StatusSeqNo, ");
		sql.append("COALESCE(bp.Name, '') AS BPartnerName, ");
		sql.append("COALESCE(rt.Name, '') AS RequestTypeName, ");
		sql.append("COALESCE(u.Name, '') AS SalesRepName ");
		sql.append("FROM R_Request r ");
		sql.append("JOIN R_Status s ON r.R_Status_ID = s.R_Status_ID ");
		sql.append("LEFT JOIN C_BPartner bp ON r.C_BPartner_ID = bp.C_BPartner_ID ");
		sql.append("LEFT JOIN R_RequestType rt ON r.R_RequestType_ID = rt.R_RequestType_ID ");
		sql.append("LEFT JOIN AD_User u ON r.SalesRep_ID = u.AD_User_ID ");
		sql.append("WHERE r.AD_Client_ID = ? ");
		params.add(clientId);

		// Org filter
		if (orgId > 0) {
			sql.append("AND (r.AD_Org_ID = 0 OR r.AD_Org_ID = ?) ");
			params.add(orgId);
		}

		// Closed filter
		sql.append("AND s.IsClosed = ? ");
		params.add(closed ? "Y" : "N");

		// Request type filter
		if (requestTypeIdParam != null && !requestTypeIdParam.isEmpty()) {
			try {
				int rtId = Integer.parseInt(requestTypeIdParam);
				sql.append("AND r.R_RequestType_ID = ? ");
				params.add(rtId);
			} catch (NumberFormatException ignored) {}
		}

		// Scope filter
		switch (scope) {
			case "Private":
				sql.append("AND r.SalesRep_ID = ? ");
				params.add(userId);
				break;
			case "Subordinates":
				sql.append("AND (r.SalesRep_ID = ? OR r.SalesRep_ID IN (");
				sql.append("  WITH RECURSIVE subordinates AS (");
				sql.append("    SELECT AD_User_ID FROM AD_User WHERE Supervisor_ID = ? AND IsActive='Y'");
				sql.append("    UNION ALL");
				sql.append("    SELECT u.AD_User_ID FROM AD_User u JOIN subordinates s ON u.Supervisor_ID = s.AD_User_ID WHERE u.IsActive='Y'");
				sql.append("  ) SELECT AD_User_ID FROM subordinates");
				sql.append(")) ");
				params.add(userId);
				params.add(userId);
				break;
			case "All":
				// No SalesRep filter — client + org isolation only
				break;
			default: // Team or unknown → default to Private
				sql.append("AND r.SalesRep_ID = ? ");
				params.add(userId);
				break;
		}

		sql.append("ORDER BY s.SeqNo, r.Priority, r.DateNextAction NULLS LAST");

		JsonArray cards = new JsonArray();
		try (PreparedStatement pstmt = DB.prepareStatement(sql.toString(), null)) {
			for (int i = 0; i < params.size(); i++) {
				Object p = params.get(i);
				if (p instanceof Integer) pstmt.setInt(i + 1, (Integer) p);
				else pstmt.setString(i + 1, (String) p);
			}
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					JsonObject card = new JsonObject();
					card.addProperty("id", rs.getInt("R_Request_ID"));
					card.addProperty("documentNo", rs.getString("DocumentNo"));
					card.addProperty("summary", nvl(rs.getString("Summary")));
					card.addProperty("statusId", rs.getInt("R_Status_ID"));
					card.addProperty("statusName", rs.getString("StatusName"));
					card.addProperty("priority", nvl(rs.getString("Priority")));
					card.addProperty("dueType", nvl(rs.getString("DueType")));
					Timestamp dna = rs.getTimestamp("DateNextAction");
					if (dna != null) {
						card.addProperty("dateNextAction", dna.getTime());
					}
					card.addProperty("salesRepId", rs.getInt("SalesRep_ID"));
					card.addProperty("salesRepName", rs.getString("SalesRepName"));
					card.addProperty("bpartnerName", rs.getString("BPartnerName"));
					card.addProperty("requestTypeName", rs.getString("RequestTypeName"));
					cards.add(card);
				}
			}
		} catch (Exception e) {
			sendError(resp, 500, e);
			return;
		}

		JsonObject result = new JsonObject();
		result.add("cards", cards);
		resp.getWriter().print(result.toString());
	}

	/**
	 * POST /cards/{id}/move  body: {"targetStatusId": N}
	 */
	private void moveCard(HttpServletRequest req, HttpServletResponse resp, String pathInfo) throws IOException {
		// Parse card ID from path: /123/move
		int cardId;
		try {
			String idStr = pathInfo.substring(1, pathInfo.indexOf("/move"));
			cardId = Integer.parseInt(idStr);
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().print("{\"error\":\"Invalid card ID\"}");
			return;
		}

		// Parse target status from body
		StringBuilder body = new StringBuilder();
		req.getReader().lines().forEach(body::append);
		int targetStatusId = extractInt(body.toString(), "targetStatusId");
		if (targetStatusId <= 0) {
			resp.setStatus(400);
			resp.getWriter().print("{\"error\":\"Missing targetStatusId\"}");
			return;
		}

		int clientId = AuthContext.getClientId(req);
		int userId = AuthContext.getUserId(req);

		// Verify card belongs to this client
		int cardClientId = DB.getSQLValueEx(null,
				"SELECT AD_Client_ID FROM R_Request WHERE R_Request_ID=?", cardId);
		if (cardClientId != clientId) {
			resp.setStatus(403);
			resp.getWriter().print("{\"error\":\"Access denied\"}");
			return;
		}

		// Execute move in a single transaction
		final int[] oldStatusId = {0};
		try {
			Properties ctx = Env.getCtx();
			Env.setContext(ctx, "#AD_Client_ID", clientId);
			Env.setContext(ctx, "#AD_Org_ID", AuthContext.getOrgId(req));
			Env.setContext(ctx, "#AD_User_ID", userId);

			Trx.run(new TrxRunnable() {
				@Override
				public void run(String trxName) {
					// Update R_Request status
					MRequest request = new MRequest(Env.getCtx(), cardId, trxName);
					oldStatusId[0] = request.getR_Status_ID();
					request.setR_Status_ID(targetStatusId);
					request.saveEx(trxName);

					// Write move log
					int logId = DB.getNextID(clientId, "RK_Card_Move_Log", trxName);
					String logSql = "INSERT INTO RK_Card_Move_Log "
							+ "(RK_Card_Move_Log_ID, AD_Client_ID, AD_Org_ID, IsActive, "
							+ "Created, CreatedBy, Updated, UpdatedBy, "
							+ "R_Request_ID, R_Status_ID_From, R_Status_ID_To) "
							+ "VALUES (?, ?, ?, 'Y', "
							+ "statement_timestamp(), ?, statement_timestamp(), ?, "
							+ "?, ?, ?)";
					DB.executeUpdateEx(logSql, new Object[]{
							logId, clientId, AuthContext.getOrgId(req),
							userId, userId,
							cardId, oldStatusId[0], targetStatusId
					}, trxName);
				}
			});
		} catch (Exception e) {
			sendError(resp, 500, e);
			return;
		}

		JsonObject result = new JsonObject();
		result.addProperty("success", true);
		result.addProperty("cardId", cardId);
		result.addProperty("fromStatusId", oldStatusId[0]);
		result.addProperty("toStatusId", targetStatusId);

		// Publish refresh event for real-time push to other desktops
		try {
			Map<String, Object> eventData = new HashMap<>();
			eventData.put("AD_Client_ID", clientId);
			eventData.put("R_Request_ID", cardId);
			org.osgi.service.event.Event osgiEvent = EventManager.newEvent("kanban/refresh", eventData);
			EventManager.getInstance().sendEvent(osgiEvent);
		} catch (Exception ignored) {
			// Push failure should not fail the move response
		}

		resp.getWriter().print(result.toString());
	}

	private void sendError(HttpServletResponse resp, int status, Exception e) throws IOException {
		resp.setStatus(status);
		String msg = e.getMessage();
		if (msg != null) msg = msg.replace("\"", "'").replace("\n", " ").replace("\r", "");
		resp.getWriter().print("{\"error\":\"" + msg + "\"}");
	}

	private static String nvl(String s) {
		return s != null ? s : "";
	}

	private static int extractInt(String json, String key) {
		if (json == null) return -1;
		String search = "\"" + key + "\":";
		int idx = json.indexOf(search);
		if (idx < 0) return -1;
		idx += search.length();
		StringBuilder sb = new StringBuilder();
		for (int i = idx; i < json.length(); i++) {
			char c = json.charAt(i);
			if (Character.isDigit(c)) sb.append(c);
			else if (sb.length() > 0) break;
		}
		return sb.length() > 0 ? Integer.parseInt(sb.toString()) : -1;
	}
}
