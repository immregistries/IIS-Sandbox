package org.immregistries.iis.kernal.model;

import junit.framework.TestCase;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

import java.util.List;

public class TestPatientReported extends TestCase {
  public void test() {
    SessionFactory factory = new AnnotationConfiguration().configure().buildSessionFactory();
    Session dataSession = factory.openSession();

    Query query = dataSession.createQuery("from PatientReported");
    List<PatientReported> patientReportedList = query.list();
    for (PatientReported patientReported : patientReportedList) {
      System.out
          .println("--> patientReported.getReportedMrn() = " + patientReported.getReportedMrn());
    }

    dataSession.close();
  }
}
