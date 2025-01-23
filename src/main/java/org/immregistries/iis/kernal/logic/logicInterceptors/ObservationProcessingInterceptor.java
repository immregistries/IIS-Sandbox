package org.immregistries.iis.kernal.logic.logicInterceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.logic.ProcessingException;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.logic.ack.IisReportableSeverity;
import org.immregistries.iis.kernal.mapping.Interfaces.ObservationMapper;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.model.ProcessingFlavor;
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
public class ObservationProcessingInterceptor extends AbstractLogicInterceptor {
	Logger logger = LoggerFactory.getLogger(PatientProcessingInterceptor.class);

	public static final String OBX_COUNT = "ObxCount";
	public static final String PATIENT_BIRTH_DATE = "patientBirthDate";
	@Autowired
	private ObservationMapper observationMapper;

	@Hook(value = SERVER_INCOMING_REQUEST_PRE_HANDLED, order = 2001)
	public void handle(RequestDetails requestDetails) throws InvalidRequestException, ProcessingException {
		Set<ProcessingFlavor> processingFlavorSet = ProcessingFlavor.getProcessingStyle(requestDetails.getTenantId());
		List<IisReportable> iisReportableList = iisReportableList(requestDetails);
		if (requestDetails.getResource() == null || requestDetails.getOperation() == null) {
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
		IBaseResource result = requestDetails.getResource();
		if ((requestDetails.getOperation().equals("create") || requestDetails.getOperation().equals("update"))
			&& (requestDetails.getResource() instanceof org.hl7.fhir.r4.model.Observation || requestDetails.getResource() instanceof org.hl7.fhir.r5.model.Observation)) {
			ObservationReported observationReported = processAndValidateObservationReported(observationMapper.getReported(requestDetails.getResource()), iisReportableList, processingFlavorSet, obxCount, patientBirthDate);
			result = observationMapper.getFhirResource(observationReported);
		}
		requestDetails.setResource(result);
		requestDetails.setAttribute(IIS_REPORTABLE_LIST, iisReportableList);
	}

	private ObservationReported processAndValidateObservationReported(ObservationReported observationReported, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet, int obxCount, Date patientBirthDate) throws ProcessingException {
//		testMapping(observationMapper,observationReported);
		if (observationReported.getIdentifierCode().equals("30945-0")) // contraindication!
		{
			CodeMap codeMap = CodeMapManager.getCodeMap();
			Code contraCode = codeMap.getCodeForCodeset(CodesetType.CONTRAINDICATION_OR_PRECAUTION, observationReported.getValueCode());
			if (contraCode == null) {
				ProcessingException pe = new ProcessingException("Unrecognized contraindication or precaution", "OBX", obxCount, 5, IisReportableSeverity.WARN);
				iisReportableList.add(IisReportable.fromProcessingException(pe));
			}
			if (observationReported.getObservationDate() != null) {
				Date today = new Date();
				if (observationReported.getObservationDate().after(today)) {
					ProcessingException pe = new ProcessingException("Contraindication or precaution observed in the future", "OBX", obxCount, 5, IisReportableSeverity.WARN);
					iisReportableList.add(IisReportable.fromProcessingException(pe));
				}
				if (patientBirthDate != null && observationReported.getObservationDate().before(patientBirthDate)) {
					ProcessingException pe = new ProcessingException("Contraindication or precaution observed before patient was born", "OBX", obxCount, 14, IisReportableSeverity.WARN);
					iisReportableList.add(IisReportable.fromProcessingException(pe));
				} 
			}
		}
		return observationReported;

	}


}
