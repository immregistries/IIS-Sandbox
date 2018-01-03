package org.immregistries.iis.kernal.model;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

import junit.framework.TestCase;

/**
 * Created by Eric on 12/20/17.
 */
public class TestOrgAccess extends TestCase {
    public void test() {
        SessionFactory factory = new AnnotationConfiguration().configure().buildSessionFactory();
        Session dataSession = factory.openSession();

        Query query = dataSession.createQuery("from OrgAccess");
        List<OrgAccess> orgAccessList = query.list();
        for (OrgAccess orgAccess : orgAccessList) {
            System.out
                    .println("--> orgAccess.getAccessName() = " + orgAccess.getAccessName());
        }

        dataSession.close();
    }
}
