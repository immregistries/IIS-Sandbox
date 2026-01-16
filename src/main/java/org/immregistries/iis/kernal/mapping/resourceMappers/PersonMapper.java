package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.model.ModelPerson;

/**
 * Retired and deprecated
 * 
 * @param <Person>
 */
public abstract class PersonMapper<Person extends IAnyResource>
// extends IisFhirMapperMaster<ModelPerson, Person>
{
	public String fhirType() {
		return "Person";
	}

	public Class<ModelPerson> localMasterType() {
		return ModelPerson.class;
	}

	public static final String ORGANIZATION_ASSIGNING_AUTHORITY = "AssigningAuthority";

	public abstract ModelPerson localObject(Person person);

	public abstract Person fhirResource(ModelPerson modelPerson);
}
