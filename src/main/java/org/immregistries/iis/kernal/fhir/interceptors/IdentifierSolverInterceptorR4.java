package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.immregistries.iis.kernal.fhir.annotations.OnR4Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.interceptor.Interceptor;

import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED;
import static org.immregistries.iis.kernal.mapping.interfaces.PatientMapper.MRN_SYSTEM;
import static org.immregistries.iis.kernal.mapping.internalClient.FhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.mapping.internalClient.FhirRequester.GOLDEN_SYSTEM_TAG;

@Interceptor
@Conditional(OnR4Condition.class)
@Service
public class IdentifierSolverInterceptorR4 implements IIdentifierSolverInterceptor<Identifier, Immunization, Group> {

	Logger logger = LoggerFactory.getLogger(IdentifierSolverInterceptorR4.class);

	@Autowired
	private IFhirResourceDao<Patient> patientDao;


	@Override
	@Hook(SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void handle(RequestDetails requestDetails) throws InvalidRequestException {
		if (requestDetails.getResource() instanceof Immunization) {
			handleImmunization(requestDetails, (Immunization) requestDetails.getResource());
		} else if (requestDetails.getResource() instanceof Group) {
			handleGroup(requestDetails, (Group) requestDetails.getResource());
		}
	}

	@Override
	public void handleImmunization(RequestDetails requestDetails, Immunization immunization) {

		if (immunization == null
			|| immunization.getPatient().getIdentifier() == null
			|| immunization.getPatient().getIdentifier().getValue() == null
			|| immunization.getPatient().getIdentifier().getSystem() == null
		) {
			return;
		}
		/**
		 * Linking record to golden
		 */
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
			} else {
//				// TODO set flavor
//				if (identifier.getSystem().equals(MRN_SYSTEM)) {
//					throw new InvalidRequestException("There is no matching patient for MRN " + identifier.getValue());
//				} else {
//					throw new InvalidRequestException("There is no matching patient for " + identifier.getSystem() + " " + identifier.getValue());
//				}
			}
		}
		requestDetails.setResource(group);
	}


	@Override
	public String solvePatientIdentifier(RequestDetails requestDetails, Identifier identifier) {
		String id = null;
		/**
		 * searching for matching patient golden record first
		 */
		SearchParameterMap goldenSearchParameterMap = new SearchParameterMap()
			.add("_tag", new TokenParam()
				.setSystem(GOLDEN_SYSTEM_TAG)
				.setValue(GOLDEN_RECORD));
		if (StringUtils.isNotBlank(identifier.getSystem())) {
			goldenSearchParameterMap.add(Patient.SP_IDENTIFIER, new TokenParam()
				.setSystem(identifier.getSystem())
				.setValue(identifier.getValue()));
		} else {
			goldenSearchParameterMap.add(Patient.SP_IDENTIFIER, new TokenParam()
				.setValue(identifier.getValue()));
		}

		// TODO get golden record, or merge and add identifiers to golden record
		IBundleProvider goldenBundleProvider = patientDao.search(goldenSearchParameterMap, requestDetails);
		if (!goldenBundleProvider.isEmpty()) {
			id = goldenBundleProvider.getAllResourceIds().get(0);
		} else {
			/**
			 * If no golden record matched, regular records are checked
			 */
			// TODO set flavor
			SearchParameterMap searchParameterMap = new SearchParameterMap();
			if (StringUtils.isNotBlank(identifier.getSystem())) {
				searchParameterMap.add(Patient.SP_IDENTIFIER, new TokenParam()
					.setSystem(identifier.getSystem())
					.setValue(identifier.getValue()));
			} else {
				searchParameterMap.add(Patient.SP_IDENTIFIER, new TokenParam()
					.setValue(identifier.getValue()));
			}

			IBundleProvider bundleProvider = patientDao.search(searchParameterMap, requestDetails);
			if (!bundleProvider.isEmpty()) {
				id = bundleProvider.getAllResourceIds().get(0);
			}
		}
		return id;
	}
}
