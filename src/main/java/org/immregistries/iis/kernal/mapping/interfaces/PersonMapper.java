package org.immregistries.iis.kernal.mapping.interfaces;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.ModelPerson;

/**
 * Retired and deprecated
 * @param <Person>
 */
public interface PersonMapper<Person extends IBaseResource>
//	extends IisFhirMapperMaster<ModelPerson, Person>
{
	default String fhirType() {
		return "Person";
	}

	default Class<ModelPerson> localMasterType() {
		return ModelPerson.class;
	}

	String ORGANIZATION_ASSIGNING_AUTHORITY = "AssigningAuthority";

	ModelPerson localObject(Person person);

	Person fhirResource(ModelPerson modelPerson);
}
