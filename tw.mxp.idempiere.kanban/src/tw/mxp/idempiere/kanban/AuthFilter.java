package tw.mxp.idempiere.kanban;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT authentication filter. Validates Bearer token on all paths except /web/*.
 * Sets AD_Client_ID, AD_Org_ID, AD_User_ID, AD_Role_ID as request attributes.
 */
public class AuthFilter implements Filter {

	@Override
	public void init(FilterConfig config) throws ServletException {}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpReq = (HttpServletRequest) req;
		HttpServletResponse httpResp = (HttpServletResponse) res;
		String path = httpReq.getServletPath();

		// Skip auth for static resources
		if (path.startsWith("/web/")) {
			chain.doFilter(req, res);
			return;
		}

		String auth = httpReq.getHeader("Authorization");
		if (auth == null || !auth.startsWith("Bearer ")) {
			sendError(httpResp, 401, "Missing token");
			return;
		}

		Map<String, Object> claims = JwtUtil.validate(auth.substring(7));
		if (claims == null) {
			sendError(httpResp, 401, "Invalid or expired token");
			return;
		}

		// Set attributes for downstream servlets (via AuthContext)
		req.setAttribute("AD_Client_ID", claims.get("AD_Client_ID"));
		req.setAttribute("AD_Org_ID", claims.get("AD_Org_ID"));
		req.setAttribute("AD_User_ID", claims.get("AD_User_ID"));
		req.setAttribute("AD_Role_ID", claims.get("AD_Role_ID"));
		req.setAttribute("AD_Language", claims.get("AD_Language"));

		chain.doFilter(req, res);
	}

	@Override
	public void destroy() {}

	private void sendError(HttpServletResponse resp, int status, String msg) throws IOException {
		resp.setStatus(status);
		resp.setContentType("application/json; charset=UTF-8");
		PrintWriter out = resp.getWriter();
		out.print("{\"error\":\"" + msg.replace("\"", "'") + "\"}");
	}
}
