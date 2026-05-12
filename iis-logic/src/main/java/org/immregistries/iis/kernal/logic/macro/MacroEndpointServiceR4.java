package org.immregistries.iis.kernal.logic.macro;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.security.TenantAuthService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR4Condition.class)
public class MacroEndpointServiceR4 implements MacroEndpointService {

	@Autowired
	FhirContext fhirContext;
	@Autowired
	IFhirResourceDao<Patient> patientDao;
	@Autowired
	IFhirResourceDao<Immunization> immunizationDao;
	@Autowired
	IFhirResourceDao<ImmunizationRecommendation> immunizationRecommendationDao;
	@Autowired
	IFhirResourceDao<Practitioner> practitionerDao;
	@Autowired
	TenantAuthService tenantAuthService;

	public @NotNull Tenant generateTenantAndContent(String bundleString, UserAccess userAccess) {
		Bundle facilityBundle = fhirContext.newJsonParser().parseResource(Bundle.class, bundleString);
		ServletRequestDetails requestDetails;
		Tenant tenant = null;
		/**
		 * one and only one organization must be specified in bundle
		 * TODO deal with organization/Facility as managing organization
		 */
		for (Bundle.BundleEntryComponent entry : facilityBundle.getEntry()) {
			if (entry.getResource() instanceof Organization) {
				if (tenant != null) {
					throw new InvalidRequestException("More than one organization present");
				}
				tenant = tenantAuthService.authenticateTenant(userAccess, ((Organization) entry.getResource()).getName());
			}
		}

		if (tenant == null) {
			throw new InvalidRequestException("No organization information specified");
		} else {
			requestDetails = new ServletRequestDetails();
			requestDetails.setTenantId(tenant.getOrganizationName());
			fillFacility(requestDetails, facilityBundle);
			return tenant;
		}
	}

	private void fillFacility(ServletRequestDetails requestDetails, Bundle bundle) {
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof Practitioner) {
				Practitioner practitioner = (Practitioner) entry.getResource();
				practitionerDao.create(practitioner, requestDetails);
			}
		}

		// /**
		// * Map<remoteId,newLocalId>
		// */
		// Map<String, String> patients = new HashMap<>(bundle.getEntry().size() - 1);
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof Patient) {
				Patient patient = (Patient) entry.getResource();
				DaoMethodOutcome daoMethodOutcome = patientDao.create(patient, requestDetails);
				// String localId = daoMethodOutcome.getId().getIdPart(); //TODO check
				// patients.put(patient.getIdElement().getIdPart(), localId);
			}
		}

		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof Immunization) {
				Immunization immunization = (Immunization) entry.getResource();
				/**
				 * if mrn specified, references are solved within interceptor
				 */
				immunizationDao.create(immunization, requestDetails);
			}
		}

		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof ImmunizationRecommendation) {
				ImmunizationRecommendation immunizationRecommendation = (ImmunizationRecommendation) entry
					.getResource();
				immunizationRecommendationDao.create(immunizationRecommendation, requestDetails);
			}
		}
	}
}
