package org.immregistries.iis.kernal.controllers.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.logic.match.VaccinationDedupService;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.vaccination_deduplication.LinkedImmunization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.immregistries.iis.kernal.security.CurrentTenantUtil.SESSION_REQUEST_TENANT;

@RestController
@RequestMapping(RestUrlUtil.REST_TENANT_PATH + VacDedupRestController.VAC_DEDUP_PATH)
public class VacDedupRestController {

	public static final String VAC_DEDUP_PATH = "/vacDedup";


	@Autowired
	private VaccinationDedupService vaccinationDedupService;

	@PostMapping
	public List<LinkedImmunization> deduplicate(
		@RequestAttribute(name = SESSION_REQUEST_TENANT) Tenant tenant,
		@RequestBody VacDedupRequest vacDedupRequest,
		HttpServletRequest req) {

		return vaccinationDedupService.getLinkedImmunizations(vacDedupRequest);
	}

	public static class VacDedupRequest {
		private String algorithm;
		private List<ImmunizationItem> immunizations;

		public String getAlgorithm() {
			return algorithm;
		}

		public void setAlgorithm(String algorithm) {
			this.algorithm = algorithm;
		}

		public List<ImmunizationItem> getImmunizations() {
			return immunizations;
		}

		public void setImmunizations(List<ImmunizationItem> immunizations) {
			this.immunizations = immunizations;
		}

		public static class ImmunizationItem {
			private String date;
			private String cvx;
			private String mvx;
			private String lot;
			private String org;
			private String source;

			public String getDate() {
				return date;
			}

			public void setDate(String date) {
				this.date = date;
			}

			public String getCvx() {
				return cvx;
			}

			public void setCvx(String cvx) {
				this.cvx = cvx;
			}

			public String getMvx() {
				return mvx;
			}

			public void setMvx(String mvx) {
				this.mvx = mvx;
			}

			public String getLot() {
				return lot;
			}

			public void setLot(String lot) {
				this.lot = lot;
			}

			public String getOrg() {
				return org;
			}

			public void setOrg(String org) {
				this.org = org;
			}

			public String getSource() {
				return source;
			}

			public void setSource(String source) {
				this.source = source;
			}
		}
	}
}
