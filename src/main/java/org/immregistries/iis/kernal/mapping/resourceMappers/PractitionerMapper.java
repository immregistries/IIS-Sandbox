package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.ModelPerson;

public interface PractitionerMapper<Practitioner extends IBaseResource>
		extends IisResourceMasterMapper<ModelPerson, Practitioner> {
	default String fhirType() {
		return PRACTITIONER;
	}

	default Class<ModelPerson> localType() {
		return ModelPerson.class;
	}

	String PRACTITIONER = "Practitioner";
}
