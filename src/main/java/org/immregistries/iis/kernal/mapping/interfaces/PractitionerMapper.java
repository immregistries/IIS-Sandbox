package org.immregistries.iis.kernal.mapping.interfaces;


import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.ModelPerson;

public interface PractitionerMapper<Practitioner extends IBaseResource> extends IisFhirMapperMaster<ModelPerson, Practitioner> {
	String PRACTITIONER = "Practitioner";
//	ModelPerson localObject(Practitioner practitioner);
//	Practitioner fhirResource(ModelPerson modelPerson);
}
