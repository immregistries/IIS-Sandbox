package org.immregistries.iis.kernal.logic.logicInterceptors;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.logic.ack.IisReportable;
import org.immregistries.iis.kernal.logic.ack.IisReportableSeverity;
import org.immregistries.iis.kernal.mapping.interfaces.IisFhirMapperMasterReported;
import org.immregistries.iis.kernal.model.AbstractMappedObject;
import org.immregistries.mqe.hl7util.model.CodedWithExceptions;
import org.immregistries.mqe.hl7util.model.Hl7Location;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractLogicInterceptor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

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

	protected boolean testMapping(IisFhirMapperMasterReported<AbstractMappedObject, AbstractMappedObject, IBaseResource> mapper, AbstractMappedObject abstractMappedObject) {
		IBaseResource resource = mapper.fhirResource(abstractMappedObject);
		AbstractMappedObject abstractMappedObject1 = mapper.localObjectReported(resource);
		if (abstractMappedObject1 == null) {
			abstractMappedObject1 = mapper.localObject(resource);
		}
		boolean res = abstractMappedObject.toString().equals(abstractMappedObject1.toString());
		if (!res) {
			logger.info("Object Mapping check failed\n{}\n\n{}\n", abstractMappedObject, abstractMappedObject1);
		}
		return res;
	}

	protected boolean testMappingFhir(IisFhirMapperMasterReported<AbstractMappedObject, AbstractMappedObject, IBaseResource> mapper, IBaseResource resource, IParser parser) {
		AbstractMappedObject abstractMappedObject1 = mapper.localObjectReported(resource);
		IBaseResource resource1 = mapper.fhirResource(abstractMappedObject1);
		String s1 = parser.encodeResourceToString(resource);
		String s2 = parser.encodeResourceToString(resource1);
		boolean res = s1.equals(s2);
//		if (!res) {
//			logger.info("FHIR Mapping check failed {}\n\n{}", s1, s2);
//		}
		return res;
	}

}
