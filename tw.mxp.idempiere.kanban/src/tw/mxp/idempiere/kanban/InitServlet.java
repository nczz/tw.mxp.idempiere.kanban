package tw.mxp.idempiere.kanban;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

		JsonObject result = new JsonObject();

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
					st.addProperty("statusCategoryId", rs.getInt("R_StatusCategory_ID"));
					statuses.add(st);
				}
			}
		} catch (Exception e) {
			sendError(resp, 500, e);
			return;
		}
		result.add("statuses", statuses);

		// Current user info
		JsonObject user = new JsonObject();
		user.addProperty("id", userId);
		String uSql = "SELECT Name FROM AD_User WHERE AD_User_ID=?";
		String userName = DB.getSQLValueStringEx(null, uSql, userId);
		user.addProperty("name", userName != null ? userName : "");
		user.addProperty("roleId", AuthContext.getRoleId(req));
		result.add("user", user);

		resp.getWriter().print(result.toString());
	}

	private void sendError(HttpServletResponse resp, int status, Exception e) throws IOException {
		resp.setStatus(status);
		String msg = e.getMessage();
		if (msg != null) msg = msg.replace("\"", "'").replace("\n", " ").replace("\r", "");
		resp.getWriter().print("{\"error\":\"" + msg + "\"}");
	}
}
