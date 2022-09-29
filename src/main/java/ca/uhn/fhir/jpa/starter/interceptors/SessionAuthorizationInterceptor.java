package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.AuthenticationException;
import org.hibernate.Session;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.immregistries.iis.kernal.servlet.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.interceptor.Interceptor;
import javax.servlet.http.HttpSession;
import java.util.List;

@Component
@Interceptor
public class SessionAuthorizationInterceptor extends AuthorizationInterceptor {
	private static final Logger log = LoggerFactory.getLogger(SessionAuthorizationInterceptor.class);
	@Override
	public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
		HttpSession session;
		ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
		session = attr.getRequest().getSession(false); // true == allow create
//		try {
//
//		} catch (NoClassDefFoundError | ExceptionInInitializerError error) {
//			session = null;
//		}
		Session dataSession = PopServlet.getDataSession();

		String authHeader = theRequestDetails.getHeader("Authorization");
		OrgAccess orgAccess;
		try {
			if(authHeader != null && authHeader.startsWith("Basic ")) {
				String base64 = authHeader.substring("Basic ".length());
				String base64decoded = new String(Base64.decodeBase64(base64));
				String[] parts = base64decoded.split(":");
				String facilityId = theRequestDetails.getTenantId();
				String userId = parts[0];
				String password = parts[1];
				orgAccess = ServletHelper.authenticateOrgAccess(userId, password, facilityId, dataSession);
				if (session != null) {
					session.setAttribute("orgAccess",orgAccess);
				}
				if (orgAccess == null) {
					throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
				}
			} else if (authHeader.isBlank() && session != null) {
				orgAccess = (OrgAccess) session.getAttribute("orgAccess");
				if (orgAccess == null) {
					throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
				}
			} else { // TODO add oAuth system
				// Throw an HTTP 401
				throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
			}
		}
		catch (AuthenticationException authenticationException) {
			// TODO raise issue or figure why examples are wrong on overriding and exceptions
			log.error(authenticationException.getMessage());
			return new RuleBuilder()
				.denyAll()
				.build();
		}
		if (orgAccess.getOrg().getOrganizationName()!=null) {
			log.info("Identification {}",orgAccess.getOrg().getOrganizationName());
			return new RuleBuilder()
				.allowAll().forTenantIds(orgAccess.getOrg().getOrganizationName())
				.build();
		}
		// By default, deny everything. This should never get hit, but it's
		// good to be defensive
		return new RuleBuilder()
			.denyAll()
			.build();
	}
}
