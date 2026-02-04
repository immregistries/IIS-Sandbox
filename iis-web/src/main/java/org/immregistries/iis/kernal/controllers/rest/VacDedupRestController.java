package org.immregistries.iis.kernal.controllers.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.GlobalConstants;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.logic.match.VacDedupRequest;
import org.immregistries.iis.kernal.logic.match.VaccinationDedupService;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.vaccination_deduplication.LinkedImmunization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(IisRestPath.REST_TENANT_PATH + IisRestPath.BasePath.VAC_DEDUP_PATH)
public class VacDedupRestController {

	@Autowired
	private VaccinationDedupService vaccinationDedupService;

	@PostMapping
	public List<LinkedImmunization> deduplicate(
		@RequestAttribute(name = GlobalConstants.SESSION_REQUEST_TENANT) Tenant tenant,
			@RequestBody VacDedupRequest vacDedupRequest,
			HttpServletRequest req) {

		return vaccinationDedupService.getLinkedImmunizations(vacDedupRequest);
	}

}
