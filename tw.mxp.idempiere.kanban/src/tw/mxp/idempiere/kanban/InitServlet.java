package tw.mxp.idempiere.kanban;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.compiere.model.MSysConfig;
import org.compiere.util.DB;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * GET /init — returns statuses, request types, user info for the kanban board.
 */
public class InitServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");

		int clientId = AuthContext.getClientId(req);
		int userId = AuthContext.getUserId(req);
		String lang = "en_US";
		Object reqLang = req.getAttribute("AD_Language");
		if (reqLang instanceof String && !((String)reqLang).isEmpty()) lang = (String) reqLang;

		JsonObject result = new JsonObject();

		// Active request type (from AD_SysConfig or first available)
		int activeRtId = 0;
		String artVal = MSysConfig.getValue("KANBAN_ACTIVE_REQUEST_TYPE", "", clientId);
		if (artVal != null && !artVal.isEmpty()) {
			try { activeRtId = Integer.parseInt(artVal); } catch (Exception ignored) {}
		}
		result.addProperty("activeRequestTypeId", activeRtId);

		// Request types available for this client
		JsonArray requestTypes = new JsonArray();
		String rtSql = "SELECT rt.R_RequestType_ID, rt.Name, rt.R_StatusCategory_ID "
				+ "FROM R_RequestType rt "
				+ "WHERE rt.AD_Client_ID IN (0, ?) AND rt.IsActive='Y' "
				+ "ORDER BY rt.Name";
		try (PreparedStatement pstmt = DB.prepareStatement(rtSql, null)) {
			pstmt.setInt(1, clientId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					JsonObject rt = new JsonObject();
					rt.addProperty("id", rs.getInt("R_RequestType_ID"));
					rt.addProperty("name", rs.getString("Name"));
					rt.addProperty("statusCategoryId", rs.getInt("R_StatusCategory_ID"));
					requestTypes.add(rt);
				}
			}
		} catch (Exception e) {
			sendError(resp, 500, e);
			return;
		}
		result.add("requestTypes", requestTypes);

		// Statuses grouped by status category (for all request types of this client)
		JsonArray statuses = new JsonArray();
		String stSql = "SELECT DISTINCT s.R_Status_ID, s.Name, s.SeqNo, s.IsClosed, s.IsOpen, "
				+ "s.IsFinalClose, s.R_StatusCategory_ID "
				+ "FROM R_Status s "
				+ "WHERE s.R_StatusCategory_ID IN ("
				+ "  SELECT DISTINCT rt.R_StatusCategory_ID FROM R_RequestType rt "
				+ "  WHERE rt.AD_Client_ID IN (0, ?) AND rt.IsActive='Y'"
				+ ") AND s.IsActive='Y' "
				+ "ORDER BY s.R_StatusCategory_ID, s.SeqNo";
		try (PreparedStatement pstmt = DB.prepareStatement(stSql, null)) {
			pstmt.setInt(1, clientId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					JsonObject st = new JsonObject();
					st.addProperty("id", rs.getInt("R_Status_ID"));
					st.addProperty("name", rs.getString("Name"));
					st.addProperty("seqNo", rs.getInt("SeqNo"));
					st.addProperty("isClosed", "Y".equals(rs.getString("IsClosed")));
					st.addProperty("isOpen", "Y".equals(rs.getString("IsOpen")));
					st.addProperty("isFinalClose", "Y".equals(rs.getString("IsFinalClose")));
					st.addProperty("statusCategoryId", rs.getInt("R_StatusCategory_ID"));
					statuses.add(st);
				}
			}
		} catch (Exception e) {
			sendError(resp, 500, e);
			return;
		}
		result.add("statuses", statuses);

		// Priorities from AD_Ref_List (with i18n)
		JsonArray priorities = new JsonArray();
		String prSql = "SELECT rl.Value, COALESCE(t.Name, rl.Name) AS Name "
				+ "FROM AD_Ref_List rl "
				+ "LEFT JOIN AD_Ref_List_Trl t ON rl.AD_Ref_List_ID=t.AD_Ref_List_ID AND t.AD_Language=? AND t.IsTranslated='Y' "
				+ "WHERE rl.AD_Reference_ID=(SELECT AD_Reference_ID FROM AD_Reference WHERE Name='_PriorityRule' AND IsActive='Y') "
				+ "AND rl.IsActive='Y' ORDER BY rl.Value";
		try (PreparedStatement pstmt = DB.prepareStatement(prSql, null)) {
			pstmt.setString(1, lang);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					JsonObject p = new JsonObject();
					p.addProperty("value", rs.getString("Value"));
					p.addProperty("name", rs.getString("Name"));
					priorities.add(p);
				}
			}
		} catch (Exception e) { /* non-critical */ }
		// Fallback if reference not found
		if (priorities.size() == 0) {
			for (String[] pv : new String[][]{{"1","Urgent"},{"3","High"},{"5","Medium"},{"7","Low"},{"9","Minor"}}) {
				JsonObject p = new JsonObject(); p.addProperty("value", pv[0]); p.addProperty("name", pv[1]); priorities.add(p);
			}
		}
		result.add("priorities", priorities);

		// Sales reps (active users in this client)
		JsonArray salesReps = new JsonArray();
		String srSql = "SELECT AD_User_ID, Name FROM AD_User "
				+ "WHERE AD_Client_ID IN (0, ?) AND IsActive='Y' AND Name IS NOT NULL "
				+ "ORDER BY Name";
		try (PreparedStatement pstmt = DB.prepareStatement(srSql, null)) {
			pstmt.setInt(1, clientId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					JsonObject sr = new JsonObject();
					sr.addProperty("id", rs.getInt("AD_User_ID"));
					sr.addProperty("name", rs.getString("Name"));
					salesReps.add(sr);
				}
			}
		} catch (Exception e) { /* non-critical */ }
		result.add("salesReps", salesReps);

		// Active BPartners for this client
		JsonArray bpartners = new JsonArray();
		String bpSql = "SELECT C_BPartner_ID, Name FROM C_BPartner "
				+ "WHERE AD_Client_ID=? AND IsActive='Y' ORDER BY Name";
		try (PreparedStatement pstmt = DB.prepareStatement(bpSql, null)) {
			pstmt.setInt(1, clientId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					JsonObject bp = new JsonObject();
					bp.addProperty("id", rs.getInt("C_BPartner_ID"));
					bp.addProperty("name", rs.getString("Name"));
					bpartners.add(bp);
				}
			}
		} catch (Exception e) { /* non-critical */ }
		result.add("bpartners", bpartners);

		// Active Projects for this client
		JsonArray projects = new JsonArray();
		String pjSql = "SELECT C_Project_ID, Name FROM C_Project "
				+ "WHERE AD_Client_ID IN (0, ?) AND IsActive='Y' ORDER BY Name";
		try (PreparedStatement pstmt = DB.prepareStatement(pjSql, null)) {
			pstmt.setInt(1, clientId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					JsonObject pj = new JsonObject();
					pj.addProperty("id", rs.getInt("C_Project_ID"));
					pj.addProperty("name", rs.getString("Name"));
					projects.add(pj);
				}
			}
		} catch (Exception e) { /* non-critical */ }
		result.add("projects", projects);

		// Current user info
		JsonObject user = new JsonObject();
		user.addProperty("id", userId);
		String uSql = "SELECT Name FROM AD_User WHERE AD_User_ID=?";
		String userName = DB.getSQLValueStringEx(null, uSql, userId);
		user.addProperty("name", userName != null ? userName : "");
		user.addProperty("roleId", AuthContext.getRoleId(req));
		result.add("user", user);

		// i18n messages from AD_Message (keys starting with 'Kanban')
		JsonObject messages = new JsonObject();

		String msgSql = "SELECT m.Value, COALESCE(t.MsgText, m.MsgText) AS MsgText "
				+ "FROM AD_Message m "
				+ "LEFT JOIN AD_Message_Trl t ON m.AD_Message_ID=t.AD_Message_ID AND t.AD_Language=? AND t.IsTranslated='Y' "
				+ "WHERE m.Value LIKE 'Kanban%' AND m.IsActive='Y'";
		try (PreparedStatement pstmt = DB.prepareStatement(msgSql, null)) {
			pstmt.setString(1, lang);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					messages.addProperty(rs.getString("Value"), rs.getString("MsgText"));
				}
			}
		} catch (Exception e) { /* non-critical */ }
		result.add("messages", messages);

		// WIP limits from AD_SysConfig (KANBAN_WIP_{statusId})
		JsonObject wipLimits = new JsonObject();
		String wipSql = "SELECT Name, Value FROM AD_SysConfig WHERE Name LIKE 'KANBAN_WIP_%' AND AD_Client_ID IN (0, ?) AND IsActive='Y'";
		try (PreparedStatement pstmt = DB.prepareStatement(wipSql, null)) {
			pstmt.setInt(1, clientId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					String key = rs.getString("Name").replace("KANBAN_WIP_", "");
					try { wipLimits.addProperty(key, Integer.parseInt(rs.getString("Value"))); } catch (Exception ignored) {}
				}
			}
		} catch (Exception e) { /* non-critical */ }
		result.add("wipLimits", wipLimits);

		// Priority colors from AD_SysConfig (KANBAN_COLOR_P{value})
		JsonObject priorityColors = new JsonObject();
		String pcSql = "SELECT Name, Value FROM AD_SysConfig WHERE Name LIKE 'KANBAN_COLOR_P%' AND AD_Client_ID IN (0, ?) AND IsActive='Y'";
		try (PreparedStatement pstmt = DB.prepareStatement(pcSql, null)) {
			pstmt.setInt(1, clientId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					String key = rs.getString("Name").replace("KANBAN_COLOR_P", "");
					priorityColors.addProperty(key, rs.getString("Value"));
				}
			}
		} catch (Exception e) { /* non-critical */ }
		result.add("priorityColors", priorityColors);

		resp.getWriter().print(result.toString());
	}

	private void sendError(HttpServletResponse resp, int status, Exception e) throws IOException {
		resp.setStatus(status);
		String msg = e.getMessage();
		if (msg != null) msg = msg.replace("\"", "'").replace("\n", " ").replace("\r", "");
		resp.getWriter().print("{\"error\":\"" + msg + "\"}");
	}
}
