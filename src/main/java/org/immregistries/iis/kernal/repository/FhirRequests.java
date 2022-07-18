package org.immregistries.iis.kernal.repository;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Immunization;
import org.hl7.fhir.r5.model.Observation;
import org.hl7.fhir.r5.model.Patient;
import org.immregistries.iis.kernal.mapping.ImmunizationHandler;
import org.immregistries.iis.kernal.mapping.ObservationMapper;
import org.immregistries.iis.kernal.mapping.PatientHandler;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FhirRequests {
	@Autowired
	IFhirResourceDao<Patient> patientDao;
	@Autowired
	RepositoryClientFactory repositoryClientFactory;

	Logger logger = LoggerFactory.getLogger(FhirRequests.class);

	public PatientReported searchPatientReported(IGenericClient fhirClient, ICriterion... where) {
		PatientReported patientReported = null;
		Bundle bundle = search(Patient.class,fhirClient, where);
		if (bundle.hasEntry()) {
			Patient patient = (Patient) bundle.getEntryFirstRep().getResource();
			patientReported = PatientHandler.getReported(patient);
		}
		return patientReported;
	}
	public VaccinationReported searchVaccinationReported(IGenericClient fhirClient, ICriterion... where) {
		VaccinationReported vaccinationReported = null;
		Bundle bundle = search(Immunization.class,fhirClient, where);
		if (bundle.hasEntry()) {
			vaccinationReported = ImmunizationHandler.getReported((Immunization) bundle.getEntryFirstRep().getResource());
		}
		return vaccinationReported;
	}
	public ObservationReported searchObservationReported(IGenericClient fhirClient, ICriterion... where) {
		ObservationReported observationReported = null;
		Bundle bundle = search(Observation.class,fhirClient, where);
		if (bundle.hasEntry()) {
			observationReported = ObservationMapper.getReported((Observation) bundle.getEntryFirstRep().getResource());
		}
		return observationReported;
	}

	private Bundle search(
		Class<? extends org.hl7.fhir.instance.model.api.IBaseResource> aClass,
		IGenericClient fhirClient,
		ICriterion... where) {
		try {
			IQuery<Bundle> query = fhirClient.search().forResource(aClass).returnBundle(Bundle.class);
			int size = where.length;
			if (size > 0) {
				query = query.where(where[0]);
			}
			int i = 1;
			while (i++ < size) {
				query = query.and(where[i]);
			}
			return  query.execute();
		} catch (ResourceNotFoundException e) {
			return  new Bundle();
		}
	}

}
