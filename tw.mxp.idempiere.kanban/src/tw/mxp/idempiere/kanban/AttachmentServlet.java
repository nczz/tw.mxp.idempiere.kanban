package tw.mxp.idempiere.kanban;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.adempiere.base.event.EventManager;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MTable;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Card attachment management.
 *   GET  /attachments/{cardId}           — list attachments
 *   POST /attachments/{cardId}           — upload {name, data(base64)}
 *   GET  /attachments/{cardId}/{name}    — download file
 *   DELETE /attachments/{cardId}/{name}  — delete file
 */
public class AttachmentServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final int R_REQUEST_TABLE_ID = 417;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String pathInfo = req.getPathInfo();
		if (pathInfo == null) { resp.setStatus(400); return; }

		String[] parts = pathInfo.substring(1).split("/", 2);
		int cardId;
		try { cardId = Integer.parseInt(parts[0]); } catch (Exception e) { resp.setStatus(400); return; }

		// Verify access
		int clientId = AuthContext.getClientId(req);
		if (DB.getSQLValueEx(null, "SELECT AD_Client_ID FROM R_Request WHERE R_Request_ID=?", cardId) != clientId) {
			resp.setStatus(403); return;
		}

		if (parts.length == 1) {
			// List attachments
			listAttachments(cardId, resp);
		} else {
			// Download file
			downloadAttachment(cardId, parts[1], resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		String pathInfo = req.getPathInfo();
		if (pathInfo == null) { resp.setStatus(400); return; }

		int cardId;
		try { cardId = Integer.parseInt(pathInfo.substring(1)); } catch (Exception e) { resp.setStatus(400); return; }

		int clientId = AuthContext.getClientId(req);
		if (DB.getSQLValueEx(null, "SELECT AD_Client_ID FROM R_Request WHERE R_Request_ID=?", cardId) != clientId) {
			resp.setStatus(403); resp.getWriter().print("{\"error\":\"Access denied\"}"); return;
		}

		StringBuilder body = new StringBuilder();
		req.getReader().lines().forEach(body::append);
		JsonObject json = JsonParser.parseString(body.toString()).getAsJsonObject();
		String name = json.has("name") ? json.get("name").getAsString() : "";
		String data = json.has("data") ? json.get("data").getAsString() : "";
		if (name.isEmpty() || data.isEmpty()) {
			resp.setStatus(400); resp.getWriter().print("{\"error\":\"name and data required\"}"); return;
		}

		try {
			Properties ctx = Env.getCtx();
			Env.setContext(ctx, "#AD_Client_ID", clientId);
			Env.setContext(ctx, "#AD_User_ID", AuthContext.getUserId(req));

			MAttachment att = MAttachment.get(ctx, R_REQUEST_TABLE_ID, cardId, null);
			if (att == null) {
				att = new MAttachment(ctx, R_REQUEST_TABLE_ID, cardId, null);
			}
			byte[] bytes = Base64.getDecoder().decode(data);
			att.addEntry(name, bytes);
			att.saveEx();

			resp.setStatus(201);
			resp.getWriter().print("{\"success\":true}");
			sendRefreshEvent(clientId, cardId);
		} catch (Exception e) {
			resp.setStatus(500);
			String msg = e.getMessage();
			if (msg != null) msg = msg.replace("\"", "'").replace("\n", " ");
			resp.getWriter().print("{\"error\":\"" + msg + "\"}");
		}
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		String pathInfo = req.getPathInfo();
		if (pathInfo == null) { resp.setStatus(400); return; }

		String[] parts = pathInfo.substring(1).split("/", 2);
		if (parts.length < 2) { resp.setStatus(400); return; }

		int cardId;
		try { cardId = Integer.parseInt(parts[0]); } catch (Exception e) { resp.setStatus(400); return; }
		String fileName = java.net.URLDecoder.decode(parts[1], "UTF-8");

		int clientId = AuthContext.getClientId(req);
		if (DB.getSQLValueEx(null, "SELECT AD_Client_ID FROM R_Request WHERE R_Request_ID=?", cardId) != clientId) {
			resp.setStatus(403); resp.getWriter().print("{\"error\":\"Access denied\"}"); return;
		}

		Properties ctx = Env.getCtx();
		Env.setContext(ctx, "#AD_Client_ID", clientId);
		MAttachment att = MAttachment.get(ctx, R_REQUEST_TABLE_ID, cardId, null);
		if (att != null) {
			for (int i = 0; i < att.getEntryCount(); i++) {
				if (fileName.equals(att.getEntryName(i))) {
					att.deleteEntry(i);
					att.saveEx();
					resp.getWriter().print("{\"success\":true}");
					sendRefreshEvent(clientId, cardId);
					return;
				}
			}
		}
		resp.setStatus(404);
		resp.getWriter().print("{\"error\":\"File not found\"}");
	}

	private void sendRefreshEvent(int clientId, int cardId) {
		try {
			Map<String, Object> d = new HashMap<>();
			d.put("AD_Client_ID", clientId);
			d.put("R_Request_ID", cardId);
			EventManager.getInstance().sendEvent(EventManager.newEvent("kanban/refresh", d));
		} catch (Exception ignored) {}
	}

	private void listAttachments(int cardId, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		JsonArray files = new JsonArray();
		Properties ctx = Env.getCtx();
		MAttachment att = MAttachment.get(ctx, R_REQUEST_TABLE_ID, cardId, null);
		if (att != null) {
			for (int i = 0; i < att.getEntryCount(); i++) {
				MAttachmentEntry entry = att.getEntry(i);
				JsonObject f = new JsonObject();
				f.addProperty("name", entry.getName());
				f.addProperty("size", entry.getData() != null ? entry.getData().length : 0);
				files.add(f);
			}
		}
		JsonObject result = new JsonObject();
		result.add("attachments", files);
		resp.getWriter().print(result.toString());
	}

	private void downloadAttachment(int cardId, String fileName, HttpServletResponse resp) throws IOException {
		try { fileName = java.net.URLDecoder.decode(fileName, "UTF-8"); } catch (Exception ignored) {}
		Properties ctx = Env.getCtx();
		MAttachment att = MAttachment.get(ctx, R_REQUEST_TABLE_ID, cardId, null);
		if (att != null) {
			for (int i = 0; i < att.getEntryCount(); i++) {
				if (fileName.equals(att.getEntryName(i))) {
					byte[] data = att.getEntryData(i);
					if (data != null) {
						resp.setContentType("application/octet-stream");
						resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
						resp.setContentLength(data.length);
						OutputStream out = resp.getOutputStream();
						out.write(data);
						out.flush();
						return;
					}
				}
			}
		}
		resp.setStatus(404);
	}
}
