package org.immregistries.iis.kernal.model;

import junit.framework.TestCase;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import java.util.List;

public class TestVaccinationReported extends TestCase {
  public void test() {
    SessionFactory factory = new AnnotationConfiguration().configure().buildSessionFactory();
    Session dataSession = factory.openSession();

    Query query = dataSession.createQuery("from VaccinationReported");
    @SuppressWarnings("unchecked")
	List<VaccinationReported> vaccinationReportedList = query.list();
    for (VaccinationReported vaccinationReported : vaccinationReportedList) {
      System.out.println("--> vaccinationReported.getVaccineCvxCode() = "
          + vaccinationReported.getVaccineCvxCode());
    }

    dataSession.close();
  }
}
