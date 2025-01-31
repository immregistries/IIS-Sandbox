package org.immregistries.iis.kernal.mapping.interfaces;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;

public interface ImmunizationMapper<Immunization extends IBaseResource> extends IisFhirMapperMasterReported<VaccinationMaster, VaccinationReported, Immunization> {
	String CVX = "http://hl7.org/fhir/sid/cvx";
	String MVX = "http://terminology.hl7.org/CodeSystem/MVX";
	String NDC = "NDC";
	String INFORMATION_SOURCE = "NIP001"; //TODO get system from actual message
	String INFORMATION_SOURCE_EXTENSION = "informationSource"; //TODO get system from actual message
	String PERFORMER_FUNCTION_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0443";
	String ENTERING = "EP";
	String ENTERING_DISPLAY = "Entering Provider";
	String ORDERING = "OP";
	String ORDERING_DISPLAY = "Ordering Provider";
	String ADMINISTERING = "AP";
	String ADMINISTERING_DISPLAY = "Administering Provider";
	String REFUSAL_REASON_CODE = "refusalReasonCode";
	String BODY_PART = "bodyPart";
	String BODY_ROUTE = "bodyRoute";
	String FUNDING_SOURCE = "fundingSource";
	String FUNDING_ELIGIBILITY = "fundingEligibility";
	String RECORDED = "recorded";
	String ACTION_CODE_EXTENSION = "actionCode";
	String ACTION_CODE_SYSTEM = "0206";

	VaccinationReported localObjectReportedWithMaster(Immunization immunization);

//	IVaccinationMaster fillFromFhirResource(VaccinationMaster vaccinationMaster, Immunization i);

	VaccinationReported localObjectReported(Immunization immunization);

	VaccinationMaster localObject(Immunization immunization);

	Immunization fhirResource(VaccinationMaster vaccinationMaster);
}
