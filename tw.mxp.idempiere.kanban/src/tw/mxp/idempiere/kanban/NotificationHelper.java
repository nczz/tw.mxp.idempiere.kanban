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
		String docNo = DB.getSQLValueStringEx(null, "SELECT DocumentNo FROM R_Request WHERE R_Request_ID=?", cardId);
		if (docNo == null) docNo = String.valueOf(cardId);

		List<int[]> watchers = getWatchers(cardId, actorUserId);
		for (int[] w : watchers) {
			int userId = w[0];
			try {
				String lang = DB.getSQLValueStringEx(null,
					"SELECT AD_Language FROM AD_User WHERE AD_User_ID=?", userId);
				if (lang == null || lang.isEmpty()) lang = "en_US";

				// Get translated template
				String template = getTranslatedMessage(msgKey, lang);
				String subject = docNo + " — " + template;
				String body = docNo + " — " + template + (detail != null && !detail.isEmpty() ? "\n\n" + detail : "");

				// AD_Note
				MNote note = new MNote(Env.getCtx(), 0, null);
				note.setAD_User_ID(userId);
				note.setClientOrg(clientId, 0);
				note.setAD_Table_ID(R_REQUEST_TABLE_ID);
				note.setRecord_ID(cardId);
				note.setTextMsg(body);
				note.setDescription(subject);
				note.setReference(docNo);
				note.saveEx();

				// Email
				sendEmail(clientId, userId, subject, body);
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
			client.sendEMail(email, subject, body, null, false);
		} catch (Exception e) {
			log.log(Level.FINE, "Email failed", e);
		}
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
