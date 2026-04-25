package tw.mxp.idempiere.kanban;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class NoCacheFilter implements Filter {

	@Override
	public void init(FilterConfig config) throws ServletException {}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletResponse httpRes = (HttpServletResponse) res;
		httpRes.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		httpRes.setHeader("Pragma", "no-cache");
		httpRes.setDateHeader("Expires", 0);
		chain.doFilter(req, res);
	}

	@Override
	public void destroy() {}
}
