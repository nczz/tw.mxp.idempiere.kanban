package tw.mxp.idempiere.kanban;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.compiere.model.MSysConfig;
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
			// WIP limits: {"wipLimits": {"100": 5, "101": 3}}
			if (json.has("wipLimits")) {
				JsonObject wip = json.getAsJsonObject("wipLimits");
				for (Map.Entry<String, JsonElement> e : wip.entrySet()) {
					String key = "KANBAN_WIP_" + e.getKey();
					int value = e.getValue().getAsInt();
					saveSysConfig(key, String.valueOf(value), clientId);
				}
			}

			// Priority colors: {"priorityColors": {"1": "#EF4444", "3": "#F97316"}}
			if (json.has("priorityColors")) {
				JsonObject colors = json.getAsJsonObject("priorityColors");
				for (Map.Entry<String, JsonElement> e : colors.entrySet()) {
					String key = "KANBAN_COLOR_P" + e.getKey();
					saveSysConfig(key, e.getValue().getAsString(), clientId);
				}
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
