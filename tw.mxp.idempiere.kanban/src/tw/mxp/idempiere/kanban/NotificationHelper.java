package tw.mxp.idempiere.kanban;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;

import org.compiere.model.MClient;
import org.compiere.model.MNote;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * Notification helper — writes AD_Note + sends email to card watchers.
 * Templates use AD_Message for i18n.
 */
public class NotificationHelper {

	private static final CLogger log = CLogger.getCLogger(NotificationHelper.class);
	private static final int R_REQUEST_TABLE_ID = 417;

	/**
	 * Notify all watchers of a card (except the actor).
	 * @param clientId AD_Client_ID
	 * @param cardId R_Request_ID
	 * @param actorUserId user who performed the action (excluded from notifications)
	 * @param msgKey AD_Message key for the notification template
	 * @param detail additional detail text (e.g. status change, comment excerpt)
	 */
	public static void notifyWatchers(int clientId, int cardId, int actorUserId, String msgKey, String detail) {
		// Gather card info
		String docNo = DB.getSQLValueStringEx(null, "SELECT DocumentNo FROM R_Request WHERE R_Request_ID=?", cardId);
		String summary = DB.getSQLValueStringEx(null, "SELECT Summary FROM R_Request WHERE R_Request_ID=?", cardId);
		String actorName = actorUserId > 0 ? DB.getSQLValueStringEx(null, "SELECT Name FROM AD_User WHERE AD_User_ID=?", actorUserId) : "Kanban Reminder";
		if (docNo == null) docNo = String.valueOf(cardId);
		if (summary == null) summary = "";
		if (actorName == null) actorName = "";
		String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date());

