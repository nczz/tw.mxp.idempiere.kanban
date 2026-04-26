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
			// GET /cards/{id} — single card detail
			getCardDetail(req, resp, pathInfo);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		String pathInfo = req.getPathInfo();

		if (pathInfo != null && pathInfo.endsWith("/move")) {
			moveCard(req, resp, pathInfo);
		} else if (pathInfo == null || pathInfo.equals("/")) {
			createCard(req, resp);
		} else {
			resp.setStatus(404);
			resp.getWriter().print("{\"error\":\"Not found\"}");
		}
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		updateCard(req, resp);
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
		String search = req.getParameter("search");

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

		// Search filter
		if (search != null && !search.trim().isEmpty()) {
			sql.append("AND (LOWER(r.Summary) LIKE ? OR LOWER(r.DocumentNo) LIKE ? OR LOWER(COALESCE(bp.Name,'')) LIKE ?) ");
			String like = "%" + search.trim().toLowerCase() + "%";
			params.add(like);
			params.add(like);
			params.add(like);
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

	// ============================================================
	// GET /cards/{id} — single card detail with move history
	// ============================================================
	private void getCardDetail(HttpServletRequest req, HttpServletResponse resp, String pathInfo) throws IOException {
		int cardId = parsePathId(pathInfo);
		if (cardId <= 0) { resp.setStatus(400); resp.getWriter().print("{\"error\":\"Invalid ID\"}"); return; }

		int clientId = AuthContext.getClientId(req);
		String sql = "SELECT r.R_Request_ID, r.DocumentNo, r.Summary, r.Result, "
			+ "r.R_Status_ID, r.R_RequestType_ID, r.R_Category_ID, "
			+ "r.Priority, r.PriorityUser, r.DueType, r.DateNextAction, r.StartDate, r.EndTime, r.CloseDate, "
			+ "r.SalesRep_ID, r.AD_User_ID, r.CreatedBy, r.Created, "
			+ "r.C_BPartner_ID, r.M_Product_ID, r.C_Order_ID, r.C_Invoice_ID, "
			+ "r.C_Payment_ID, r.C_Project_ID, r.C_Campaign_ID, r.A_Asset_ID, "
			+ "r.IsEscalated, "
			+ "COALESCE(bp.Name,'') AS BPartnerName, "
			+ "COALESCE(pd.Name,'') AS ProductName, "
			+ "COALESCE(ord.DocumentNo,'') AS OrderNo, "
			+ "COALESCE(inv.DocumentNo,'') AS InvoiceNo, "
			+ "COALESCE(pay.DocumentNo,'') AS PaymentNo, "
			+ "COALESCE(pj.Name,'') AS ProjectName, "
			+ "COALESCE(cp.Name,'') AS CampaignName, "
			+ "COALESCE(ast.Name,'') AS AssetName, "
			+ "COALESCE(rt.Name,'') AS RequestTypeName, "
			+ "COALESCE(sr.Name,'') AS SalesRepName, "
			+ "COALESCE(req.Name,'') AS RequesterName, "
			+ "COALESCE(cr.Name,'') AS CreatorName, "
			+ "s.Name AS StatusName "
			+ "FROM R_Request r "
			+ "JOIN R_Status s ON r.R_Status_ID=s.R_Status_ID "
			+ "LEFT JOIN C_BPartner bp ON r.C_BPartner_ID=bp.C_BPartner_ID "
			+ "LEFT JOIN M_Product pd ON r.M_Product_ID=pd.M_Product_ID "
			+ "LEFT JOIN C_Order ord ON r.C_Order_ID=ord.C_Order_ID "
			+ "LEFT JOIN C_Invoice inv ON r.C_Invoice_ID=inv.C_Invoice_ID "
			+ "LEFT JOIN C_Payment pay ON r.C_Payment_ID=pay.C_Payment_ID "
			+ "LEFT JOIN C_Project pj ON r.C_Project_ID=pj.C_Project_ID "
			+ "LEFT JOIN C_Campaign cp ON r.C_Campaign_ID=cp.C_Campaign_ID "
			+ "LEFT JOIN A_Asset ast ON r.A_Asset_ID=ast.A_Asset_ID "
			+ "LEFT JOIN R_RequestType rt ON r.R_RequestType_ID=rt.R_RequestType_ID "
			+ "LEFT JOIN AD_User sr ON r.SalesRep_ID=sr.AD_User_ID "
			+ "LEFT JOIN AD_User req ON r.AD_User_ID=req.AD_User_ID "
			+ "LEFT JOIN AD_User cr ON r.CreatedBy=cr.AD_User_ID "
			+ "WHERE r.R_Request_ID=? AND r.AD_Client_ID=?";

		JsonObject card = null;
		try (PreparedStatement pstmt = DB.prepareStatement(sql, null)) {
			pstmt.setInt(1, cardId);
			pstmt.setInt(2, clientId);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					card = new JsonObject();
					card.addProperty("id", rs.getInt("R_Request_ID"));
					card.addProperty("documentNo", rs.getString("DocumentNo"));
					card.addProperty("summary", nvl(rs.getString("Summary")));
					card.addProperty("result", nvl(rs.getString("Result")));
					card.addProperty("statusId", rs.getInt("R_Status_ID"));
					card.addProperty("statusName", rs.getString("StatusName"));
					card.addProperty("requestTypeId", rs.getInt("R_RequestType_ID"));
					card.addProperty("requestTypeName", rs.getString("RequestTypeName"));
					card.addProperty("priority", nvl(rs.getString("Priority")));
					card.addProperty("dueType", nvl(rs.getString("DueType")));
					card.addProperty("isEscalated", "Y".equals(rs.getString("IsEscalated")));
					// Dates
					addTimestamp(card, "dateNextAction", rs.getTimestamp("DateNextAction"));
					addTimestamp(card, "startDate", rs.getTimestamp("StartDate"));
					addTimestamp(card, "endTime", rs.getTimestamp("EndTime"));
					addTimestamp(card, "closeDate", rs.getTimestamp("CloseDate"));
					addTimestamp(card, "created", rs.getTimestamp("Created"));
					// People
					card.addProperty("salesRepId", rs.getInt("SalesRep_ID"));
					card.addProperty("salesRepName", rs.getString("SalesRepName"));
					card.addProperty("requesterId", rs.getInt("AD_User_ID"));
					card.addProperty("requesterName", rs.getString("RequesterName"));
					card.addProperty("createdBy", rs.getInt("CreatedBy"));
					card.addProperty("creatorName", rs.getString("CreatorName"));
					// ERP relationships (id + name for each)
					addFk(card, "bpartnerId", rs, "C_BPartner_ID");
					card.addProperty("bpartnerName", rs.getString("BPartnerName"));
					addFk(card, "productId", rs, "M_Product_ID");
					card.addProperty("productName", rs.getString("ProductName"));
					addFk(card, "orderId", rs, "C_Order_ID");
					card.addProperty("orderName", rs.getString("OrderNo"));
					addFk(card, "invoiceId", rs, "C_Invoice_ID");
					card.addProperty("invoiceName", rs.getString("InvoiceNo"));
					addFk(card, "paymentId", rs, "C_Payment_ID");
					card.addProperty("paymentName", rs.getString("PaymentNo"));
					addFk(card, "projectId", rs, "C_Project_ID");
					card.addProperty("projectName", rs.getString("ProjectName"));
					addFk(card, "campaignId", rs, "C_Campaign_ID");
					card.addProperty("campaignName", rs.getString("CampaignName"));
					addFk(card, "assetId", rs, "A_Asset_ID");
					card.addProperty("assetName", rs.getString("AssetName"));
				}
			}
		} catch (Exception e) { sendError(resp, 500, e); return; }

		if (card == null) { resp.setStatus(404); resp.getWriter().print("{\"error\":\"Not found\"}"); return; }

		// Move history
		JsonArray history = new JsonArray();
		String hSql = "SELECT l.Created, l.CreatedBy, l.R_Status_ID_From, l.R_Status_ID_To, l.Note, "
			+ "COALESCE(u.Name,'') AS UserName, "
			+ "COALESCE(sf.Name,'') AS FromStatus, COALESCE(st.Name,'') AS ToStatus "
			+ "FROM RK_Card_Move_Log l "
			+ "LEFT JOIN AD_User u ON l.CreatedBy=u.AD_User_ID "
			+ "LEFT JOIN R_Status sf ON l.R_Status_ID_From=sf.R_Status_ID "
			+ "LEFT JOIN R_Status st ON l.R_Status_ID_To=st.R_Status_ID "
			+ "WHERE l.R_Request_ID=? ORDER BY l.Created DESC";
		try (PreparedStatement pstmt = DB.prepareStatement(hSql, null)) {
			pstmt.setInt(1, cardId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					JsonObject h = new JsonObject();
					addTimestamp(h, "date", rs.getTimestamp("Created"));
					h.addProperty("userName", rs.getString("UserName"));
					h.addProperty("fromStatus", rs.getString("FromStatus"));
					h.addProperty("toStatus", rs.getString("ToStatus"));
					h.addProperty("note", nvl(rs.getString("Note")));
					history.add(h);
				}
			}
		} catch (Exception ignored) {}

		card.add("moveHistory", history);
		resp.getWriter().print(card.toString());
	}

	// ============================================================
	// PUT /cards/{id} — update card fields
	// ============================================================
	private void updateCard(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String pathInfo = req.getPathInfo();
		int cardId = parsePathId(pathInfo);
		if (cardId <= 0) { resp.setStatus(400); resp.getWriter().print("{\"error\":\"Invalid ID\"}"); return; }

		int clientId = AuthContext.getClientId(req);
		int userId = AuthContext.getUserId(req);

		int cardClientId = DB.getSQLValueEx(null, "SELECT AD_Client_ID FROM R_Request WHERE R_Request_ID=?", cardId);
		if (cardClientId != clientId) { resp.setStatus(403); resp.getWriter().print("{\"error\":\"Access denied\"}"); return; }

		// Parse JSON body with Gson
		StringBuilder body = new StringBuilder();
		req.getReader().lines().forEach(body::append);
		JsonObject json = com.google.gson.JsonParser.parseString(body.toString()).getAsJsonObject();

		try {
			Properties ctx = Env.getCtx();
			Env.setContext(ctx, "#AD_Client_ID", clientId);
			Env.setContext(ctx, "#AD_Org_ID", AuthContext.getOrgId(req));
			Env.setContext(ctx, "#AD_User_ID", userId);

			Trx.run(new TrxRunnable() {
				@Override
				public void run(String trxName) {
					MRequest request = new MRequest(Env.getCtx(), cardId, trxName);
					if (json.has("summary")) request.setSummary(json.get("summary").getAsString());
					if (json.has("priority")) request.setPriority(json.get("priority").getAsString());
					if (json.has("statusId")) request.setR_Status_ID(json.get("statusId").getAsInt());
					if (json.has("salesRepId")) request.setSalesRep_ID(json.get("salesRepId").getAsInt());
					if (json.has("requestTypeId")) request.setR_RequestType_ID(json.get("requestTypeId").getAsInt());
					if (json.has("dateNextAction") && !json.get("dateNextAction").isJsonNull())
						request.setDateNextAction(new Timestamp(json.get("dateNextAction").getAsLong()));
					if (json.has("bpartnerId")) request.setC_BPartner_ID(json.get("bpartnerId").getAsInt());
					if (json.has("productId")) request.setM_Product_ID(json.get("productId").getAsInt());
					if (json.has("orderId")) request.set_ValueOfColumn("C_Order_ID", json.get("orderId").getAsInt() > 0 ? json.get("orderId").getAsInt() : null);
					if (json.has("invoiceId")) request.set_ValueOfColumn("C_Invoice_ID", json.get("invoiceId").getAsInt() > 0 ? json.get("invoiceId").getAsInt() : null);
					if (json.has("paymentId")) request.set_ValueOfColumn("C_Payment_ID", json.get("paymentId").getAsInt() > 0 ? json.get("paymentId").getAsInt() : null);
					if (json.has("projectId")) request.setC_Project_ID(json.get("projectId").getAsInt());
					if (json.has("campaignId")) request.setC_Campaign_ID(json.get("campaignId").getAsInt());
					if (json.has("assetId")) request.set_ValueOfColumn("A_Asset_ID", json.get("assetId").getAsInt() > 0 ? json.get("assetId").getAsInt() : null);
					request.saveEx(trxName);

					// Result is updated via direct SQL because MRequest.setResult()
					// writes to R_RequestUpdate, not R_Request.Result
					if (json.has("result")) {
						DB.executeUpdateEx("UPDATE R_Request SET Result=?, Updated=now(), UpdatedBy=? WHERE R_Request_ID=?",
							new Object[]{json.get("result").getAsString(), userId, cardId}, trxName);
					}
				}
			});
		} catch (Exception e) { sendError(resp, 500, e); return; }

		JsonObject result = new JsonObject();
		result.addProperty("success", true);
		resp.getWriter().print(result.toString());
	}

	// ============================================================
	// POST /cards — create new request
	// ============================================================
	private void createCard(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		int clientId = AuthContext.getClientId(req);
		int orgId = AuthContext.getOrgId(req);
		int userId = AuthContext.getUserId(req);

		StringBuilder body = new StringBuilder();
		req.getReader().lines().forEach(body::append);
		JsonObject json = com.google.gson.JsonParser.parseString(body.toString()).getAsJsonObject();

		String summary = json.has("summary") ? json.get("summary").getAsString() : "";
		if (summary.isEmpty()) {
			resp.setStatus(400); resp.getWriter().print("{\"error\":\"Summary is required\"}"); return;
		}

		final int[] newId = {0};
		try {
			Properties ctx = Env.getCtx();
			Env.setContext(ctx, "#AD_Client_ID", clientId);
			Env.setContext(ctx, "#AD_Org_ID", orgId > 0 ? orgId : DB.getSQLValueEx(null,
				"SELECT MIN(AD_Org_ID) FROM AD_Org WHERE AD_Client_ID=? AND AD_Org_ID>0 AND IsActive='Y'", clientId));
			Env.setContext(ctx, "#AD_User_ID", userId);

			Trx.run(new TrxRunnable() {
				@Override
				public void run(String trxName) {
					MRequest request = new MRequest(Env.getCtx(), 0, trxName);
					request.setSummary(summary);
					request.setSalesRep_ID(userId);
					if (json.has("requestTypeId")) request.setR_RequestType_ID(json.get("requestTypeId").getAsInt());
					if (json.has("bpartnerId")) request.setC_BPartner_ID(json.get("bpartnerId").getAsInt());
					if (json.has("priority")) request.setPriority(json.get("priority").getAsString());
					if (json.has("salesRepId")) request.setSalesRep_ID(json.get("salesRepId").getAsInt());
					if (json.has("dateNextAction") && !json.get("dateNextAction").isJsonNull())
						request.setDateNextAction(new Timestamp(json.get("dateNextAction").getAsLong()));
					request.saveEx(trxName);
					newId[0] = request.getR_Request_ID();
				}
			});
		} catch (Exception e) { sendError(resp, 500, e); return; }

		// Publish refresh event
		try {
			Map<String, Object> eventData = new HashMap<>();
			eventData.put("AD_Client_ID", clientId);
			eventData.put("R_Request_ID", newId[0]);
			EventManager.getInstance().sendEvent(EventManager.newEvent("kanban/refresh", eventData));
		} catch (Exception ignored) {}

		JsonObject result = new JsonObject();
		result.addProperty("success", true);
		result.addProperty("id", newId[0]);
		resp.setStatus(201);
		resp.getWriter().print(result.toString());
	}

	// ============================================================
	// Helpers
	// ============================================================
	private static void addTimestamp(JsonObject obj, String key, Timestamp ts) {
		if (ts != null) obj.addProperty(key, ts.getTime());
	}

	private static void addFk(JsonObject obj, String key, ResultSet rs, String col) throws java.sql.SQLException {
		int v = rs.getInt(col);
		if (!rs.wasNull() && v > 0) obj.addProperty(key, v);
	}

	private static int parsePathId(String pathInfo) {
		if (pathInfo == null) return -1;
		String[] parts = pathInfo.split("/");
		for (String p : parts) {
			try { return Integer.parseInt(p); } catch (NumberFormatException ignored) {}
		}
		return -1;
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

	private static String extractString(String json, String key) {
		if (json == null) return null;
		String search = "\"" + key + "\":\"";
		int idx = json.indexOf(search);
		if (idx < 0) return null;
		idx += search.length();
		int end = json.indexOf("\"", idx);
		return end > idx ? json.substring(idx, end).replace("\\\"", "\"").replace("\\n", "\n") : null;
	}
}
