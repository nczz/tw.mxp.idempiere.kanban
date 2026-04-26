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
 * GET /metrics — cycle time and lead time statistics from RK_Card_Move_Log.
 */
public class MetricsServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		int clientId = AuthContext.getClientId(req);

		JsonObject result = new JsonObject();

		// Cycle time per status (avg days spent in each status)
		JsonArray cycleTime = new JsonArray();
		String ctSql = "SELECT s.Name AS StatusName, "
			+ "ROUND(AVG(EXTRACT(EPOCH FROM (next_move.Created - l.Created)) / 86400), 1) AS AvgDays, "
			+ "COUNT(*) AS CardCount "
			+ "FROM RK_Card_Move_Log l "
			+ "JOIN R_Status s ON l.R_Status_ID_To = s.R_Status_ID "
			+ "LEFT JOIN LATERAL ("
			+ "  SELECT Created FROM RK_Card_Move_Log l2 "
			+ "  WHERE l2.R_Request_ID = l.R_Request_ID AND l2.Created > l.Created "
			+ "  ORDER BY l2.Created LIMIT 1"
			+ ") next_move ON true "
			+ "WHERE l.AD_Client_ID = ? AND next_move.Created IS NOT NULL "
			+ "GROUP BY s.Name, s.SeqNo ORDER BY s.SeqNo";
		try (PreparedStatement pstmt = DB.prepareStatement(ctSql, null)) {
			pstmt.setInt(1, clientId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					JsonObject ct = new JsonObject();
					ct.addProperty("status", rs.getString("StatusName"));
					ct.addProperty("avgDays", rs.getDouble("AvgDays"));
					ct.addProperty("count", rs.getInt("CardCount"));
					cycleTime.add(ct);
				}
			}
		} catch (Exception ignored) {}
		result.add("cycleTime", cycleTime);

		// Throughput: completed cards per week (last 12 weeks)
		JsonArray throughput = new JsonArray();
		String tpSql = "SELECT DATE_TRUNC('week', l.Created) AS Week, COUNT(DISTINCT l.R_Request_ID) AS Completed "
			+ "FROM RK_Card_Move_Log l "
			+ "JOIN R_Status s ON l.R_Status_ID_To = s.R_Status_ID "
			+ "WHERE l.AD_Client_ID = ? AND s.IsClosed = 'Y' "
			+ "AND l.Created > NOW() - INTERVAL '12 weeks' "
			+ "GROUP BY DATE_TRUNC('week', l.Created) ORDER BY 1";
		try (PreparedStatement pstmt = DB.prepareStatement(tpSql, null)) {
			pstmt.setInt(1, clientId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					JsonObject tp = new JsonObject();
					tp.addProperty("week", rs.getTimestamp("Week").getTime());
					tp.addProperty("count", rs.getInt("Completed"));
					throughput.add(tp);
				}
			}
		} catch (Exception ignored) {}
		result.add("throughput", throughput);

		resp.getWriter().print(result.toString());
	}
}
