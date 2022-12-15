package org.immregistries.iis.kernal.mapping.Interfaces;


import org.immregistries.iis.kernal.model.ModelPerson;

public interface PractitionerMapper<Practitioner> {
	public ModelPerson getModelPerson(Practitioner practitioner);
	public Practitioner getFhirResource(ModelPerson modelPerson);
}
