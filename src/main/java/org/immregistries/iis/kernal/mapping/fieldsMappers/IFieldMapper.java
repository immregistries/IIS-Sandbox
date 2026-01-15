package org.immregistries.iis.kernal.mapping.fieldsMappers;


import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.immregistries.iis.kernal.model.IisMappedToFhir;

public interface IFieldMapper<LocalType extends IisMappedToFhir, R4 extends IBaseDatatype, R5 extends IBaseDatatype> {

	Class<LocalType> localType();

	Class<R5> r5Type();

	Class<R4> r4Type();

	String fhirType();

	R5 toR5(LocalType localField);

	R4 toR4(LocalType localField);

	LocalType fromR5(R5 r5);

	LocalType fromR4(R4 r4);

	/**
	 * TODO Fix bad practice if class cast
	 *
	 * @param datatype
	 * @return
	 */
	default LocalType localObject(IBaseDatatype datatype) {
		try {
			return fromR4((R4) datatype);
		} catch (ClassCastException classCastException) {
			try {
				return fromR5((R5) datatype);
			} catch (ClassCastException classCastException2) {
				throw new RuntimeException(classCastException2);
			}
		}
	}
	
}
