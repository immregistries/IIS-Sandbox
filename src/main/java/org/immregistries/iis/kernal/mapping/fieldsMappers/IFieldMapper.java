package org.immregistries.iis.kernal.mapping.fieldsMappers;


public interface IFieldMapper<LocalType, R4, R5> {

	Class<LocalType> localType();

	R5 toR5(LocalType localField);

	R4 toR4(LocalType localField);

	LocalType fromR5(R5 r5);

	LocalType fromR4(R4 r4);
	
}
