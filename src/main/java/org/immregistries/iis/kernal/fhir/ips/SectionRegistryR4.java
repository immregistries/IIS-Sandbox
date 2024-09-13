package org.immregistries.iis.kernal.fhir.ips;

import ca.uhn.fhir.jpa.ips.api.SectionRegistry;

public class SectionRegistryR4 extends SectionRegistry {

	@Override
	/**
	 * Add the various sections to the registry in order. overridden for
	 * customization.
	 */
	protected void addSections() {
		addSectionAllergyIntolerance();
		addSectionMedicationSummary();
		addSectionProblemList();
		addSectionImmunizations();
//		addSectionProcedures();
//		addSectionMedicalDevices();
//		addSectionDiagnosticResults();
//		addSectionVitalSigns();
//		addSectionPregnancy();
//		addSectionSocialHistory();
//		addSectionIllnessHistory();
//		addSectionFunctionalStatus();
//		addSectionPlanOfCare();
//		addSectionAdvanceDirectives();
	}
}
