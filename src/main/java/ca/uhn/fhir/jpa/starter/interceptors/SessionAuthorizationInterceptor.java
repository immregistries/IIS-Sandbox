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
		HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession(false);
		Session dataSession = PopServlet.getDataSession();
		// TODO add secure verification to see if request originates from HL7v2 IIS sandbox functionalities

		String authHeader = theRequestDetails.getHeader("Authorization");
		OrgAccess orgAccess;
		try {
			if(authHeader != null && authHeader.startsWith("Basic ")) {
				String base64 = authHeader.substring("Basic ".length());
				String base64decoded = new String(Base64.decodeBase64(base64));
				String[] parts = base64decoded.split(":");
				orgAccess = ServletHelper.authenticateOrgAccess( parts[0], parts[1], theRequestDetails.getTenantId(), dataSession);
				if (session != null) {
					session.setAttribute("orgAccess",orgAccess);
				}
			} else if (session != null) {
				orgAccess = (OrgAccess) session.getAttribute("orgAccess");
			} else { // TODO add oAuth system
				throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
			}
			if (orgAccess == null) {
				throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
			}
		}
		catch (AuthenticationException authenticationException) {
			// TODO raise issue or figure why examples are wrong on overriding and exceptions
			return new RuleBuilder()
				.denyAll(authenticationException.getMessage())
				.build();
		}
		if (orgAccess.getOrg().getOrganizationName() != null) {
			return new RuleBuilder()
				.allow().read().resourcesOfType("Subscription").withAnyId().forTenantIds("DEFAULT").andThen()
				.allowAll("Logged in as " + orgAccess.getOrg().getOrganizationName()).forTenantIds(orgAccess.getOrg().getOrganizationName())
				.build();
		}
		return new RuleBuilder()
			.denyAll("Missing or invalid Authorization header value")
			.build();
	}
}
