package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hl7.fhir.r4.model.*;
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
        System.err.println(id);
        System.err.println(Integer.parseInt(id));
        try {
            orgAccess = Authentication.authenticateOrgAccess(theRequestDetails,dataSession);
            {
                Query query = dataSession
                        .createQuery("from VaccinationMaster where vaccinationId= ?");
                //query.setParameter(0, orgAccess.getOrg());
                query.setParameter(0, Integer.parseInt(id));
                List<VaccinationMaster> vaccinationMasterList = query.list();
                if (vaccinationMasterList.size() > 0) {
                    vaccinationMaster = vaccinationMasterList.get(0);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataSession.close();
        }

        if (vaccinationMaster != null){
            medicationAdministration =  new MedicationAdministration();
            medicationAdministration.setId(id);
            medicationAdministration.setEffective(new DateTimeType(vaccinationMaster.getAdministeredDate()));
            medicationAdministration.setSubject( new Reference("Patient/" + vaccinationMaster.getPatient().getPatientId()));
            //medicationAdministration.setMedication(new CodeType(vaccinationMaster.getVaccineCvxCode()));
            Extension links = new Extension("#links");
            Extension link = new Extension();
            link.setValue(new CodeType("level1")).setUrl("Immunization/"+vaccinationMaster.getVaccinationId());
            links.addExtension(link);
            medicationAdministration.addExtension(links);
        }
        return medicationAdministration;
    }
}
