package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationFlagsEnum;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthenticationException;
import org.hibernate.Session;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.immregistries.iis.kernal.servlet.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.interceptor.Interceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Interceptor
public class SessionAuthorizationInterceptor extends AuthorizationInterceptor {
	private static final Logger log = LoggerFactory.getLogger(SessionAuthorizationInterceptor.class);

	@Override
	public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
		ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
		HttpSession session = attr.getRequest().getSession(false); // true == allow create
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
				if (orgAccess == null) {
					throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
				}
			} else if (session != null && (authHeader ==null || authHeader.isBlank())) {
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
			return new RuleBuilder()
				.denyAll(authenticationException.getMessage())
				.build();
		}
		if (orgAccess.getOrg().getOrganizationName() != null) {
			log.info("Identification {}",orgAccess.getOrg().getOrganizationName());
			return new RuleBuilder()
				.allowAll().forTenantIds(orgAccess.getOrg().getOrganizationName())
				.build();
		}
		// By default, deny everything. This should never get hit, but it's
		// good to be defensive
		return new RuleBuilder()
			.denyAll("not identified")
			.build();
	}

//	private static final AtomicInteger ourInstanceCount = new AtomicInteger(0);
//	private final int myInstanceIndex;
//	private final String myRequestRuleListKey;
//
//	public SessionAuthorizationInterceptor() {
//		super();
//		this.myInstanceIndex = ourInstanceCount.incrementAndGet();
//		String var10001 = AuthorizationInterceptor.class.getName();
//		this.myRequestRuleListKey = var10001 + "_" + this.myInstanceIndex + "_RULELIST";
//	}

//	public Verdict applyRulesAndReturnDecision(RestOperationTypeEnum theOperation, RequestDetails theRequestDetails, IBaseResource theInputResource, IIdType theInputResourceId, IBaseResource theOutputResource, Pointcut thePointcut) {
//		List<IAuthRule> rules = (List)theRequestDetails.getUserData().get(this.myRequestRuleListKey);
//		if (rules == null) {
//			rules = this.buildRuleList(theRequestDetails);
//			theRequestDetails.getUserData().put(this.myRequestRuleListKey, rules);
//		}
//
//		Set<AuthorizationFlagsEnum> flags = this.getFlags();
//		ourLog.trace("Applying {} rules to render an auth decision for operation {}, theInputResource type={}, theOutputResource type={} ", new Object[]{rules.size(), theOperation, theInputResource != null && theInputResource.getIdElement() != null ? theInputResource.getIdElement().getResourceType() : "", theOutputResource != null && theOutputResource.getIdElement() != null ? theOutputResource.getIdElement().getResourceType() : ""});
//		Verdict verdict = null;
//		Iterator var10 = rules.iterator();
//
//		while(var10.hasNext()) {
//			IAuthRule nextRule = (IAuthRule)var10.next();
//			ourLog.trace("Rule being applied - {}", nextRule);
//			verdict = nextRule.applyRule(theOperation, theRequestDetails, theInputResource, theInputResourceId, theOutputResource, this, flags, thePointcut);
//			if (verdict != null) {
//				ourLog.trace("Rule {} returned decision {}", nextRule, verdict.getDecision());
//				break;
//			}
//		}
//
//		if (verdict == null) {
//			ourLog.trace("No rules returned a decision, applying default {}", this.myDefaultPolicy);
//			return new Verdict(this.getDefaultPolicy(), (IAuthRule)null);
//		} else {
//			return verdict;
//		}
//	}
}
