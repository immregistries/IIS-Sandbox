package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.Immunization;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.IisRequestAttribute;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.logic.hl7v2.writing.IExampleMessageWriter;
import org.immregistries.iis.kernal.mapping.requesters.FhirSearchRequester;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.immregistries.iis.kernal.controllers.IisRestPath.BasePath.VXU_DOWNLOAD_PATH;

@RestController
@RequestMapping(IisRestPath.REST_TENANT_PATH + VXU_DOWNLOAD_PATH)
public class VxuDownloadRestController {

	@Autowired
	private FhirSearchRequester fhirSearchRequester;
	@Autowired
	private IExampleMessageWriter exampleMessageWriter;

	public static class VxuDownloadRequest {
		private String dateStart;
		private String dateEnd;
		private String cvxCodes;
		private boolean includePhi;

		public VxuDownloadRequest() {
		}

		public VxuDownloadRequest(String dateStart, String dateEnd, String cvxCodes, boolean includePhi) {
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
	public String generate(
		@RequestAttribute(name = IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
		@RequestBody VxuDownloadRequest request) throws ParseException {

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
			.collect(Collectors.toList());

		StringBuilder sb = new StringBuilder();
		for (VaccinationReported vr : vaccinationReportedList) {
			sb.append(exampleMessageWriter.buildVxu(vr, tenant));
			sb.append("\r");
		}

		return sb.toString();
	}
}
