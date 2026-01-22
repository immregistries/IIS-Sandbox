package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.interceptor.Interceptor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.mapping.mappers.fields.BusinessIdentifierMapper;
import org.immregistries.iis.kernal.mapping.mappers.fields.ModelReferenceMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
import org.immregistries.iis.kernal.model.ModelReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import static org.immregistries.iis.kernal.mapping.mappers.resources.PatientMapper.MRN_SYSTEM;

@Interceptor
@Conditional(OnR5Condition.class)
@Service
public class IdentifierSolverInterceptorR5 extends IdentifierSolverInterceptor<Patient, Immunization, Group, Observation> {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private IFhirResourceDao<Patient> patientDao;

	@Autowired
	private ImmunizationMapper<Immunization> immunizationMapper;
	@Autowired
	private BusinessIdentifierMapper<Identifier> businessIdentifierMapper;
	@Autowired
	private ModelReferenceMapper<Reference> modelReferenceMapper;


	@Override
	public void handleImmunization(RequestDetails requestDetails, Immunization immunization) {
		if (immunization == null
			|| immunization.getPatient().getIdentifier() == null
			|| immunization.getPatient().getIdentifier().getValue() == null
			|| immunization.getPatient().getIdentifier().getSystem() == null
		) {
			return;
		}
		/*
		 * Linking record to golden
		 */
		ModelReference modelReference = immunizationMapper.extractPatientReference(immunization);
		Identifier identifier = immunization.getPatient().getIdentifier();
		String id = solvePatientIdentifier(requestDetails, identifier);

		if (id != null) {
			logger.info("Identifier reference solved {}|{} to {}", identifier.getSystem(), identifier.getValue(), id);
			immunization.setPatient(new Reference("Patient/" + new IdType(id).getIdPart()));
			requestDetails.setResource(immunization);
		} else {
			// TODO set flavor
			if (identifier.getSystem().equals(MRN_SYSTEM)) {
				throw new InvalidRequestException("There is no matching patient for MRN " + identifier.getValue());
			} else {
				throw new InvalidRequestException("There is no matching patient for " + identifier.getSystem() + " " + identifier.getValue());
			}
		}
	}

	@Override
	public void handleObservation(RequestDetails requestDetails, Observation observation) {
		if (observation == null) {
			return;
		}
		/*
		 * Linking record to golden
		 */
		Identifier identifier = observation.getSubject().getIdentifier();
		if (identifier == null || identifier.getValue() == null || identifier.getSystem() == null) {
			return;
		}
		String id = solvePatientIdentifier(requestDetails, identifier);

		if (id != null) {
			logger.info("Identifier reference solved {}|{} to {}", identifier.getSystem(), identifier.getValue(), id);
			observation.setSubject(new Reference("Patient/" + new IdType(id).getIdPart()));
			requestDetails.setResource(observation);
		} else {
			// TODO set flavor
			if (identifier.getSystem().equals(MRN_SYSTEM)) {
				throw new InvalidRequestException("There is no matching patient for MRN " + identifier.getValue());
			} else {
				throw new InvalidRequestException("There is no matching patient for " + identifier.getSystem() + " " + identifier.getValue());
			}
		}
	}

	@Override
	public void handleGroup(RequestDetails requestDetails, Group group) {
		logger.info("Identifier reference interception for Group");
		for (Group.GroupMemberComponent memberComponent : group.getMember()) {
			if (!memberComponent.getEntity().hasIdentifier()) {
				break;
			}
			Identifier identifier = memberComponent.getEntity().getIdentifier();
			String id = solvePatientIdentifier(requestDetails, identifier);
			if (StringUtils.isNotBlank(id)) {
				logger.info("Identifier reference solved {}|{} to {} for Group", identifier.getSystem(), identifier.getValue(), id);
				memberComponent.setEntity(new Reference("Patient/" + new IdType(id).getIdPart()).setIdentifier(identifier));
			}
		}
		requestDetails.setResource(group);
	}


	public String solvePatientIdentifier(RequestDetails requestDetails, Identifier identifier) {
		return solvePatientIdentifier(requestDetails, businessIdentifierMapper.localObject(identifier));
	}
}
