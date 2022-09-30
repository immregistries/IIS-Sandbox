package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.impl.RestfulClientFactory;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.repository.RepositoryClientFactory;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpSession;
import java.util.List;

public class ServletHelper {
  private static String BAD_PASSWORD = "badpassword";

  public static OrgAccess authenticateOrgAccess(
      String userId, String password, String facilityId, Session dataSession) {

    if (BAD_PASSWORD.equals(password)) {
      return null;
    }
    OrgMaster orgMaster = null;
    OrgAccess orgAccess = null;

	 Query query = dataSession.createQuery("from OrgMaster where organizationName = ?1");
	 query.setParameter(1, facilityId);

	 List<OrgMaster> orgMasterList = query.list();
    if (orgMasterList.size() > 0) {
      orgMaster = orgMasterList.get(0);
    } else {
      orgMaster = new OrgMaster();
      orgMaster.setOrganizationName(facilityId);
      orgAccess = new OrgAccess();
      orgAccess.setOrg(orgMaster);
      orgAccess.setAccessName(userId);
//      orgAccess.setAccessKey(BCrypt.hashpw(password, BCrypt.gensalt(5))); TODO after auth checks fix in fhir
      orgAccess.setAccessKey(password);
      Transaction transaction = dataSession.beginTransaction();
      dataSession.save(orgMaster);
      dataSession.save(orgAccess);
      transaction.commit();
    }

    if (orgAccess == null) {
      orgAccess = authenticateOrgAccessForFacility(userId, password, dataSession, orgMaster);
    }
    return orgAccess;
  }

  public static OrgAccess authenticateOrgAccessForFacility(
      String userId, String password, Session dataSession, OrgMaster orgMaster) {
    OrgAccess orgAccess = null;
    String facilityId = orgMaster.getOrganizationName();
    String queryString = "from OrgAccess where accessName = ?0 and org = ?1";
    Query query = dataSession.createQuery(queryString);
    query.setParameter(0, userId);
    query.setParameter(1, orgMaster);

    @SuppressWarnings("unchecked")
    List<OrgAccess> orgAccessList = query.list();
    if (orgAccessList.size() != 0) {
//      if (BCrypt.checkpw(password, orgAccessList.get(0).getAccessKey())) { TODO after auth checks fix in fhir
      if (password.equals(orgAccessList.get(0).getAccessKey())) {
        orgAccess = orgAccessList.get(0);
      } else {
        throw new AuthenticationException("password for ID : " + facilityId);
      }
    } else {
      throw new AuthenticationException("password for ID : " + facilityId);
    }
    
    return orgAccess;
  }

  public static IGenericClient getFhirClient(HttpSession session, RepositoryClientFactory repositoryClientFactory) {
	  if (session.getAttribute("fhirClient") == null) {
		  OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
		  session.setAttribute("fhirClient", repositoryClientFactory.newGenericClient(orgAccess));
	  }
	  return (IGenericClient) session.getAttribute("fhirClient");

  }
}
