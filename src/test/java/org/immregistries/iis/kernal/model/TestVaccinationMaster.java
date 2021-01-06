package org.immregistries.iis.kernal.model;

import junit.framework.TestCase;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

import java.util.List;

public class TestVaccinationMaster extends TestCase {
  public void test() {
    SessionFactory factory = new AnnotationConfiguration().configure().buildSessionFactory();
    Session dataSession = factory.openSession();

    Query query = dataSession.createQuery("from VaccinationMaster");
    @SuppressWarnings("unchecked")
	List<VaccinationMaster> vaccinationMasterList = query.list();
    for (VaccinationMaster vaccinationMaster : vaccinationMasterList) {
      System.out
          .println("--> vaccinationMaster.getVaccinationCvxCode() = " + vaccinationMaster.getVaccineCvxCode());
    }

    dataSession.close();
  }
}
