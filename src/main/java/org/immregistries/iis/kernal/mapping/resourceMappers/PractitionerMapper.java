package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.ModelPerson;

public abstract class PractitionerMapper<Practitioner extends IAnyResource>
		implements IisResourceMasterMapper<ModelPerson, Practitioner> {
	public String fhirResourceName() {
		return PRACTITIONER;
	}

	public Class<ModelPerson> localType() {
		return ModelPerson.class;
	}

	public static final String PRACTITIONER = "Practitioner";
}
