package org.immregistries.iis.kernal.mapping.mappers.fields.r5;

import org.hl7.fhir.r5.model.DataType;
import org.immregistries.iis.kernal.mapping.mappers.IR5Mapper;
import org.immregistries.iis.kernal.model.IisMappedToFhirField;

/**
 * Interface to prepare a potential coexistence of mappers in certain contexts
 *
 * @param <LocalField>   extends IisMappedToFhirField
 * @param <FhirDataType> extends R5 Resource
 */
public interface IR5FieldMapper<LocalField extends IisMappedToFhirField, FhirDataType extends DataType> extends IR5Mapper<LocalField, FhirDataType> {
}
