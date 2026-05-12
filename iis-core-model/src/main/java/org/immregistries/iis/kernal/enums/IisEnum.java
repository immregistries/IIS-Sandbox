package org.immregistries.iis.kernal.enums;

public interface IisEnum {
	String getCode();

	String getLabel();

	org.hl7.fhir.r4.model.Coding toR4();

	org.hl7.fhir.r5.model.Coding toR5();
}
