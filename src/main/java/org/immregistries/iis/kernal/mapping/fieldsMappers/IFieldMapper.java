package org.immregistries.iis.kernal.mapping.fieldsMappers;


import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.model.IisMappedToFhir;

public interface IFieldMapper<LocalType extends IisMappedToFhir, FhirType extends IBaseDatatype> {

	Class<LocalType> localType();

	Class<FhirType> fhirType();

	String fhirTypeName();

	FhirType toFhir(LocalType localField);

	LocalType localObject(FhirType r5);

}
