package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.AuthenticationException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.immregistries.iis.kernal.model.UserAccess;
import org.immregistries.iis.kernal.model.Tenant;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.interceptor.Interceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.List;

import static org.immregistries.iis.kernal.fhir.security.ServletHelper.SESSION_USER_ACCESS;
import static org.immregistries.iis.kernal.fhir.security.ServletHelper.SESSION_TENANT;

@Component
@Interceptor
public class SessionAuthorizationInterceptor extends AuthorizationInterceptor {
	public static final String CONNECTATHON_USER = "Connectathon";
	private static final Logger logger = LoggerFactory.getLogger(SessionAuthorizationInterceptor.class);
	private static final String CONNECTATHON_AUTH = "78q3gb#QPGK!FmHKrgJjzkbpSCtiUtlchoClU1pC/UCdKxZ=PhRgtsL!4att8/6QKrUe1gS?p2ME!ixXP0Sg5lWnHP6t=U=6zeJXWnILR-BLc8HxVsfrLhp5/1q-DXuk?ljL?zwqJxB=we0SDKlT2j8WgNEkalit7Sf35F/R8W-QtrFbyO9IZPXJ1172OzvwfJBq-m9Z10DbSxIA?6f=3e!H7TLg/DwHByVlUSlZ6HWrytJkOFXljk9!z/BPrb9H";
	private static final String DEFAULT_USER = "DEFAULT";

	@Override
	public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
		/**
		 * could be
		 * HttpServletRequest request = theRequestDetails.getRequest()
		 *
		 */
		HttpServletRequest request = ((ServletRequestDetails) theRequestDetails).getServletRequest();
		HttpSession session = request.getSession(false);
		Session dataSession = PopServlet.getDataSession();
		String authHeader = theRequestDetails.getHeader("Authorization");
		Tenant tenant = null;
		try {
//			if (PartitionCreationInterceptor.extractPartitionName(theRequestDetails).equals(CONNECTATHON_USER)) {
//				return connectathonSpecialUser(theRequestDetails,authHeader,dataSession);
//			}
			/**
			 * Checking Auth header else SESSION Cookie
			 */
			if (authHeader!= null) {
				/**
				 * Basic auth
				 */
				tenant = tryAuthHeader(authHeader, PartitionCreationInterceptor.extractPartitionName(theRequestDetails), dataSession);
				/**
				 * Token bearer ?
				 */
				if (tenant == null){
					// TODO TOKEN VERIFICATION
//					logger.info("token {} ", authHeader);
				}
			} else {
				/**
				 * Cookie SESSIONID
				 */
				if (session != null) {
					UserAccess userAccess = ServletHelper.getUserAccess();
					/**
					 * if user authenticated, Tenant/Facility is then selected
					 */
					if (userAccess != null) {
						tenant = ServletHelper.authenticateTenant(userAccess,PartitionCreationInterceptor.extractPartitionName(theRequestDetails),dataSession);
					}
				}
			}

			if (session != null ) {
//				session.setAttribute(SESSION_ORGMASTER, null); // Tenant selection is set in requestDetails
//				session.setAttribute(SESSION_ORGMASTER, tenant); // TODO MAYBE REMOVE
			}
			if (tenant == null) {
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

		if (tenant.getOrganizationName() != null) {
			theRequestDetails.setAttribute(SESSION_TENANT, tenant);
			return new RuleBuilder()
				.allow().read()
				.resourcesOfType("Subscription").withAnyId().forTenantIds(DEFAULT_USER)
				.andThen().allow().read()
				.resourcesOfType("SubscriptionTopic").withAnyId().forTenantIds(DEFAULT_USER)
				.andThen()
				.allowAll("Logged in as " + tenant.getOrganizationName())
				.forTenantIds(tenant.getOrganizationName())
				.build();
		}
		return new RuleBuilder()
			.denyAll("Missing or invalid Authorization header value")
			.build();
	}

	public Tenant tryAuthHeader(String authHeader, String tenantId, Session dataSession) {
		if (authHeader != null && authHeader.startsWith("Basic ")) {
			String base64 = authHeader.substring("Basic ".length());
			String base64decoded = new String(Base64.decodeBase64(base64));
			String[] parts = base64decoded.split(":");
			return ServletHelper.authenticateTenant(parts[0], parts[1], tenantId, dataSession);
		} else { // TODO token ?
			return null;
		}
	}

	private List<IAuthRule> connectathonSpecialUser(RequestDetails theRequestDetails,String authHeader, Session dataSession) {
		/**
		 *	If connecting as Connectathon with TOKEN : give only specific rights
		 * Else : treat as usual
		 */
		UserAccess userAccess = null;
		if (authHeader != null && authHeader.startsWith("Bearer " + CONNECTATHON_AUTH)) {
			Query query = dataSession.createQuery("from Tenant where organizationName = ?1");
			query.setParameter(1, CONNECTATHON_USER);
			Iterator<Tenant> tenant = query.iterate();
			if (tenant.hasNext()) {
				Query queryAccess = dataSession.createQuery("from UserAccess where org = ?0");
				queryAccess.setParameter(0, tenant.next());
				Iterator<UserAccess> userAccessIterator = queryAccess.iterate();
				if (userAccessIterator.hasNext()) {
					userAccess = userAccessIterator.next();
					userAccess.setAccessKey(CONNECTATHON_AUTH);
					userAccess.setAccessName(null);
					userAccess.setUserAccessId(-1);
					theRequestDetails.setAttribute(SESSION_USER_ACCESS, userAccess);
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

						.andThen().allow().operation()
						.named("$match").atAnyLevel()
						.andAllowAllResponses().forTenantIds(CONNECTATHON_USER)

//								.andThen().allow().operation().withAnyName().atAnyLevel().andAllowAllResponses().forTenantIds(CONNECTATHON_USER)
						.andThen().allow().operation().withAnyName().atAnyLevel().andAllowAllResponses().forTenantIds(CONNECTATHON_USER)
//								.andThen().allow().operation().named("$member-remove").atAnyLevel().andAllowAllResponses().forTenantIds(CONNECTATHON_USER)


						.andThen().allow().operation().named(JpaConstants.OPERATION_EVERYTHING)
						.atAnyLevel().andAllowAllResponses().forTenantIds(CONNECTATHON_USER)

						.andThen().allow().read()
						.resourcesOfType("Group").withAnyId().forTenantIds(CONNECTATHON_USER)
						.andThen().allow().create()
						.resourcesOfType("Group").withAnyId().forTenantIds(CONNECTATHON_USER)
						.andThen().allow().read()
						.resourcesOfType("Immunization").withAnyId().forTenantIds(CONNECTATHON_USER)
						.andThen().allow().create()
						.resourcesOfType("Immunization").withAnyId().forTenantIds(CONNECTATHON_USER)
						.andThen().allow().read()
						.resourcesOfType("Patient").withAnyId().forTenantIds(CONNECTATHON_USER)
						.andThen().allow().create()
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
		return null;
	}
}
