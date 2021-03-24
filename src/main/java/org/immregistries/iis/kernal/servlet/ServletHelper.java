package org.immregistries.iis.kernal.servlet;

import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;

public class ServletHelper {

  private static String BAD_PASSWORD = "badpassword";

  public static OrgAccess authenticateOrgAccess(
      String userId, String password, String facilityId, Session dataSession) {

    if (BAD_PASSWORD.equals(password)) {
      return null;
    }
    OrgMaster orgMaster = null;
    OrgAccess orgAccess = null;

    Query query = dataSession.createQuery("from OrgMaster where organizationName = ?");
    query.setParameter(0, facilityId);

    @SuppressWarnings("unchecked")
    List<OrgMaster> orgMasterList = query.list();
    if (orgMasterList.size() > 0) {
      orgMaster = orgMasterList.get(0);
    } else {
      orgMaster = new OrgMaster();
      orgMaster.setOrganizationName(facilityId);
      orgAccess = new OrgAccess();
      orgAccess.setOrg(orgMaster);
      orgAccess.setAccessName(userId);
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
    String queryString = "from OrgAccess where accessName = ? and accessKey = ? and org = ?";
    Query query = dataSession.createQuery(queryString);
    query.setParameter(0, userId);
    query.setParameter(1, password);
    query.setParameter(2, orgMaster);

    @SuppressWarnings("unchecked")
    List<OrgAccess> orgAccessList = query.list();
    if (orgAccessList.size() != 0) {
      orgAccess = orgAccessList.get(0);
    }
    return orgAccess;
  }
}
