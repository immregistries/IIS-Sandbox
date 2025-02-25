package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.jpa.patch.FhirPatch;
import ca.uhn.fhir.jpa.provider.DiffProvider;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

@Service
public class CustomDiffProvider extends DiffProvider {


	@Nonnull
	@Override
	public FhirPatch newPatch(IPrimitiveType<Boolean> theIncludeMeta) {
		FhirPatch fhirPatch = super.newPatch(theIncludeMeta);
		fhirPatch.addIgnorePath("*.text.div");
		fhirPatch.addIgnorePath("*.id");

		return fhirPatch;
	}
}
