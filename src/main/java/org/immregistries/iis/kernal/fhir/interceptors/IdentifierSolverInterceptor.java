package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.mdm.dao.MdmLinkDaoSvc;
import ca.uhn.fhir.jpa.mdm.svc.MdmLinkSvcImpl;
import ca.uhn.fhir.jpa.mdm.svc.MdmResourceDaoSvc;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.interceptor.Interceptor;

import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED;
import static org.immregistries.iis.kernal.repository.FhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.repository.FhirRequester.GOLDEN_SYSTEM_TAG;

@Interceptor
@Conditional(OnR5Condition.class)
@Service
public class IdentifierSolverInterceptor {

	Logger logger = LoggerFactory.getLogger(IdentifierSolverInterceptor.class);

	@Autowired
	MdmLinkDaoSvc mdmLinkDaoSvc;
	@Autowired
	MdmResourceDaoSvc mdmResourceDaoSvc;
	@Autowired
	MdmLinkSvcImpl mdmLinkSvc;

	@Autowired
	IFhirResourceDao<Patient> patientDao;

	/**
	 * TODO determine hook
	 * if immunization solve patient, observation, subject
	 * <p>
	 * Point to Golden resource or to same source normal resource ?
	 */

	@Hook(SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void handle(RequestDetails requestDetails, ServletRequestDetails servletRequestDetails, RestOperationTypeEnum restOperationTypeEnum) {
		try {
			Immunization immunization = (Immunization) requestDetails.getResource();
			if (immunization == null || immunization.getPatient().getIdentifier() == null) {
				return;
			}
			Identifier identifier = immunization.getPatient().getIdentifier();
			/**
			 * searching for patient golden record
			 */
			SearchParameterMap searchParameterMap = new SearchParameterMap()
				.add("identifier", new TokenParam()
					.setSystem(identifier.getSystem())
					.setValue(identifier.getValue()))
//				.add("_tag", new TokenParam()
//					.setSystem(GOLDEN_SYSTEM_TAG)
//					.setValue(GOLDEN_RECORD))
				;
			// TODO get golden record, or merge and add identifiers to golden record

			IBundleProvider bundleProvider = patientDao.search(searchParameterMap, requestDetails);
			if (bundleProvider.isEmpty()) {
				// TODO throw exception or set flavor
				logger.info("No match found for identifier {} {}", identifier.getSystem(), identifier.getValue());
			} else {
				String id = bundleProvider.getAllResourceIds().get(0);
				logger.info("Identifier reference solved {} to {}", identifier, id);
				immunization.setPatient(new Reference("Patient/" + new IdType(id).getIdPart()));
			}
		} catch (ClassCastException classCastException) {

		}

	}

	public void solveIdentifierReference(RequestDetails requestDetails, IBaseResource resource) {
		Immunization immunization = (Immunization) resource;
		if (immunization.getPatient().getIdentifier() == null) {
			return;
		}
		Identifier identifier = immunization.getPatient().getIdentifier();
//		IGenericClient client = repositoryClientFactory.newGenericClient(theRequestDetails);

		/**
		 * searching for patient golden record
		 */
		SearchParameterMap searchParameterMap = new SearchParameterMap()
			.add("_tag", new TokenParam()
				.setSystem(GOLDEN_SYSTEM_TAG)
				.setValue(GOLDEN_RECORD))
			.add("identifier", new TokenParam()
				.setSystem(identifier.getSystem())
				.setValue(identifier.getValue()));

		IBundleProvider bundleProvider = patientDao.search(new SearchParameterMap(), requestDetails);
		if (bundleProvider.isEmpty()) {
			// TODO throw exception or set flavor
			logger.info("No match found for identifier {}", identifier);
		} else {
			String id = bundleProvider.getAllResourceIds().get(0);
			logger.info("Identifier reference solved {} to {}", identifier, id);

			immunization.setPatient(new Reference(id));
		}


//		Bundle bundle = (Bundle) client.search().byUrl(
//			"/Patient?_tag=" + GOLDEN_SYSTEM_TAG + "|" + GOLDEN_RECORD
//				+ "&identifier=" + immunization.getPatient().getIdentifier().getSystem()
//				+ "|" + immunization.getPatient().getIdentifier().getValue()
//		);
//		if (bundle.hasEntry()) {
//
//		}

	}


}
