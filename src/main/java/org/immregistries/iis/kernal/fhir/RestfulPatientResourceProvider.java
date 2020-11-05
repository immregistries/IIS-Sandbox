package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.*;

import java.util.*;

/**
 * All resource providers must implement IResourceProvider
 */
public class RestfulPatientResourceProvider implements IResourceProvider {
    private Map<Long,Patient> myPatients = new HashMap<Long,Patient>();
    private long nextId=0;



    public RestfulPatientResourceProvider() {
        /*long patientId= nextId++;

        Patient patient = new Patient();
        patient.setId(Long.toString(patientId));
        patient.addIdentifier();
        patient.getIdentifier().get(0).setSystem(String.valueOf(new UriDt("urn:hapitest:mrns")));
        patient.getIdentifier().get(0).setValue("00002");
        patient.addName().setFamily("Achi");
        patient.getName().get(0).addGiven("Flavie");
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);

        myPatients.put(patientId,patient);*/

    }

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
        Patient patient= myPatients.get(theId.getIdPartAsLong());
        if(patient==null){
            throw new ResourceNotFoundException(theId);
        }
        return patient;
    }

    @Create
    public MethodOutcome createPatient(@ResourceParam Patient thePatient) {
        //TODO must add validation method later

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

        long id= nextId++;

        // Save this patient to the database...
        myPatients.put(id,thePatient);

        return new MethodOutcome(new IdType(id));
    }

    /**
     * The "@Update" annotation indicates that this method supports replacing an existing
     * resource (by ID) with a new instance of that resource.
     *
     * @param theId      This is the ID of the patient to update
     * @param thePatient This is the actual resource to save
     * @return This method returns a "MethodOutcome"
     */

    @Update
    public MethodOutcome updatePatient(@IdParam IdType theId, @ResourceParam Patient thePatient) {
        //TODO add validation method later

        Long resourceId;
        try {
            resourceId = theId.getIdPartAsLong();

        } catch (DataFormatException e) {
            throw new InvalidRequestException("Invalid ID " + theId.getValue() + " - Must be numeric");
        }

        if (!myPatients.containsKey(resourceId)) {
            throw new ResourceNotFoundException(theId);
        }


        // ... perform the update ...
        myPatients.put(resourceId,thePatient);
        return new MethodOutcome();

    }


    @Delete()
    public MethodOutcome deletePatient(@IdParam IdType theId) {

        Long resourceId;
        try {
            resourceId = theId.getIdPartAsLong();

        } catch (DataFormatException e) {
            throw new InvalidRequestException("Invalid ID " + theId.getValue() + " - Must be numeric");
        }

        if (!myPatients.containsKey(resourceId)) {
            throw new ResourceNotFoundException(theId);
        }

        // .. Delete the patient ..
        myPatients.remove(resourceId);

        return new MethodOutcome();
    }

}
