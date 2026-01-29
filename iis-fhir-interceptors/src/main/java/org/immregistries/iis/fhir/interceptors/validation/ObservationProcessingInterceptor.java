package org.immregistries.iis.fhir.interceptors.validation;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.immregistries.iis.kernal.logic.validation.ObservationValidator;
import org.immregistries.iis.kernal.logic.validation.ProcessingException;
import org.immregistries.iis.kernal.mapping.mappers.resources.ObservationMapper;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.model.ack.IisReportable;
import org.immregistries.iis.kernal.model.enums.ProcessingFlavor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED;

@Interceptor
@Service
public class ObservationProcessingInterceptor extends IisLogicInterceptor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String OBX_COUNT = "ObxCount";
	public static final String PATIENT_BIRTH_DATE = "patientBirthDate";
	@Autowired
	private ObservationMapper observationMapper;
	@Autowired
	private ObservationValidator observationValidator;

	@Hook(value = SERVER_INCOMING_REQUEST_PRE_HANDLED, order = 2001)
	public void handle(RequestDetails requestDetails) throws InvalidRequestException, ProcessingException {
		Set<ProcessingFlavor> processingFlavorSet = ProcessingFlavor.getProcessingStyle(requestDetails.getTenantId());
		List<IisReportable> iisReportableList = iisReportableList(requestDetails);
		if (requestDetails.getResource() == null || requestDetails.getRestOperationType() == null) {
			return;
		}
		int obxCount = 0;
		if (requestDetails.getAttribute(OBX_COUNT) != null) { // If in a v2 context
			obxCount = (int) requestDetails.getAttribute(OBX_COUNT);
		}
		Date patientBirthDate = null;
		if (requestDetails.getAttribute(PATIENT_BIRTH_DATE) != null) { // If in a v2 context
			patientBirthDate = (Date) requestDetails.getAttribute(PATIENT_BIRTH_DATE);
		}
		IAnyResource result = (IAnyResource) requestDetails.getResource();
		if (requestDetails.getRestOperationType().equals(RestOperationTypeEnum.CREATE) || requestDetails.getRestOperationType().equals(RestOperationTypeEnum.CREATE)) {
			if (requestDetails.getResource() instanceof org.hl7.fhir.r4.model.Observation || requestDetails.getResource() instanceof org.hl7.fhir.r5.model.Observation) {
				ObservationReported observationReported = observationValidator.processAndValidateObservationReported(observationMapper.localObjectReported(result), iisReportableList, processingFlavorSet, obxCount, patientBirthDate);
				result = observationMapper.fhirObject(observationReported);
			}
		}
		requestDetails.setResource(result);
		requestDetails.setAttribute(IIS_REPORTABLE_LIST, iisReportableList);
	}


}
