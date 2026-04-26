package tw.mxp.idempiere.kanban;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.compiere.model.MSysConfig;
import org.compiere.util.DB;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * POST /config — save kanban configuration (WIP limits, priority colors).
 * Stores in AD_SysConfig with KANBAN_ prefix.
 */
public class ConfigServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");

		int clientId = AuthContext.getClientId(req);
		int userId = AuthContext.getUserId(req);

		StringBuilder body = new StringBuilder();
		req.getReader().lines().forEach(body::append);
		JsonObject json = JsonParser.parseString(body.toString()).getAsJsonObject();

		Properties ctx = Env.getCtx();
		Env.setContext(ctx, "#AD_Client_ID", clientId);
		Env.setContext(ctx, "#AD_User_ID", userId);

		try {
			// Active request type
			if (json.has("activeRequestTypeId")) {
				saveSysConfig("KANBAN_ACTIVE_REQUEST_TYPE", json.get("activeRequestTypeId").getAsString(), clientId);
			}

			// WIP limits
			if (json.has("wipLimits")) {
				JsonObject wip = json.getAsJsonObject("wipLimits");
				for (Map.Entry<String, JsonElement> e : wip.entrySet()) {
					saveSysConfig("KANBAN_WIP_" + e.getKey(), String.valueOf(e.getValue().getAsInt()), clientId);
				}
			}

			// Priority colors
			if (json.has("priorityColors")) {
				JsonObject colors = json.getAsJsonObject("priorityColors");
				for (Map.Entry<String, JsonElement> e : colors.entrySet()) {
					saveSysConfig("KANBAN_COLOR_P" + e.getKey(), e.getValue().getAsString(), clientId);
				}
			}

			// Create new status
			if (json.has("createStatus")) {
				JsonObject s = json.getAsJsonObject("createStatus");
				int catId = s.get("statusCategoryId").getAsInt();
				int seqNo = s.has("seqNo") ? s.get("seqNo").getAsInt() : 99;
				int sId = DB.getNextID(0, "R_Status", null);
				DB.executeUpdateEx("INSERT INTO R_Status (R_Status_ID, AD_Client_ID, AD_Org_ID, IsActive, "
					+ "Created, CreatedBy, Updated, UpdatedBy, Name, R_StatusCategory_ID, SeqNo, "
					+ "IsOpen, IsClosed, IsFinalClose, IsDefault, R_Status_UU) "
					+ "VALUES (?, 0, 0, 'Y', now(), ?, now(), ?, ?, ?, ?, ?, ?, ?, 'N', generate_uuid())",
					new Object[]{sId, userId, userId,
						s.get("name").getAsString(), catId, seqNo,
						s.has("isOpen") && s.get("isOpen").getAsBoolean() ? "Y" : "N",
						s.has("isClosed") && s.get("isClosed").getAsBoolean() ? "Y" : "N",
						s.has("isFinalClose") && s.get("isFinalClose").getAsBoolean() ? "Y" : "N"
					}, null);
			}

			// Update status
			if (json.has("updateStatus")) {
				JsonObject s = json.getAsJsonObject("updateStatus");
				int sId = s.get("id").getAsInt();
				DB.executeUpdateEx("UPDATE R_Status SET Name=?, SeqNo=?, IsOpen=?, IsClosed=?, IsFinalClose=?, Updated=now(), UpdatedBy=? "
					+ "WHERE R_Status_ID=?",
					new Object[]{
						s.get("name").getAsString(),
						s.has("seqNo") ? s.get("seqNo").getAsInt() : 0,
						s.has("isOpen") && s.get("isOpen").getAsBoolean() ? "Y" : "N",
						s.has("isClosed") && s.get("isClosed").getAsBoolean() ? "Y" : "N",
						s.has("isFinalClose") && s.get("isFinalClose").getAsBoolean() ? "Y" : "N",
						userId, sId
					}, null);
			}

			// Delete status (only if no cards use it)
			if (json.has("deleteStatusId")) {
				int sId = json.get("deleteStatusId").getAsInt();
				int count = DB.getSQLValueEx(null, "SELECT COUNT(*) FROM R_Request WHERE R_Status_ID=?", sId);
				if (count > 0) {
					resp.setStatus(400);
					resp.getWriter().print("{\"error\":\"Cannot delete: " + count + " cards use this status\"}");
					return;
				}
				DB.executeUpdateEx("DELETE FROM R_Status WHERE R_Status_ID=?", new Object[]{sId}, null);
			}

		} catch (Exception e) {
			resp.setStatus(500);
			String msg = e.getMessage();
			if (msg != null) msg = msg.replace("\"", "'").replace("\n", " ");
			resp.getWriter().print("{\"error\":\"" + msg + "\"}");
			return;
		}

		resp.getWriter().print("{\"success\":true}");
	}

	private void saveSysConfig(String name, String value, int clientId) {
		int existing = DB.getSQLValueEx(null,
			"SELECT AD_SysConfig_ID FROM AD_SysConfig WHERE Name=? AND AD_Client_ID=?", name, clientId);
		if (existing > 0) {
			DB.executeUpdateEx("UPDATE AD_SysConfig SET Value=?, Updated=now() WHERE AD_SysConfig_ID=?",
				new Object[]{value, existing}, null);
		} else {
			int id = DB.getNextID(clientId, "AD_SysConfig", null);
			DB.executeUpdateEx("INSERT INTO AD_SysConfig (AD_SysConfig_ID, AD_Client_ID, AD_Org_ID, IsActive, "
				+ "Created, CreatedBy, Updated, UpdatedBy, Name, Value, EntityType, ConfigurationLevel, AD_SysConfig_UU) "
				+ "VALUES (?, ?, 0, 'Y', now(), 100, now(), 100, ?, ?, 'U', 'C', generate_uuid())",
				new Object[]{id, clientId, name, value}, null);
		}
	}
}
