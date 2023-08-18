package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hibernate.Session;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController("/$create")
@Conditional(OnR5Condition.class)
public class MacroEndpointController {
	@Autowired
	private FhirContext fhirContext;
	@Autowired
	IFhirResourceDao<Patient> patientDao;
	@Autowired
	IFhirResourceDao<Immunization> immunizationDao;
	@Autowired
	IFhirResourceDao<ImmunizationRecommendation> immunizationRecommendationDao;
	@Autowired
	IFhirResourceDao<Practitioner> practitionerDao;

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {

		Bundle facilityBundle = fhirContext.newJsonParser().parseResource(Bundle.class,req.getReader());
		ServletRequestDetails requestDetails;
		OrgAccess orgAccess = ServletHelper.getOrgAccess();
		Session dataSession = PopServlet.getDataSession();
		OrgMaster orgMaster = null;
		/**
		 * one and only one organization must be specified in bundle
		 */
		try {
			for (Bundle.BundleEntryComponent entry : facilityBundle.getEntry()) {
				if (entry.getResource() instanceof Organization) {
					if (orgMaster != null) {
						throw new InvalidRequestException("More than one organisation present");
					}
					orgMaster = ServletHelper.authenticateOrgMaster(orgAccess, ((Organization) entry.getResource()).getName(), dataSession);
				}
			}
		} finally {
			dataSession.close();
		}

		if (orgMaster == null) {
			throw new InvalidRequestException("No organisation information specified");
		} else  {
			requestDetails = new ServletRequestDetails();
			requestDetails.setTenantId(orgMaster.getOrganizationName());
			fillFacility(requestDetails,facilityBundle);
		}
	}

	@GetMapping
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}


	public ResponseEntity fillFacility(ServletRequestDetails requestDetails, Bundle bundle) {
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof Practitioner) {
				Practitioner practitioner = (Practitioner) entry.getResource();
				practitionerDao.create(practitioner,requestDetails);
			}
		}

//		/**
//		 * Map<remoteId,newLocalId>
//		 */
//		Map<String, String> patients = new HashMap<>(bundle.getEntry().size() - 1);
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof Patient) {
				Patient patient = (Patient) entry.getResource();
				DaoMethodOutcome daoMethodOutcome = patientDao.create(patient, requestDetails);
//				String localId = daoMethodOutcome.getId().getIdPart(); //TODO check
//				patients.put(patient.getIdElement().getIdPart(), localId);
			}
		}

		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof Immunization) {
				Immunization immunization = (Immunization) entry.getResource();
				/**
				 * if mrn specified, references are solved within interceptor
				 */
				immunizationDao.create(immunization,requestDetails);
			}
		}

		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof ImmunizationRecommendation) {
				ImmunizationRecommendation immunizationRecommendation = (ImmunizationRecommendation) entry.getResource();
				immunizationRecommendationDao.create(immunizationRecommendation,requestDetails);
			}
		}
		return ResponseEntity.ok().build();
	}
}
