package org.immregistries.iis.kernal.servlet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.immregistries.iis.kernal.servlet.shlink.PatientShLinkManifestController.PATIENT_MANIFEST_FULL_PATH;

public class TenantUrlFilter extends OncePerRequestFilter {
	public static final String TENANT_NAME_URL = "TENANT_NAME_URL";
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private final AntPathMatcher antPathMatcher = new AntPathMatcher("/");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		String path = request.getServletPath();

		if (!path.startsWith(TenantController.TENANT_BASE_PATH + "/")) {
			filterChain.doFilter(request, response);
			return;
		}

		/*
		 * For Smart health links manifest retrieval, authentication is dealt with later
		 */
		if (antPathMatcher.match(PATIENT_MANIFEST_FULL_PATH, path)) {
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
