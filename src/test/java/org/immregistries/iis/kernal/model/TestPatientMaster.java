package org.immregistries.iis.kernal.model;

import junit.framework.TestCase;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import java.util.List;

public class TestPatientMaster extends TestCase {
  public void test() {
    SessionFactory factory = new AnnotationConfiguration().configure().buildSessionFactory();
    Session dataSession = factory.openSession();

    Query query = dataSession.createQuery("from PatientMaster");
    List<PatientMaster> patientMasterList = query.list();
    for (PatientMaster patientMaster : patientMasterList) {
      System.out.println(
          "--> patientMaster.getPatientNameLast() = " + patientMaster.getPatientNameLast());
    }

    dataSession.close();
  }
}
