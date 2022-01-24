package org.immregistries.iis.kernal.fhir;

import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.servlet.ServletHelper;
import org.mindrot.jbcrypt.BCrypt;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;


@Interceptor
public class Authentication {

  /**
   * This interceptor implements HTTP Basic Auth, which specifies that
   * a username and password are provided in a header called Authorization.
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
  public static OrgAccess authenticateOrgAccess(RequestDetails theRequestDetails,
      Session dataSession) throws AuthenticationException {
    OrgMaster orgMaster;
    OrgAccess orgAccess = null;
    String authHeader = theRequestDetails.getHeader("Authorization");
    System.err.println();

    // The format of the header must be:
    // Authorization: Basic [base64 of username:password]
    if (authHeader == null || authHeader.startsWith("Basic ") == false) {
      throw new AuthenticationException("Missing or invalid Authorization header");
    }

    String base64 = authHeader.substring("Basic ".length());
    String base64decoded = new String(Base64.decodeBase64(base64));
    String[] parts = base64decoded.split(":");

    String facilityId = theRequestDetails.getTenantId();
    String userId = parts[0];
    String password = parts[1];
    return ServletHelper.authenticateOrgAccess(userId, password, facilityId, dataSession);
  }
}
