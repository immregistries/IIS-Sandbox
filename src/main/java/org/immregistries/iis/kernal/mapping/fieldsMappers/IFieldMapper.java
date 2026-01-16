package org.immregistries.iis.kernal.mapping.fieldsMappers;


import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.mapping.IisMapper;
import org.immregistries.iis.kernal.model.IisMappedToFhir;

public interface IFieldMapper<LocalType extends IisMappedToFhir, FhirType extends IBaseDatatype> extends IisMapper<LocalType, FhirType> {

	Class<FhirType> fhirType();

	FhirType fhirObject(LocalType localField);

	LocalType localObject(FhirType fhirType);

}
