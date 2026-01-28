package org.immregistries.iis.kernal.mapping.mappers;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r5.model.Base;
import org.immregistries.iis.kernal.model.IisMappedToFhir;

/**
 * Interface to prepare a potential coexistence of mappers in certain contexts
 *
 * @param <LocalResource> extends IisMappedToFhir
 * @param <FhirType>      extends R5 Resource
 */
public interface IR5Mapper<LocalResource extends IisMappedToFhir, FhirType extends Base> extends IisMapper<LocalResource, FhirType> {

	default FhirVersionEnum fhirVersion() {
		return FhirVersionEnum.R5;
	}
}
