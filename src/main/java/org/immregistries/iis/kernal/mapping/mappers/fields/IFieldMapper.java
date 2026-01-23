package org.immregistries.iis.kernal.mapping.mappers.fields;


import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.mapping.mappers.IisMapper;
import org.immregistries.iis.kernal.model.IisMappedToFhirField;

public interface IFieldMapper<LocalType extends IisMappedToFhirField, FhirType extends IBaseDatatype> extends IisMapper<LocalType, FhirType> {

	Class<FhirType> fhirType();

	FhirType fhirObject(LocalType localField);

	LocalType localObject(FhirType fhirType);

}
