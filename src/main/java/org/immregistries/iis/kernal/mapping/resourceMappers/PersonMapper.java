package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.ModelPerson;

/**
 * Retired and deprecated
 * @param <Person>
 */
public interface PersonMapper<Person extends IAnyResource>
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
