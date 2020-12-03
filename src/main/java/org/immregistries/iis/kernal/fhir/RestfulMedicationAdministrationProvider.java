package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.immregistries.iis.kernal.model.*;

import java.util.List;

public class RestfulMedicationAdministrationProvider implements IResourceProvider {
    protected Session dataSession = null;
    protected OrgAccess orgAccess = null;
    protected OrgMaster orgMaster = null;
    protected VaccinationMaster vaccinationMaster= null;
    private static SessionFactory factory;

    @Override
    public Class<MedicationAdministration> getResourceType() {
        return MedicationAdministration.class;
    }

    public static Session getDataSession() {
        if (factory == null) {
            factory = new AnnotationConfiguration().configure().buildSessionFactory();
        }
        return factory.openSession();
    }


    @Read()
    public MedicationAdministration getResourceById(RequestDetails theRequestDetails, @IdParam IdType theId) {
        MedicationAdministration medicationAdministration = null;
        Session dataSession = getDataSession();
        String id = theId.getIdPart();
        try {
            orgAccess = Authentication.authenticateOrgAccess(theRequestDetails,dataSession);
            {
                Query query = dataSession
                        .createQuery("from VaccinationMaster where orgReported = ? and vaccinationId= ?");
                query.setParameter(0, orgAccess.getOrg());
                query.setParameter(1, id);
                List<VaccinationMaster> vaccinationMasterList = query.list();
                if (vaccinationMasterList.size() > 0) {
                    vaccinationMaster = vaccinationMasterList.get(0);
                    medicationAdministration =  new MedicationAdministration();
                    medicationAdministration.setId(id);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataSession.close();
        }
        return medicationAdministration;
    }
}
