package org.immregistries.iis.kernal.logic.match;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.controllers.rest.VacDedupRestController;
import org.immregistries.vaccination_deduplication.Immunization;
import org.immregistries.vaccination_deduplication.LinkedImmunization;
import org.immregistries.vaccination_deduplication.VaccinationDeduplication;
import org.immregistries.vaccination_deduplication.reference.ImmunizationSource;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class VaccinationDedupService {
	private final VaccinationDeduplication vaccinationDeduplication = new VaccinationDeduplication();

	public static final String ALGORITHM_DETERMINISTIC = "Deterministic";
	public static final String ALGORITHM_WEIGHTED = "Weighted";
	public static final String ALGORITHM_HYBRID = "Hybrid";

	public List<LinkedImmunization> getLinkedImmunizations(VacDedupRestController.VacDedupRequest vacDedupRequest) {
		LinkedImmunization linkedImmunizationList = new LinkedImmunization();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

		if (vacDedupRequest.getImmunizations() != null) {
			for (VacDedupRestController.VacDedupRequest.ImmunizationItem item : vacDedupRequest.getImmunizations()) {
				Date parsedDate = null;
				if (StringUtils.isNotBlank(item.getDate())) {
					try {
						parsedDate = sdf.parse(item.getDate());
						if (StringUtils.isNotBlank(item.getCvx())) {
							linkedImmunizationList.add(toVacDedupImmunization(item, parsedDate));
						}
					} catch (ParseException ignored) {
					}
				}
			}
		}

		return deduplicatedImmunizations(vacDedupRequest.getAlgorithm(), linkedImmunizationList);
	}

	public List<LinkedImmunization> deduplicatedImmunizations(String algorithm, LinkedImmunization immunizationList) {
		int size = immunizationList.size();
		List<LinkedImmunization> immunizationListResults;
		if (size >= 2) {
			switch (algorithm) {
				case ALGORITHM_WEIGHTED:
					immunizationListResults = vaccinationDeduplication.deduplicateWeighted(immunizationList);
					break;
				case ALGORITHM_HYBRID:
					immunizationListResults = vaccinationDeduplication.deduplicateHybrid(immunizationList);
					break;
				case ALGORITHM_DETERMINISTIC:
				default:
					immunizationListResults = vaccinationDeduplication.deduplicateDeterministic(immunizationList);
					break;
			}
		} else {
			immunizationListResults = List.of();
		}
		return immunizationListResults;
	}

	public @NotNull Immunization toVacDedupImmunization(VacDedupRestController.VacDedupRequest.ImmunizationItem item, Date parsedDate) {
		Immunization immunization = new Immunization();
		immunization.setCVX(item.getCvx());
		immunization.setDate(parsedDate);
		immunization.setMVX(item.getMvx());
		immunization.setLotNumber(item.getLot());
		immunization.setOrganisationID(item.getOrg());

		ImmunizationSource source = ImmunizationSource.HISTORICAL;
		if (item.getSource() != null && !item.getSource().isEmpty()) {
			try {
				source = ImmunizationSource.valueOf(item.getSource());
			} catch (IllegalArgumentException e) {
				// default to HISTORICAL
			}
		}
		immunization.setSource(source);
		return immunization;
	}
}
