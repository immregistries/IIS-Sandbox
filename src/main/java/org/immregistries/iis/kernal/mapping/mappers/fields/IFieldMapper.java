package org.immregistries.iis.kernal.mapping.mappers.fields;


import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.mapping.mappers.IisMapper;
import org.immregistries.iis.kernal.model.IisMappedToFhir;

public interface IFieldMapper<LocalType extends IisMappedToFhir, FhirType extends IBaseDatatype> extends IisMapper<LocalType, FhirType> {

	Class<FhirType> fhirType();

	FhirType fhirObject(LocalType localField);

	LocalType localObject(FhirType fhirType);

}
