package org.immregistries.iis.kernal.mapping.mappers.resources;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.mapping.requesters.FhirRequesterUtil;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.model.IisVaccination;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ImmunizationMapper<Immunization extends IAnyResource>
	implements IisResourceMasterReportedMapper<VaccinationMaster, VaccinationReported, IisVaccination, Immunization> {

	public static final String CVX_SYSTEM = "http://hl7.org/fhir/sid/cvx";
	public static final String MVX_SYSTEM = "http://terminology.hl7.org/CodeSystem/MVX";
	public static final String NDC_SYSTEM = "NDC";
	public static final String INFORMATION_SOURCE = "NIP001"; // TODO get system from actual message
	public static final String INFORMATION_SOURCE_EXTENSION = "informationSource"; // TODO get system from actual message
	public static final String PERFORMER_FUNCTION_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0443";
	public static final String ENTERING_VALUE = "EP";
	public static final String ENTERING_DISPLAY = "Entering Provider";
	public static final String ORDERING_VALUE = "OP";
	public static final String ORDERING_DISPLAY = "Ordering Provider";
	public static final String ADMINISTERING_VALUE = "AP";
	public static final String ADMINISTERING_DISPLAY = "Administering Provider";
	public static final String REFUSAL_REASON_CODE = "refusalReasonCode";
	public static final String BODY_PART_SITE_SYSTEM = "http://hl7.org/fhir/ValueSet/immunization-site";
	public static final String BODY_ROUTE_SYSTEM = "http://hl7.org/fhir/ValueSet/immunization-route";
	public static final String FUNDING_SOURCE_SYSTEM = "http://hl7.org/fhir/ValueSet/immunization-funding-source";
	public static final String FUNDING_ELIGIBILITY = "http://hl7.org/fhir/ValueSet/immunization-program-eligibility";
	public static final String RECORDED = "recorded";
	public static final String ACTION_CODE_EXTENSION = "actionCode";
	public static final String ACTION_CODE_SYSTEM = "0206";
	public static final String COMPLETION_STATUS_EXTENSION = "completionStatus";
	public static final String COMPLETION_STATUS_SYSTEM = "0322";

	@Autowired
	private FhirSearchRequester fhirSearchRequester;

	public String fhirTypeName() {
		return IMMUNIZATION;
	}

	public static final String IMMUNIZATION = "Immunization";

	public Class<IisVaccination> localType() {
		return IisVaccination.class;
	}

	public Class<VaccinationMaster> localMasterType() {
		return VaccinationMaster.class;
	}

	public Class<VaccinationReported> localReportedType() {
		return VaccinationReported.class;
	}

	public VaccinationReported localObjectReportedWithMaster(Immunization i) {
		VaccinationReported vaccinationReported = this.localObjectReported(i);
		VaccinationMaster vaccinationMaster = fhirSearchRequester.searchVaccinationMaster(
			new SearchParameterMap("identifier",
				new TokenParam().setValue(vaccinationReported.getFillerBusinessIdentifier().getValue()))
			// Immunization.IDENTIFIER.exactly().systemAndIdentifier(
			// vaccinationReported.getExternalLinkSystem(),
			// vaccinationReported.getExternalLink())
		);
		if (vaccinationMaster != null) {
			vaccinationReported.setMasterRecord(vaccinationMaster);
		}
		return vaccinationReported;
	}

	public VaccinationReported localObjectReported(Immunization i) {
		if (FhirRequesterUtil.isGoldenRecord(i)) {
			return null;
		}
		VaccinationReported vaccinationReported = new VaccinationReported();
		fillFromFhirResource(vaccinationReported, i);
		return vaccinationReported;
	}

	public VaccinationMaster localObjectMaster(Immunization i) {
		VaccinationMaster vaccinationMaster = new VaccinationMaster();
		if (FhirRequesterUtil.isNotGoldenRecord(i)) {
			return null;
		}
		fillFromFhirResource(vaccinationMaster, i);
		return vaccinationMaster;
	}

	public IisVaccination localObject(Immunization i) {
		IisVaccination iisVaccination = new IisVaccination();
		fillFromFhirResource(iisVaccination, i);
		return iisVaccination;
	}



	/**
	 * This method create the immunization resource based on the vaccinationReported
	 * information
	 *
	 * @param vaccinationMaster the vaccination local object
	 * @return the Immunization resource
	 */
	public abstract Immunization fhirObject(IisVaccination vaccinationMaster);

	public abstract void fillFromFhirResource(IisVaccination localPatient, Immunization immunization);
}
