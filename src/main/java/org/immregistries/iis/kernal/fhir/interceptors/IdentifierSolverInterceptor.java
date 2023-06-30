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
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.fhir.annotations.OnR5Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.interceptor.Interceptor;

import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED;
import static org.immregistries.iis.kernal.mapping.Interfaces.PatientMapper.MRN_SYSTEM;
import static org.immregistries.iis.kernal.InternalClient.FhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.InternalClient.FhirRequester.GOLDEN_SYSTEM_TAG;

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
	 * Resolves business identifier resources to actual resources references id
	 * Currently only Immunization supported
	 * TODO support Observation and other
	 * TODO add flavours
	 */
	@Hook(SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void handle(RequestDetails requestDetails,
							 ServletRequestDetails servletRequestDetails,
							 RestOperationTypeEnum restOperationTypeEnum)
		throws InvalidRequestException {

		try {
			Immunization immunization = (Immunization) requestDetails.getResource();
			if (immunization == null
				|| immunization.getPatient().getIdentifier() == null
				|| immunization.getPatient().getIdentifier().getValue() == null
				|| immunization.getPatient().getIdentifier().getSystem() == null
			) {
				return;
			}
			Identifier identifier = immunization.getPatient().getIdentifier();
			String id = null;
			/**
			 * searching for matching patient golden record first
			 */
			SearchParameterMap goldenSearchParameterMap = new SearchParameterMap()
				.add("identifier", new TokenParam()
					.setSystem(identifier.getSystem())
					.setValue(identifier.getValue()))
				.add("_tag", new TokenParam()
					.setSystem(GOLDEN_SYSTEM_TAG)
					.setValue(GOLDEN_RECORD));
			// TODO get golden record, or merge and add identifiers to golden record
			IBundleProvider goldenBundleProvider = patientDao.search(goldenSearchParameterMap, requestDetails);
			if (!goldenBundleProvider.isEmpty()) {
				id = goldenBundleProvider.getAllResourceIds().get(0);
			} else {
				/**
				 * If no golden record matched, regular records are checked
				 */
				// TODO set flavor
				SearchParameterMap searchParameterMap = new SearchParameterMap().add("identifier", new TokenParam()
					.setSystem(identifier.getSystem()).setValue(identifier.getValue()));
				IBundleProvider bundleProvider = patientDao.search(searchParameterMap, requestDetails);
				if (!bundleProvider.isEmpty()) {
					id = bundleProvider.getAllResourceIds().get(0);
				}
			}
			if (id != null) {
				logger.info("Identifier reference solved {}|{} to {}", identifier.getSystem(), identifier.getValue(), id);
				immunization.setPatient(new Reference("Patient/" + new IdType(id).getIdPart()));
			} else {
				// TODO set flavor
				if (identifier.getSystem().equals(MRN_SYSTEM)) {
					throw new InvalidRequestException("There is no matching patient for MRN " + identifier.getValue());
				} else {
					throw new InvalidRequestException("There is no matching patient for " + identifier.getSystem() + " " + identifier.getValue());
				}
			}
		} catch (ClassCastException classCastException) {}
	}
}
