package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.ModelPerson;

public interface PractitionerMapper<Practitioner extends IBaseResource>
		extends IisFhirMapperMaster<ModelPerson, Practitioner> {
	default String fhirType() {
		return PRACTITIONER;
	}

	default Class<ModelPerson> localMasterType() {
		return ModelPerson.class;
	}

	String PRACTITIONER = "Practitioner";
}
