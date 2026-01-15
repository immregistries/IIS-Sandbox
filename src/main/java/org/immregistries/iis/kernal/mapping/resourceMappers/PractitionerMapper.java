package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.ModelPerson;

public interface PractitionerMapper<Practitioner extends IAnyResource>
		extends IisResourceMasterMapper<ModelPerson, Practitioner> {
	default String fhirType() {
		return PRACTITIONER;
	}

	default Class<ModelPerson> localType() {
		return ModelPerson.class;
	}

	String PRACTITIONER = "Practitioner";
}
