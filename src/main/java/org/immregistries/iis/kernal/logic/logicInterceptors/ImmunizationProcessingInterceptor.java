package org.immregistries.iis.kernal.logic.logicInterceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.logic.ProcessingException;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.mapping.Interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED;

@Interceptor
@Service
public class ImmunizationProcessingInterceptor extends AbstractLogicInterceptor {
	Logger logger = LoggerFactory.getLogger(ImmunizationProcessingInterceptor.class);

	@Autowired
	private ImmunizationMapper immunizationMapper;

	@Hook(value = SERVER_INCOMING_REQUEST_PRE_HANDLED, order = 2001)
	public void handle(RequestDetails requestDetails) throws InvalidRequestException, ProcessingException {
		Set<ProcessingFlavor> processingFlavorSet = ProcessingFlavor.getProcessingStyle(requestDetails.getTenantId());
		List<IisReportable> iisReportableList = iisReportableList(requestDetails);
		if (requestDetails.getResource() == null || requestDetails.getOperation() == null) {
			return;
		}
		IBaseResource result = requestDetails.getResource();
		if ((requestDetails.getOperation().equals("create") || requestDetails.getOperation().equals("update"))
			&& (requestDetails.getResource() instanceof org.hl7.fhir.r4.model.Immunization || requestDetails.getResource() instanceof org.hl7.fhir.r5.model.Immunization)) {
			VaccinationReported vaccinationReported = processAndValidateVaccinationReported(immunizationMapper.getReported(requestDetails.getResource()), iisReportableList, processingFlavorSet);
			result = immunizationMapper.getFhirResource(vaccinationReported);
		}
		requestDetails.setResource(result);
		requestDetails.setAttribute(IIS_REPORTABLE_LIST, iisReportableList);
	}

	private VaccinationReported processAndValidateVaccinationReported(VaccinationReported vaccinationReported, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet) throws ProcessingException {
		testMapping(immunizationMapper, vaccinationReported);
		return vaccinationReported;
	}


}
