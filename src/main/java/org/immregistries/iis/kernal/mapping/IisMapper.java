package org.immregistries.iis.kernal.mapping;

import ca.uhn.fhir.model.api.IElement;
import org.immregistries.iis.kernal.model.IisMappedToFhir;

public interface IisMapper<LocalType extends IisMappedToFhir, FhirElement extends IElement> {
	String fhirTypeName();
	Class<LocalType> localType();

	FhirElement fhirObject(LocalType localField);

	LocalType localObject(FhirElement fhirElement);


}
