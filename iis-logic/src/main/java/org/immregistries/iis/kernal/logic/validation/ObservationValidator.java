package org.immregistries.iis.kernal.logic.validation;

import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.logic.hl7v2.ack.IisReportableUtilService;
import org.immregistries.iis.kernal.mapping.mappers.resources.ObservationMapper;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.model.ack.IisReportable;
import org.immregistries.iis.kernal.model.ack.IisReportableSeverity;
import org.immregistries.iis.kernal.model.enums.ProcessingFlavor;
import org.immregistries.iis.kernal.services.CodeMapManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Set;


public class ObservationValidator extends IisValidator {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ObservationMapper observationMapper;
	@Autowired
	private CodeMapManagerService codeMapManagerService;
	@Autowired
	private IisReportableUtilService iisReportableUtilService;


	public ObservationReported processAndValidateObservationReported(ObservationReported observationReported, List<IisReportable> iisReportableList, Set<ProcessingFlavor> processingFlavorSet, int obxCount, Date patientBirthDate) throws ProcessingException {
		testMapping(observationMapper, observationReported);
		if ("30945-0".equals(observationReported.getIdentifierCode())) // contraindication!
		{
			CodeMap codeMap = codeMapManagerService.getCodeMap();
			Code contraCode = codeMap.getCodeForCodeset(CodesetType.CONTRAINDICATION_OR_PRECAUTION, observationReported.getValueCode());
			if (contraCode == null) {
				ProcessingException pe = new ProcessingException("Unrecognized contraindication or precaution", "OBX", obxCount, 5, IisReportableSeverity.WARN);
				iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
			}
			if (observationReported.getObservationDate() != null) {
				Date today = new Date();
				if (observationReported.getObservationDate().after(today)) {
					ProcessingException pe = new ProcessingException("Contraindication or precaution observed in the future", "OBX", obxCount, 5, IisReportableSeverity.WARN);
					iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
				}
				if (patientBirthDate != null && observationReported.getObservationDate().before(patientBirthDate)) {
					ProcessingException pe = new ProcessingException("Contraindication or precaution observed before patient was born", "OBX", obxCount, 14, IisReportableSeverity.WARN);
					iisReportableList.add(iisReportableUtilService.fromProcessingException(pe));
				}
			}
		}
		return observationReported;

	}
}
