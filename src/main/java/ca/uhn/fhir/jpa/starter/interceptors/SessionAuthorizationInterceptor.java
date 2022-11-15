package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpRequest;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@Component
@Interceptor
public class SessionAuthorizationInterceptor extends AuthorizationInterceptor {
	private static final Logger log = LoggerFactory.getLogger(SessionAuthorizationInterceptor.class);
	private static final String key = "wertyuhkjbasv!#$GFRqer678GaefgAgdf:[rW4r5ty1gv2y1532efu1yeb1 k!@$534t"; // TODO automatic generation at start and chang regularly


	@Override
	public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
		HttpServletRequest request =  ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		HttpSession session = request.getSession(false);

		Session dataSession = PopServlet.getDataSession();

		String authHeader = theRequestDetails.getHeader("Authorization");
		OrgAccess orgAccess = null;
		try {
			if (authHeader != null && authHeader.startsWith("Bearer " + "Inside-job " + key)) { // TODO set random hidden key generation
				return new RuleBuilder()
					.allowAll("Self made request") // TODO use tenant id in header
					.build();
			} else if (authHeader != null && authHeader.startsWith("Basic ")) {
				String base64 = authHeader.substring("Basic ".length());
				String base64decoded = new String(Base64.decodeBase64(base64));
				String[] parts = base64decoded.split(":");
				orgAccess = ServletHelper.authenticateOrgAccess( parts[0], parts[1], theRequestDetails.getTenantId(), dataSession);
				session.setAttribute("orgAccess",orgAccess);
			} else if (session != null && session.getAttribute("orgAccess") != null) {
				orgAccess = (OrgAccess) session.getAttribute("orgAccess");
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
				.allow()
				.read()
				.resourcesOfType("Subscription").withAnyId().forTenantIds("DEFAULT")
				.andThen()
				.allowAll("Logged in as " + orgAccess.getOrg().getOrganizationName())
				.forTenantIds(orgAccess.getOrg().getOrganizationName())
				.build();
		}

		return new RuleBuilder()
			.denyAll("Missing or invalid Authorization header value")
			.build();
	}
}