		List<int[]> watchers = getWatchers(cardId, actorUserId);
		for (int[] w : watchers) {
			int userId = w[0];
			try {
				String lang = DB.getSQLValueStringEx(null,
					"SELECT AD_Language FROM AD_User WHERE AD_User_ID=?", userId);
				if (lang == null || lang.isEmpty()) lang = "en_US";

				String action = getTranslatedMessage(msgKey, lang);
				String cardLabel = getTranslatedMessage("KanbanNotifyCard", lang);
				String actorLabel = getTranslatedMessage("KanbanNotifyActor", lang);
				String timeLabel = getTranslatedMessage("KanbanNotifyTime", lang);

				// Subject
				String subject = actorUserId > 0
					? "[" + docNo + "] " + summary + " — " + actorName + " " + action
					: "[" + docNo + "] " + summary + " — ⏰ " + action;

				// Body (plain text for AD_Note)
				StringBuilder body = new StringBuilder();
				body.append(actorUserId > 0 ? actorName + " " : "⏰ ").append(action);
				if (detail != null && !detail.isEmpty()) body.append("\n").append(detail);
				body.append("\n\n");
				body.append(cardLabel).append(": ").append(docNo).append(" — ").append(summary).append("\n");
				if (actorUserId > 0) body.append(actorLabel).append(": ").append(actorName).append("\n");
				body.append(timeLabel).append(": ").append(timestamp);

				// AD_Note
				MNote note = new MNote(Env.getCtx(), 0, null);
				note.setAD_User_ID(userId);
				note.setClientOrg(clientId, 0);
				note.setAD_Table_ID(R_REQUEST_TABLE_ID);
				note.setRecord_ID(cardId);
				note.setTextMsg(body.toString());
				note.setDescription(subject);
				note.setReference(docNo);
				note.saveEx();

				// Email (HTML)
				String htmlBody = buildHtmlBody(cardId, actorUserId, msgKey, detail, lang);
				sendEmail(clientId, userId, subject, htmlBody);
			} catch (Exception e) {
				log.log(Level.FINE, "Notify failed for user " + userId, e);
			}
		}
	}

	/** Get watcher user IDs for a card, excluding the actor. */
	private static List<int[]> getWatchers(int cardId, int excludeUserId) {
		List<int[]> result = new ArrayList<>();
		String sql = "SELECT DISTINCT AD_User_ID FROM RK_Card_Member "
			+ "WHERE R_Request_ID=? AND IsActive='Y' AND AD_User_ID<>?";
		try (var pstmt = DB.prepareStatement(sql, null)) {
			pstmt.setInt(1, cardId);
			pstmt.setInt(2, excludeUserId);
			try (var rs = pstmt.executeQuery()) {
				while (rs.next()) result.add(new int[]{rs.getInt(1)});
			}
		} catch (Exception ignored) {}
		return result;
	}

	/** Get translated AD_Message text. */
	private static String getTranslatedMessage(String msgKey, String lang) {
		String translated = DB.getSQLValueStringEx(null,
			"SELECT COALESCE(t.MsgText, m.MsgText) FROM AD_Message m "
			+ "LEFT JOIN AD_Message_Trl t ON m.AD_Message_ID=t.AD_Message_ID AND t.AD_Language=? AND t.IsTranslated='Y' "
			+ "WHERE m.Value=?", lang, msgKey);
		return translated != null ? translated : msgKey;
	}

	/** Send email if user has an email address. */
	private static void sendEmail(int clientId, int userId, String subject, String body) {
		try {
			MUser user = MUser.get(Env.getCtx(), userId);
			String email = user.getEMail();
			if (email == null || email.isEmpty()) return;

			MClient client = MClient.get(Env.getCtx(), clientId);
			client.sendEMail(email, subject, body, null, true); // true = HTML
		} catch (Exception e) {
			log.log(Level.FINE, "Email failed", e);
		}
	}

	/** Build HTML email body with card info. */
	static String buildHtmlBody(int cardId, int actorUserId, String msgKey, String detail, String lang) {
		String docNo = DB.getSQLValueStringEx(null, "SELECT DocumentNo FROM R_Request WHERE R_Request_ID=?", cardId);
		String summary = DB.getSQLValueStringEx(null, "SELECT Summary FROM R_Request WHERE R_Request_ID=?", cardId);
		String actorName = actorUserId > 0 ? DB.getSQLValueStringEx(null, "SELECT Name FROM AD_User WHERE AD_User_ID=?", actorUserId) : "Kanban Reminder";
		String priority = DB.getSQLValueStringEx(null,
			"SELECT COALESCE(t.Name, rl.Name) FROM AD_Ref_List rl "
			+ "LEFT JOIN AD_Ref_List_Trl t ON rl.AD_Ref_List_ID=t.AD_Ref_List_ID AND t.AD_Language=? AND t.IsTranslated='Y' "
			+ "WHERE rl.AD_Reference_ID=154 AND rl.Value=(SELECT Priority FROM R_Request WHERE R_Request_ID=?)",
			lang, cardId);
		String status = DB.getSQLValueStringEx(null,
			"SELECT s.Name FROM R_Status s JOIN R_Request r ON r.R_Status_ID=s.R_Status_ID WHERE r.R_Request_ID=?", cardId);
		String assignee = DB.getSQLValueStringEx(null,
			"SELECT u.Name FROM AD_User u JOIN R_Request r ON r.SalesRep_ID=u.AD_User_ID WHERE r.R_Request_ID=?", cardId);
		String dueDate = DB.getSQLValueStringEx(null,
			"SELECT TO_CHAR(DateNextAction, 'YYYY-MM-DD') FROM R_Request WHERE R_Request_ID=?", cardId);
		String orgName = DB.getSQLValueStringEx(null,
			"SELECT o.Name FROM AD_Org o JOIN R_Request r ON r.AD_Org_ID=o.AD_Org_ID WHERE r.R_Request_ID=?", cardId);

		if (docNo == null) docNo = "";
		if (summary == null) summary = "";
		if (actorName == null) actorName = "";
		if (priority == null) priority = "";
		if (status == null) status = "";
		if (assignee == null) assignee = "";
		if (dueDate == null) dueDate = "—";
		if (orgName == null) orgName = "";

		String action = getTranslatedMessage(msgKey, lang);
		String lCard = getTranslatedMessage("KanbanNotifyCard", lang);
		String lPriority = getTranslatedMessage("KanbanEmailPriority", lang);
		String lStatus = getTranslatedMessage("KanbanEmailStatus", lang);
		String lAssignee = getTranslatedMessage("KanbanEmailAssignee", lang);
		String lDueDate = getTranslatedMessage("KanbanEmailDueDate", lang);
		String lOrg = getTranslatedMessage("KanbanEmailOrg", lang);
		String lActor = getTranslatedMessage("KanbanNotifyActor", lang);
		String lTime = getTranslatedMessage("KanbanNotifyTime", lang);
		String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date());

		StringBuilder h = new StringBuilder();
		h.append("<div style=\"font-family:sans-serif;max-width:560px;margin:0 auto\">");
		// Header
		h.append("<div style=\"background:#3b82f6;color:#fff;padding:12px 16px;border-radius:8px 8px 0 0;font-size:14px\">");
		h.append(actorUserId > 0
			? "<strong>" + esc(actorName) + "</strong> " + esc(action)
			: "⏰ " + esc(action));
		h.append("</div>");
		// Card info
		h.append("<div style=\"border:1px solid #e5e7eb;border-top:none;border-radius:0 0 8px 8px;padding:16px\">");
		h.append("<div style=\"font-size:18px;font-weight:bold;margin-bottom:4px\">").append(esc(summary)).append("</div>");
		h.append("<div style=\"font-size:12px;color:#9ca3af;margin-bottom:12px\">").append(esc(docNo)).append("</div>");
		// Detail
		if (detail != null && !detail.isEmpty()) {
			h.append("<div style=\"background:#f9fafb;border-radius:6px;padding:10px;margin-bottom:12px;font-size:13px;color:#374151;white-space:pre-wrap\">");
			h.append(esc(detail));
			h.append("</div>");
		}
		// Info table
		h.append("<table style=\"font-size:13px;color:#6b7280;width:100%;border-collapse:collapse\">");
		row(h, lPriority, priority); row(h, lStatus, status); row(h, lAssignee, assignee);
		row(h, lDueDate, dueDate); row(h, lOrg, orgName);
		if (actorUserId > 0) row(h, lActor, actorName);
		row(h, lTime, timestamp);
		h.append("</table>");
		h.append("</div></div>");
		return h.toString();
	}

	private static void row(StringBuilder h, String label, String value) {
		h.append("<tr><td style=\"padding:3px 8px 3px 0;color:#9ca3af\">").append(esc(label))
		  .append("</td><td style=\"padding:3px 0\">").append(esc(value)).append("</td></tr>");
	}

	private static String esc(String s) {
		return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
	}

	/** Add a watcher to a card (idempotent). */
	public static void addWatcher(int clientId, int cardId, int userId) {
		int existing = DB.getSQLValueEx(null,
			"SELECT COUNT(*) FROM RK_Card_Member WHERE R_Request_ID=? AND AD_User_ID=? AND IsActive='Y'",
			cardId, userId);
		if (existing > 0) return;
		int id = DB.getNextID(clientId, "RK_Card_Member", null);
		DB.executeUpdateEx("INSERT INTO RK_Card_Member (RK_Card_Member_ID, AD_Client_ID, AD_Org_ID, IsActive, "
			+ "Created, CreatedBy, Updated, UpdatedBy, R_Request_ID, AD_User_ID, MemberRole) "
			+ "VALUES (?, ?, 0, 'Y', now(), ?, now(), ?, ?, ?, 'W')",
			new Object[]{id, clientId, userId, userId, cardId, userId}, null);
	}

	/** Remove a watcher from a card. */
	public static void removeWatcher(int cardId, int userId) {
		DB.executeUpdate("DELETE FROM RK_Card_Member WHERE R_Request_ID=? AND AD_User_ID=?",
			new Object[]{cardId, userId}, false, null);
	}

	/** Check if user is watching a card. */
	public static boolean isWatching(int cardId, int userId) {
		return DB.getSQLValueEx(null,
			"SELECT COUNT(*) FROM RK_Card_Member WHERE R_Request_ID=? AND AD_User_ID=? AND IsActive='Y'",
			cardId, userId) > 0;
	}

	/** Get watcher names for a card. */
	public static String getWatcherNames(int cardId) {
		StringBuilder sb = new StringBuilder();
		String sql = "SELECT u.Name FROM RK_Card_Member m JOIN AD_User u ON m.AD_User_ID=u.AD_User_ID "
			+ "WHERE m.R_Request_ID=? AND m.IsActive='Y' ORDER BY u.Name";
		try (var pstmt = DB.prepareStatement(sql, null)) {
			pstmt.setInt(1, cardId);
			try (var rs = pstmt.executeQuery()) {
				while (rs.next()) {
					if (sb.length() > 0) sb.append(", ");
					sb.append(rs.getString(1));
				}
			}
		} catch (Exception ignored) {}
		return sb.toString();
	}

	/** Parse @mentions from text and add as watchers + notify. */
	public static void processMentions(int clientId, int cardId, int actorUserId, String text) {
		if (text == null) return;
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("@(\\w+)").matcher(text);
		while (m.find()) {
			String username = m.group(1);
			int mentionedUserId = DB.getSQLValueEx(null,
				"SELECT AD_User_ID FROM AD_User WHERE Name=? AND AD_Client_ID IN (0,?) AND IsActive='Y'",
				username, clientId);
			if (mentionedUserId > 0 && mentionedUserId != actorUserId) {
				addWatcher(clientId, cardId, mentionedUserId);
				notifyWatchers(clientId, cardId, actorUserId, "KanbanNotifyMention",
					"@" + username + ": " + (text.length() > 200 ? text.substring(0, 200) + "..." : text));
			}
		}
	}
}
