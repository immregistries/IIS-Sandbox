package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IBasicClient;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;

public interface IPatientClient extends IBasicClient
{
    /** Read a patient from a server by ID */
    @Read
    Patient readPatient(@IdParam IdType theId);

    @Create
    public abstract MethodOutcome createNewPatient(@ResourceParam Patient thePatient);
}
