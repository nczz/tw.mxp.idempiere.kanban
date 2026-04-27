package tw.mxp.idempiere.kanban;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.model.MSysConfig;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * Scheduled process: scans R_Request for due/overdue/starting cards and sends notifications.
 * Runs daily via AD_Scheduler.
 *
 * Reminders:
 *   - DateNextAction = tomorrow → notify watchers
 *   - DateNextAction = today → notify watchers
 *   - DateNextAction < today (open) → notify watchers (first day only)
 *   - StartDate = tomorrow → notify assignee
 *
 * Escalation:
 *   - 3+ days overdue → notify supervisor
 *   - 7+ days overdue → auto-block (IsEscalated='Y')
 */
public class KanbanReminderProcess extends SvrProcess {

	@Override
	protected void prepare() {}

	@Override
	protected String doIt() throws Exception {
		int clientId = getAD_Client_ID();
		if (!"Y".equals(MSysConfig.getValue("KANBAN_REMINDER_ENABLED", "Y", clientId))) {
			return "Reminders disabled";
		}

		int dueTomorrow = 0, dueToday = 0, overdue = 0, startTomorrow = 0, escalated = 0, blocked = 0;

		// Due tomorrow
		String sql = "SELECT R_Request_ID, SalesRep_ID FROM R_Request r "
			+ "JOIN R_Status s ON r.R_Status_ID=s.R_Status_ID "
			+ "WHERE r.AD_Client_ID=? AND s.IsClosed='N' AND r.DateNextAction::date = CURRENT_DATE + 1";
		try (PreparedStatement ps = DB.prepareStatement(sql, null)) {
			ps.setInt(1, clientId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int cardId = rs.getInt(1), salesRepId = rs.getInt(2);
					NotificationHelper.addWatcher(clientId, cardId, salesRepId);
					NotificationHelper.notifyWatchers(clientId, cardId, 0, "KanbanNotifyDueTomorrow", "");
					dueTomorrow++;
				}
			}
		}

		// Due today
		sql = "SELECT R_Request_ID, SalesRep_ID FROM R_Request r "
			+ "JOIN R_Status s ON r.R_Status_ID=s.R_Status_ID "
			+ "WHERE r.AD_Client_ID=? AND s.IsClosed='N' AND r.DateNextAction::date = CURRENT_DATE";
		try (PreparedStatement ps = DB.prepareStatement(sql, null)) {
			ps.setInt(1, clientId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int cardId = rs.getInt(1), salesRepId = rs.getInt(2);
					NotificationHelper.addWatcher(clientId, cardId, salesRepId);
					NotificationHelper.notifyWatchers(clientId, cardId, 0, "KanbanNotifyDueToday", "");
					dueToday++;
				}
			}
		}

		// Overdue (first day only: yesterday became overdue)
		sql = "SELECT R_Request_ID, SalesRep_ID, (CURRENT_DATE - r.DateNextAction::date) AS days_overdue "
			+ "FROM R_Request r JOIN R_Status s ON r.R_Status_ID=s.R_Status_ID "
			+ "WHERE r.AD_Client_ID=? AND s.IsClosed='N' AND r.DateNextAction::date = CURRENT_DATE - 1";
		try (PreparedStatement ps = DB.prepareStatement(sql, null)) {
			ps.setInt(1, clientId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int cardId = rs.getInt(1);
					NotificationHelper.notifyWatchers(clientId, cardId, 0, "KanbanNotifyOverdue", "1");
					overdue++;
				}
			}
		}

		// Escalation: 3+ days overdue → notify supervisor
		sql = "SELECT r.R_Request_ID, r.SalesRep_ID, u.Supervisor_ID, (CURRENT_DATE - r.DateNextAction::date) AS days "
			+ "FROM R_Request r JOIN R_Status s ON r.R_Status_ID=s.R_Status_ID "
			+ "LEFT JOIN AD_User u ON r.SalesRep_ID=u.AD_User_ID "
			+ "WHERE r.AD_Client_ID=? AND s.IsClosed='N' AND r.DateNextAction::date = CURRENT_DATE - 3";
		try (PreparedStatement ps = DB.prepareStatement(sql, null)) {
			ps.setInt(1, clientId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int cardId = rs.getInt(1), supervisorId = rs.getInt(3);
					if (supervisorId > 0) {
						NotificationHelper.addWatcher(clientId, cardId, supervisorId);
						NotificationHelper.notifyWatchers(clientId, cardId, 0, "KanbanNotifyEscalateSupervisor", "3");
					}
					escalated++;
				}
			}
		}

		// Escalation: 7+ days overdue → auto-block
		sql = "SELECT R_Request_ID FROM R_Request r "
			+ "JOIN R_Status s ON r.R_Status_ID=s.R_Status_ID "
			+ "WHERE r.AD_Client_ID=? AND s.IsClosed='N' AND r.IsEscalated='N' "
			+ "AND r.DateNextAction::date <= CURRENT_DATE - 7";
		try (PreparedStatement ps = DB.prepareStatement(sql, null)) {
			ps.setInt(1, clientId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int cardId = rs.getInt(1);
					DB.executeUpdate("UPDATE R_Request SET IsEscalated='Y', Updated=now() WHERE R_Request_ID=" + cardId, false, null);
					NotificationHelper.notifyWatchers(clientId, cardId, 0, "KanbanNotifyEscalateBlocked", "7+");
					blocked++;
				}
			}
		}

		// Start tomorrow
		sql = "SELECT R_Request_ID, SalesRep_ID FROM R_Request r "
			+ "JOIN R_Status s ON r.R_Status_ID=s.R_Status_ID "
			+ "WHERE r.AD_Client_ID=? AND s.IsClosed='N' AND r.StartDate::date = CURRENT_DATE + 1";
		try (PreparedStatement ps = DB.prepareStatement(sql, null)) {
			ps.setInt(1, clientId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int cardId = rs.getInt(1), salesRepId = rs.getInt(2);
					NotificationHelper.addWatcher(clientId, cardId, salesRepId);
					NotificationHelper.notifyWatchers(clientId, cardId, 0, "KanbanNotifyStartTomorrow", "");
					startTomorrow++;
				}
			}
		}

		return String.format("Due tomorrow: %d, Due today: %d, Overdue: %d, Escalated: %d, Blocked: %d, Start tomorrow: %d",
			dueTomorrow, dueToday, overdue, escalated, blocked, startTomorrow);
	}
}
