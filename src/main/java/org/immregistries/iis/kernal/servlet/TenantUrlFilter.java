package org.immregistries.iis.kernal.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TenantUrlFilter extends OncePerRequestFilter {
	public static final String TENANT_NAME_URL = "TENANT_NAME_URL";
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		String path = request.getServletPath();

		if (!path.startsWith(TenantController.TENANT_BASE_PATH + "/")) {
			filterChain.doFilter(request, response);
			return;
		}
		path = path.replace(TenantController.TENANT_BASE_PATH + "/", "");
		int indexOfNext = path.indexOf("/");
		if (indexOfNext <= 0) {
			filterChain.doFilter(request, response);
			return;
		}
		path = path.substring(0, indexOfNext);
		request.setAttribute(TENANT_NAME_URL, path);
		filterChain.doFilter(request, response);
	}

}
