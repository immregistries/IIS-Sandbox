package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.*;

import java.util.Collections;
import java.util.List;

/**
 * All resource providers must implement IResourceProvider
 */
public class RestfulPatientResourceProvider implements IResourceProvider {

    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }

    /**
     * The "@Read" annotation indicates that this method supports the
     * read operation. Read operations should return a single resource
     * instance.
     *
     * @param theId
     *    The read operation takes one parameter, which must be of type
     *    IdType and must be annotated with the "@Read.IdParam" annotation.
     * @return
     *    Returns a resource matching this identifier, or null if none exists.
     */
    @Read()
    public Patient getResourceById(@IdParam IdType theId) {
        Patient patient = new Patient();
        patient.addIdentifier();
        patient.getIdentifier().get(0).setSystem(String.valueOf(new UriDt("urn:hapitest:mrns")));
        patient.getIdentifier().get(0).setValue("00002");
        patient.addName().setFamily("Test");
        patient.getName().get(0).addGiven("PatientOne");
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        return patient;
    }

    /**
     * The "@Search" annotation indicates that this method supports the
     * search operation. You may have many different methods annotated with
     * this annotation, to support many different search criteria. This
     * example searches by family name.
     *
     * @param theFamilyName
     *    This operation takes one parameter which is the search criteria. It is
     *    annotated with the "@Required" annotation. This annotation takes one argument,
     *    a string containing the name of the search criteria. The datatype here
     *    is StringParam, but there are other possible parameter types depending on the
     *    specific search criteria.
     * @return
     *    This method returns a list of Patients. This list may contain multiple
     *    matching resources, or it may also be empty.
     */
    @Search()
    public List<Patient> getPatient(@RequiredParam(name = Patient.SP_FAMILY) StringParam theFamilyName) {
        Patient patient = new Patient();
        patient.addIdentifier();
        patient.getIdentifier().get(0).setUse(Identifier.IdentifierUse.OFFICIAL);
        patient.getIdentifier().get(0).setSystem(String.valueOf(new UriDt("urn:hapitest:mrns")));
        patient.getIdentifier().get(0).setValue("00001");
        patient.addName();
        patient.getName().get(0).setFamily(theFamilyName.getValue());
        patient.getName().get(0).addGiven("PatientOne");
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        return Collections.singletonList(patient);
    }

    @Create
    public MethodOutcome createPatient(@ResourceParam Patient thePatient) {

        /*
         * First we might want to do business validation. The UnprocessableEntityException
         * results in an HTTP 422, which is appropriate for business rule failure
         */
        if (thePatient.getIdentifierFirstRep().isEmpty()) {
            /* It is also possible to pass an OperationOutcome resource
             * to the UnprocessableEntityException if you want to return
             * a custom populated OperationOutcome. Otherwise, a simple one
             * is created using the string supplied below.
             */
            throw new UnprocessableEntityException("No identifier supplied");
        }

        // Save this patient to the database...

        //savePatientToDatabase(thePatient);

        // This method returns a MethodOutcome object which contains
        // the ID (composed of the type Patient, the logical ID 3746, and the
        // version ID 1)
        MethodOutcome retVal = new MethodOutcome();
        retVal.setId(new IdType("Patient", "3746", "1"));

        // You can also add an OperationOutcome resource to return
        // This part is optional though:
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue().setDiagnostics("One minor issue detected");
        retVal.setOperationOutcome(outcome);

        return retVal;
    }

}
