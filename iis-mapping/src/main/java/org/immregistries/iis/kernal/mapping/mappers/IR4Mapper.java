package org.immregistries.iis.kernal.mapping.mappers;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r4.model.Base;
import org.immregistries.iis.kernal.model.IisMappedToFhir;

/**
 * Interface to prepare a potential coexistence of mappers in certain contexts
 *
 * @param <LocalResource> extends IisMappedToFhir
 * @param <FhirElement>   extends R4 Resource
 */
public interface IR4Mapper<LocalResource extends IisMappedToFhir, FhirElement extends Base> extends IisMapper<LocalResource, FhirElement> {

	default FhirVersionEnum fhirVersion() {
		return FhirVersionEnum.R4;
	}
}
