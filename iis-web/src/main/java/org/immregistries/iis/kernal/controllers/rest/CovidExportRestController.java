package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.starter.annotations.OnR5Condition;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.Immunization;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.IisRequestAttribute;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.mapping.mappers.resources.r5.LocationMapperR5;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.mapping.requesters.IFhirReadRequester;
import org.immregistries.iis.kernal.model.OrgLocation;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.immregistries.iis.kernal.controllers.IisRestPath.BasePath.COVID_EXPORT_PATH;

@RestController
@RequestMapping(IisRestPath.REST_TENANT_PATH + COVID_EXPORT_PATH)
@Conditional(OnR5Condition.class)
public class CovidExportRestController {

	private static final String EXPORT_DATE_FORMAT = "yyyy-MM-dd";

	@Autowired
	private FhirSearchRequester fhirSearchRequester;
	@Autowired
	private IFhirReadRequester fhirReadRequester;
	@Autowired
	private LocationMapperR5 locationMapper;

	public static class CovidExportRequest {
		private String dateStart;
		private String dateEnd;
		private String cvxCodes;
		private boolean includePhi;

		public CovidExportRequest() {
		}

		public CovidExportRequest(String dateStart, String dateEnd, String cvxCodes, boolean includePhi) {
			this.dateStart = dateStart;
			this.dateEnd = dateEnd;
			this.cvxCodes = cvxCodes;
			this.includePhi = includePhi;
		}

		public String getDateStart() {
			return dateStart;
		}

		public void setDateStart(String dateStart) {
			this.dateStart = dateStart;
		}

		public String getDateEnd() {
			return dateEnd;
		}

		public void setDateEnd(String dateEnd) {
			this.dateEnd = dateEnd;
		}

		public String getCvxCodes() {
			return cvxCodes;
		}

		public void setCvxCodes(String cvxCodes) {
			this.cvxCodes = cvxCodes;
		}

		public boolean isIncludePhi() {
			return includePhi;
		}

		public void setIncludePhi(boolean includePhi) {
			this.includePhi = includePhi;
		}
	}

