package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.r5.model.Identifier;
import org.hl7.fhir.r5.model.Reference;

public class MappingHelper {

	public  static Reference getFhirReference(String fhirType, String dbType, String identifier) {
		return new Reference(fhirType + "?identifier=" + dbType + "|"
			+ identifier)
			.setType(fhirType)
			.setIdentifier(getFhirIdentifier(dbType,identifier));
	}

	public  static Identifier getFhirIdentifier(String dbType, String identifier) {
		return new Identifier()
				.setSystem(dbType)
				.setValue(identifier);
	}


}
