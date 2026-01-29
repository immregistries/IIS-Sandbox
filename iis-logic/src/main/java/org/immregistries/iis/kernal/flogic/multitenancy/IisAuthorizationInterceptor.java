package org.immregistries.iis.kernal.flogic.multitenancy;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRuleFinished;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import jakarta.interceptor.Interceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.Strings;
import org.apache.http.auth.AuthenticationException;
import org.immregistries.iis.kernal.GlobalConstants;
import org.immregistries.iis.kernal.fhir.IisFhirInterceptor;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.persisted.repository.TenantRepository;
import org.immregistries.iis.kernal.persisted.repository.UserAccessRepository;
import org.immregistries.iis.kernal.security.JwtUtils;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.immregistries.iis.kernal.security.UserAccessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static org.immregistries.iis.kernal.GlobalConstants.SESSION_REQUEST_TENANT;
import static org.immregistries.iis.kernal.security.UserAccessUtil.SESSION_USER_ACCESS;

/**
 * Interceptor dealing with Authorization of FHIR Requests, allowing several
 * ways including Session cookie, Basic Auth, Token bearer currently only for
 * specific usage
 */
@Component
@Interceptor
public class IisAuthorizationInterceptor extends AuthorizationInterceptor implements IisFhirInterceptor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final String BEARER_PREFIX = "Bearer ";

	@Autowired
	private JwtUtils jwtUtils;
	@Autowired
	private UserAccessRepository userAccessRepository;
	@Autowired
	private TenantRepository tenantRepository;
	@Autowired
	private TenantAuthService tenantAuthService;

	/**
	 * Authenticates request with Session cookie, Basic Auth (Token bearer currently
	 * only for specific usage ) and produces HAPI FHIR Authorization rules
	 *
	 * @param theRequestDetails
	 * @return HAPI FHIR Authorization rules
	 */
	@Override
	public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
		/*
		 * could be
		 * HttpServletRequest request = theRequestDetails.getRequest()
		 */
		HttpServletRequest request = ((ServletRequestDetails) theRequestDetails).getServletRequest();
		HttpSession httpSession = request.getSession(false);
		String authHeader = theRequestDetails.getHeader("Authorization");
		Tenant tenant = null;
		try {
			if (PartitionTenantCreationInterceptor.extractPartitionName(theRequestDetails).equals(GlobalConstants.CONNECTATHON_USER)) {
				if (theRequestDetails.getTenantId().endsWith("Unsafe")) {
					return connectathonUserAuthorized(theRequestDetails).build();
				}
				List<IAuthRule> rules = connectathonSpecialUser(theRequestDetails, authHeader);
				if (rules != null) {
					return rules;
				}
			}
			/*
			 * Checking Auth header else SESSION Cookie
			 */
			if (authHeader != null) {
				/*
				 * Basic auth
				 */
				tenant = tryAuthHeaderBasic(authHeader,
						PartitionTenantCreationInterceptor.extractPartitionName(theRequestDetails));
				/*
				 * Token bearer TODO
				 */
			} else {
				/*
				 * Cookie SESSIONID
				 */
				if (httpSession != null) {
					UserAccess userAccess = UserAccessUtil.get().getUserAccess();
					/*
					 * if user authenticated, Tenant/Facility is then selected
					 */
					if (userAccess != null) {
						tenant = tenantAuthService.authenticateTenant(userAccess,
								PartitionTenantCreationInterceptor.extractPartitionName(theRequestDetails));
					}
				}
			}
			if (tenant == null) {
				throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
			}
		} catch (AuthenticationException authenticationException) {
			// TODO raise issue or figure why examples are wrong on overriding and
			// exceptions
			return new RuleBuilder()
					.denyAll(authenticationException.getMessage())
					.build();
		}

		if (tenant.getOrganizationName() != null) {
			theRequestDetails.setAttribute(SESSION_REQUEST_TENANT, tenant);
			return new RuleBuilder()
					.allow().read()
					.resourcesOfType("Subscription").withAnyId().forTenantIds(GlobalConstants.DEFAULT_USER)
					.andThen().allow().read()
					.resourcesOfType("SubscriptionTopic").withAnyId().forTenantIds(GlobalConstants.DEFAULT_USER)
					.andThen()
					.allowAll("Logged in as " + tenant.getOrganizationName())
					.forTenantIds(tenant.getOrganizationName())
					.build();
		}
		return new RuleBuilder()
				.denyAll("Missing or invalid Authorization header value")
				.build();
	}

	/**
	 * Basic Authentication Credentials extraction
	 *
	 * @param authHeader  HTTP Auth header
	 * @param tenantName  tenant name
	 * @return tenant object if authenticated, null if not recognized
	 */
	public Tenant tryAuthHeaderBasic(String authHeader, String tenantName) {
		if (Strings.CS.startsWith(authHeader, "Basic ")) {
			String base64 = authHeader.substring("Basic ".length());
			String base64decoded = new String(Base64.decodeBase64(base64));
			String[] parts = base64decoded.split(":");
			return tenantAuthService.authenticateTenant(parts[0], parts[1], tenantName);
		} else { // TODO token ?
			return null;
		}
	}

	/**
	 * Custom User config for Connectathon specific needs
	 * 
	 * @param theRequestDetails request details
	 * @param authHeader        HTTP authorization header
	 * @return Connectathon Authorization rules
	 */
	private List<IAuthRule> connectathonSpecialUser(RequestDetails theRequestDetails, String authHeader) {
		/*
		 * If connecting as Connectathon with TOKEN : give only specific rights
		 * Else : treat as usual
		 */
		if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
			String token = authHeader.split(BEARER_PREFIX)[1];
			if (jwtUtils.validateJwtToken(token) && jwtUtils.getUserNameFromJwtToken(token).equals(GlobalConstants.CONNECTATHON_USER)) {
				return connectathonUserAuthorized(theRequestDetails).build();
			}
		}
		return null;
	}

	/**
	 * Custom authorization rules for Connectathon specific needs
	 * 
	 * @param theRequestDetails request details
	 * @return Connectathon Authorization rules
	 */
	private IAuthRuleFinished connectathonUserAuthorized(RequestDetails theRequestDetails) {
		String tenantId = theRequestDetails.getTenantId();
		UserAccess userAccess;
		Optional<UserAccess> userAccessOptional = userAccessRepository.findByAccessName(GlobalConstants.CONNECTATHON_USER);
		if (userAccessOptional.isPresent()) {
			userAccess = userAccessOptional.get();
			Optional<Tenant> tenantOptional = tenantRepository.findByOrganizationName(GlobalConstants.CONNECTATHON_USER);
			if (tenantOptional.isPresent()) {
				Tenant tenant = tenantOptional.get();
				theRequestDetails.setAttribute(SESSION_USER_ACCESS, userAccess);
				theRequestDetails.setAttribute(SESSION_REQUEST_TENANT, tenant);
				return new RuleBuilder()
						.allow().read()
						.resourcesOfType("Subscription").withAnyId().forTenantIds(GlobalConstants.DEFAULT_USER)
						.andThen().allow().read()
						.resourcesOfType("SubscriptionTopic").withAnyId().forTenantIds(GlobalConstants.DEFAULT_USER)
						.andThen()
						.allowAll("Logged in as " + GlobalConstants.CONNECTATHON_USER)
						.forTenantIds(GlobalConstants.CONNECTATHON_USER, "ConnectathonUnsafe")
						.andThen().allow().read()
						.resourcesOfType("Binary").withAnyId()
						.forTenantIds(GlobalConstants.CONNECTATHON_USER, "ConnectathonUnsafe", "DEFAULT", "default");
				// return new RuleBuilder()
				// .allow().operation()
				// .named(JpaConstants.OPERATION_EXPORT).atAnyLevel()
				// .andAllowAllResponses().forTenantIds(tenantId)
				// .andThen().allow().operation()
				// .named(JpaConstants.OPERATION_EXPORT_POLL_STATUS).atAnyLevel()
				// .andAllowAllResponses().forTenantIds(tenantId)
				// .andThen().allow().operation()
				// .named(JpaConstants.OPERATION_EVERYTHING).atAnyLevel()
				// .andAllowAllResponses().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				//
				// .andThen().allow().operation()
				// .named("$match").atAnyLevel()
				// .andAllowAllResponses().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				// .andThen().allow().operation()
				// .named("$member-remove").onAnyType()
				// .andAllowAllResponses().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				// .andThen().allow().operation()
				// .named("$member-add").onAnyType()
				// .andAllowAllResponses().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				//
				//// .andThen().allow().operation().withAnyName().atAnyLevel().andAllowAllResponses().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				// .andThen().allow().operation().withAnyName().atAnyLevel().andAllowAllResponses().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				//// .andThen().allow().operation().named("$member-remove").atAnyLevel().andAllowAllResponses().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				//
				//
				// .andThen().allow().operation().named(JpaConstants.OPERATION_EVERYTHING)
				// .atAnyLevel().andAllowAllResponses().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				//
				// .andThen().allow().read()
				// .resourcesOfType("Group").withAnyId().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				// .andThen().allow().create()
				// .resourcesOfType("Group").withAnyId().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				// .andThen().allow().write()
				// .resourcesOfType("Group").withAnyId().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				// .andThen().allow().transaction().withAnyOperation().andApplyNormalRules().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				//// .resourcesOfType("Group").withAnyId().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				// .andThen().allow().operation().withAnyName().onAnyType().andAllowAllResponses().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				// .andThen().allow().read()
				// .resourcesOfType("Immunization").withAnyId().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				// .andThen().allow().read()
				// .resourcesOfType("Observation").withAnyId().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				// .andThen().allow().create()
				// .resourcesOfType("Immunization").withAnyId().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				// .andThen().allow().read()
				// .resourcesOfType("Patient").withAnyId().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				// .andThen().allow().create()
				// .resourcesOfType("Patient").withAnyId().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe")
				// .andThen().allow().read()
				// .resourcesOfType("Binary").withAnyId().forTenantIds(CONNECTATHON_USER,"ConnectathonUnsafe","DEFAULT",
				// "default")
				//
				// .andThen().allow()
				// .bulkExport().any()
				// .withResourceTypes(Lists.newArrayList("Patient", "Immunization",
				// "RelatedPerson"));
				// TODO Make list of allowed Binary read, right now every binary is accessible
			}
		}
		return null;
	}
}
