package org.immregistries.iis.kernal.mapping.Interfaces;

import org.immregistries.iis.kernal.model.ModelPerson;

public interface PersonMapper<Person> {
	public static final String ORGANISATION_ASSIGNING_AUTHORITY = "AssigningAuthority";

	public ModelPerson getModelPerson(Person practitioner);

	public Person getFhirResource(ModelPerson modelPerson);
}
