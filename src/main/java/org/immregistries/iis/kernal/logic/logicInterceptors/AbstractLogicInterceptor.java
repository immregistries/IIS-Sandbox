package org.immregistries.iis.kernal.logic.logicInterceptors;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.logic.ack.IisReportableSeverity;
import org.immregistries.iis.kernal.mapping.Interfaces.IisFhirMapperMasterReported;
import org.immregistries.iis.kernal.model.AbstractMappedObject;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractLogicInterceptor {
	Logger logger = LoggerFactory.getLogger(AbstractLogicInterceptor.class);

	public static final String IIS_REPORTABLE_LIST = "iisReportableList";

	public static List<IisReportable> iisReportableList(RequestDetails requestDetails) {
		if (requestDetails.getAttribute(IIS_REPORTABLE_LIST) == null) {
			requestDetails.setAttribute(IIS_REPORTABLE_LIST, new ArrayList<>(20));
		}
		return (List<IisReportable>) requestDetails.getAttribute(IIS_REPORTABLE_LIST);
	}

	public static @NotNull IisReportable err5IisReportable(String number, String text, List<@NotNull Hl7Location> hl7LocationList) {
		IisReportable iisReportable = new IisReportable();
		iisReportable.setHl7LocationList(hl7LocationList);
		CodedWithExceptions applicationErrorCode = new CodedWithExceptions("ERR-5");
		applicationErrorCode.setIdentifier(number);
		applicationErrorCode.setText(text);
		iisReportable.setApplicationErrorCode(applicationErrorCode);
		iisReportable.setHl7ErrorCode(applicationErrorCode);
		iisReportable.setSeverity(IisReportableSeverity.WARN);
		return iisReportable;
	}

	protected boolean testMapping(IisFhirMapperMasterReported mapper, AbstractMappedObject abstractMappedObject) {
		IBaseResource resource = mapper.fhirResource(abstractMappedObject);
		AbstractMappedObject abstractMappedObject1 = mapper.localObjectReported(resource);
		logger.info("original {}", abstractMappedObject);
		logger.info("mapped {}", abstractMappedObject1);
		boolean res = abstractMappedObject.toString().equals(abstractMappedObject1.toString());
		logger.info("comparison {} {}", res, abstractMappedObject.toString().compareTo(abstractMappedObject1.toString()));
		return res;
	}


}
