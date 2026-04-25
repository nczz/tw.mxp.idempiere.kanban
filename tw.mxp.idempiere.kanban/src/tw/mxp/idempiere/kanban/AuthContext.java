package tw.mxp.idempiere.kanban;

import javax.servlet.http.HttpServletRequest;

/**
 * Extracts authenticated user context set by AuthFilter.
 * All values come from JWT claims, not from Env.getCtx() (WAB has no ZK context).
 */
public class AuthContext {

	public static int getClientId(HttpServletRequest req) {
		Object v = req.getAttribute("AD_Client_ID");
		return v instanceof Integer ? (Integer) v : -1;
	}

	public static int getOrgId(HttpServletRequest req) {
		Object v = req.getAttribute("AD_Org_ID");
		return v instanceof Integer ? (Integer) v : 0;
	}

	public static int getUserId(HttpServletRequest req) {
		Object v = req.getAttribute("AD_User_ID");
		return v instanceof Integer ? (Integer) v : -1;
	}

	public static int getRoleId(HttpServletRequest req) {
		Object v = req.getAttribute("AD_Role_ID");
		return v instanceof Integer ? (Integer) v : -1;
	}
}