	@PostMapping(produces = "text/plain")
	public String exportData(
		@RequestAttribute(name = IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
		@RequestBody CovidExportRequest request) throws ParseException {

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		Date dateStart = sdf.parse(request.getDateStart());
		Date dateEnd = sdf.parse(request.getDateEnd());

		Set<String> cvxCodeSet = new HashSet<>();
		if (StringUtils.isNotEmpty(request.getCvxCodes())) {
			for (String c : request.getCvxCodes().split(",")) {
				if (StringUtils.isNotEmpty(c.trim())) {
					cvxCodeSet.add(c.trim());
				}
			}
		}

		List<VaccinationReported> vaccinationReportedList = fhirSearchRequester
			.searchVaccinationReportedList(
				new SearchParameterMap(Immunization.SP_PATIENT,
					new ReferenceParam().setChain(Patient.SP_ORGANIZATION)
						.setValue(String.valueOf(tenant.getOrgId()))));

		vaccinationReportedList = vaccinationReportedList.stream()
			.filter(vr -> vr.getReportedDate().after(dateStart) && vr.getReportedDate().before(dateEnd))
			.filter(vr -> cvxCodeSet.contains(vr.getVaccineCvxCode()))
			.filter(vr -> !vr.getCompletionStatus().equals("NA"))
			.collect(Collectors.toList());

		StringBuilder sb = new StringBuilder();
		printHeaderLine(sb);
		for (VaccinationReported vr : vaccinationReportedList) {
			printLine(sb, vr, request.isIncludePhi());
		}

		return sb.toString();
	}

	private void printHeaderLine(StringBuilder sb) {
		String[] headers = {
			"vax_event_id", "ext_type", "pprl_id", "recip_id",
			"recip_first_name", "recip_middle_name", "recip_last_name",
			"recip_dob", "recip_sex",
			"recip_address_street", "recip_address_street_2",
			"recip_address_city", "recip_address_county",
			"recip_address_state", "recip_address_zip",
			"recip_race_1", "recip_race_2", "recip_race_3",
			"recip_race_4", "recip_race_5", "recip_race_6",
			"recip_ethnicity", "admin_date", "cvx", "ndc", "mvx",
			"lot_number", "vax_expiration", "vax_admin_site", "vax_route",
			"dose_num", "vax_series_complete",
			"responsible_org", "admin_name", "vtrcks_prov_pin", "admin_type",
			"admin_address_street", "admin_address_street_2",
			"admin_address_city", "admin_address_county",
			"admin_address_state", "admin_address_zip",
			"vax_refusal", "cmorbid_status", "serology"
		};
		sb.append(String.join("\t", headers)).append("\n");
	}

	private void printLine(StringBuilder sb, VaccinationReported vr, boolean includePhi) {
		SimpleDateFormat sdf = new SimpleDateFormat(EXPORT_DATE_FORMAT);
		PatientReported pr = vr.getPatientReported();
		PatientMaster pm = pr.getMasterRecord();
		boolean administered = StringUtils.isEmpty(vr.getCompletionStatus())
			|| vr.getCompletionStatus().equals("CP");

		appendField(sb, vr.getMasterRecord().getVaccinationId());
		appendField(sb, includePhi ? "I" : "D");
		appendField(sb, "");
		appendField(sb, pm.getPatientId());
		appendField(sb, includePhi ? pr.getNameFirst() : "Redacted");
		appendField(sb, includePhi ? pr.getNameMiddle() : "Redacted");
		appendField(sb, includePhi ? pr.getNameLast() : "Redacted");
		appendField(sb, pr.getBirthDate() != null ? sdf.format(pr.getBirthDate()) : "");
		appendField(sb, pr.getSex());

		appendField(sb, includePhi ? pr.getFirstAddress().getAddressLine1() : "Redacted");
		appendField(sb, includePhi ? pr.getFirstAddress().getAddressLine2() : "Redacted");
		appendField(sb, includePhi ? pr.getFirstAddress().getAddressCity() : "Redacted");
		appendField(sb, pr.getFirstAddress().getAddressCountyParish());
		appendField(sb, pr.getFirstAddress().getAddressState());
		appendField(sb, pr.getFirstAddress().getAddressZip());

		if (pm.getRaces().isEmpty()) {
			appendField(sb, "UNK");
		} else {
			for (String race : pr.getRaces()) {
				appendField(sb, race);
			}
		}

		appendField(sb, vr.getAdministeredDate() != null ? sdf.format(vr.getAdministeredDate()) : "");
		if (administered) {
			appendField(sb, vr.getVaccineCvxCode());
			appendField(sb, vr.getVaccineNdcCode());
			appendField(sb, vr.getVaccineMvxCode());
			appendField(sb, vr.getLotnumber());
			appendField(sb, vr.getExpirationDate() != null ? sdf.format(vr.getExpirationDate()) : "");
			appendField(sb, vr.getBodySite());
			appendField(sb, vr.getBodyRoute());
			appendField(sb, "UNK");
			appendField(sb, "UNK");
		} else {
			appendField(sb, vr.getCompletionStatus().equals("RE") ? vr.getVaccineCvxCode() : "");
			for (int i = 0; i < 8; i++) appendField(sb, "");
		}

		OrgLocation orgLocation = vr.getOrgLocation();
		if (orgLocation == null) {
			orgLocation = fhirReadRequester.readAsOrgLocation(vr.getOrgLocationId());
		}
		if (orgLocation != null) {
			appendField(sb, orgLocation.getOrgFacilityCode());
			appendField(sb, orgLocation.getOrgFacilityName());
			appendField(sb, orgLocation.getVfcProviderPin());
			appendField(sb, orgLocation.getLocationType());
			appendField(sb, orgLocation.getAddressLine1());
			appendField(sb, orgLocation.getAddressLine2());
			appendField(sb, orgLocation.getAddressCity());
			appendField(sb, orgLocation.getAddressCountyParish());
			appendField(sb, orgLocation.getAddressState());
			appendField(sb, orgLocation.getAddressZip());
		} else {
			for (int i = 0; i < 10; i++) appendField(sb, "");
		}

		appendField(sb, administered ? "No" : (vr.getCompletionStatus().equals("RE") ? "Yes" : "No"));
		appendField(sb, "UNK");
		appendField(sb, "UNK");
		sb.append("\n");
	}

	private void appendField(StringBuilder sb, String value) {
		if (value != null) sb.append(value);
		sb.append("\t");
	}
}
