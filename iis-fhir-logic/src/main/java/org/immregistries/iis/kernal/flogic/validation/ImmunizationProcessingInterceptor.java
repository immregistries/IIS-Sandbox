package org.immregistries.iis.kernal.flogic.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.logic.validation.ImmunizationValidator;
import org.immregistries.iis.kernal.logic.validation.ProcessingException;
import org.immregistries.iis.kernal.mapping.mappers.resources.IisResourceMasterReportedMapper;
import org.immregistries.iis.kernal.mapping.mappers.resources.ImmunizationMapper;
import org.immregistries.iis.kernal.model.IisMappedToFhirResource;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.immregistries.iis.kernal.model.ack.IisReportable;
import org.immregistries.iis.kernal.model.enums.ProcessingFlavor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED;

@Interceptor
@Service
public class ImmunizationProcessingInterceptor extends IisLogicInterceptor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ImmunizationMapper immunizationMapper;
	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private ImmunizationValidator immunizationValidator;

	@Hook(value = SERVER_INCOMING_REQUEST_PRE_HANDLED, order = 2001)
	public void handle(RequestDetails requestDetails) throws InvalidRequestException, ProcessingException {
		Set<ProcessingFlavor> processingFlavorSet = ProcessingFlavor.getProcessingStyle(requestDetails.getTenantId());
		List<IisReportable> iisReportableList = iisReportableList(requestDetails);
		if (requestDetails.getResource() == null || requestDetails.getRestOperationType() == null) {
			return;
		}
		IAnyResource result = (IAnyResource) requestDetails.getResource();
		if (requestDetails.getRestOperationType().equals(RestOperationTypeEnum.UPDATE) || requestDetails.getRestOperationType().equals(RestOperationTypeEnum.CREATE)) {
			if (requestDetails.getResource() instanceof org.hl7.fhir.r4.model.Immunization || requestDetails.getResource() instanceof org.hl7.fhir.r5.model.Immunization) {
				testMappingFhir((IisResourceMasterReportedMapper<IisMappedToFhirResource, IisMappedToFhirResource, IisMappedToFhirResource, IAnyResource>) immunizationMapper, (IAnyResource) requestDetails.getResource(), fhirContext.newJsonParser());
				VaccinationReported vaccinationReported = immunizationMapper.localObjectReported((IAnyResource) requestDetails.getResource());
				vaccinationReported = immunizationValidator.processAndValidateVaccinationReported(vaccinationReported, iisReportableList, processingFlavorSet, -1, -1, -1, "");
				result = immunizationMapper.fhirObject(vaccinationReported);
			}
		}
		requestDetails.setResource(result);
		requestDetails.setAttribute(IIS_REPORTABLE_LIST, iisReportableList);
	}



}
