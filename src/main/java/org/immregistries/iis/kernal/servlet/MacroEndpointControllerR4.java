package org.immregistries.iis.kernal.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hibernate.Session;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.fhir.security.ServletHelper;
import org.immregistries.iis.kernal.model.Tenant;
import org.immregistries.iis.kernal.model.UserAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController()
@RequestMapping("/$create")
@Conditional(OnR4Condition.class)
public class MacroEndpointControllerR4 {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
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

	//	@GetMapping("/StructureDefinition")
//	public ResponseEntity<String> getStructureDefinition() {
//		fhirContext.
//		StructureDefinition structureDefinition = ClasspathUtil.loadResource(fhirContext,StructureDefinition.class, "org/hl7/fhir/r4/model/patient.json");
//		return ResponseEntity.ok().body(structureDefinition.getUrl());
//	}
	@GetMapping("/StructureDefinition")
	public ResponseEntity<String> getStructureDefinition2() {
		RuntimeResourceDefinition runtimeResourceDefinition = fhirContext.getResourceDefinition("Patient");
		logger.info("profile test{}", runtimeResourceDefinition.getResourceProfile("localhost:8080/fhir/b"));
		logger.info("profile test{}", runtimeResourceDefinition);

		return ResponseEntity.ok().body(fhirContext.newJsonParser().encodeResourceToString(runtimeResourceDefinition.toProfile("test")));
	}

	@PostMapping
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {

		Bundle facilityBundle = fhirContext.newJsonParser().parseResource(Bundle.class, req.getReader());
		ServletRequestDetails requestDetails;
		UserAccess userAccess = ServletHelper.getUserAccess();
		Session dataSession = PopServlet.getDataSession();
		Tenant tenant = null;
		/**
		 * one and only one organization must be specified in bundle
		 * TODO deal with organization/Facility as managing organization
		 */
		try {
			for (Bundle.BundleEntryComponent entry : facilityBundle.getEntry()) {
				if (entry.getResource() instanceof Organization) {
					if (tenant != null) {
						throw new InvalidRequestException("More than one organization present");
					}
					tenant = ServletHelper.authenticateTenant(userAccess, ((Organization) entry.getResource()).getName(), dataSession);
				}
			}
		} finally {
			dataSession.close();
		}

		if (tenant == null) {
			throw new InvalidRequestException("No organization information specified");
		} else {
			requestDetails = new ServletRequestDetails();
			requestDetails.setTenantId(tenant.getOrganizationName());
			fillFacility(requestDetails, facilityBundle);
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
				practitionerDao.create(practitioner, requestDetails);
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
				immunizationDao.create(immunization, requestDetails);
			}
		}

		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof ImmunizationRecommendation) {
				ImmunizationRecommendation immunizationRecommendation = (ImmunizationRecommendation) entry.getResource();
				immunizationRecommendationDao.create(immunizationRecommendation, requestDetails);
			}
		}
		return ResponseEntity.ok().build();
	}
}
