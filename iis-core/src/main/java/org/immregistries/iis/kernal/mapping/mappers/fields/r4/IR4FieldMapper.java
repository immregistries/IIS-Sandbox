package org.immregistries.iis.kernal.mapping.mappers.fields.r4;

import org.hl7.fhir.r4.model.Type;
import org.immregistries.iis.kernal.mapping.mappers.IR4Mapper;
import org.immregistries.iis.kernal.model.IisMappedToFhirField;

/**
 * Interface to prepare a potential coexistence of mappers in certain contexts
 *
 * @param <LocalField>   extends IisMappedToFhirField
 * @param <FhirDataType> extends R4 Resource
 */
public interface IR4FieldMapper<LocalField extends IisMappedToFhirField, FhirDataType extends Type> extends IR4Mapper<LocalField, FhirDataType> {
}
