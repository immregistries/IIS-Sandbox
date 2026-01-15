package org.immregistries.iis.kernal.mapping.resourceMappers;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.IisVaccination;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;

public interface ImmunizationMapper<Immunization extends IBaseResource>
	extends IisResourceMasterReportedMapper<VaccinationMaster, VaccinationReported, IisVaccination, Immunization> {
	default String fhirType() {
		return IMMUNIZATION;
	}

	String IMMUNIZATION = "Immunization";

	default Class<IisVaccination> localType() {
		return IisVaccination.class;
	}
	default Class<VaccinationMaster> localMasterType() {
		return VaccinationMaster.class;
	}
	default Class<VaccinationReported> localReportedType() {
		return VaccinationReported.class;
	}

	String CVX_SYSTEM = "http://hl7.org/fhir/sid/cvx";
	String MVX_SYSTEM = "http://terminology.hl7.org/CodeSystem/MVX";
	String NDC_SYSTEM = "NDC";
	String INFORMATION_SOURCE = "NIP001"; // TODO get system from actual message
	String INFORMATION_SOURCE_EXTENSION = "informationSource"; // TODO get system from actual message
	String PERFORMER_FUNCTION_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0443";
	String ENTERING_VALUE = "EP";
	String ENTERING_DISPLAY = "Entering Provider";
	String ORDERING_VALUE = "OP";
	String ORDERING_DISPLAY = "Ordering Provider";
	String ADMINISTERING_VALUE = "AP";
	String ADMINISTERING_DISPLAY = "Administering Provider";
	String REFUSAL_REASON_CODE = "refusalReasonCode";
	String BODY_PART_SITE_SYSTEM = "http://hl7.org/fhir/ValueSet/immunization-site";
	String BODY_ROUTE_SYSTEM = "http://hl7.org/fhir/ValueSet/immunization-route";
	String FUNDING_SOURCE_SYSTEM = "http://hl7.org/fhir/ValueSet/immunization-funding-source";
	String FUNDING_ELIGIBILITY = "http://hl7.org/fhir/ValueSet/immunization-program-eligibility";
	String RECORDED = "recorded";
	String ACTION_CODE_EXTENSION = "actionCode";
	String ACTION_CODE_SYSTEM = "0206";
	String COMPLETION_STATUS_EXTENSION = "completionStatus";
	String COMPLETION_STATUS_SYSTEM = "0322";

	VaccinationReported localObjectReportedWithMaster(Immunization immunization);

	VaccinationReported localObjectReported(Immunization immunization);

	IisVaccination localObject(Immunization immunization);

	/**
	 * This method create the immunization resource based on the vaccinationReported
	 * information
	 *
	 * @param vaccinationMaster the vaccination local object
	 * @return the Immunization resource
	 */
	Immunization fhirResource(IisVaccination vaccinationMaster);
}
