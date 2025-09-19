package org.immregistries.iis.kernal.logic.shlink.evc;

import com.syadem.nuva.Vaccine;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class EvCUtil {
	Logger logger = LoggerFactory.getLogger(this.getClass());


	@Autowired
	NuvaService nuvaService;

	/**
	 * Converts a FHIR Immunization resource into an EvC VaccinationRecord.
	 * <p>
	 * This method assumes the FHIR resource contains the necessary data points
	 * to populate the VaccinationRecord object. It uses the `meta.profile` to determine
	 * the registry code and extracts other information from the resource.
	 * </p>
	 *
	 * @param immunization The FHIR Immunization resource to convert.
	 * @param patient      The FHIR Patient resource to calculate age from.
	 * @return A VaccinationRecord object populated with data from the FHIR resource.
	 */
	public EvCPayload.VaccinationRecord toVaccinationRecord(Immunization immunization, Patient patient) {
		EvCPayload.VaccinationRecord record = new EvCPayload.VaccinationRecord();

		// Assumption: Registry Code (reg) is derived from the profile URL in meta.
		// This is a placeholder and may need to be adjusted based on the actual FHIR implementation.
		if (immunization.getMeta().hasProfile()) {
			String profileUrl = immunization.getMeta().getProfile().get(0).getValue();
			record.setRegistryCode(extractRegistryCodeFromUrl(profileUrl));
		}

		// Assumption: Repository Index (rep) and Reference (i) are available as extensions or identifiers.
		// For this example, we'll use a placeholder or check for a specific identifier.
		// This may need to be customized based on your FHIR data.
		record.setRepositoryIndex(5);
		record.setReference(1296);

		// TODO support more codes like snomed
		Coding cvxCoding = MappingHelper.filterCodeableConceptR4(immunization.getVaccineCode(), ImmunizationMapper.CVX_SYSTEM);
		if (cvxCoding != null && StringUtils.isNotBlank(cvxCoding.getCode())) {
			Optional<Vaccine> byCvx = nuvaService.findByCvx(cvxCoding.getCode());
			byCvx.ifPresent(vaccine -> record.setNuvaCode(vaccine.getCode()));
		}

		// Calculate age in days (a)
		if (immunization.hasOccurrenceDateTimeType() && patient.hasBirthDate()) {
			Date immunizationDate = immunization.getOccurrenceDateTimeType().getValue();
			Date birthDate = patient.getBirthDate();
			long diffInMillies = Math.abs(immunizationDate.getTime() - birthDate.getTime());
			long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
			record.setAgeInDays((int) diff);
		}

		return record;
	}

	/**
	 * Converts a FHIR Patient resource into an EvCPayload.
	 * <p>
	 * This method populates the demographic information of the EvCPayload
	 * from the FHIR Patient resource.
	 * </p>
	 *
	 * @param patient The FHIR Patient resource to convert.
	 * @return An EvCPayload object populated with patient data.
	 */
	public EvCPayload toEvCPayload(Patient patient) {
		EvCPayload payload = new EvCPayload();

		// Set version
		payload.setVersion("1.0.0");

		// Populate name
		if (patient.hasName()) {
			HumanName fhirName = patient.getNameFirstRep();
			EvCPayload.Name evcName = new EvCPayload.Name();
			if (fhirName.hasFamily()) {
				evcName.setFamilyName(fhirName.getFamily());
			}
			if (fhirName.hasGiven()) {
				evcName.setGivenName(fhirName.getGivenAsSingleString());
			}
			payload.setName(evcName);
		}

		// Populate date of birth
		if (patient.hasBirthDate()) {
			payload.setDateOfBirth(patient.getBirthDate());
		}

		// Populate person identifier (pid) if available
		if (patient.hasIdentifier()) {
			Identifier fhirIdentifier = patient.getIdentifierFirstRep();
			EvCPayload.PersonIdentifier evcId = new EvCPayload.PersonIdentifier();
			if (fhirIdentifier.hasSystem()) {
				evcId.setObjectIdentifier(fhirIdentifier.getSystem());
			}
			if (fhirIdentifier.hasValue()) {
				evcId.setId(fhirIdentifier.getValue());
			}
			payload.setPersonIdentifier(evcId);
		}

		// Initialize vaccination records list
		payload.setVaccinationRecords(new ArrayList<>());

		return payload;
	}

	/**
	 * Helper method to extract registry code from a FHIR profile URL.
	 *
	 * @param url The profile URL string.
	 * @return The extracted registry code.
	 */
	private String extractRegistryCodeFromUrl(String url) {
		// This is a simple example. A more robust implementation might be needed.
		// E.g., urn:example:registry:FRA -> FRA
		String[] parts = url.split(":");
		if (parts.length > 2) {
			return parts[parts.length - 1];
		}
		return "N/A";
	}

	/**
	 * Converts a FHIR IPS Bundle resource to an EvCPayload.
	 * <p>
	 * This method extracts patient and immunization data from the bundle
	 * to create the EvCPayload.
	 * </p>
	 *
	 * @param ipsBundle The FHIR IPS Bundle resource to convert.
	 * @return An EvCPayload object containing the data from the bundle.
	 */
	public EvCPayload toEvCPayloadFromBundle(Bundle ipsBundle) {
		Patient patient = null;
		List<Immunization> immunizations = new ArrayList<>();

		// Find the Patient and Immunization resources in the bundle
		for (Bundle.BundleEntryComponent entry : ipsBundle.getEntry()) {
			Resource resource = entry.getResource();
			if (resource.getResourceType() == ResourceType.Patient) {
				patient = (Patient) resource;
			} else if (resource.getResourceType() == ResourceType.Immunization) {
				immunizations.add((Immunization) resource);
			}
		}

		if (patient == null) {
			throw new IllegalArgumentException("FHIR IPS Bundle must contain a Patient resource.");
		}

		// Create the EvC Payload and populate patient data
		EvCPayload payload = toEvCPayload(patient);

		// Convert and add immunization records
		List<EvCPayload.VaccinationRecord> vaccinationRecords = new ArrayList<>();
		for (Immunization immunization : immunizations) {
			vaccinationRecords.add(toVaccinationRecord(immunization, patient));
		}
		payload.setVaccinationRecords(vaccinationRecords);

		return payload;
	}
}
