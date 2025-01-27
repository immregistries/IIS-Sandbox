package org.immregistries.iis.kernal.mapping.Interfaces;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.ModelPerson;

public interface PersonMapper<Person extends IBaseResource> extends IisFhirMapperMaster<ModelPerson, Person> {
	String ORGANIZATION_ASSIGNING_AUTHORITY = "AssigningAuthority";

	ModelPerson localObject(Person practitioner);

	Person fhirResource(ModelPerson modelPerson);
}
