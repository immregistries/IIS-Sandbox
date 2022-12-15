package org.immregistries.iis.kernal.mapping.Interfaces;

import org.immregistries.iis.kernal.model.ModelPerson;

public interface PersonMapper<Person> {
	public ModelPerson getModelPerson(Person practitioner);
	public Person getFhirResource(ModelPerson modelPerson);
}
