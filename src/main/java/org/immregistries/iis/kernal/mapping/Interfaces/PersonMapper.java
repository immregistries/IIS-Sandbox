package org.immregistries.iis.kernal.mapping.Interfaces;

import org.immregistries.iis.kernal.model.ModelPerson;

public interface PersonMapper<Person> {
	String ORGANIZATION_ASSIGNING_AUTHORITY = "AssigningAuthority";

	ModelPerson getModelPerson(Person practitioner);

	Person getFhirResource(ModelPerson modelPerson);
}
