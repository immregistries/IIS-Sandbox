package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.AuthenticationException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hl7.fhir.r4.model.Group;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
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
import java.util.Iterator;
import java.util.List;

@Component
@Interceptor
public class SessionAuthorizationInterceptor extends AuthorizationInterceptor {
	private static final Logger logger = LoggerFactory.getLogger(SessionAuthorizationInterceptor.class);
	public static final String CONNECTATHON_USER = "Connectathon";
	private static final String CONNECTATHON_AUTH = "78q3gb#QPGK!FmHKrgJjzkbpSCtiUtlchoClU1pC/UCdKxZ=PhRgtsL!4att8/6QKrUe1gS?p2ME!ixXP0Sg5lWnHP6t=U=6zeJXWnILR-BLc8HxVsfrLhp5/1q-DXuk?ljL?zwqJxB=we0SDKlT2j8WgNEkalit7Sf35F/R8W-QtrFbyO9IZPXJ1172OzvwfJBq-m9Z10DbSxIA?6f=3e!H7TLg/DwHByVlUSlZ6HWrytJkOFXljk9!z/BPrb9H";
	private static final String DEFAULT_USER = "DEFAULT";

	@Override
	public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		HttpSession session = request.getSession(false);
		Session dataSession = PopServlet.getDataSession();
		String authHeader = theRequestDetails.getHeader("Authorization");
		OrgAccess orgAccess = null;
		try {
			if (theRequestDetails.getTenantId().equals(CONNECTATHON_USER)) {
				/**
				 *	If connecting as Connectathon with TOKEN : give only specific rights
				 * Else : treat as usual
				 */
				if (authHeader != null && authHeader.startsWith("Bearer " + CONNECTATHON_AUTH)) {
					Query query = dataSession.createQuery("from OrgMaster where organizationName = ?1");
					query.setParameter(1, CONNECTATHON_USER);
					Iterator<OrgMaster> orgMaster = query.iterate();
					if (orgMaster.hasNext()) {
						Query queryAccess = dataSession.createQuery("from OrgAccess where org = ?0");
						queryAccess.setParameter(0, orgMaster.next());
						Iterator<OrgAccess> orgAccessIterator = queryAccess.iterate();
						if (orgAccessIterator.hasNext()) {
							orgAccess = orgAccessIterator.next();
							orgAccess.setAccessKey(CONNECTATHON_AUTH);
							orgAccess.setAccessName(null);
							orgAccess.setOrgAccessId(-1);
							theRequestDetails.setAttribute("orgAccess", orgAccess);
//							session.setAttribute("orgAccess", orgAccess);
							return new RuleBuilder()
								.allow().operation()
								.named(JpaConstants.OPERATION_EXPORT).atAnyLevel()
								.andAllowAllResponses().forTenantIds(CONNECTATHON_USER)
								.andThen().allow().operation()
								.named(JpaConstants.OPERATION_EXPORT_POLL_STATUS).atAnyLevel()
								.andAllowAllResponses().forTenantIds(CONNECTATHON_USER)
								.andThen().allow().operation()
								.named(JpaConstants.OPERATION_EVERYTHING).atAnyLevel()
								.andAllowAllResponses().forTenantIds(CONNECTATHON_USER)

								.andThen().allow().operation().named(JpaConstants.OPERATION_EVERYTHING)
								.atAnyLevel().andAllowAllResponses().forTenantIds(CONNECTATHON_USER)

								.andThen().allow().read()
								.resourcesOfType("Group").withAnyId().forTenantIds(CONNECTATHON_USER)
								.andThen().allow().read()
								.resourcesOfType("Immunization").withAnyId().forTenantIds(CONNECTATHON_USER)
								.andThen().allow().read()
								.resourcesOfType("Patient").withAnyId().forTenantIds(CONNECTATHON_USER)
								.andThen().allow().read()
								.resourcesOfType("Binary").withAnyId().forTenantIds(CONNECTATHON_USER)

								.andThen().allow()
								.bulkExport().any()
								.withResourceTypes(Lists.newArrayList("Patient", "Immunization", "RelatedPerson"))

								.build();
							// TODO Make list of allowed Binary read, right now every binary is accessible
						}
					}
				}
			}
			orgAccess = tryAuthHeader(authHeader, theRequestDetails.getTenantId(), dataSession);
			if (orgAccess != null && session != null) { // If connection with authHeader was successful
				session.setAttribute("orgAccess", orgAccess);
			} else if (authHeader == null && session != null && session.getAttribute("orgAccess") != null) { //If no header specified and there is a valid session running
				orgAccess = (OrgAccess) session.getAttribute("orgAccess");
			}

			if (orgAccess == null) {
				throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
			}
		} catch (AuthenticationException authenticationException) {
			// TODO raise issue or figure why examples are wrong on overriding and exceptions
			return new RuleBuilder()
				.denyAll(authenticationException.getMessage())
				.build();
		} finally {
			dataSession.close();
		}

		if (orgAccess.getOrg().getOrganizationName() != null) {
			theRequestDetails.setAttribute("orgAccess", orgAccess);
			return new RuleBuilder()
				.allow().read()
				.resourcesOfType("Subscription").withAnyId().forTenantIds(DEFAULT_USER)
				.andThen().allow().read()
				.resourcesOfType("SubscriptionTopic").withAnyId().forTenantIds(DEFAULT_USER)
				.andThen()
				.allowAll("Logged in as " + orgAccess.getOrg().getOrganizationName())
				.forTenantIds(orgAccess.getOrg().getOrganizationName())
				.build();
		}
		return new RuleBuilder()
			.denyAll("Missing or invalid Authorization header value")
			.build();
	}

	public OrgAccess tryAuthHeader(String authHeader, String tenantId, Session dataSession) {
		if (authHeader != null && authHeader.startsWith("Basic ")) {
			String base64 = authHeader.substring("Basic ".length());
			String base64decoded = new String(Base64.decodeBase64(base64));
			String[] parts = base64decoded.split(":");
			return ServletHelper.authenticateOrgAccess(parts[0], parts[1], tenantId, dataSession);
		} else { // TODO token ?
			return null;
		}
	}
}
