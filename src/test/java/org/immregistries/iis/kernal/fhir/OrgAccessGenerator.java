package org.immregistries.iis.kernal.fhir;

import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;

public class OrgAccessGenerator {

  private static String PARAM_USERID = "TELECOM NANCY";
  private static String PARAM_PASSWORD = "1234";
  private static String PARAM_FACILITYID = "TELECOMNANCY";
  private static SessionFactory factory;
  private static OrgAccess orgAccess= null;
  private static OrgMaster orgMaster = null;
  private static Session dataSession = null;

  public static OrgAccess getOrgAccess() {
    return orgAccess;
  }

  public static Session getDataSession() {
    return dataSession;
  }

  public static OrgMaster getOrgMaster() {
    return orgMaster;
  }

  public static void  authentification(){
    if (factory == null) {
    factory = new AnnotationConfiguration().configure("hibernate.h2.cfg.xml").buildSessionFactory();
  }
    dataSession =factory.openSession();
    try {
      if (orgAccess == null) {
      Query query = dataSession.createQuery("from OrgMaster where organizationName = ?");
      query.setParameter(0, PARAM_FACILITYID);
      List<OrgMaster> orgMasterList = query.list();
      if (orgMasterList.size() > 0) {
        orgMaster = orgMasterList.get(0);
      } else {
        orgMaster = new OrgMaster();
        orgMaster.setOrganizationName(PARAM_FACILITYID);
        orgAccess = new OrgAccess();
        orgAccess.setOrg(orgMaster);
        orgAccess.setAccessName(PARAM_USERID);
        orgAccess.setAccessKey(PARAM_PASSWORD);
        Transaction transaction = dataSession.beginTransaction();
        dataSession.save(orgMaster);
        dataSession.save(orgAccess);
        transaction.commit();
      }
    }

    if (orgAccess == null) {
      Query query = dataSession
          .createQuery("from OrgAccess where accessName = ? and accessKey = ? and org = ?");
      query.setParameter(0, PARAM_USERID);
      query.setParameter(1, PARAM_PASSWORD);
      query.setParameter(2, orgMaster);
      List<OrgAccess> orgAccessList = query.list();
      if (orgAccessList.size() != 0) {
        orgAccess = orgAccessList.get(0);
      }
    }
  } catch (Exception e) {
    e.printStackTrace();
  } finally {
    //dataSession.close();
  }
  //dataSession= factory.openSession();


}


}
