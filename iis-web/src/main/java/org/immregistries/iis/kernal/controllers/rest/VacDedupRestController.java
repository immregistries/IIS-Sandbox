package org.immregistries.iis.kernal.controllers.rest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.immregistries.iis.kernal.IisRequestAttribute;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.logic.match.VacDedupRequest;
import org.immregistries.iis.kernal.logic.match.VaccinationDedupService;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.vaccination_deduplication.Immunization;
import org.immregistries.vaccination_deduplication.LinkedImmunization;
import org.immregistries.vaccination_deduplication.reference.LinkedImmunizationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(IisRestPath.REST_TENANT_PATH + IisRestPath.BasePath.VAC_DEDUP_PATH)
public class VacDedupRestController {

	@Autowired
	private VaccinationDedupService vaccinationDedupService;

	@Data
	@AllArgsConstructor
	public static class LinkedImmunizationResult {
		private LinkedImmunizationType type;
		private List<Immunization> immunizations;

		static LinkedImmunizationResult from(LinkedImmunization li) {
			return new LinkedImmunizationResult(li.getType(), new ArrayList<>(li));
		}
	}

	@PostMapping
	public List<LinkedImmunizationResult> deduplicate(
		@RequestAttribute(name = IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
			@RequestBody VacDedupRequest vacDedupRequest,
			HttpServletRequest req) {

		return vaccinationDedupService.getLinkedImmunizations(vacDedupRequest)
			.stream().map(LinkedImmunizationResult::from).collect(Collectors.toList());
	}

}
