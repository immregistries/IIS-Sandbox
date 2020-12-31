package org.immregistries.iis.kernal.model;

import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import junit.framework.TestCase;

public class TestOrgMaster extends TestCase {
  public void test() {
    SessionFactory factory = new AnnotationConfiguration().configure().buildSessionFactory();
    Session dataSession = factory.openSession();

    Query query = dataSession.createQuery("from OrgMaster");
    List<OrgMaster> orgMasterList = query.list();
    for (OrgMaster orgMaster : orgMasterList) {
      System.out
          .println("--> orgMaster.getOrganizationName() = " + orgMaster.getOrganizationName());
    }

    dataSession.close();
  }
}
